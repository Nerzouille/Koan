package dev.nerzouille.koan.ui

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens

/**
 * Page ↔ overview, 0 = page … 1 = overview. Springs to the open state on its own, or follows the
 * finger 1:1 on the dock while [dragging] (swipe up from the page, down from the overview). Read
 * [progress] in layers and draw only.
 */
@Stable
class OverviewMotion(open: Boolean) {
    var progress by mutableFloatStateOf(if (open) 1f else 0f)
        private set

    /** A finger drives [progress]; the open/close spring waits for the release. */
    var dragging by mutableStateOf(false)
        private set

    /** Finger travel for the whole morph. */
    var distancePx = 1f
    private var fromOpen = false
    private var velocity = 0f

    fun grab(open: Boolean) {
        fromOpen = open
        dragging = true
    }

    /** Finger moved by [dyPx] (> 0 = down = towards the page). */
    fun dragBy(dyPx: Float) {
        progress = (progress - dyPx / distancePx).coerceIn(0f, 1f)
    }

    /**
     * Finger up with vertical velocity [vyPx] px/s: true = settle on the overview. Commits to the
     * other side past [PagerTokens.COMMIT_FRACTION] of the way, or on a fling towards it.
     */
    fun release(vyPx: Float, flingPx: Float): Boolean {
        velocity = -vyPx / distancePx
        dragging = false
        return when {
            vyPx < -flingPx -> true
            vyPx > flingPx -> false
            fromOpen -> progress > 1f - PagerTokens.COMMIT_FRACTION
            else -> progress > PagerTokens.COMMIT_FRACTION
        }
    }

    /** Springs to [open], carrying the release velocity of a drag. */
    suspend fun settle(open: Boolean) {
        val v = velocity
        velocity = 0f
        animate(progress, if (open) 1f else 0f, v, Motion.Overview) { p, _ -> progress = p }
    }
}
