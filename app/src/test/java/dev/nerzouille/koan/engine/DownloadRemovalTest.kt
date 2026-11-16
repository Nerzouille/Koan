package dev.nerzouille.koan.engine

import dev.nerzouille.koan.engine.downloads.DownloadRemoval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A deleted download's row goes only with its file: a failed delete never hides a file that stays. */
class DownloadRemovalTest {
    @Test fun theRowGoesOnceTheFileIsGone() {
        val calls = mutableListOf<String>()
        val removed = DownloadRemoval.deleteFileThenRow(
            deleteFile = { calls += "file" },
            fileStillThere = { false },
            removeRow = { calls += "row" },
        )
        assertTrue(removed)
        assertEquals(listOf("file", "row"), calls)
    }

    @Test fun aFileThatStaysKeepsItsRow() {
        var rowRemoved = false
        val removed = DownloadRemoval.deleteFileThenRow(
            deleteFile = { /* the delete fails: another app's file, a folder Koan can't write */ },
            fileStillThere = { true },
            removeRow = { rowRemoved = true },
        )
        assertFalse("the file is still on the phone: report it", removed)
        assertFalse("the row must stay, or the file is lost from view", rowRemoved)
    }
}
