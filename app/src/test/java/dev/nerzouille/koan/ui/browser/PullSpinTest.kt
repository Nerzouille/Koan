package dev.nerzouille.koan.ui.browser

import dev.nerzouille.koan.ui.theme.PullTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The K's turn: a flick that slows down, small uneven pushes while loading, never back to full speed. */
class PullSpinTest {
    @Test fun `the flick slows down on its own`() {
        val slower = PullSpin.decayed(PullTokens.K_FLICK_DEG_PER_S, 100f)
        assertTrue(slower < PullTokens.K_FLICK_DEG_PER_S)
        assertTrue(PullSpin.decayed(PullTokens.K_FLICK_DEG_PER_S, 3000f) < 2f)
    }

    @Test fun `a push is small and never reaches the flick's speed`() {
        assertEquals(PullTokens.K_PUSH_MIN, PullSpin.pushed(0f, 0f), 1e-3f)
        assertTrue(PullSpin.pushed(0f, 1f) <= PullTokens.K_PUSH_CAP)
        assertTrue(PullSpin.pushed(400f, 1f) <= PullTokens.K_PUSH_CAP)
        assertTrue(PullTokens.K_PUSH_CAP < PullTokens.K_FLICK_DEG_PER_S / 3f)
    }

    @Test fun `while loading it gets uneven pushes, the blobs drift with it, loaded it only slows`() {
        val spin = PullSpin(Random(7))
        spin.flick()
        val pushesAt = mutableListOf<Int>()
        var last = spin.speed
        for (frame in 1..1200) {
            spin.step(8f, loading = true)
            if (spin.speed > last + 1f) {
                pushesAt += frame
                assertTrue("pushed too far at $frame: ${spin.speed}", spin.speed <= PullTokens.K_PUSH_CAP)
            }
            last = spin.speed
        }
        assertTrue("too few pushes: $pushesAt", pushesAt.size >= 5)
        val gaps = pushesAt.zipWithNext { a, b -> b - a }
        assertTrue("even intervals: $gaps", gaps.toSet().size > 1)
        assertTrue(spin.drift > 0f)
        // Loaded: no more pushes.
        last = spin.speed
        repeat(300) {
            spin.step(8f, loading = false)
            assertTrue(spin.speed <= last)
            last = spin.speed
        }
    }
}
