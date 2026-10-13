package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.ui.theme.StackTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthCurveTest {
    @Test
    fun `pile follows the geometric perspective curve`() {
        assertEquals(1f, DepthCurve.scale(0f), 1e-5f)
        // Sketch: scale(d) = 0.58 + 0.42·0.52ᵈ → .80 · .69 · .64
        assertEquals(0.798f, DepthCurve.scale(1f), 1e-3f)
        assertEquals(0.694f, DepthCurve.scale(2f), 1e-3f)
        assertEquals(0.639f, DepthCurve.scale(3f), 1e-3f)
        assertEquals(0f, DepthCurve.rise(0f), 1e-5f)
        assertEquals(0.55f, DepthCurve.rise(1f), 1e-4f)
        assertEquals(1f - 0.45f * 0.45f, DepthCurve.rise(2f), 1e-4f)
    }

    @Test
    fun `rise and scale are monotone and converge`() {
        var prevRise = -1f
        var prevScale = 2f
        var d = -2f
        while (d <= 12f) {
            val rise = DepthCurve.rise(d)
            val scale = DepthCurve.scale(d)
            assertTrue("rise must not fall at d=$d", rise >= prevRise)
            if (d >= 0f) assertTrue("scale must shrink with depth at d=$d", scale <= prevScale)
            prevRise = rise
            prevScale = scale
            d += 0.05f
        }
        assertTrue(DepthCurve.scale(12f) > StackTokens.S_MIN)
        assertTrue(DepthCurve.rise(12f) < 1f)
    }

    @Test
    fun `card in front is slightly bigger and cards past it fade out`() {
        assertEquals(StackTokens.PEEK_SCALE, DepthCurve.scale(-1f), 1e-5f)
        assertEquals(StackTokens.PEEK_SCALE, DepthCurve.scale(-1.5f), 1e-5f)
        assertEquals(1f, DepthCurve.alpha(-1f), 1e-5f)
        assertEquals(0f, DepthCurve.alpha(DepthCurve.MIN_VISIBLE_D), 1e-5f)
    }

    @Test
    fun `values stay in range`() {
        var d = -2f
        while (d <= 10f) {
            assertTrue(DepthCurve.alpha(d) in 0f..1f)
            assertTrue(DepthCurve.dim(d) in 0f..1f)
            assertTrue(DepthCurve.blurDp(d) >= 0f)
            assertTrue(DepthCurve.headerDim(d) <= StackTokens.HEADER_DIM_MAX)
            d += 0.01f
        }
    }

    @Test
    fun `a short drag commits to the next card, a tiny one springs back`() {
        assertEquals(4f, snapTarget(3.2f, start = 3f, last = 10))
        assertEquals(2f, snapTarget(2.8f, start = 3f, last = 10))
        assertEquals(3f, snapTarget(3.1f, start = 3f, last = 10))
        assertEquals(3f, snapTarget(2.9f, start = 3f, last = 10))
    }

    @Test
    fun `snap never skips past where the finger left the stack`() {
        assertEquals(6f, snapTarget(5.4f, start = 3f, last = 10))
        assertEquals(1f, snapTarget(1.6f, start = 3f, last = 10))
    }

    @Test
    fun `snap clamps to the ends`() {
        assertEquals(10f, snapTarget(10.3f, start = 9f, last = 10))
        assertEquals(0f, snapTarget(-0.4f, start = 0f, last = 10))
        assertEquals(0f, snapTarget(0.3f, start = 0.4f, last = -1))
    }

    @Test
    fun `pile keyframes are joined linearly`() {
        assertEquals((StackTokens.Dim[1] + StackTokens.Dim[2]) / 2f, DepthCurve.dim(1.5f), 1e-5f)
    }

    @Test
    fun `a fling carries through several cards and lands on one`() {
        // (20 − 2.2) × 0.55 ≈ 10 cards further.
        assertEquals(13f, flingTarget(3.2f, 20f, last = 49))
        // A normal flick (≈ 7.5 slots/s) goes about 3 cards.
        assertEquals(13f, flingTarget(10f, 7.5f, last = 49))
        assertEquals(0f, flingTarget(8f, -40f, last = 49))
        assertEquals(49f, flingTarget(45f, 40f, last = 49))
    }

    @Test
    fun `a weak fling still reaches the next card`() {
        assertEquals(4f, flingTarget(3.1f, 0.1f, last = 49))
        assertEquals(2f, flingTarget(3f, -0.1f, last = 49))
    }
}
