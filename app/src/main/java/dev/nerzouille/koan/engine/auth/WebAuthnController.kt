package dev.nerzouille.koan.engine.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.UUID
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.activity.ActivityDelegate

/** Returns system activity results to GeckoView's delegate. GeckoView owns the WebAuthn request. */
internal class WebAuthnController(
    activity: ComponentActivity,
    private val engine: Engine,
    private val exitFullscreen: () -> Unit,
) : ActivityDelegate, DefaultLifecycleObserver {
    private val registry = activity.activityResultRegistry
    private val savedState = activity.savedStateRegistry
    private val pending = ViewModelProvider(activity)[PendingPasskeyResult::class.java]
    private val main = Handler(Looper.getMainLooper())
    private val launchers = mutableMapOf<String, ActivityResultLauncher<IntentSenderRequest>>()
    private var started = false

    init {
        // Only routing IDs go into saved state. An abandoned request's result is discarded
        // after process death. A recreated Activity can still return a live ViewModel callback.
        val restoredIds = savedState.consumeRestoredStateForKey(STATE_KEY)
            ?.getStringArrayList(REQUEST_IDS).orEmpty()
        (restoredIds + listOfNotNull(pending.id)).distinct().forEach(::register)
        savedState.registerSavedStateProvider(STATE_KEY) {
            Bundle().apply { putStringArrayList(REQUEST_IDS, ArrayList(launchers.keys)) }
        }
        activity.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        started = true
        engine.registerActivityDelegate(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        started = false
        engine.unregisterActivityDelegate()
        // Opening the provider can stop Koan. Its pending result must stay registered.
    }

    override fun onDestroy(owner: LifecycleOwner) {
        started = false
        launchers.values.forEach { it.unregister() }
        launchers.clear()
        savedState.unregisterSavedStateProvider(STATE_KEY)
    }

    override fun startIntentSenderForResult(intent: IntentSender, onResult: (Intent?) -> Unit) {
        val id = if (started) pending.begin(onResult) else null
        if (id == null) {
            onResult(null)
            return
        }

        try {
            exitFullscreen()
            register(id).launch(IntentSenderRequest.Builder(intent).build())
        } catch (_: SecurityException) {
            launchers.remove(id)?.unregister()
            pending.complete(id, null)
        }
    }

    private fun register(id: String): ActivityResultLauncher<IntentSenderRequest> {
        val launcher = registry.register("$STATE_KEY.$id", ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // AndroidX removes its in-flight marker after this callback returns. Unregister
            // afterwards so it can also release the request code. This covers queued delivery.
            main.post { launchers.remove(id)?.unregister() }
            pending.complete(id, result.data.takeIf { result.resultCode == Activity.RESULT_OK })
        }
        launchers[id] = launcher
        return launcher
    }

    private companion object {
        const val STATE_KEY = "koan.webauthn"
        const val REQUEST_IDS = "requestIds"
    }
}

/** Retains one callback across Activity recreation without retaining the Activity itself. */
internal class PendingPasskeyResult : ViewModel() {
    private var request: Request? = null
    val id: String? get() = request?.id

    fun begin(callback: (Intent?) -> Unit): String? {
        if (request != null) return null
        val id = UUID.randomUUID().toString()
        request = Request(id, callback)
        return id
    }

    fun complete(id: String, data: Intent?) {
        val current = request?.takeIf { it.id == id } ?: return
        request = null
        current.callback(data)
    }

    override fun onCleared() {
        id?.let { complete(it, null) }
    }

    private data class Request(val id: String, val callback: (Intent?) -> Unit)
}
