package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Pager across spaces, driven only by the workspace switcher. [pos] is fractional while dragging;
 * [settled] only changes when a gesture starts or a switch lands, so neighbour pages are composed
 * before the drag and never mid-gesture.
 */
@Stable
class SpacePager(initial: Int, private val scope: CoroutineScope) {
    var pos by mutableFloatStateOf(initial.toFloat())
        private set
    var settled by mutableIntStateOf(initial)
        private set

    /** Where a switch is landing; equals [settled] at rest and while dragging. */
    var target by mutableIntStateOf(initial)
        private set
    var count = 1

    /** A finger is on the switcher or a switch is landing. Background work waits for this. */
    var moving by mutableStateOf(false)
        private set

    /** One page of travel: the page width plus the gap between spaces, so pages follow the finger 1:1. */
    var pageWidthPx = 1f
    private var job: Job? = null

    /** Jump without animation, e.g. when the overview opens on the current space. */
    fun snapTo(index: Int) {
        job?.cancel()
        pos = index.toFloat()
        settled = index
        target = index
        moving = false
    }

    /** Finger down on the switcher: freeze. A switch that was still landing counts as landed. */
    fun grab() {
        job?.cancel()
        settled = pos.roundToInt().coerceIn(0, count - 1)
        target = settled
        moving = true
    }

    /** 1:1 with the finger; never more than one page away from the settled one. */
    fun dragBy(dxPx: Float) {
        val lo = (settled - 1).coerceAtLeast(0).toFloat()
        val hi = (settled + 1).coerceAtMost(count - 1).toFloat()
        val next = pos - dxPx / pageWidthPx
        // Rubber band at the first/last space.
        pos = when {
            next < lo -> lo - (lo - next) * PagerTokens.RUBBER_BAND
            next > hi -> hi + (next - hi) * PagerTokens.RUBBER_BAND
            else -> next
        }
    }

    /**
     * Finger up with horizontal velocity [vxPx] px/s. Dragged past [PagerTokens.COMMIT_FRACTION] of
     * a page, or flung faster than [flingPx] px/s in the drag direction: go to that neighbour.
     * Otherwise spring back. Returns true when this commits to another space.
     */
    fun release(vxPx: Float, flingPx: Float): Boolean {
        val from = settled
        return releaseTo(vxPx, flingPx) != from
    }

    /** [release], returning the space it lands on (known before the motion ends). */
    fun releaseTo(vxPx: Float, flingPx: Float): Int {
        val delta = pos - settled
        val v = -vxPx // > 0 = towards the next space
        val dir = when {
            abs(v) > flingPx -> if (delta == 0f || sign(v) == sign(delta)) sign(v).toInt() else 0
            delta > PagerTokens.COMMIT_FRACTION -> 1
            delta < -PagerTokens.COMMIT_FRACTION -> -1
            else -> 0
        }
        val target = (settled + dir).coerceIn(0, count - 1)
        animateTo(target, v / pageWidthPx)
        return target
    }

    fun animateTo(index: Int, velocity: Float = 0f) {
        job?.cancel()
        moving = true
        target = index
        job = scope.launch {
            animate(pos, index.toFloat(), velocity, Motion.PagerSettle) { v, _ -> pos = v }
            settled = index
            moving = false
        }
    }
}
