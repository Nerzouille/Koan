package dev.nerzouille.koan.engine.downloads

import mozilla.components.support.utils.DownloadFileUtils

/**
 * Android Components' file helpers, with one check before a download is opened: the file is
 * still there. A moved or deleted file used to "open" with a stale address (the other app failed,
 * or nothing happened). Every way of opening goes through here: Downloads, the toast's Open, and
 * the notification (the download service calls [openFile] too). A missing file calls [onMissing]
 * with its name and counts as handled (true), so the service's own "can't open this type" toast,
 * which would be wrong, stays away. False still means no app opens this type.
 */
class CheckedDownloadFiles(
    private val inner: DownloadFileUtils,
    private val onMissing: (fileName: String?) -> Unit,
) : DownloadFileUtils by inner {
    override fun openFile(fileName: String?, directoryPath: String, contentType: String?): Boolean {
        if (fileName.isNullOrEmpty() || !runCatching { inner.fileExists(directoryPath, fileName) }.getOrDefault(false)) {
            onMissing(fileName)
            return true
        }
        return inner.openFile(fileName, directoryPath, contentType)
    }
}
