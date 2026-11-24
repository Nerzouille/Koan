package dev.nerzouille.koan.engine.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import java.lang.reflect.Proxy
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.activity.ActivityDelegate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WebAuthnControllerTest {
    private val activity = Robolectric.buildActivity(Host::class.java).setup()
    private val host get() = activity.get()

    @After fun close() = activity.close()

    @Test fun cancellationReleasesTheRequestAndLateResultsCannotCompleteARetry() {
        val results = mutableListOf<Intent?>()
        host.request(results::add)
        val first = host.requestCodes.single()

        // A second request cannot replace the one that opened the system prompt.
        val overlapping = mutableListOf<Intent?>()
        host.request(overlapping::add)
        assertEquals(listOf<Intent?>(null), overlapping)
        assertEquals(listOf(first), host.requestCodes)

        assertTrue(host.deliver(first, Activity.RESULT_CANCELED, Intent("test.cancel")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf<Intent?>(null), results)
        host.request(results::add)
        val second = host.requestCodes.last()

        assertFalse(host.deliver(first, Activity.RESULT_OK, Intent("test.late")))
        val answer = Intent("test.answer")
        assertTrue(host.deliver(second, Activity.RESULT_OK, answer))
        assertEquals(listOf(null, answer), results)
    }

    @Test fun recreationReturnsTheResultToTheOriginalCallback() {
        val results = mutableListOf<Intent?>()
        host.request(results::add)
        val code = host.requestCodes.single()
        activity.recreate()

        val answer = Intent("test.after-recreation")
        assertTrue(host.deliver(code, Activity.RESULT_OK, answer))
        assertEquals(listOf(answer), results)
    }

    @Test fun aHostRestoredWithoutTheOldCallbackDiscardsTheAbandonedResult() {
        val oldResults = mutableListOf<Intent?>()
        host.request(oldResults::add)
        val oldCode = host.requestCodes.single()
        val savedState = Bundle()
        activity.pause().stop().saveInstanceState(savedState)

        // A fresh Activity gets the saved routing ID without the old ViewModel callback.
        Robolectric.buildActivity(Host::class.java).setup(savedState).use { replacement ->
            val results = mutableListOf<Intent?>()
            val other = replacement.get()
            other.request(results::add)
            assertTrue(other.deliver(oldCode, Activity.RESULT_OK, Intent("test.abandoned")))
            assertTrue(results.isEmpty())
            assertTrue(oldResults.isEmpty())
            val answer = Intent("test.new-request")
            assertTrue(other.deliver(other.requestCodes.single(), Activity.RESULT_OK, answer))
            assertEquals(listOf(answer), results)
        }
    }

    @Test fun aStoppedHostRejectsNewLaunchesAndKeepsTheExistingResult() {
        val results = mutableListOf<Intent?>()
        host.request(results::add)
        val code = host.requestCodes.single()
        val oldDelegate = requireNotNull(host.delegate)
        activity.pause().stop()

        assertNull(host.delegate)
        val rejected = mutableListOf<Intent?>()
        oldDelegate.startIntentSenderForResult(host.sender, rejected::add)
        assertEquals(listOf<Intent?>(null), rejected)
        assertEquals(listOf(code), host.requestCodes)
        assertTrue(results.isEmpty())

        activity.restart().start().resume()
        val answer = Intent("test.after-stop")
        assertTrue(host.deliver(code, Activity.RESULT_OK, answer))
        assertEquals(listOf(answer), results)
    }

    @Test fun aLaunchFailureReturnsCancellationAndAllowsRetry() {
        val results = mutableListOf<Intent?>()
        host.failNextLaunch = true
        host.request(results::add)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf<Intent?>(null), results)

        host.request(results::add)
        val answer = Intent("test.retry")
        assertTrue(host.deliver(host.requestCodes.single(), Activity.RESULT_OK, answer))
        assertEquals(listOf(null, answer), results)
    }

    class Host : ComponentActivity() {
        var delegate: ActivityDelegate? = null
            private set
        var failNextLaunch = false
        val requestCodes = mutableListOf<Int>()
        val sender: IntentSender
            get() = PendingIntent.getActivity(this, 0, Intent(this, Host::class.java), PendingIntent.FLAG_IMMUTABLE)
                .intentSender

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Only model the Engine's delegate registration. AndroidX owns the result registry.
            val engine = Proxy.newProxyInstance(Engine::class.java.classLoader, arrayOf(Engine::class.java)) { _, call, args ->
                when (call.name) {
                    "registerActivityDelegate" -> delegate = args?.single() as ActivityDelegate
                    "unregisterActivityDelegate" -> delegate = null
                    else -> error("Unexpected Engine call: ${call.name}")
                }
                null
            } as Engine
            WebAuthnController(this, engine) {}
        }

        fun request(callback: (Intent?) -> Unit) = requireNotNull(delegate).startIntentSenderForResult(sender, callback)

        fun deliver(code: Int, result: Int, data: Intent): Boolean = activityResultRegistry.dispatchResult(code, result, data)

        @Suppress("OVERRIDE_DEPRECATION") // AndroidX's contract still calls this platform method.
        override fun startIntentSenderForResult(
            intent: IntentSender,
            requestCode: Int,
            fillInIntent: Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
            options: Bundle?,
        ) {
            if (failNextLaunch) {
                failNextLaunch = false
                throw IntentSender.SendIntentException("Test launch failure")
            }
            requestCodes += requestCode
        }
    }
}
