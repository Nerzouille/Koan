package dev.nerzouille.koan.auth.testing

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nerzouille.koan.KoanApp
import dev.nerzouille.koan.MainActivity
import dev.nerzouille.koan.engine.auth.PendingPasskeyResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasskeyIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Test fun pendingCallbackSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val done = CompletableFuture<Intent?>()
            scenario.onActivity { activity ->
                val state = ViewModelProvider(activity)[PendingPasskeyResult::class.java]
                assertTrue(state.begin { done.complete(it) })
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[PendingPasskeyResult::class.java].complete(Intent().putExtra("fixture-result", "restored"))
            }
            assertEquals("restored", done.get(20, TimeUnit.SECONDS)?.getStringExtra("fixture-result"))
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
}
