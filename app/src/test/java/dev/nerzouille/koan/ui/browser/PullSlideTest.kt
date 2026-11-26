package dev.nerzouille.koan.ui.browser

import dev.nerzouille.koan.ui.theme.PullTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The prototypes' pull: 1 : 1 with the finger, a rubber band only near the end. */
class PullSlideTest {
    private val max = 900f

    @Test fun `the page follows the finger 1 to 1 up to the knee`() {
        val knee = max * PullTokens.SLIDE_KNEE
        assertEquals(0f, rubberBand(-20f, max))
        assertEquals(100f, rubberBand(100f, max), 1e-3f)
        assertEquals(knee, rubberBand(knee, max), 1e-3f)
    }

    @Test fun `then it slows down smoothly and never passes the max`() {
        var last = 0f
        var lastStep = Float.MAX_VALUE
        for (dy in 1..5000) {
            val y = rubberBand(dy.toFloat(), max)
            val step = y - last
            assertTrue("went back at $dy", y >= last)
            assertTrue("past the max at $dy", y <= max)
            assertTrue("jolted at $dy", step <= lastStep + 1e-3f)
            last = y
            lastStep = step
        }
    }
}
