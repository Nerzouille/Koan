package dev.nerzouille.koan.engine.auth

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/** Credential Manager needs the browser's Activity context to keep its UI in the right task. */
object CredentialUiHost {
    private var current: WeakReference<FragmentActivity>? = null
    private val main = Handler(Looper.getMainLooper())

    fun attach(activity: FragmentActivity) { current = WeakReference(activity) }

    fun detach(activity: FragmentActivity) {
        // A retiring Activity must not clear the replacement that is already visible.
        if (current?.get() === activity) current = null
    }

    @JvmStatic
    fun context(fallback: Context): Context {
        val activity = current?.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return fallback
        return object : ContextWrapper(activity) {
            // Android can report a result before its selector has finished. Deliver it only
            // after the browser resumes, so a website's next request cannot overtake that UI.
            override fun getMainExecutor(): Executor = Executor { command -> main.post { deliver(command) } }
        }
    }

    private fun deliver(command: Runnable) {
        val owner = current?.get()
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
                main.post { deliver(command) }
            }
        }
        owner.lifecycle.addObserver(observer)
    }
}
