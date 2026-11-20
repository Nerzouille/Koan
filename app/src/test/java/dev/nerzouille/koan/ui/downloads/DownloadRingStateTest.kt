package dev.nerzouille.koan.ui.downloads

import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRingStateTest {
    private fun d(id: String, status: Status, length: Long? = 100, copied: Long = 50, private: Boolean = false) =
        DownloadState("https://example.com/$id.zip", fileName = "$id.zip", contentLength = length, currentBytesCopied = copied, status = status, id = id, private = private, directoryPath = "/downloads")

    @Test
    fun finishingInTheSameUpdateThatEndsTheProgressTicks() {
        val running = nextRingState(DownloadRingState(), listOf(d("a", Status.DOWNLOADING)), privateOn = false)
        assertEquals(0.5f, running.progress!!, 0.001f)
        val done = nextRingState(running, listOf(d("a", Status.COMPLETED, copied = 100)), privateOn = false)
        assertNull(done.progress)
        assertEquals(1, done.doneTick)
        // Later updates (the list, another field) don't tick again.
        assertEquals(1, nextRingState(done, listOf(d("a", Status.COMPLETED, copied = 100)), privateOn = false).doneTick)
    }

    @Test
    fun failedCancelledOrNeverSeenRunningDoesNotTick() {
        val running = nextRingState(DownloadRingState(), listOf(d("a", Status.DOWNLOADING)), privateOn = false)
        assertEquals(0, nextRingState(running, listOf(d("a", Status.FAILED)), privateOn = false).doneTick)
        assertEquals(0, nextRingState(running, emptyList(), privateOn = false).doneTick)
        // Restored from disk already completed: no check.
        assertEquals(0, nextRingState(DownloadRingState(), listOf(d("b", Status.COMPLETED)), privateOn = false).doneTick)
    }

    @Test
    fun oneOfTwoFinishingTicksWhileTheOtherKeepsTheProgress() {
        val both = nextRingState(DownloadRingState(), listOf(d("a", Status.DOWNLOADING), d("b", Status.DOWNLOADING, length = null)), privateOn = false)
        assertTrue(both.progress!!.isNaN())
        val one = nextRingState(both, listOf(d("a", Status.COMPLETED), d("b", Status.DOWNLOADING, length = 200, copied = 20)), privateOn = false)
        assertEquals(1, one.doneTick)
        assertEquals(0.1f, one.progress!!, 0.001f)
    }

    @Test
    fun privateDownloadsOnlyInPrivate() {
        val list = listOf(d("p", Status.DOWNLOADING, private = true))
        assertNull(nextRingState(DownloadRingState(), list, privateOn = false).progress)
        val running = nextRingState(DownloadRingState(), list, privateOn = true)
        assertEquals(1, nextRingState(running, listOf(d("p", Status.COMPLETED, private = true)), privateOn = true).doneTick)
    }
}
