package dev.nerzouille.koan.engine.auth

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/** Credential Manager needs the browser's Activity context to keep its UI in the right task. */
object CredentialUiHost {
    private class Host(activity: FragmentActivity) {
        val activity = WeakReference(activity)
        val identity = ViewModelProvider(activity)[CredentialUiIdentity::class.java]
    }
    private var current: Host? = null
    private val main = Handler(Looper.getMainLooper())

    fun attach(activity: FragmentActivity) { current = Host(activity) }

    fun detach(activity: FragmentActivity) {
        // A retiring Activity must not clear the replacement that is already visible.
        if (current?.activity?.get() === activity) current = null
    }

    @JvmStatic
    fun context(fallback: Context): Context {
        val host = current ?: return fallback
        val activity = host.activity.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return fallback
        return object : ContextWrapper(activity) {
            // Android can report a result before its selector has finished. Deliver it only
            // after the browser resumes, so a website's next request cannot overtake that UI.
            override fun getMainExecutor(): Executor = Executor { command -> main.post { deliver(command, host) } }
        }
    }

    private fun deliver(command: Runnable, host: Host) {
        // Follow rotation within this logical host, never a fresh Activity's lifecycle.
        val owner = (current?.takeIf { it.identity === host.identity } ?: host).activity.get()
        if (owner == null || owner.lifecycle.currentState == Lifecycle.State.DESTROYED ||
            owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            command.run()
            return
        }
        var delivered = false
        lateinit var observer: LifecycleEventObserver
        observer = LifecycleEventObserver { _, event ->
            if (!delivered && (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_DESTROY)) {
                delivered = true
                owner.lifecycle.removeObserver(observer)
                // Let all resume observers (including the engine view) finish first.
                main.post { deliver(command, host) }
            }
        }
        owner.lifecycle.addObserver(observer)
    }
}

/** Retained across Activity recreation; contains no Activity, credential or callback. */
internal class CredentialUiIdentity : ViewModel()
