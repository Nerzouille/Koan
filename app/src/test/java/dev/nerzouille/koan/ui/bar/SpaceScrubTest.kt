package dev.nerzouille.koan.ui.bar

import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceScrubTest {
    private fun scrub() = SpaceScrub(TestScope()).apply {
        minStepPx = 24f
        maxStepPx = 90f
    }

    /** Drags in small moves (like a finger) by [totalPx]; the highlighted space after. */
    private fun SpaceScrub.drag(totalPx: Float): Int {
        val moves = 100
        repeat(moves) { dragBy(totalPx / moves) }
        return index
    }

    @Test fun `an edge reaches the first and the last space whatever the count`() {
        for (count in listOf(2, 5, 12, 40)) {
            val s = scrub()
            s.begin(current = count / 2, count = count, fingerX = 300f, widthPx = 1000f)
            assertEquals(0, s.drag(-300f))
            val t = scrub()
            t.begin(current = count / 2, count = count, fingerX = 300f, widthPx = 1000f)
            assertEquals(count - 1, t.drag(700f))
        }
    }

    @Test fun `steps are capped for a few spaces`() {
        val s = scrub()
        // One space on the right, 700 px away: one step is 90 px, not the whole way.
        s.begin(current = 0, count = 2, fingerX = 300f, widthPx = 1000f)
        assertEquals(0, s.drag(40f))
        assertEquals(1, s.drag(10f))
    }
}
