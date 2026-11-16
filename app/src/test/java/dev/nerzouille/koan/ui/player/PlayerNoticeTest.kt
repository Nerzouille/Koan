package dev.nerzouille.koan.ui.player

import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNoticeTest {
    private fun d(status: Status, length: Long? = null, name: String? = null) =
        DownloadState("https://example.com/v/clip.webm", fileName = name, contentLength = length, status = status, directoryPath = "/downloads")

    @Test fun `nothing until the size is known`() {
        assertNull(downloadNotice(null, "clip.webm"))
        assertNull(downloadNotice(d(Status.INITIATED), "clip.webm"))
        assertNull(downloadNotice(d(Status.DOWNLOADING), "clip.webm"))
    }

    @Test fun `downloading with its size and name`() {
        val n = downloadNotice(d(Status.DOWNLOADING, length = 3L shl 30, name = "Bunny.webm"), "clip.webm")!!
        assertEquals("Bunny.webm", n.title)
        assertTrue(n.detail!!.startsWith("Downloading · 3"))
        assertTrue(n.detail!!.endsWith("GB"))
    }

    @Test fun `a failure says so`() {
        val n = downloadNotice(d(Status.FAILED), "clip.webm")!!
        assertTrue(n.error)
        assertEquals("clip.webm", n.detail)
    }

    @Test fun `file names from addresses`() {
        assertEquals("Big Buck Bunny.webm", videoFileName("https://example.org/t/Big%20Buck%20Bunny.webm?x=1"))
        assertEquals("Video", videoFileName("https://example.org/"))
    }

    @Test fun `a streamed video downloads the plain file its page lists`() {
        val file = "https://example.org/transcoded/clip.webm/clip.webm.1080p.vp9.webm"
        assertEquals(VideoSave.File(file), videoSave("", drm = false, kind = "stream", live = false, file = file))
        assertEquals(VideoSave.No("Streamed video, can't be saved"), videoSave("", false, "stream", false, file = ""))
        assertEquals(VideoSave.No("Protected video"), videoSave("", drm = true, kind = "stream", live = false, file = file))
    }
}
