package dev.nerzouille.koan.ui.browser

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlin.math.exp
import kotlin.random.Random

/**
 * The K's turn while the page reloads, and the blobs' drift with it: one signal, [speed] (°/s).
 * Let go: a flick ([flick]) that slows down on its own. While the page still loads: small pushes
 * at uneven intervals, like the load line's shimmer, each one slowing down again and never back
 * to the flick's speed. Loaded: no more pushes, it keeps slowing while the page covers it.
 * [angle] and [drift] are read at draw time only.
 */
@Stable
class PullSpin(private val random: Random = Random.Default) {
    var speed by mutableFloatStateOf(0f)
        private set
    var angle by mutableFloatStateOf(0f)
        private set

    /** How far the blobs have drifted (radians of their slow circle), following the K's turn. */
    var drift by mutableFloatStateOf(0f)
        private set

    /** Time to the next push while loading (ms); none until the flick has slowed. */
    private var untilPush = 0f

    fun flick() {
        speed = PullTokens.K_FLICK_DEG_PER_S
        untilPush = nextPushIn() + PullTokens.K_FIRST_PUSH_DELAY_MS
    }

    /** The K and the blobs go still (remove animations, or nothing to show). */
    fun stop() {
        speed = 0f
    }

    /** [dtMs] later; [loading]: the page still loads, so pushes keep coming. */
    fun step(dtMs: Float, loading: Boolean) {
        speed = decayed(speed, dtMs)
        if (loading) {
            untilPush -= dtMs
            if (untilPush <= 0f) {
                speed = pushed(speed, random.nextFloat())
                untilPush = nextPushIn()
            }
        }
        val turn = speed * dtMs / MS_PER_S
        angle = (angle + turn) % FULL_TURN
        drift += turn * PullTokens.ARC_DRIFT_PER_DEG
    }

    private fun nextPushIn(): Float =
        PullTokens.K_PUSH_MIN_MS + random.nextFloat() * (PullTokens.K_PUSH_MAX_MS - PullTokens.K_PUSH_MIN_MS)

    companion object {
        /** [speed] after [dtMs]: it slows like a wheel, exponentially. */
        fun decayed(speed: Float, dtMs: Float): Float = speed * exp(-dtMs / PullTokens.K_DECAY_MS)

        /** A push ([r] 0…1 picks its size), small, never past [PullTokens.K_PUSH_CAP]. */
        fun pushed(speed: Float, r: Float): Float {
            val push = PullTokens.K_PUSH_MIN + r * (PullTokens.K_PUSH_MAX - PullTokens.K_PUSH_MIN)
            return (speed + push).coerceAtMost(maxOf(speed, PullTokens.K_PUSH_CAP))
        }

        private const val MS_PER_S = 1000f
        private const val FULL_TURN = 360f
    }
}
