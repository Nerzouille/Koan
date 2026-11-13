package dev.nerzouille.koan.ui.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PrivateFlipMathTest {
    /** Runs frames of [dt] s toward [goal] until settled; the frames it took. */
    private fun run(from: Float, v0: Float, goal: Float, dt: Float, max: Int = 2000): Triple<Float, Float, Int> {
        var p = from
        var v = v0
        repeat(max) { i ->
            val (np, nv) = springStep(p, v, goal, dt)
            p = np
            v = nv
            if (settled(p, v, goal)) return Triple(p, v, i + 1)
        }
        return Triple(p, v, max)
    }

    @Test fun `the spring settles on the goal in about the pager's time`() {
        val (p, _, frames) = run(0f, 0f, 1f, 1 / 120f)
        assertEquals(1f, p, 0.002f)
        val seconds = frames / 120f
        assertTrue("took $seconds s", seconds in 0.3f..1.2f)
    }

    @Test fun `turning around mid-way starts from where it is, at its speed`() {
        var p = 0f
        var v = 0f
        repeat(12) { springStep(p, v, 1f, 1 / 120f).let { p = it.first; v = it.second } }
        assertTrue("moving toward Private", p in 0.05f..0.9f && v > 0f)
        // Reversed: the next frame is still near the same place (no jump), then it heads back.
        val (p2, v2) = springStep(p, v, 0f, 1 / 120f)
        assertTrue("no jump: $p → $p2", abs(p2 - p) < 0.05f)
        assertTrue("slowing down", v2 < v)
        val (end, _, _) = run(p2, v2, 0f, 1 / 120f)
        assertEquals(0f, end, 0.002f)
    }

    @Test fun `a slow frame moves no further than a capped one`() {
        // The driver caps a frame at MAX_FRAME_S: a 400 ms stall advances like 20 ms, never to the end.
        val capped = springStep(0f, 0f, 1f, PrivateFlip.MAX_FRAME_S).first
        assertTrue("one capped frame: $capped", capped < 0.15f)
    }

    @Test fun `reduced motion goes straight there in 150 ms`() {
        var p = 0f
        var t = 0f
        while (p < 1f && t < 1f) {
            p = linearStep(p, 1f, 1 / 60f).first
            t += 1 / 60f
        }
        assertEquals(1f, p, 0f)
        assertTrue("took $t s", t in 0.14f..0.17f)
        assertEquals(0.5f, linearStep(0.6f, 0f, 0.015f).first, 0.001f)
    }
}
