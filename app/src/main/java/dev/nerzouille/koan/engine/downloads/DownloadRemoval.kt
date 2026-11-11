package dev.nerzouille.koan.engine.downloads

/** Deleting a download for good: its file, then its row. */
internal object DownloadRemoval {
    /**
     * Deletes the file, then drops the row only if the file is really gone. False: the file is still
     * on the phone (another app's file, a folder Koan can't write…), so the row stays and the caller
     * says so; a failed delete must never hide a file from the list.
     */
    fun deleteFileThenRow(deleteFile: () -> Unit, fileStillThere: () -> Boolean, removeRow: () -> Unit): Boolean {
        deleteFile()
        if (fileStillThere()) return false
        removeRow()
        return true
    }
}
