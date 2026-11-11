package dev.nerzouille.koan.ui.privacy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PrivateTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The Private on / off transition (owner, 2026-11-11): Private is one more page beside the space
 * we were on, and a flip is the space pager's own motion between the two, on its spring ([slide]).
 * The tabs switch first, the page that comes in composes off screen, then it slides: into Private
 * from the right, back to normal from the left. Entering Private, the stars appear one by one from
 * the right. Everything is read in draw and layer lambdas only.
 * Reduced motion: a 150 ms slide, no twinkle.
 */
@Stable
class PrivateFlip(private val scope: CoroutineScope, initial: Boolean) {
    /** The slide: 0 = old overview in place … 1 = new one in place. At rest it sits at 1. */
    val slide = Animatable(1f)

    /** Ms since the slide started, for the stars; at rest it sits at the end. */
    private val clock = Animatable(END_MS)

    private val knobPos = Animatable(if (initial) 1f else 0f)

    /** Where the knob is going (and stays): Private on. */
    var target by mutableStateOf(initial)
        private set

    /** The dock shows Private's texts (they switch a third of the way in). */
    var labels by mutableStateOf(initial)
        private set

    /** The overview shows the private tabs, in Private's palette (switched before anything moves). */
    var data by mutableStateOf(initial)
        private set

    /** A flip is running: taps on the toggle are ignored meanwhile. */
    var running by mutableStateOf(false)
        private set

    /** Animations off on the phone (developer options or "Remove animations"). */
    var reduced = false

    /** Snaps to [on] with no motion (Private left from elsewhere, e.g. a lock cancelled). */
    fun snap(on: Boolean) {
        target = on
        labels = on
        data = on
        scope.launch {
            slide.snapTo(1f)
            clock.snapTo(END_MS)
            knobPos.snapTo(if (on) 1f else 0f)
        }
    }

    /** Runs the flip to [on]; [onData] first (the caller swaps its data then), before anything moves. */
    fun run(on: Boolean, onData: () -> Unit) {
        if (busy || on == target) return
        busy = true
        target = on
        scope.launch { knobPos.animateTo(if (on) 1f else 0f, tween(if (reduced) 0 else KNOB_MS)) }
        scope.launch {
            try {
                slide.snapTo(0f)
                clock.snapTo(0f)
                running = true
                data = on
                onData()
                // The page that comes in composes (off screen) before anything moves: wait for
                // two quick frames in a row.
                awaitCalm()
                val stars = if (reduced) null else launch { clock.animateTo(END_MS, tween(END_MS.toInt(), easing = LinearEasing)) }
                slide.animateTo(1f, if (reduced) tween(PrivateTokens.REDUCED_MS) else Motion.PagerSettle) {
                    if (labels != on && value >= PrivateTokens.LABELS_AT) labels = on
                }
                labels = on
                stars?.join()
            } finally {
                labels = on
                data = on
                running = false
                busy = false
            }
        }
    }

    private var busy = false

    /** Waits until two frames in a row came quickly, at most [CALM_MAX_FRAMES] frames. */
    private suspend fun awaitCalm() {
        var last = withFrameNanos { it }
        var calm = 0
        repeat(CALM_MAX_FRAMES) {
            val now = withFrameNanos { it }
            calm = if (now - last < CALM_FRAME_NS) calm + 1 else 0
            last = now
            if (calm >= 2) return
        }
    }

    /** The slide, clamped (the spring overshoots a little). */
    private val p get() = slide.value.coerceIn(0f, 1f)

    /** 0 = normal … 1 = private: the dock's colour and anything that simply crossfades. */
    fun colour(): Float {
        if (!running) return if (target) 1f else 0f
        return if (target) p else 1f - p
    }

    /** The knob: 0 = off … 1 = on. */
    fun knob(): Float = knobPos.value

    /** The dock's texts: they dip out and back in around [PrivateTokens.LABELS_AT], where they switch. */
    fun textAlpha(): Float {
        if (!running) return 1f
        return (abs(p - PrivateTokens.LABELS_AT) / PrivateTokens.LABELS_AT).coerceIn(0f, 1f)
    }

    /**
     * A star or sparkle at [x] (0 = left … 1 = right): alpha factor, scale. Entering Private they
     * appear right to left with a small twinkle; leaving, the normal background simply covers them.
     */
    fun star(x: Float): Pair<Float, Float> {
        if (!running || !target) return 1f to 1f
        if (reduced) return p to 1f
        val start = PrivateTokens.STARS_FROM_MS + PrivateTokens.STARS_SPREAD_MS * (1f - x.coerceIn(0f, 1f))
        val f = ((clock.value - start) / PrivateTokens.STAR_IN_MS).coerceIn(0f, 1f)
        if (f <= 0f) return 0f to 0f
        return f to 0.4f + 0.6f * f + 0.45f * sin(PI.toFloat() * f)
    }

    companion object {
        private const val KNOB_MS = 200
        private const val CALM_MAX_FRAMES = 12
        private const val CALM_FRAME_NS = 20_000_000L

        /** The star clock's length: the leftmost star's appearance ends here. */
        const val END_MS = (PrivateTokens.STARS_FROM_MS + PrivateTokens.STARS_SPREAD_MS + PrivateTokens.STAR_IN_MS).toFloat()
    }
}
