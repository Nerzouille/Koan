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
 * small sideways move scrubs the highlight from space to space ([stepPx] each). Releasing switches
 * to the highlighted space; releasing on the current one cancels. Nothing changes until release.
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

    var stepPx = 1f
    private var start = 0
    private var count = 1
    private var raw = 0f
    private var posJob: Job? = null
    private var shownJob: Job? = null

    fun begin(current: Int, count: Int) {
        this.count = count.coerceAtLeast(1)
        start = current.coerceIn(0, this.count - 1)
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
        raw = (raw + dxPx).coerceIn((-start - 0.5f) * stepPx, (count - 1 - start + 0.5f) * stepPx)
        val i = (start + (raw / stepPx).roundToInt()).coerceIn(0, count - 1)
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

    private fun reveal(to: Float) {
        shownJob?.cancel()
        shownJob = scope.launch { animate(shown, to, 0f, Motion.Overview) { v, _ -> shown = v } }
    }
}
