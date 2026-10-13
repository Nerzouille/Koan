package dev.nerzouille.koan.ui.overview

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The depth stack's transforms as pure functions of `d`, the card's distance in slots from the
 * focus line (docs/UI_SPEC.md "Depth stack"). d = 0 focused, d > 0 behind (above), d < 0 in front.
 *
 * Keyframes sit at d = -1, 0, 1 … 7 and are joined with monotone cubic splines, so every property
 * is smooth (no velocity kinks when a card crosses a slot) and never overshoots its keyframes.
 */
object DepthCurve {
    /** How far a card in front of the focus sits below it, per slot. "Full peek" of the focused card. */
    const val PEEK_DP = 220f

    /**
     * Distance of the card's top edge above the focus line, in dp. Negative = below. Spacing is ~1.5×
     * the spec table so the deck fills the screen up to the header on a 6.6" phone.
     */
    val offsetDp = MonotoneSpline(-1f, floatArrayOf(-PEEK_DP, 0f, 150f, 255f, 330f, 380f, 412f, 432f, 445f), clamp = false)

    val scale = MonotoneSpline(-1f, floatArrayOf(1f, 1f, 0.92f, 0.85f, 0.79f, 0.74f, 0.72f, 0.71f, 0.70f))

    /** Degrees; negative tilts the top edge away from the viewer. */
    val rotationX = MonotoneSpline(-1f, floatArrayOf(0f, 0f, -6f, -10f, -13f, -15f, -15f, -15f, -15f))

    /** Black overlay alpha on the thumbnail (1 - brightness). */
    val dim = MonotoneSpline(-1f, floatArrayOf(0f, 0f, 0.12f, 0.26f, 0.40f, 0.55f, 0.70f, 0.85f, 1f))

    /** Thumbnail blur radius in dp. Above the spec's floor on purpose: v2 back cards are clearly soft. */
    val blurDp = MonotoneSpline(-1f, floatArrayOf(0f, 0f, 2f, 5f, 8f, 10f, 12f, 12f, 12f))

    /** Whole-card alpha: fully visible to d = 4, gone at d = 7. */
    val alpha = MonotoneSpline(-1f, floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 0.6f, 0.2f, 0f))

    /** Shadow strength, 1 = focused card. */
    val shadow = MonotoneSpline(-1f, floatArrayOf(1f, 1f, 0.55f, 0.3f, 0.15f, 0.05f, 0f, 0f, 0f))

    /** Header strip overlay alpha: dimmed to 80% at most, so titles stay legible down to d = 3. */
    fun headerDim(d: Float): Float = (dim(d) * 0.5f).coerceAtMost(0.2f)

    /** Cards further back than this are invisible: skip drawing and hit-testing them. */
    const val MAX_VISIBLE_D = 7f
}

/**
 * Where a released stack settles: the nearest stop after carrying the fling a bit further.
 * Stops are the integer slots in [0, last], plus [revealStop] (< 0) for the expanded header block.
 */
fun snapTarget(pos: Float, velocitySlotsPerSec: Float, last: Int, revealStop: Float): Float {
    val projected = pos + velocitySlotsPerSec * FLING_CARRY_SECONDS
    if (projected < revealStop / 2f) return revealStop
    return projected.roundToInt().coerceIn(0, last.coerceAtLeast(0)).toFloat()
}

private const val FLING_CARRY_SECONDS = 0.18f

/** Monotone cubic Hermite spline (Fritsch–Carlson) through evenly spaced keyframes at x0, x0+1, … */
class MonotoneSpline(private val x0: Float, private val ys: FloatArray, private val clamp: Boolean = true) {
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

    operator fun invoke(x: Float): Float {
        val last = ys.size - 1
        val u = x - x0
        if (u <= 0f) return if (clamp) ys[0] else ys[0] + m[0] * u
        if (u >= last) return if (clamp) ys[last] else ys[last] + m[last] * (u - last)
        val i = u.toInt()
        val t = u - i
        val t2 = t * t
        val t3 = t2 * t
        return (2 * t3 - 3 * t2 + 1) * ys[i] + (t3 - 2 * t2 + t) * m[i] +
            (-2 * t3 + 3 * t2) * ys[i + 1] + (t3 - t2) * m[i + 1]
    }
}
