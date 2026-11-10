package dev.nerzouille.koan.engine.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.support.utils.DownloadFileUtils

/** What the Downloads page can do with a download. The service does the work (feature-downloads). */
class Downloads(
    private val context: Context,
    private val files: DownloadFileUtils,
    private val useCases: DownloadsUseCases,
) {
    fun pause(download: DownloadState) = tell(AbstractFetchDownloadService.ACTION_PAUSE, download)
    fun resume(download: DownloadState) = tell(AbstractFetchDownloadService.ACTION_RESUME, download)
    fun cancel(download: DownloadState) = tell(AbstractFetchDownloadService.ACTION_CANCEL, download)
    fun retry(download: DownloadState) = tell(AbstractFetchDownloadService.ACTION_TRY_AGAIN, download)

    /** Opens the file in the app Android picks for it. False when nothing can (or it is gone). */
    fun open(download: DownloadState): Boolean =
        files.openFile(download.fileName, download.directoryPath, download.contentType)

    fun share(download: DownloadState): Boolean {
        val uri = files.findShareableDownloadFileUri(download.fileName, download.directoryPath) ?: return false
        val send = Intent(Intent.ACTION_SEND)
            .setType(download.contentType ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    /** Deletes the file and drops it from the list. */
    fun delete(download: DownloadState) {
        files.deleteMediaFile(context.contentResolver, download.fileName, download.directoryPath)
        useCases.removeDownload(download.id)
    }

    /**
     * Hidden or deleted from the page: gone from the list at once. Only once its Undo had its
     * chance ([restore] in time keeps it untouched) is it dropped from the store, and with
     * [deleteFile] its file deleted, so Undo never has to re-add a download (which would start it
     * again) or bring a file back. If Koan is killed meanwhile, nothing happens: the file stays.
     */
    fun removeLater(download: DownloadState, deleteFile: Boolean) {
        pending[download.id]?.cancel()
        _hidden.value = _hidden.value + download.id
        pending[download.id] = scope.launch {
            delay(UNDO_WINDOW_MS)
            if (deleteFile) delete(download) else useCases.removeDownload(download.id)
            pending.remove(download.id)
            _hidden.value = _hidden.value - download.id
        }
    }

    /** Several files at once, in one share sheet. False when none of them can be found. */
    fun shareAll(downloads: List<DownloadState>): Boolean {
        val uris = downloads.mapNotNullTo(ArrayList()) { files.findShareableDownloadFileUri(it.fileName, it.directoryPath) }
        if (uris.isEmpty()) return false
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("*/*")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun restore(download: DownloadState) {
        pending.remove(download.id)?.cancel()
        _hidden.value = _hidden.value - download.id
    }

    /** Downloads hidden or deleted, waiting for their Undo window to pass. */
    val hidden: StateFlow<Set<String>> get() = _hidden
    private val _hidden = MutableStateFlow(emptySet<String>())
    private val pending = mutableMapOf<String, Job>()
    private val scope = MainScope()

    /** Clears the list; the files stay where they are. */
    fun clearHistory() = useCases.removeAllDownloads()

    /** The system's own view of the downloads folder. */
    fun showFolder() {
        runCatching {
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** The service listens for the same actions as its notification buttons. */
    private fun tell(action: String, download: DownloadState) {
        context.sendBroadcast(
            Intent(action).setPackage(context.packageName).putExtra(EXTRA_DOWNLOAD_ID, download.id),
        )
    }

    private companion object {
        /** The toast's time on screen and its way out: Undo can't come later. */
        const val UNDO_WINDOW_MS = 4_000L

        /** The id extra the service's receiver reads (AbstractFetchDownloadService). */
        const val EXTRA_DOWNLOAD_ID = "downloadId"
    }
}
