package dev.nerzouille.koan.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import dev.nerzouille.koan.ui.theme.Motion

/**
 * Lets the display run fast only while something moves, never a fixed rate. HyperOS caps unknown
 * apps at 60 Hz, so input asks for the highest mode at the current resolution; once input has been
 * quiet for [Motion.REFRESH_BOOST_IDLE_MS] (longer than any animation it can start), the request is
 * dropped and the system's own policy takes over again (adaptive refresh, battery).
 */
class RefreshBooster(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    private val release = Runnable { request(NO_PREFERENCE) }
    private var requested = NO_PREFERENCE

    /** Call on every touch/key event. Cheap: the window is only updated when the request changes. */
    fun onInput() {
        handler.removeCallbacks(release)
        handler.postDelayed(release, Motion.REFRESH_BOOST_IDLE_MS)
        request(bestModeId() ?: return)
    }

    fun stop() {
        handler.removeCallbacks(release)
        request(NO_PREFERENCE)
    }

    private fun request(modeId: Int) {
        if (modeId == requested) return
        requested = modeId
        val window = activity.window
        window.attributes = window.attributes.apply { preferredDisplayModeId = modeId }
    }

    private fun bestModeId(): Int? {
        val display = activity.display ?: return null
        val current = display.mode
        return display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?.modeId
    }

    private companion object {
        const val NO_PREFERENCE = 0
    }
}
