package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

/**
 * Page view: a horizontal swipe on the dock goes to the previous/next tab of the space. The page
 * follows the finger ([offset], > 0 = moving right, the previous tab coming in from the left) and the
 * neighbour's screenshot slides in beside it. A committed swipe opens the neighbour once it has slid
 * all the way in, and the page snaps back under it when that tab is on screen. Rubber band at the
 * first/last tab, no wrap. Read [offset] in layers only.
 */
@Stable
class TabSwipe(private val scope: CoroutineScope, private val current: StateFlow<String?>) {
    var offset by mutableFloatStateOf(0f)
        private set

    /** The neighbours, set when a swipe starts (composed only then). */
    var previous by mutableStateOf<TabEntity?>(null)
        private set
    var next by mutableStateOf<TabEntity?>(null)
        private set

    /** One page of travel: the page width plus the gap between pages. */
    var pageWidthPx = 1f
    private var raw = 0f
    private var job: Job? = null

    /** A committed swipe is sliding in or waiting for its tab: new swipes wait. */
    private var landing = false

    /** -1..1 towards the previous / next tab, for the address pill. */
    val fraction: Float get() = -offset / pageWidthPx

    /** Finger down. Returns false while the last swipe is still landing. */
    fun grab(previous: TabEntity?, next: TabEntity?): Boolean {
        if (landing) return false
        job?.cancel()
        this.previous = previous
        this.next = next
        raw = offset
        return true
    }

    fun dragBy(dxPx: Float) {
        raw += dxPx
        val blocked = (raw > 0f && previous == null) || (raw < 0f && next == null)
        offset = if (blocked) raw * PagerTokens.RUBBER_BAND else raw.coerceIn(-pageWidthPx, pageWidthPx)
    }

    /**
     * Finger up with horizontal velocity [vxPx] px/s. Dragged past [PagerTokens.COMMIT_FRACTION]
     * of the width, or flung that way: the neighbour slides in and [onCommit] opens it. Otherwise the
     * page springs back. Returns true when it commits.
     */
    fun release(vxPx: Float, flingPx: Float, onCommit: (TabEntity) -> Unit): Boolean {
        // > 0 = towards the next tab (the page moving left).
        val v = -vxPx
        val d = -offset / pageWidthPx
        val dir = when {
            abs(v) > flingPx -> if (d == 0f || sign(v) == sign(d)) sign(v).toInt() else 0
            d > PagerTokens.COMMIT_FRACTION -> 1
            d < -PagerTokens.COMMIT_FRACTION -> -1
            else -> 0
        }
        val target = when (dir) {
            1 -> next
            -1 -> previous
            else -> null
        }
        job?.cancel()
        if (target == null) {
            job = scope.launch {
                animate(offset, 0f, vxPx, Motion.TabSwipe) { o, _ -> offset = o }
                previous = null
                next = null
            }
            return false
        }
        landing = true
        job = scope.launch {
            animate(offset, -dir * pageWidthPx, vxPx, Motion.TabSwipe) { o, _ -> offset = o }
            onCommit(target)
            // The page view shows the tab's cover (its screenshot) a frame after it becomes current.
            withTimeoutOrNull(LAND_TIMEOUT_MS) { current.first { it == target.koanTabId } }
            withFrameNanos { }
            withFrameNanos { }
            offset = 0f
            previous = null
            next = null
            landing = false
        }
        return true
    }

    private companion object {
        /** Opening a tab first screenshots the one leaving (up to 150 ms), then selects it. */
        const val LAND_TIMEOUT_MS = 1500L
    }
}
