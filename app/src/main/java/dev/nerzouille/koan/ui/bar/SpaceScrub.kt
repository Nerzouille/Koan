package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Page view space mode: after a long press on the dock, the space row shows above the pill and a
 * sideways move scrubs the highlight from space to space. The steps fit the screen: the way to the
 * left edge is shared by the spaces on the left, the way to the right edge by those on the right,
 * so sliding towards an edge always reaches the first or last space just before it ([minStepPx] … [maxStepPx] each, smaller only
 * when that many spaces wouldn't fit). Releasing switches to the highlighted space; releasing on
 * the current one cancels. Nothing changes until release.
 */
@Stable
class SpaceScrub(private val scope: CoroutineScope) {
    /** The finger is scrubbing. */
    var active by mutableStateOf(false)
        private set

    /** The highlighted space. */
    var index by mutableIntStateOf(0)
        private set

    /** The highlight's position, springing to [index]; read in layers and draw only. */
    var pos by mutableFloatStateOf(0f)
        private set

    /** 0 … 1: the space row growing out of the dock; read in layers and draw only. */
    var shown by mutableFloatStateOf(0f)
        private set

    var minStepPx = 1f
    var maxStepPx = 1f

    /** The first and last spaces are reached this far before the screen's edges. */
    var edgePx = 0f
    private var leftStep = 1f
    private var rightStep = 1f
    private var start = 0
    private var count = 1
    private var raw = 0f
    private var posJob: Job? = null
    private var shownJob: Job? = null

    /** [fingerX]: where the finger is, on a [widthPx] wide screen. */
    fun begin(current: Int, count: Int, fingerX: Float, widthPx: Float) {
        this.count = count.coerceAtLeast(1)
        start = current.coerceIn(0, this.count - 1)
        leftStep = stepFor(fingerX, start)
        rightStep = stepFor(widthPx - fingerX, this.count - 1 - start)
        index = start
        posJob?.cancel()
        pos = start.toFloat()
        raw = 0f
        active = true
        reveal(1f)
    }

    /** Finger moved by [dxPx]. Returns true when the highlight moved to another space. */
    fun dragBy(dxPx: Float): Boolean {
        // Kept within half a step past the ends, so coming back never needs extra travel.
        raw = (raw + dxPx).coerceIn((-start - 0.5f) * leftStep, (count - 1 - start + 0.5f) * rightStep)
        val step = if (raw < 0f) leftStep else rightStep
        val i = (start + (raw / step).roundToInt()).coerceIn(0, count - 1)
        if (i == index) return false
        index = i
        posJob?.cancel()
        posJob = scope.launch { animate(pos, i.toFloat(), 0f, Motion.ScrubHighlight) { v, _ -> pos = v } }
        return true
    }

    /** Finger up: the highlighted space, or null when it is the one the scrub started on. */
    fun end(): Int? {
        active = false
        reveal(0f)
        return index.takeIf { it != start }
    }

    /** One step on a side with [spaces] spaces and [roomPx] to its edge. */
    private fun stepFor(roomPx: Float, spaces: Int): Float {
        if (spaces <= 0) return maxStepPx
        // The thumb never has to reach the very edge.
        val fit = (roomPx - edgePx).coerceAtLeast(spaces.toFloat()) / spaces
        // Too many spaces for the smallest step: the whole way to the edge, shared evenly.
        return if (fit < minStepPx) fit.coerceAtLeast(1f) else fit.coerceAtMost(maxStepPx)
    }

    private fun reveal(to: Float) {
        shownJob?.cancel()
        shownJob = scope.launch { animate(shown, to, 0f, Motion.Overview) { v, _ -> shown = v } }
    }
}
