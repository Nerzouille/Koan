package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

/** `pos` at which the essentials/pinned block is fully revealed. 0 = collapsed, oldest card focused. */
const val REVEAL_STOP = -1f

/** Per-card animated values, all read in layer/draw lambdas only. */
@Stable
class CardAnim(slot: Float) {
    /** Animated index in the stack; differs from the real index while the gap closes / reorders. */
    var slot by mutableFloatStateOf(slot)
    var swipeX by mutableFloatStateOf(0f)

    /** ✕-close progress, 0..1. */
    var exit by mutableFloatStateOf(0f)

    /** Long-press lift, 0..1. */
    var lift by mutableFloatStateOf(0f)

    /** Swiped/closed away, waiting for Room to drop it. Not drawn, not hit-testable. */
    var gone = false
    internal var slotJob: Job? = null
}

/**
 * Scroll state of one space's stack. [pos] is the stack position in card slots: the index of the
 * card on the focus line (fractional while moving). Values in [REVEAL_STOP, 0) reveal the header block.
 */
@Stable
class StackState(initialPos: Float, private val scope: CoroutineScope) {
    var pos by mutableFloatStateOf(initialPos)
        private set

    /** Index of the last card; kept up to date by the stack. */
    var last = 0
        internal set

    /** Finger travel (px) that moves the stack by one slot. */
    var slotPx = 1f
        internal set

    var liftedId by mutableStateOf<String?>(null)
        internal set

    internal val cards = HashMap<String, CardAnim>()
    private var scrollJob: Job? = null

    /** [pos] when the current drag started; a release snaps relative to it. */
    private var dragStart = initialPos

    /** Header block reveal: 0 collapsed … 1 fully revealed. */
    val expansion: Float get() = (pos / REVEAL_STOP).coerceIn(0f, 1f)

    /** Card position in the stack, ignoring the header reveal. */
    val stackPos: Float get() = max(pos, 0f)

    fun anim(id: String, index: Int): CardAnim = cards.getOrPut(id) { CardAnim(index.toFloat()) }

    /** Finger down: stop any settling and remember where this drag starts. */
    fun startDrag() {
        scrollJob?.cancel()
        dragStart = pos
    }

    /** Finger moved by [dyPx] (positive = down). Rubber-bands past both ends. */
    fun dragBy(dyPx: Float) {
        val delta = -dyPx / slotPx
        val over = max(REVEAL_STOP - pos, pos - last)
        val goingFurtherOut = (pos < REVEAL_STOP && delta < 0) || (pos > last && delta > 0)
        pos += if (over > 0f && goingFurtherOut) delta * StackTokens.RUBBER_BAND / (1f + over * 2f) else delta
    }

    /** Finger lifted with vertical velocity [vyPx] px/s: settle on the nearest stop. */
    fun fling(vyPx: Float) {
        val velocity = -vyPx / slotPx
        animateTo(snapTarget(pos, velocity, dragStart, last, REVEAL_STOP), velocity)
    }

    fun animateTo(target: Float, velocity: Float = 0f) {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            animate(pos, target, velocity, Motion.StackSnap) { v, _ -> pos = v }
        }
    }

    internal fun animateSlot(a: CardAnim, target: Float) {
        a.slotJob?.cancel()
        a.slotJob = scope.launch {
            animate(a.slot, target, 0f, Motion.CardSlot) { v, _ -> a.slot = v }
        }
    }

    internal fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    internal suspend fun animateExit(a: CardAnim) =
        animate(0f, 1f, 0f, tween(Motion.CARD_EXIT_MS)) { v, _ -> a.exit = v }
}

@Composable
fun rememberStackState(key: Any, initialPos: () -> Float): StackState {
    val scope = rememberCoroutineScope()
    return remember(key) { StackState(initialPos(), scope) }
}
