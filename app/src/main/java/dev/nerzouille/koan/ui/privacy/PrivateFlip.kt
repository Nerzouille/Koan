package dev.nerzouille.koan.ui.privacy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.ui.theme.PrivateTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The Private on / off transition (owner, 2026-11-11): Private is one more page beside the space
 * we were on, and a flip is the space pager's own motion between the two. The screen is one
 * position, 0 = normal … 1 = Private, on the pager's spring: into Private from the right, back to
 * normal from the left. A toggle always acts at once: a new one mid-flip turns the motion around
 * from where it is, speed kept, and the screen ends on the latest state (owner, 2026-11-12; it
 * used to ignore taps for ~1.4 s). The spring advances at most [MAX_FRAME_S] per frame, so a slow
 * frame (the tabs switching) slows the motion instead of skipping it. Entering Private, the stars
 * appear one by one from the right, on their own clock (they never hold the toggle).
 * Everything is read in draw and layer lambdas only.
 * Reduced motion: a 150 ms slide, no twinkle.
 */
@Stable
class PrivateFlip(private val scope: CoroutineScope, initial: Boolean) {
    /** Where the screen is: 0 = normal … 1 = Private (the spring overshoots a little). */
    private var pos by mutableFloatStateOf(if (initial) 1f else 0f)
    private var velocity = 0f

    /** Ms since entering Private started, for the stars; at rest it sits at the end. */
    private val clock = Animatable(END_MS)

    private val knobPos = Animatable(if (initial) 1f else 0f)

    /** Where the knob is going (and stays): Private on. The screen follows it. */
    var target by mutableStateOf(initial)
        private set

    /** The dock shows Private's texts (they switch a third of the way in). */
    var labels by mutableStateOf(initial)
        private set

    /** The overview shows the private tabs, in Private's palette (switched before anything moves). */
    var data by mutableStateOf(initial)
        private set

    /** The screen is between the two modes. */
    var running by mutableStateOf(false)
        private set

    /** Animations off on the phone (developer options or "Remove animations"). */
    var reduced = false

    /** The fingerprint is being asked before entering (its prompt covers the screen meanwhile). */
    var unlocking = false

    private var driver: Job? = null
    private var stars: Job? = null

    /** Snaps to [on] with no motion (Private left from elsewhere, e.g. a link from another app). */
    fun snap(on: Boolean) {
        driver?.cancel()
        stars?.cancel()
        target = on
        labels = on
        data = on
        pos = if (on) 1f else 0f
        velocity = 0f
        running = false
        scope.launch {
            clock.snapTo(END_MS)
            knobPos.snapTo(if (on) 1f else 0f)
        }
    }

    /** Only the knob, ahead of the screen (the fingerprint is asked first). */
    fun knobTo(on: Boolean) {
        scope.launch { knobPos.animateTo(if (on) 1f else 0f, tween(if (reduced) 0 else KNOB_MS)) }
    }

    /**
     * Goes to [on] from wherever the screen is; [onData] first (the caller swaps its data then).
     * Called again mid-flip, the motion turns around from where it is.
     */
    fun run(on: Boolean, onData: () -> Unit) {
        if (on == target) return
        target = on
        knobTo(on)
        data = on
        onData()
        if (on && !reduced) {
            // The stars come in again only from a screen that was (nearly) all normal.
            stars?.cancel()
            stars = scope.launch {
                if (pos < STARS_RESTART_BELOW) clock.snapTo(0f)
                clock.animateTo(END_MS, tween(((END_MS - clock.value).coerceAtLeast(0f)).toInt(), easing = LinearEasing))
            }
        }
        if (driver?.isActive == true) return
        running = true
        driver = scope.launch {
            try {
                drive()
            } finally {
                pos = if (target) 1f else 0f
                velocity = 0f
                labels = target
                data = target
                running = false
            }
        }
    }

