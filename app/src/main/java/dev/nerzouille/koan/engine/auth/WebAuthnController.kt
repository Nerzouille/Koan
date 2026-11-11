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
    private var cleared = false
    var requestId: String? = null
        private set

    fun begin(callback: (Intent?) -> Unit): String? {
        if (cleared || pending != null) {
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
        cleared = true
        requestId?.let { complete(it, null) }
    }
}

/** AndroidX restores Activity results after recreation; the ViewModel keeps the engine callback. */
class WebAuthnController(
    activity: FragmentActivity,
    private val engine: Engine,
    private val beforeLaunch: () -> Unit,
    private val registry: ActivityResultRegistry = activity.activityResultRegistry,
    restoredRequestIds: List<String> = emptyList(),
) : ActivityDelegate {
    private val pending = ViewModelProvider(activity)[PendingPasskeyResult::class.java]
    private val registrations = mutableMapOf<String, ActivityResultLauncher<IntentSenderRequest>>()
    private enum class HostState { STOPPED, STARTED, DESTROYED }
    private var hostState = HostState.STOPPED

    init {
        CredentialUiHost.attach(activity)
        // Rotation retains a live callback; after process death these keys only drain old
        // results. Never leave a credential response sitting in AndroidX's pending-result state.
        (restoredRequestIds + listOfNotNull(pending.requestId)).distinct().forEach(::register)
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    CredentialUiHost.attach(activity)
                    start()
                }
                Lifecycle.Event.ON_STOP -> stop()
                Lifecycle.Event.ON_DESTROY -> {
                    stop()
                    hostState = HostState.DESTROYED
                    CredentialUiHost.detach(activity)
                    registrations.keys.toList().forEach(::unregister)
                }
                else -> Unit
            }
        })
    }

    /** Saved state contains opaque routing IDs only, never an Intent or credential response. */
    fun pendingRequestIds(): List<String> = registrations.keys.toList()

    fun start() {
        if (hostState != HostState.STOPPED) return
        hostState = HostState.STARTED
        engine.registerActivityDelegate(this)
    }

    fun stop() {
        if (hostState != HostState.STARTED) return
        hostState = HostState.STOPPED
        engine.unregisterActivityDelegate()
        // The provider normally stops the host Activity; keep its pending result alive.
    }

    private fun unregister(id: String) {
        registrations.remove(id)?.unregister()
    }

    private fun register(id: String): ActivityResultLauncher<IntentSenderRequest> {
        var delivered = false
        val launcher = registry.register("koan.webauthn.$id", ActivityResultContracts.StartIntentSenderForResult()) { result ->
            delivered = true
            unregister(id)
            pending.complete(id, result.data.takeIf { result.resultCode == Activity.RESULT_OK })
        }
        // register() may synchronously deliver a result queued during Activity recreation.
        if (!delivered) registrations[id] = launcher else launcher.unregister()
        return launcher
    }

    override fun startIntentSenderForResult(intent: IntentSender, onResult: (Intent?) -> Unit) {
        if (hostState != HostState.STARTED) {
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
