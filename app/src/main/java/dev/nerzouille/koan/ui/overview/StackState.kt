package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

/** Per-card animated values, all read in layer/draw lambdas only. */
@Stable
class CardAnim(slot: Float) {
    /** Animated index in the stack; differs from the real index while the gap closes / reorders. */
    var slot by mutableFloatStateOf(slot)

    /** Horizontal swipe offset in px while the finger drags the card sideways. */
    var swipeX by mutableFloatStateOf(0f)

    /** Fly-off progress 0..1 after a close, starting from [flyFromX] towards [flyDir]. */
    var fly by mutableFloatStateOf(0f)
    var flyFromX = 0f
    var flyDir = 1f

    /** Long-press lift, 0..1. */
    var lift by mutableFloatStateOf(0f)

    /** Closed, waiting for Room to drop it. Not hit-testable. */
    var gone = false
    internal var slotJob: Job? = null
}

/**
 * Scroll state of one space's stack. [pos] is the stack position in card slots: the index of the
 * focused card (fractional while moving).
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

    fun anim(id: String, index: Int): CardAnim = cards.getOrPut(id) { CardAnim(index.toFloat()) }

    /** Finger down: stop any settling/fling and remember where this drag starts. True if it caught one. */
    fun startDrag(): Boolean {
        val caught = scrollJob?.isActive == true
        scrollJob?.cancel()
        dragStart = pos
        return caught
    }

    /** Finger moved by [dyPx] (positive = down = earlier cards come forward). Rubber-bands past both ends. */
    fun dragBy(dyPx: Float) {
        val delta = -dyPx / slotPx
        val over = max(-pos, pos - last)
        val goingFurtherOut = (pos < 0f && delta < 0) || (pos > last && delta > 0)
        pos += if (over > 0f && goingFurtherOut) delta * StackTokens.RUBBER_BAND / (1f + over * 2f) else delta
    }

    /**
     * Finger lifted with vertical velocity [vyPx] px/s. Slow: settle by the prototype's distance rule.
     * Fast: momentum scroll through as many cards as the velocity carries, landing on a card.
     */
    fun release(vyPx: Float = 0f) {
        val v = -vyPx / slotPx
        if (abs(v) < StackTokens.FLING_MIN_VELOCITY) return animateTo(snapTarget(pos, dragStart, last))
        val target = flingTarget(pos, v, last)
        val distance = target - pos
        if (distance == 0f || sign(distance) != sign(v)) return animateTo(target)
        // Cubic ease-out x(t) = x0 + D·(1 − (1 − t/T)³) starts at speed 3D/T: pick T to match the finger.
        val p = StackTokens.FLING_EASE_POWER
        val duration = (p * distance / v).coerceIn(StackTokens.FLING_MIN_SECONDS, StackTokens.FLING_MAX_SECONDS)
        scrollJob?.cancel()
        scrollJob = scope.launch {
            val from = pos
            val start = withFrameNanos { it }
            var t = 0f
            while (t < 1f) {
                withFrameNanos { now ->
                    t = ((now - start) / 1_000_000_000f / duration).coerceAtMost(1f)
                    pos = from + distance * (1f - (1f - t).pow(p))
                }
            }
        }
    }

    /** Exponential glide to [target]: fast start, no overshoot, like the prototype. */
    fun animateTo(target: Float) {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            var lastFrame = withFrameNanos { it }
            while (abs(target - pos) > 0.002f) {
                withFrameNanos { now ->
                    val dt = (now - lastFrame) / 1_000_000_000f
                    lastFrame = now
                    pos += (target - pos) * (1f - exp(-dt / Motion.STACK_SNAP_TAU_SECONDS))
                }
            }
            pos = target
        }
    }

    internal fun animateSlot(a: CardAnim, target: Float) {
        a.slotJob?.cancel()
        a.slotJob = scope.launch {
            animate(a.slot, target, 0f, Motion.CardSlot) { v, _ -> a.slot = v }
        }
    }

    internal fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    internal suspend fun animateFlyOff(a: CardAnim) =
        animate(0f, 1f, 0f, tween(Motion.FLY_OFF_MS, easing = FastOutLinearInEasing)) { v, _ -> a.fly = v }
}
