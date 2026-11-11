package dev.nerzouille.koan.engine

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Process-lifetime coroutine scopes. A background job that fails (a database or disk error, a full
 * disk) is logged and dropped instead of crashing Koan, at start or later. The log names only the
 * exception class and where it came from: an exception's message can hold a URL or a path.
 */
object KoanScopes {
    private const val TAG = "KoanScopes"

    /** Called for every uncaught failure (tests replace it). */
    @Volatile
    internal var report: (String, Throwable) -> Unit = { name, e ->
        val at = e.stackTrace.firstOrNull()?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
        runCatching { Log.e(TAG, "$name: ${e.javaClass.name} at $at") }
    }

    fun handler(name: String) = CoroutineExceptionHandler { _, e -> report(name, e) }

    /** A supervisor scope on [dispatcher] that reports and survives its children's failures. */
    fun guarded(name: String, dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher + handler(name))
}
