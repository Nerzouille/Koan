package dev.nerzouille.koan.ui.downloads

import mozilla.components.browser.state.state.content.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFilterTest {
    private val normal = DownloadState("https://example.com/a.pdf", id = "n", private = false, directoryPath = "/downloads")
    private val secret = DownloadState("https://example.com/b.pdf", id = "p", private = true, directoryPath = "/downloads")
    private val all = listOf(normal, secret)

    @Test fun `normal mode never sees a private download`() {
        assertEquals(listOf(normal), visibleDownloads(all, privateOn = false))
        // The pill is Private's only: ignored in normal mode.
        assertEquals(listOf(normal), visibleDownloads(all, privateOn = false, privateOnly = true))
        assertFalse(visibleIn(secret, privateOn = false))
        assertTrue(visibleIn(normal, privateOn = false))
    }

    @Test fun `private mode sees everything, the pill only private ones`() {
        assertEquals(all, visibleDownloads(all, privateOn = true))
        assertEquals(listOf(secret), visibleDownloads(all, privateOn = true, privateOnly = true))
        assertTrue(visibleIn(secret, privateOn = true))
    }
}
