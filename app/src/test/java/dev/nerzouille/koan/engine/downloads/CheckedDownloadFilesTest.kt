package dev.nerzouille.koan.engine.downloads

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import mozilla.components.support.utils.DownloadFileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckedDownloadFilesTest {
    private class Fake(val there: Set<String>, val appOpens: Boolean = true) : DownloadFileUtils {
        val opened = mutableListOf<String?>()
        override val currentDownloadLocation = "/downloads"
        override fun guessFileName(contentDisposition: String?, url: String?, mimeType: String?) = "file"
        override fun openFile(fileName: String?, directoryPath: String, contentType: String?): Boolean {
            opened += fileName
            return appOpens
        }
        override fun createOpenFileIntent(fileName: String?, directoryPath: String, contentType: String?): Intent? = null
        override fun findDownloadFileUri(fileName: String?, directoryPath: String): Uri? = null
        override fun findShareableDownloadFileUri(fileName: String?, directoryPath: String): Uri? = null
        override fun getSafeContentType(fileName: String?, contentType: String?, uri: Uri?): String = contentType ?: "*/*"
        override fun fileExists(directoryPath: String, fileName: String?) = fileName in there
        override fun uniqueFileName(directoryPath: String, fileName: String) = fileName
        override fun deleteMediaFile(contentResolver: ContentResolver, fileName: String?, directoryPath: String) = false
        override fun renameFile(directoryPath: String, oldName: String?, newName: String) = false
    }

    @Test
    fun aFileStillThereOpens() {
        val fake = Fake(setOf("a.pdf"))
        val missing = mutableListOf<String?>()
        assertTrue(CheckedDownloadFiles(fake) { missing += it }.openFile("a.pdf", "/downloads", "application/pdf"))
        assertEquals(listOf<String?>("a.pdf"), fake.opened)
        assertTrue(missing.isEmpty())
    }

    @Test
    fun aMovedOrDeletedFileSaysSoAndIsNotOpened() {
        val fake = Fake(emptySet())
        val missing = mutableListOf<String?>()
        val files = CheckedDownloadFiles(fake) { missing += it }
        assertTrue(files.openFile("a.pdf", "/downloads", null))
        assertTrue(files.openFile(null, "/downloads", null))
        assertTrue(fake.opened.isEmpty())
        assertEquals(listOf("a.pdf", null), missing)
    }

    @Test
    fun noAppForAFileThereIsStillFalse() {
        val missing = mutableListOf<String?>()
        assertFalse(CheckedDownloadFiles(Fake(setOf("a.xyz"), appOpens = false)) { missing += it }.openFile("a.xyz", "/downloads", null))
        assertTrue(missing.isEmpty())
    }
}
