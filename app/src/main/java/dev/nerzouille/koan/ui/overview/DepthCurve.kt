package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.ui.theme.StackTokens
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The depth stack's per-card values as pure functions of `d`, the card's distance in slots from
 * the focus (docs/UI_SPEC.md "Depth stack"). d = 0 focused, d > 0 behind (in the pile above),
 * d < 0 in front (the peek below, then gone).
 *
 * Behind the focus the deck is a one-point perspective: with ρ = rᵈ, the card rises by A·(1 − ρ)
 * and scales to sMin + (1 − sMin)·ρ, so spacing and size shrink geometrically towards a vanishing
 * point. Cards are always upright.
 */
object DepthCurve {
    private val r = StackTokens.R
    private val sMin = StackTokens.S_MIN

    /** 0 at the focus, → 1 deep in the pile: the fraction of the pile depth A a card has risen. */
    fun rise(d: Float): Float = if (d <= 0f) 0f else 1f - r.pow(d)

    /** 0 at the focus, 1 at the peek slot (d = −1) and beyond. */
    fun drop(d: Float): Float = (-d).coerceIn(0f, 1f)

    fun scale(d: Float): Float =
        if (d >= 0f) sMin + (1f - sMin) * r.pow(d) else 1f - (1f - StackTokens.PEEK_SCALE) * drop(d)

    /** Black overlay alpha on the thumbnail (1 − brightness). */
    fun dim(d: Float): Float = if (d >= 0f) dimSpline(d) else StackTokens.PEEK_DIM * drop(d)

    fun blurDp(d: Float): Float = if (d <= 0f) 0f else blurSpline(d)

    /** Whole-card alpha: the pile fades out far back, cards past the peek fade out quickly. */
    fun alpha(d: Float): Float =
        if (d >= 0f) alphaSpline(d) else (1f - (-1f - d) / StackTokens.FRONT_FADE_SLOTS).coerceIn(0f, 1f)

    fun shadow(d: Float): Float = if (d >= 0f) shadowSpline(d) else 1f

    /** Header strip overlay alpha: half the thumbnail dim, capped, so titles stay legible. */
    fun headerDim(d: Float): Float = (dim(d) * 0.5f).coerceAtMost(StackTokens.HEADER_DIM_MAX)

    /** Cards further back than this are invisible: skip drawing and hit-testing them. */
    val MAX_VISIBLE_D = (StackTokens.Alpha.size - 1).toFloat()

    /** Cards further in front than this are invisible. */
    val MIN_VISIBLE_D = -1f - StackTokens.FRONT_FADE_SLOTS

    private val dimSpline = MonotoneSpline(0f, StackTokens.Dim)
    private val blurSpline = MonotoneSpline(0f, StackTokens.BlurDp)
    private val alphaSpline = MonotoneSpline(0f, StackTokens.Alpha)
    private val shadowSpline = MonotoneSpline(0f, StackTokens.Shadow)
}

/**
 * Where a released stack settles. A release that moved more than [StackTokens.SNAP_COMMIT] slots
 * from [start] (fling carry included) commits to the next card in that direction, so a short
 * flick always changes card; otherwise it springs back. Stops are the integer slots in [0, last],
 * plus [revealStop] (< 0) for the revealed essentials/pinned block.
 */
fun snapTarget(pos: Float, velocitySlotsPerSec: Float, start: Float, last: Int, revealStop: Float): Float {
    val projected = pos + velocitySlotsPerSec * StackTokens.FLING_CARRY_SECONDS
    val moved = projected - start
    val target = when {
        moved > StackTokens.SNAP_COMMIT -> max(ceil(pos), projected.roundToInt().toFloat())
        moved < -StackTokens.SNAP_COMMIT -> min(floor(pos), projected.roundToInt().toFloat())
        else -> start.roundToInt().toFloat()
    }
    if (target < 0f) return revealStop
    // + 0f turns ceil's -0.0 into 0.0.
    return (target + 0f).coerceAtMost(last.coerceAtLeast(0).toFloat())
}

/** Monotone cubic Hermite spline (Fritsch–Carlson) through evenly spaced keyframes at x0, x0+1, … */
class MonotoneSpline(private val x0: Float, private val ys: FloatArray) {
    private val m = FloatArray(ys.size)

    init {
        require(ys.size >= 2)
        val n = ys.size
        val delta = FloatArray(n - 1) { ys[it + 1] - ys[it] }
        m[0] = delta[0]
        m[n - 1] = delta[n - 2]
        for (i in 1 until n - 1) {
            m[i] = if (delta[i - 1] * delta[i] <= 0f) 0f else (delta[i - 1] + delta[i]) / 2f
        }
        for (i in 0 until n - 1) {
            if (delta[i] == 0f) {
                m[i] = 0f
                m[i + 1] = 0f
            } else {
                val a = m[i] / delta[i]
                val b = m[i + 1] / delta[i]
                val s = a * a + b * b
                if (s > 9f) {
                    val t = 3f / sqrt(s)
                    m[i] = t * a * delta[i]
                    m[i + 1] = t * b * delta[i]
                }
            }
        }
    }

    /** Clamped to the first/last keyframe outside the range. */
    operator fun invoke(x: Float): Float {
        val last = ys.size - 1
        val u = x - x0
        if (u <= 0f) return ys[0]
        if (u >= last) return ys[last]
        val i = u.toInt()
        val t = u - i
        val t2 = t * t
        val t3 = t2 * t
        return (2 * t3 - 3 * t2 + 1) * ys[i] + (t3 - 2 * t2 + t) * m[i] +
            (-2 * t3 + 3 * t2) * ys[i + 1] + (t3 - t2) * m[i + 1]
    }
}
