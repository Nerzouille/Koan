package dev.nerzouille.koan.engine.downloads

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nerzouille.koan.ui.downloads.doneRowDetail
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MissingFilesTest {
    private fun d(id: String, status: Status = Status.COMPLETED, private: Boolean = false) = DownloadState(
        "https://example.com/files/$id.pdf", fileName = "$id.pdf", contentType = "application/pdf", contentLength = 2048,
        status = status, id = id, private = private, referrerUrl = "https://example.com/page", directoryPath = "/downloads",
    )

    @Test
    fun onlyFinishedDownloadsWithoutTheirFileAreMissing() {
        val there = setOf("kept")
        val list = listOf(d("kept"), d("gone"), d("running", Status.DOWNLOADING), d("failed", Status.FAILED))
        assertEquals(setOf("gone"), missingDownloads(list) { it.id in there })
    }

    @Test
    fun downloadAgainIsANewDownloadOfTheSameAddressInTheSameMode() {
        val old = d("gone", private = true)
        val again = downloadAgainOf(old)
        assertEquals(old.url, again.url)
        assertEquals("gone.pdf", again.fileName)
        assertEquals(old.referrerUrl, again.referrerUrl)
        assertTrue(again.private)
        assertTrue(again.skipConfirmation)
        assertEquals(Status.INITIATED, again.status)
        assertNotEquals(old.id, again.id)
        assertEquals(false, downloadAgainOf(d("normal")).private)
    }

    @Test
    fun aMissingRowSaysSoInsteadOfItsSize() {
        assertEquals("2 KB · example.com", doneRowDetail(d("kept"), missing = false))
        assertEquals("Moved or deleted · example.com", doneRowDetail(d("gone"), missing = true))
    }
}
