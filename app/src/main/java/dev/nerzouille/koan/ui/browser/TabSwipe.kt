package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Page view: a horizontal swipe on the dock moves along the tabs of the current tab's stack, like
 * Safari. The tabs form a strip ([strip], [pos] = fractional index into it); the live page sits at
 * [live], every other tab shows its screenshot. Swipes chain: a new one is taken at once, even
 * mid-motion, from where the strip is. Nothing loads while swiping: the tab the strip rests on
 * becomes live only after [BarTokens.TAB_SWIPE_REST_MS] without a finger. Rubber band at the first /
 * last tab, no wrap. Read [pos] in layers only.
 */
@Stable
class TabSwipe(private val scope: CoroutineScope, private val current: StateFlow<String?>) {
    /** The stack's tabs, oldest first, while a chain of swipes runs; empty otherwise. */
    var strip by mutableStateOf<List<TabEntity>>(emptyList())
        private set

    /** Fractional index into [strip]: where the screen is. */
    var pos by mutableFloatStateOf(0f)
        private set

    /** Index of the live page (the engine view) in [strip]. */
    var live by mutableIntStateOf(0)
        private set

    /** The tab the current drag started on: its neighbours are what the drag can reach. */
    var anchor by mutableIntStateOf(0)
        private set

    /** One page of travel: the page width plus the gap between pages. */
    var pageWidthPx = 1f
    private var grabPos = 0f
    private var raw = 0f
    private var job: Job? = null

    /** Bumped by every grab: a landing only ends the chain if no swipe came after it. */
    private var gen = 0

    val active: Boolean get() = strip.isNotEmpty()

    /** -1..1 from [anchor] towards the previous / next tab, for the address pill. */
    val fraction: Float get() = if (active) pos - anchor else 0f

    /** The tab under the screen when it is not the live one (the pill shows its host). */
    val shown: TabEntity? get() = if (active && anchor != live) strip.getOrNull(anchor) else null
    val previous: TabEntity? get() = if (active) strip.getOrNull(anchor - 1) else null
    val next: TabEntity? get() = if (active) strip.getOrNull(anchor + 1) else null

    /**
     * Finger down. Never refused: a chain in progress goes on from where it is (whatever is still
     * moving stops under the finger); otherwise [stack] gives the strip and the live tab's index.
     */
    fun grab(stack: () -> Pair<List<TabEntity>, Int>?): Boolean {
        job?.cancel()
        gen++
        if (!active) {
            val (tabs, index) = stack() ?: return false
            if (tabs.size < 2 || index !in tabs.indices) return false
            strip = tabs
            live = index
            pos = index.toFloat()
        }
        anchor = pos.roundToInt().coerceIn(0, strip.lastIndex)
        grabPos = pos
        raw = 0f
        return true
    }

    fun dragBy(dxPx: Float) {
        // The page follows the finger: moving right brings the previous tab in.
        raw -= dxPx / pageWidthPx
        val target = grabPos + raw
        val lo = (anchor - 1).coerceAtLeast(0).toFloat()
        val hi = (anchor + 1).coerceAtMost(strip.lastIndex).toFloat()
        pos = when {
            target < lo -> lo - (lo - target) * PagerTokens.RUBBER_BAND
            target > hi -> hi + (target - hi) * PagerTokens.RUBBER_BAND
            else -> target
        }
    }

    /**
     * Finger up with horizontal velocity [vxPx] px/s. Dragged past [PagerTokens.COMMIT_FRACTION]
     * of a page from [anchor], or flung that way: the strip moves on to that neighbour, else back.
     * Once it has rested a moment, [onCommit] opens the tab it rests on. Returns true when it moves
     * to another tab.
     */
    fun release(vxPx: Float, flingPx: Float, onCommit: (TabEntity) -> Unit): Boolean {
        // > 0 = towards the next tab (the page moving left).
        val v = -vxPx
        val d = pos - anchor
        val dir = when {
            abs(v) > flingPx -> if (d == 0f || sign(v) == sign(d)) sign(v).toInt() else 0
            d > PagerTokens.COMMIT_FRACTION -> 1
            d < -PagerTokens.COMMIT_FRACTION -> -1
            else -> 0
        }
        val target = (anchor + dir).coerceIn(0, strip.lastIndex)
        job?.cancel()
        val chain = gen
        job = scope.launch {
            animate(pos, target.toFloat(), v / pageWidthPx, Motion.TabSwipe) { p, _ -> pos = p }
            // Another swipe may follow: the tab only goes live once the strip rests.
            delay(BarTokens.TAB_SWIPE_REST_MS)
            land(target, chain, onCommit)
        }
        return target != anchor
    }

    /**
     * The overview takes over mid-chain: the strip goes away and the live page is back in place.
     * A landing already under way finishes on its own.
     */
    fun cancel() {
        job?.cancel()
        gen++
        strip = emptyList()
    }

    /** Makes strip tab [index] live (if it is not), then ends the chain. Never interrupted halfway. */
    private suspend fun land(index: Int, chain: Int, onCommit: (TabEntity) -> Unit) = withContext(NonCancellable) {
        val tab = strip.getOrNull(index) ?: return@withContext
        if (index != live) {
            onCommit(tab)
            // The page view shows the tab's cover (its screenshot) a frame after it becomes current;
            // only then does the engine view move to the middle under the identical preview.
            withTimeoutOrNull(LAND_TIMEOUT_MS) { current.first { it == tab.koanTabId } }
            withFrameNanos { }
            withFrameNanos { }
            live = index
        }
        // A new swipe may have started meanwhile: it keeps the strip.
        if (gen == chain) strip = emptyList()
    }

    private companion object {
        /** Opening a tab first screenshots the one leaving (up to 150 ms), then selects it. */
        const val LAND_TIMEOUT_MS = 1500L
    }
}
