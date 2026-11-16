package dev.nerzouille.koan.ui.kit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A button's press never stays pressed, however it ends (the play button that stuck). */
class PressTest {
    private val log = mutableListOf<Boolean>()

    @Test fun `a lift ends the press and gives the lift back`() {
        val up = pressing({ log += it }) { "up" }
        assertEquals("up", up)
        assertEquals(listOf(true, false), log)
    }

    @Test fun `a cancel ends the press with no lift`() {
        val up = pressing<String>({ log += it }) { null }
        assertNull(up)
        assertEquals(listOf(true, false), log)
    }

    @Test fun `a press torn down mid-way (a recomposition, the button going away) is never left pressed`() = runTest {
        val waiting = CompletableDeferred<String>()
        var clicked = false
        val job = launch {
            val up = pressing({ log += it }) { waiting.await() }
            if (up != null) clicked = true
        }
        yield()
        assertEquals(listOf(true), log)
        job.cancel(CancellationException("gesture restarted"))
        job.join()
        assertEquals(listOf(true, false), log)
        assertFalse(clicked)
        assertTrue(job.isCancelled)
    }
}
