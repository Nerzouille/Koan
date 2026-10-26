package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.ui.theme.StackTokens
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The depth stack's per-card values as pure functions of `d`, the card's distance in slots from
 * the focus (docs/UI_SPEC.md "Depth stack"). d = 0 focused, d > 0 behind (in the pile above),
 * d < 0 in front (the peek below, then gone).
 *
 * Behind the focus the deck is a one-point perspective: the card rises by A·(1 − rᵈ) and scales to
 * sMin + (1 − sMin)·r_sᵈ, so spacing and size shrink geometrically towards a vanishing point. The
 * card in front is closer to the viewer, so slightly bigger. Cards are always upright.
 */
object DepthCurve {
    private val r = StackTokens.R
    private val scaleR = StackTokens.SCALE_R
    private val sMin = StackTokens.S_MIN

    /** 0 at the focus, → 1 deep in the pile: the fraction of the pile depth A a card has risen. */
    fun rise(d: Float): Float = if (d <= 0f) 0f else 1f - r.pow(d)

    /** 0 at the focus, 1 at the peek slot (d = −1) and beyond. */
    fun drop(d: Float): Float = (-d).coerceIn(0f, 1f)

    fun scale(d: Float): Float =
        if (d >= 0f) sMin + (1f - sMin) * scaleR.pow(d) else 1f + (StackTokens.PEEK_SCALE - 1f) * drop(d)

    /** Black overlay alpha on the thumbnail (1 − brightness). */
    fun dim(d: Float): Float = if (d >= 0f) lerp(StackTokens.Dim, d) else StackTokens.PEEK_DIM * drop(d)

    fun blurDp(d: Float): Float = if (d <= 0f) 0f else lerp(StackTokens.BlurDp, d)

    /** Whole-card alpha: the pile fades out far back, cards past the peek fade out quickly. */
    fun alpha(d: Float): Float =
        if (d >= 0f) lerp(StackTokens.Alpha, d) else 1f

    /** Header strip overlay alpha: half the thumbnail dim, capped, so titles stay legible. */
    fun headerDim(d: Float): Float = (dim(d) * 0.5f).coerceAtMost(StackTokens.HEADER_DIM_MAX)

    /** Cards further back than this are invisible: skip drawing and hit-testing them. */
    val MAX_VISIBLE_D = (StackTokens.Alpha.size - 1).toFloat()

    /**
     * Cards further in front than this are invisible: past the peek a card slides straight down,
     * opaque, and is below the viewport (under the dock) by d = −2.
     */
    const val MIN_VISIBLE_D = -2f

    /** Linear interpolation through keyframes at d = 0, 1, 2 …, held at the last one. */
    private fun lerp(keys: FloatArray, d: Float): Float {
        if (d >= keys.size - 1) return keys.last()
        val i = d.toInt()
        val t = d - i
        return keys[i] + (keys[i + 1] - keys[i]) * t
    }
}

/**
 * Where a released stack settles (prototype rule, finger distance only): moved more than
 * [StackTokens.SNAP_COMMIT] slots since [start] → the next whole card in that direction from where
 * the finger left it; otherwise back to where the drag started. Always a card in [0, last].
 */
fun snapTarget(pos: Float, start: Float, last: Int): Float {
    val moved = pos - start
    val target = when {
        abs(moved) <= StackTokens.SNAP_COMMIT -> start.roundToInt().toFloat()
        moved > 0f -> ceil(pos)
        else -> floor(pos)
    }
    // + 0f turns ceil's -0.0 into 0.0.
    return (target + 0f).coerceIn(0f, last.coerceAtLeast(0).toFloat())
}

/**
 * Where a fling lands: (|velocity| − dead zone) × gain cards further (velocity capped), on the
 * nearest card, and at least the next card in the fling direction. Always a card in [0, last].
 */
fun flingTarget(pos: Float, velocitySlotsPerSec: Float, last: Int): Float {
    val speed = abs(velocitySlotsPerSec).coerceAtMost(StackTokens.FLING_MAX_VELOCITY)
    val cards = ((speed - StackTokens.FLING_DEADZONE) * StackTokens.FLING_GAIN).coerceAtLeast(1f)
    var target = (pos + sign(velocitySlotsPerSec) * cards).roundToInt().toFloat()
    if (velocitySlotsPerSec > 0f && target <= pos) target = floor(pos) + 1f
    if (velocitySlotsPerSec < 0f && target >= pos) target = ceil(pos) - 1f
    return (target + 0f).coerceIn(0f, last.coerceAtLeast(0).toFloat())
}

/**
 * Closing card [closed] of a stack whose last index is [last], focused on [focus]: true if the
 * stack follows the cards behind it (they move one slot forward with the focus), false if the
 * ones in front close the gap. For the focused card that is the side the user last scrolled
 * towards ([direction]: -1 behind, 1 in front, 0 not scrolled = behind), or the other one when
 * there is no card on that side.
 */
fun closeBringsBehind(closed: Int, focus: Int, last: Int, direction: Int): Boolean = when {
    closed != focus -> closed < focus
    direction > 0 -> closed == last && closed > 0
    else -> closed > 0
}
