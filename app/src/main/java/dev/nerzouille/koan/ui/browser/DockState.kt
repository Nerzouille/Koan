package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import dev.nerzouille.koan.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The page view's dock hiding while the page scrolls (GeckoView dynamic toolbar). [offset] is how
 * far it has slid down: 0 = shown, [heightPx] = hidden. It follows the page 1:1 while the finger
 * scrolls it, then settles shown or hidden. Read [offset] only in layer/draw lambdas.
 */
@Stable
class DockState(private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
        private set

    /** The dock's visible height in the page view; the engine reserves this much for it. */
    var heightPx = 0f

    /** Overview open, address being edited…: the dock stays put. */
    var locked = false
        set(value) {
            field = value
            if (value) show()
        }

    private var job: Job? = null

    /** Page scroll since the finger went down (> 0 = page scrolled down, dock hiding). */
    private var sinceDown = 0f

    /** A finger is on the page: only the finger moves the dock. */
    var touching = false
        private set

    /** Page content scrolled by [dy] px (> 0 = towards the end of the page). */
    fun scrollBy(dy: Float) {
        if (locked || heightPx <= 0f) return
        job?.cancel()
        sinceDown += dy
        offset = (offset + dy).coerceIn(0f, heightPx)
    }

    fun onTouchDown() {
        touching = true
        sinceDown = 0f
    }

    /** Finger up: finish in the direction the page was going, or the nearest end after a nudge. */
    fun settle(slopPx: Float) {
        touching = false
        if (offset <= 0f || offset >= heightPx) return
        val hide = if (abs(sinceDown) > slopPx) sinceDown > 0f else offset > heightPx / 2f
        animateTo(if (hide) heightPx else 0f)
    }

    fun show() {
        if (offset > 0f) animateTo(0f)
    }

    fun snapShown() {
        job?.cancel()
        offset = 0f
    }

    private fun animateTo(target: Float) {
        job?.cancel()
        job = scope.launch { animate(offset, target, animationSpec = Motion.DockSettle) { v, _ -> offset = v } }
    }

    /** Page scrolls arrive from GeckoView's nested scrolling; the dock moves, the page keeps all of it. */
    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) scrollBy(-available.y)
            return Offset.Zero
        }
    }
}
