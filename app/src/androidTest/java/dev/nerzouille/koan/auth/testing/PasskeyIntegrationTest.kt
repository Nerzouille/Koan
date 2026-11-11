package dev.nerzouille.koan.auth.testing

import android.app.PendingIntent
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nerzouille.koan.KoanApp
import dev.nerzouille.koan.MainActivity
import dev.nerzouille.koan.engine.auth.WebAuthnController
import dev.nerzouille.koan.engine.auth.LegacyFidoResult
import dev.nerzouille.koan.engine.auth.CredentialUiHost
import com.google.android.gms.tasks.Tasks
import org.mozilla.gecko.util.WebAuthnUtils
import org.mozilla.geckoview.GeckoResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasskeyIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun credentialUiContextTracksTheHostWithoutRetainingDestroyedActivities() {
        var old: MainActivity? = null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                old = activity
                assertSame(activity, (CredentialUiHost.context(activity.applicationContext) as android.content.ContextWrapper).baseContext)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                CredentialUiHost.detach(requireNotNull(old))
                assertSame(activity, (CredentialUiHost.context(activity.applicationContext) as android.content.ContextWrapper).baseContext)
                old = null
            }
        }
        assertSame(instrumentation.targetContext.applicationContext,
            CredentialUiHost.context(instrumentation.targetContext.applicationContext))
    }

    @Test fun credentialOutcomeWaitsForResumeAndStillSettlesOnDestroy() {
        for (resume in listOf(false, true)) {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                val done = CompletableFuture<Boolean>()
                instrumentation.runOnMainSync {
                    CredentialUiHost.context(instrumentation.targetContext.applicationContext).mainExecutor.execute {
                        done.complete(true)
                    }
                }
                instrumentation.waitForIdleSync()
                assertFalse(done.isDone)
                scenario.moveToState(if (resume) androidx.lifecycle.Lifecycle.State.RESUMED else androidx.lifecycle.Lifecycle.State.DESTROYED)
                assertTrue(done.get(20, TimeUnit.SECONDS))
            }
        }
    }
    @Test fun pendingCallbackSurvivesActivityRecreation() {
        val registry = ControlledRegistry()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val done = CompletableFuture<Intent?>()
            scenario.onActivity { activity ->
                controller(activity, registry).startIntentSenderForResult(sender().intentSender) { done.complete(it) }
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                // The provider may return while the old registration is detached. Rebinding
                // must consume that queued result synchronously without losing its callback.
                registry.dispatchResult(registry.codes.single(), Activity.RESULT_OK, Intent().putExtra("fixture-result", "restored"))
                controller(activity, registry)
            }
            assertEquals("restored", done.get(20, TimeUnit.SECONDS)?.getStringExtra("fixture-result"))
        }
    }

    @Test fun lateActivityResultCannotCompleteNewRequest() {
        val registry = ControlledRegistry()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val controller = controller(activity, registry)
                var firstCalls = 0
                var retryCalls = 0
                controller.startIntentSenderForResult(sender().intentSender) { assertNull(it); firstCalls++ }
                val first = registry.codes.last()
                registry.dispatchResult(first, Activity.RESULT_CANCELED, Intent().putExtra("fixture-result", "discard me"))
                controller.startIntentSenderForResult(sender().intentSender) {
                    assertEquals("retry", it?.getStringExtra("fixture-result")); retryCalls++
                }
                val retry = registry.codes.last()
                assertNotEquals(first, retry)
                registry.dispatchResult(first, Activity.RESULT_OK, Intent().putExtra("fixture-result", "old"))
                assertEquals(0, retryCalls)
                registry.dispatchResult(retry, Activity.RESULT_OK, Intent().putExtra("fixture-result", "retry"))
                assertEquals(1, firstCalls)
                assertEquals(1, retryCalls)
            }
        }
    }

    @Test fun abandonedResultsAreDrainedAfterCallbackOwnerIsLost() {
        for (queuedBeforeRestore in listOf(false, true)) {
            val registry = ControlledRegistry()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                var savedIds = emptyList<String>()
                var rejected = 0
                scenario.onActivity { activity ->
                    val old = controller(activity, registry)
                    old.startIntentSenderForResult(sender().intentSender) { assertNull(it); rejected++ }
                    savedIds = old.pendingRequestIds()
                    // Simulate loss of the in-memory callback, while Android retains its result.
                    activity.viewModelStore.clear()
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val oldCode = registry.codes.single()
                    if (queuedBeforeRestore) registry.dispatchResult(oldCode, Activity.RESULT_OK, Intent().putExtra("fixture-result", "orphan"))
                    val restored = WebAuthnController(activity, (activity.application as KoanApp).components.engine,
                        {}, registry, savedIds + savedIds).also { it.start() }
                    if (queuedBeforeRestore) assertTrue(restored.pendingRequestIds().isEmpty())
                    var retryCalls = 0
                    restored.startIntentSenderForResult(sender().intentSender) {
                        assertEquals("retry", it?.getStringExtra("fixture-result")); retryCalls++
                    }
                    if (!queuedBeforeRestore) registry.dispatchResult(oldCode, Activity.RESULT_OK, Intent().putExtra("fixture-result", "orphan"))
                    assertEquals(0, retryCalls)
                    assertEquals(1, rejected)
                    assertEquals(1, restored.pendingRequestIds().size)
                    registry.dispatchResult(registry.codes.last(), Activity.RESULT_OK, Intent().putExtra("fixture-result", "retry"))
                    assertEquals(1, retryCalls)
                    assertTrue(restored.pendingRequestIds().isEmpty())
                }
            }
        }
    }

    @Test fun launchFailureAndStoppedHostAllowSafeRetry() {
        val registry = ControlledRegistry()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var failLaunch = true
                var canceled = 0
                val controller = WebAuthnController(activity, (activity.application as KoanApp).components.engine,
                    { if (failLaunch) error("synthetic launch failure") }, registry).also { it.start() }
                controller.startIntentSenderForResult(sender().intentSender) { assertNull(it); canceled++ }
                assertTrue(registry.codes.isEmpty())
                failLaunch = false
                controller.stop()
                controller.startIntentSenderForResult(sender().intentSender) { assertNull(it); canceled++ }
                assertTrue(registry.codes.isEmpty())
                controller.start()
                var result: Intent? = null
                controller.startIntentSenderForResult(sender().intentSender) { result = it }
                registry.dispatchResult(registry.codes.single(), Activity.RESULT_OK, Intent())
                assertNotNull(result)
                assertEquals(2, canceled)
            }
        }
    }

    @Test fun legacyTaskFailureAndCancellationRejectPromptly() {
        for (task in listOf(Tasks.forException<Any>(IllegalStateException("synthetic FIDO failure")), Tasks.forCanceled<Any>())) {
            val result = GeckoResult<Any>()
            LegacyFidoResult.observe(task, result)
            val error = assertThrows(WebAuthnUtils.Exception::class.java) { result.poll(1000) }
            assertEquals("ABORT_ERR", error.message)
        }
    }

    @Test fun successfulLegacyTaskLeavesNativeResultHandlingIntact() {
        val result = GeckoResult<Any>()
        val task = Tasks.forResult("synthetic pending intent")
        assertSame(task, LegacyFidoResult.observe(task, result))
        result.complete("native response")
        assertEquals("native response", result.poll(1000))
        val canceled = GeckoResult<Any>()
        LegacyFidoResult.observeCancellation(Tasks.forCanceled<Any>(), canceled)
        assertThrows(WebAuthnUtils.Exception::class.java) { canceled.poll(1000) }
    }

    @Test fun activityFailureMapsToAbort() {
        val result = GeckoResult<Any>()
        LegacyFidoResult.activityFailure(result).accept(IllegalStateException("synthetic canceled activity"))
        val error = assertThrows(WebAuthnUtils.Exception::class.java) { result.poll(1000) }
        assertEquals("ABORT_ERR", error.message)
    }

    @Test fun destroyedHostCannotLaunchAnotherProvider() {
        val registry = ControlledRegistry()
        lateinit var controller: WebAuthnController
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { controller = controller(it, registry) }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
            instrumentation.runOnMainSync {
                var calls = 0
                controller.start()
                controller.startIntentSenderForResult(sender().intentSender) { assertNull(it); calls++ }
                assertEquals(1, calls)
                assertTrue(registry.codes.isEmpty())
            }
        }
    }

    @Test fun nativeActivityDelegateReceivesSuccessAndCancellation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (cancel in listOf(false, true)) {
                val done = CompletableFuture<Intent?>()
                scenario.onActivity { activity ->
                    val runtime = (activity.application as KoanApp).components.runtime
                    val sender = PendingIntent.getActivity(activity, if (cancel) 18 else 17,
                        fixtureIntent().putExtra("cancel", cancel), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    requireNotNull(runtime.activityDelegate?.onStartActivityForResult(sender)).accept(
                        { done.complete(it) }, { done.completeExceptionally(it) },
                    )
                }
                if (cancel) {
                    val error = assertThrows(ExecutionException::class.java) { done.get(20, TimeUnit.SECONDS) }
                    assertEquals("Activity for result failed.", error.cause?.message)
                } else assertEquals("returned", done.get(20, TimeUnit.SECONDS)?.getStringExtra("fixture-result"))
            }
        }
    }

    @Test fun unrelatedAndroidXResultStillArrives() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val done = CompletableFuture<Intent?>()
            scenario.onActivity { activity ->
                activity.activityResultRegistry.register("unrelated-result", ActivityResultContracts.StartActivityForResult()) {
                    done.complete(it.data)
                }.launch(fixtureIntent())
            }
            assertEquals("returned", done.get(20, TimeUnit.SECONDS)?.getStringExtra("fixture-result"))
        }
    }

    private fun fixtureIntent() = Intent().setComponent(ComponentName("dev.nerzouille.koan.credentials.fixture",
        "dev.nerzouille.koan.auth.testing.ActivityResultFixture"))

    private fun sender() = PendingIntent.getActivity(instrumentation.targetContext, 42,
        fixtureIntent(), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun controller(activity: MainActivity, registry: ControlledRegistry) = WebAuthnController(
        activity, (activity.application as KoanApp).components.engine, {}, registry,
    ).also { it.start() }

    private class ControlledRegistry : ActivityResultRegistry() {
        val codes = mutableListOf<Int>()
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            codes += requestCode
        }
    }
}
