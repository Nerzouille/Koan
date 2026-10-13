package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.ui.theme.StackTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthCurveTest {
    @Test
    fun `pile follows the geometric perspective curve`() {
        assertEquals(1f, DepthCurve.scale(0f), 1e-5f)
        // Design: scale(d) = 0.62 + 0.38·0.55ᵈ → .83 · .73 · .68 · .65
        assertEquals(0.829f, DepthCurve.scale(1f), 1e-3f)
        assertEquals(0.735f, DepthCurve.scale(2f), 1e-3f)
        assertEquals(0.683f, DepthCurve.scale(3f), 1e-3f)
        assertEquals(0f, DepthCurve.rise(0f), 1e-5f)
        assertEquals(0.45f, DepthCurve.rise(1f), 1e-4f)
        assertEquals(1f - 0.55f * 0.55f, DepthCurve.rise(2f), 1e-4f)
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
    fun `peek card is slightly smaller and cards past it fade out`() {
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
        assertEquals(4f, snapTarget(3.2f, 0f, start = 3f, last = 10, revealStop = -1f))
        assertEquals(2f, snapTarget(2.8f, 0f, start = 3f, last = 10, revealStop = -1f))
        assertEquals(3f, snapTarget(3.1f, 0f, start = 3f, last = 10, revealStop = -1f))
        assertEquals(3f, snapTarget(2.9f, 0f, start = 3f, last = 10, revealStop = -1f))
    }

    @Test
    fun `fling carries further and clamps to the ends`() {
        assertEquals(10f, snapTarget(9.8f, 20f, start = 9f, last = 10, revealStop = -1f))
        assertEquals(6f, snapTarget(3.5f, 15f, start = 3f, last = 10, revealStop = -1f))
        assertEquals(0f, snapTarget(0.3f, 0f, start = 0.4f, last = -1, revealStop = -1f))
    }

    @Test
    fun `pulling down past the first card reveals the header block`() {
        assertEquals(-1f, snapTarget(-0.3f, 0f, start = 0f, last = 10, revealStop = -1f))
        assertEquals(-1f, snapTarget(-1.2f, 0f, start = -1f, last = 10, revealStop = -1f))
        assertEquals(0f, snapTarget(-0.8f, 0f, start = -1f, last = 10, revealStop = -1f))
    }
}
