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
     * Koan went to the background, or into its picture-in-picture window (the activity isn't
     * stopped there, but Private is out of sight). The first one counts: closing the PiP window
     * then stops the activity, and the time away still runs from when PiP started.
     */
    fun onHidden() {
        if (hiddenAt == null) hiddenAt = SystemClock.elapsedRealtime()
    }

    /**
     * Koan is back, or its PiP window closed (back to full screen, or dismissed): locked again if
     * it was away [LOCK_AFTER_MS] or more, so the lock is up before anything private shows again.
     */
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
