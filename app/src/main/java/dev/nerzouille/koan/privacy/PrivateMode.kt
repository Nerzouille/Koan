package dev.nerzouille.koan.privacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import dev.nerzouille.koan.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Private mode's state (OVERVIEW_PRIVATE.md §4.1): whether Private is shown, and whether the
 * private tabs need the fingerprint or PIN before they show again (on by default: after 1 min
 * away from Koan, or the screen turning off). Per process: a cold start opens normal mode, locked.
 */
class PrivateMode(
    private val settings: Settings,
    private val count: StateFlow<Int>,
    /** Private bookmarks (they're kept with no private tab open): the lock covers them too. */
    private val bookmarks: suspend () -> Int = { 0 },
) {
    /** Private is shown: the private overview, or a private tab in the page view. */
    val on = MutableStateFlow(false)

    /** The private tabs ask for the fingerprint or PIN before they show. */
    val locked = MutableStateFlow(true)

    private var hiddenAt: Long? = null
    private var started = false

    /** Locks on screen off. Once per process. */
    fun start(context: Context) {
        if (started) return
        started = true
        context.applicationContext.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_SCREEN_OFF) locked.value = true
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
        )
    }

    /**
     * Nothing of Koan on screen any more (onStop): Koan went to the background, its PiP window was
     * closed, or "Audio only" ended it. A PiP window still on screen is not this: it counts as
     * Private open, with no timer.
     */
    fun onHidden() {
        hiddenAt = SystemClock.elapsedRealtime()
    }

    /** Koan is back (onStart): locked again if it was away [LOCK_AFTER_MS] or more. */
    fun onShown() {
        val at = hiddenAt ?: return
        hiddenAt = null
        if (SystemClock.elapsedRealtime() - at >= LOCK_AFTER_MS) locked.value = true
    }

    /** Whether showing Private needs the fingerprint first: the lock is on, there are private tabs or bookmarks, locked. */
    suspend fun needsUnlock(): Boolean = locked.value && (count.value > 0 || bookmarks() > 0) && settings.privateLock.first()

    companion object {
        const val LOCK_AFTER_MS = 60_000L
    }
}
