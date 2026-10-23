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
import kotlinx.coroutines.delay
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

    /** Closed: out of the stack, only drawn while it flies off. Not hit-testable. */
    var gone = false

    /** Depth the card had when it was closed: it flies off from there whatever the stack does. NaN = live. */
    var flyDepth = Float.NaN
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
    private var wheelJob: Job? = null

    /** A finger is on the stack or it is still moving: data updates wait for this to clear. */
    var busy by mutableStateOf(false)
        private set
    private var dragging = false

    /** [pos] when the current drag started; a release snaps relative to it. */
    private var dragStart = initialPos

    /**
     * Which way the user last scrolled: -1 towards the cards behind (older), 1 towards the cards in
     * front (newer), 0 not since the stack was re-centred. Picks the card a close brings forward.
     */
    var lastDirection = 0
        private set

    /** A card to bring into focus as soon as the stack shows it (an Undo puts it back). */
    var focusId by mutableStateOf<String?>(null)

    /** [id]'s values; a card back from a close (Undo) starts afresh, its old ones flew off. */
    fun anim(id: String, index: Int): CardAnim =
        cards[id]?.takeIf { !it.gone } ?: CardAnim(index.toFloat()).also { cards[id] = it }

    /** Finger down: stop any settling/fling and remember where this drag starts. True if it caught one. */
    fun startDrag(): Boolean {
        val caught = scrollJob?.isActive == true || wheelJob?.isActive == true
        scrollJob?.cancel()
        wheelJob?.cancel()
        dragStart = pos
        dragging = true
        busy = true
        return caught
    }

    /** The gesture that started with [startDrag] is over, whatever it turned out to be. */
    fun endTouch() {
        dragging = false
        busy = scrollJob?.isActive == true
    }

    /**
     * Moves the whole stack by [slots] without any visible motion: the position and every card's
     * slot shift together. Used when cards are added/removed before the focus by a sync.
     */
    internal fun shiftBy(slots: Int) {
        if (slots == 0) return
        pos += slots
        dragStart += slots
        for (a in cards.values) {
            a.slotJob?.cancel()
            a.slot += slots
        }
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
        dragging = false
        val v = -vyPx / slotPx
        val moved = if (abs(v) >= StackTokens.FLING_MIN_VELOCITY) v else pos - dragStart
        if (abs(moved) > DIRECTION_MIN_SLOTS) lastDirection = sign(moved).toInt()
        if (abs(v) < StackTokens.FLING_MIN_VELOCITY) return animateTo(snapTarget(pos, dragStart, last))
        val target = flingTarget(pos, v, last)
        val distance = target - pos
        if (distance == 0f || sign(distance) != sign(v)) return animateTo(target)
        // Cubic ease-out x(t) = x0 + D·(1 − (1 − t/T)³) starts at speed 3D/T: pick T to match the finger.
        val p = StackTokens.FLING_EASE_POWER
        val duration = (p * distance / v).coerceIn(StackTokens.FLING_MIN_SECONDS, StackTokens.FLING_MAX_SECONDS)
        startScroll {
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

    /**
     * A mouse wheel or two-finger trackpad scroll of [dyPx] (already in finger px, positive = like
     * a finger moving down). Moves like a drag; once the events stop, settles like a slow release.
     */
    fun wheel(dyPx: Float) {
        if (wheelJob?.isActive == true) {
            wheelJob?.cancel()
        } else {
            scrollJob?.cancel()
            dragStart = pos
        }
        busy = true
        dragBy(dyPx)
        wheelJob = scope.launch {
            delay(StackTokens.WHEEL_SETTLE_MS)
            release()
        }
    }

    /** Jumps to [index] without motion, e.g. to re-centre on the current tab while off screen. */
    fun snapTo(index: Int) {
        scrollJob?.cancel()
        pos = index.toFloat()
        dragStart = pos
        lastDirection = 0
    }

    /** Exponential glide to [target]: fast start, no overshoot, like the prototype. */
    fun animateTo(target: Float) {
        startScroll {
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

    private fun startScroll(block: suspend CoroutineScope.() -> Unit) {
        scrollJob?.cancel()
        busy = true
        val job = scope.launch(block = block)
        scrollJob = job
        job.invokeOnCompletion { if (scrollJob === job && !dragging) busy = false }
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

/** A drag shorter than this (in slots) says nothing about the direction. */
private const val DIRECTION_MIN_SLOTS = 0.05f
