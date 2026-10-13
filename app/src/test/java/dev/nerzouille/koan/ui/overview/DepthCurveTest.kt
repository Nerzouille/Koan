package dev.nerzouille.koan.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthCurveTest {
    @Test
    fun `keyframes are hit exactly`() {
        assertEquals(1f, DepthCurve.scale(0f), 1e-5f)
        assertEquals(0.92f, DepthCurve.scale(1f), 1e-5f)
        assertEquals(0.79f, DepthCurve.scale(3f), 1e-5f)
        assertEquals(150f, DepthCurve.offsetDp(1f), 1e-4f)
        assertEquals(-6f, DepthCurve.rotationX(1f), 1e-5f)
    }

    @Test
    fun `offset is monotone and extends linearly in front of the focus`() {
        var prev = DepthCurve.offsetDp(-3f)
        var d = -3f
        while (d <= 9f) {
            d += 0.05f
            val y = DepthCurve.offsetDp(d)
            assertTrue("offset must grow with depth at d=$d", y >= prev)
            prev = y
        }
        assertEquals(-2 * DepthCurve.PEEK_DP, DepthCurve.offsetDp(-2f), 1e-3f)
    }

    @Test
    fun `values never overshoot their keyframes`() {
        var d = -2f
        while (d <= 9f) {
            assertTrue(DepthCurve.scale(d) in 0.70f..1f)
            assertTrue(DepthCurve.alpha(d) in 0f..1f)
            assertTrue(DepthCurve.headerDim(d) <= 0.2f)
            d += 0.01f
        }
    }

    @Test
    fun `snap settles on whole slots and reveals the header on a strong pull`() {
        assertEquals(3f, snapTarget(3.2f, 0f, last = 10, revealStop = -1f))
        assertEquals(10f, snapTarget(9.8f, 20f, last = 10, revealStop = -1f))
        assertEquals(0f, snapTarget(0.1f, -1f, last = 10, revealStop = -1f))
        assertEquals(-1f, snapTarget(-0.4f, -3f, last = 10, revealStop = -1f))
        assertEquals(0f, snapTarget(0.3f, 0f, last = -1, revealStop = -1f))
    }
}
