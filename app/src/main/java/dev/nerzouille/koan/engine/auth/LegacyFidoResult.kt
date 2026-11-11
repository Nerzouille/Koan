package dev.nerzouille.koan.engine.auth

import com.google.android.gms.tasks.Task
import org.mozilla.gecko.util.WebAuthnUtils
import org.mozilla.geckoview.GeckoResult

/** GeckoView 160's legacy getAssertion task attaches success only. Always settle failures too. */
object LegacyFidoResult {
    /** Shared by registration and login; successful native response parsing remains unchanged. */
    @JvmStatic
    fun activityFailure(result: GeckoResult<*>): GeckoResult.Consumer<Throwable> = GeckoResult.Consumer {
        result.completeExceptionally(WebAuthnUtils.Exception("ABORT_ERR"))
    }

    @JvmStatic
    fun observe(task: Task<*>, result: GeckoResult<*>): Task<*> {
        // Direct listeners also handle an already-completed task; no Activity is retained.
        val direct = java.util.concurrent.Executor { it.run() }
        task.addOnFailureListener(direct) {
            result.completeExceptionally(WebAuthnUtils.Exception("ABORT_ERR"))
        }
        return observeCancellation(task, result)
    }

    @JvmStatic
    fun observeCancellation(task: Task<*>, result: GeckoResult<*>): Task<*> {
        val direct = java.util.concurrent.Executor { it.run() }
        task.addOnCanceledListener(direct) {
            result.completeExceptionally(WebAuthnUtils.Exception("ABORT_ERR"))
        }
        return task
    }
}