    /** One spring step per frame, toward [target] as it is on that frame. */
    private suspend fun drive() {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, MAX_FRAME_S)
            last = now
            val goal = if (target) 1f else 0f
            val (p, v) = if (reduced) linearStep(pos, goal, dt) else springStep(pos, velocity, goal, dt)
            pos = p
            velocity = v
            val switchAt = if (target) PrivateTokens.LABELS_AT else 1f - PrivateTokens.LABELS_AT
            labels = if (target) pos >= switchAt else pos > switchAt
            if (settled(pos, velocity, goal)) return
        }
    }

    /** The position, clamped (the spring overshoots a little). */
    private val p get() = pos.coerceIn(0f, 1f)

    /** 0 = normal … 1 = private: the overview's pages, the dock's colour, anything that crossfades. */
    fun colour(): Float = if (!running) (if (target) 1f else 0f) else p

    /** The knob: 0 = off … 1 = on. */
    fun knob(): Float = knobPos.value

    /** The dock's texts: they dip out and back in around where they switch. */
    fun textAlpha(): Float {
        if (!running) return 1f
        val switchAt = if (target) PrivateTokens.LABELS_AT else 1f - PrivateTokens.LABELS_AT
        return (abs(p - switchAt) / PrivateTokens.LABELS_AT).coerceIn(0f, 1f)
    }

    /**
     * A star or sparkle at [x] (0 = left … 1 = right): alpha factor, scale. Entering Private they
     * appear right to left with a small twinkle; leaving, the normal background simply covers them.
     */
    fun star(x: Float): Pair<Float, Float> {
        if (!target || clock.value >= END_MS) return 1f to 1f
        if (reduced) return p to 1f
        val start = PrivateTokens.STARS_FROM_MS + PrivateTokens.STARS_SPREAD_MS * (1f - x.coerceIn(0f, 1f))
        val f = ((clock.value - start) / PrivateTokens.STAR_IN_MS).coerceIn(0f, 1f)
        if (f <= 0f) return 0f to 0f
        return f to 0.4f + 0.6f * f + 0.45f * sin(PI.toFloat() * f)
    }

    companion object {
        private const val KNOB_MS = 200

        /** The longest step one frame may take (s): a slower frame slows the motion, never skips it. */
        const val MAX_FRAME_S = 0.020f

        private const val STARS_RESTART_BELOW = 0.05f

        /** The star clock's length: the leftmost star's appearance ends here. */
        const val END_MS = (PrivateTokens.STARS_FROM_MS + PrivateTokens.STARS_SPREAD_MS + PrivateTokens.STAR_IN_MS).toFloat()
    }
}

/** The pager's spring (Motion.PagerSettle: damping 0.86, stiffness 300), per unit of travel. */
private const val STIFFNESS = 300f
private const val DAMPING_RATIO = 0.86f
private const val SUBSTEP_S = 0.004f

/**
 * One spring step of [dt] seconds from [pos] at [velocity] toward [goal]: the new position and
 * velocity. Small sub-steps keep it stable for any frame length.
 */
internal fun springStep(pos: Float, velocity: Float, goal: Float, dt: Float): Pair<Float, Float> {
    val damping = 2f * DAMPING_RATIO * sqrt(STIFFNESS)
    var x = pos
    var v = velocity
    var left = dt
    while (left > 0f) {
        val h = minOf(SUBSTEP_S, left)
        v += (-STIFFNESS * (x - goal) - damping * v) * h
        x += v * h
        left -= h
    }
    return x to v
}

/** Reduced motion: straight to [goal] in [PrivateTokens.REDUCED_MS]. */
internal fun linearStep(pos: Float, goal: Float, dt: Float): Pair<Float, Float> {
    val step = dt * 1000f / PrivateTokens.REDUCED_MS
    val x = if (pos < goal) minOf(goal, pos + step) else maxOf(goal, pos - step)
    return x to 0f
}

/** Close enough to rest: the screen lands on [goal]. */
internal fun settled(pos: Float, velocity: Float, goal: Float) = abs(pos - goal) < 0.001f && abs(velocity) < 0.01f
