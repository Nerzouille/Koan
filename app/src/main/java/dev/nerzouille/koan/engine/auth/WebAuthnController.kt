package dev.nerzouille.koan.engine.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.UUID
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.activity.ActivityDelegate

/** Owns a pending engine result, without retaining the Activity that launched the provider. */
class PendingPasskeyResult : ViewModel() {
    private var pending: ((Intent?) -> Unit)? = null
    var requestId: String? = null
        private set

    fun begin(callback: (Intent?) -> Unit): String? {
        if (pending != null) {
            callback(null)
            return null
        }
        pending = callback
        return UUID.randomUUID().toString().also { requestId = it }
    }

    fun complete(id: String, data: Intent?): Boolean {
        if (id != requestId) return false
        val callback = pending
        pending = null
        requestId = null
        callback?.invoke(data)
        return true
    }

    override fun onCleared() {
        requestId?.let { complete(it, null) }
    }
}

/** AndroidX restores Activity results after recreation; the ViewModel keeps the engine callback. */
class WebAuthnController(
    activity: FragmentActivity,
    private val engine: Engine,
    private val beforeLaunch: () -> Unit,
    private val registry: ActivityResultRegistry = activity.activityResultRegistry,
) : ActivityDelegate {
    private val pending = ViewModelProvider(activity)[PendingPasskeyResult::class.java]
    private var registration: Pair<String, ActivityResultLauncher<IntentSenderRequest>>? = null
    private var started = false

    init {
        // Rebind the same request after rotation. A fresh ViewModel after process death has no
        // live engine callback; later requests use new keys and cannot consume that old result.
        pending.requestId?.let(::register)
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) registration?.first?.let(::unregister)
        })
    }

    fun start() {
        if (started) return
        started = true
        engine.registerActivityDelegate(this)
    }

    fun stop() {
        if (!started) return
        started = false
        engine.unregisterActivityDelegate()
        // The provider normally stops the host Activity; keep its pending result alive.
    }

    private fun unregister(id: String) {
        registration?.takeIf { it.first == id }?.let {
            registration = null
            it.second.unregister()
        }
    }

    private fun register(id: String): ActivityResultLauncher<IntentSenderRequest> {
        val launcher = registry.register("koan.webauthn.$id", ActivityResultContracts.StartIntentSenderForResult()) { result ->
            unregister(id)
            pending.complete(id, result.data.takeIf { result.resultCode == Activity.RESULT_OK })
        }
        // register() may synchronously deliver a result queued during Activity recreation.
        if (pending.requestId == id) registration = id to launcher else launcher.unregister()
        return launcher
    }

    override fun startIntentSenderForResult(intent: IntentSender, onResult: (Intent?) -> Unit) {
        if (!started) {
            onResult(null)
            return
        }
        val id = pending.begin(onResult) ?: return
        try {
            beforeLaunch()
            register(id).launch(IntentSenderRequest.Builder(intent).build())
        } catch (_: Exception) {
            // A canceled/unavailable provider must settle the website's request, too.
            unregister(id)
            pending.complete(id, null)
        }
    }
}
