package dev.nerzouille.koan.engine.downloads

import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.support.utils.DownloadFileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** A normal download's duplicate card never points at a private download (docs/FIXES.md 7). */
class DuplicateDownloadsPrivateTest {
    /** Every file in [present] ("folder/name") exists. */
    private class Files(private val present: Set<String>) : DownloadFileUtils {
        override val currentDownloadLocation = "/downloads"
        override fun fileExists(directoryPath: String, fileName: String?) = "$directoryPath/$fileName" in present
        override fun guessFileName(contentDisposition: String?, url: String?, mimeType: String?): String = TODO()
        override fun openFile(fileName: String?, directoryPath: String, contentType: String?): Boolean = TODO()
        override fun createOpenFileIntent(fileName: String?, directoryPath: String, downloadContentType: String?): android.content.Intent? = TODO()
        override fun findDownloadFileUri(fileName: String?, directoryPath: String): android.net.Uri? = TODO()
        override fun findShareableDownloadFileUri(fileName: String?, directoryPath: String): android.net.Uri? = TODO()
        override fun getSafeContentType(fileName: String?, contentType: String?, uri: android.net.Uri?): String = TODO()
        override fun uniqueFileName(directoryPath: String, fileName: String): String = TODO()
        override fun deleteMediaFile(contentResolver: android.content.ContentResolver, fileName: String?, directoryPath: String): Boolean = TODO()
        override fun renameFile(directoryPath: String, oldName: String?, newName: String): Boolean = TODO()
    }

    private val files = Files(setOf("/downloads/report.pdf"))

    private fun done(id: String, private: Boolean) = DownloadState(
        "https://example.com/report.pdf", fileName = "report.pdf", directoryPath = "/downloads",
        id = id, private = private, status = DownloadState.Status.COMPLETED, createdTime = 1,
    )

    private val fresh = DownloadState("https://example.com/report.pdf", fileName = "report.pdf", directoryPath = "/downloads", id = "new", private = false)

    @Test
    fun aNormalEarlierDownloadIsTheDuplicate() {
        val dup = DuplicateDownloads.find(fresh, listOf(done("old", private = false), fresh), files)
        assertEquals("old", dup?.earlier?.id)
    }

    @Test
    fun aPrivateEarlierDownloadIsNeverTheEarlierOne() {
        val dup = DuplicateDownloads.find(fresh, listOf(done("secret", private = true), fresh), files)
        // The file is in the shared folder, so the card still says it exists, but with no entry behind it.
        assertNotNull(dup)
        assertNull(dup!!.earlier)
        assertEquals("report.pdf", dup.existingName)
    }
}
