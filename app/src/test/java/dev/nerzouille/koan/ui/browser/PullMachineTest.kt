package dev.nerzouille.koan.ui.browser

import dev.nerzouille.koan.ui.browser.PullEvent.Cancel
import dev.nerzouille.koan.ui.browser.PullEvent.Grab
import dev.nerzouille.koan.ui.browser.PullEvent.Loaded
import dev.nerzouille.koan.ui.browser.PullEvent.ReleasePast
import dev.nerzouille.koan.ui.browser.PullEvent.ReleaseShort
import dev.nerzouille.koan.ui.browser.PullEvent.Settled
import dev.nerzouille.koan.ui.browser.PullPhase.Idle
import dev.nerzouille.koan.ui.browser.PullPhase.Pulling
import dev.nerzouille.koan.ui.browser.PullPhase.Refreshing
import dev.nerzouille.koan.ui.browser.PullPhase.Settling
import dev.nerzouille.koan.ui.theme.PullTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pull to refresh's phases and when a touch becomes a pull (owner's bugs, 2026-11-26). */
class PullMachineTest {
    private fun run(vararg events: PullEvent, from: PullPhase = Idle) = events.fold(from) { p, e -> PullRules.next(p, e) }

    @Test fun `a full pull, idle to pulling to refreshing to settling to idle`() {
        assertEquals(Pulling, run(Grab))
        assertEquals(Refreshing, run(Grab, ReleasePast))
        assertEquals(Settling, run(Grab, ReleasePast, Loaded))
        assertEquals(Idle, run(Grab, ReleasePast, Loaded, Settled))
    }

    @Test fun `a short pull and a second finger go back up without a reload`() {
        assertEquals(Idle, run(Grab, ReleaseShort, Settled))
        assertEquals(Idle, run(Grab, Cancel, Settled))
    }

    @Test fun `pull after pull after pull, the same page always pulls again`() {
        var p = Idle
        repeat(10) {
            p = run(Grab, ReleasePast, Loaded, Settled, from = p)
            assertEquals(Idle, p)
        }
    }

    @Test fun `a fast double pull grabs the page while it is still settling back`() {
        val settling = run(Grab, ReleasePast, Loaded)
        assertEquals(Pulling, run(Grab, from = settling))
        // The first settle ending late doesn't end the second pull.
        assertEquals(Pulling, run(Grab, Settled, from = settling))
    }

    @Test fun `nothing but the finger ends a pull`() {
        // A load ending (another tab's, a late one) never takes the page from under the finger.
        assertEquals(Pulling, run(Grab, Loaded))
        assertEquals(Pulling, run(Grab, Settled))
    }

    @Test fun `a fast double pull takes the page again while it still reloads`() {
        val refreshing = run(Grab, ReleasePast)
        assertEquals(Pulling, run(Grab, from = refreshing))
        assertEquals(Refreshing, run(Grab, ReleasePast, from = refreshing))
        // Other events don't end a reload.
        assertEquals(Refreshing, run(ReleaseShort, Cancel, Settled, from = refreshing))
    }

    private fun start(
        dx: Float,
        dy: Float,
        touch: PageTouch = PageTouch.Free,
        waited: Long = 0,
        atTop: Boolean = true,
        phase: PullPhase = Idle,
    ) = PullRules.start(phase, atTop, dx, dy, slop = 10f, touch = touch, waitedMs = waited)

    @Test fun `a touch becomes a pull once it moves down past the slop`() {
        assertEquals(PullStart.Wait, start(2f, 6f))
        assertEquals(PullStart.Start, start(3f, 14f))
        assertTrue(start(0f, -14f) is PullStart.Reject)
        assertTrue(start(20f, 14f) is PullStart.Reject)
        assertTrue(start(0f, 14f, atTop = false) is PullStart.Reject)
        assertEquals(PullStart.Start, start(0f, 14f, phase = Refreshing))
        assertTrue(start(0f, 14f, phase = Pulling) is PullStart.Reject)
        assertEquals(PullStart.Start, start(0f, 14f, phase = Settling))
    }

    @Test fun `it waits a moment for the page's answer, then pulls, unless the page keeps the touch`() {
        assertEquals(PullStart.Wait, start(0f, 14f, PageTouch.Unknown, waited = 30))
        assertEquals(PullStart.Start, start(0f, 14f, PageTouch.Unknown, waited = PullTokens.PAGE_ANSWER_WAIT_MS))
        assertTrue(start(0f, 14f, PageTouch.Kept, waited = 30) is PullStart.Reject)
    }
}
