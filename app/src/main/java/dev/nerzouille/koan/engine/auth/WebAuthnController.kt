package dev.nerzouille.koan.engine.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.activity.ActivityDelegate

/** Owns a pending engine result, without retaining the Activity that launched the provider. */
class PendingPasskeyResult : ViewModel() {
    private var pending: ((Intent?) -> Unit)? = null

    fun begin(callback: (Intent?) -> Unit): Boolean {
        if (pending != null) {
            callback(null)
            return false
        }
        pending = callback
        return true
    }

    fun complete(data: Intent?) {
        val callback = pending
        pending = null
        callback?.invoke(data)
    }

    override fun onCleared() = complete(null)
}

/** AndroidX restores Activity results after recreation; the ViewModel keeps the engine callback. */
class WebAuthnController(
    activity: FragmentActivity,
    private val engine: Engine,
    private val beforeLaunch: () -> Unit,
) : ActivityDelegate {
    private val pending = ViewModelProvider(activity)[PendingPasskeyResult::class.java]
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        pending.complete(result.data.takeIf { result.resultCode == Activity.RESULT_OK })
    }

    fun start() = engine.registerActivityDelegate(this)

    fun stop() = engine.unregisterActivityDelegate()

    override fun startIntentSenderForResult(intent: IntentSender, onResult: (Intent?) -> Unit) {
        if (!pending.begin(onResult)) return
        try {
            beforeLaunch()
            launcher.launch(IntentSenderRequest.Builder(intent).build())
        } catch (_: Exception) {
            // A canceled/unavailable provider must settle the website's request, too.
            pending.complete(null)
        }
    }
}
