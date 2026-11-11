package dev.nerzouille.koan.engine.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Job
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

    /** The finished file's content:// address, for Koan's player; null if it is gone. */
    fun uri(download: DownloadState): String? =
        files.findShareableDownloadFileUri(download.fileName, download.directoryPath)?.toString()

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
     * again) or bring a file back. That moment is when its Undo toast leaves the screen ([commit],
     * the toast's onGone); a long timer is only a safety net. If Koan is killed meanwhile, nothing
     * happens: the file stays.
     */
    fun removeLater(download: DownloadState, deleteFile: Boolean) {
        pending[download.id]?.cancel()
        _hidden.value = _hidden.value + download.id
        undo.hold(download.id)
        removals[download.id] = { if (deleteFile) delete(download) else useCases.removeDownload(download.id) }
        pending[download.id] = scope.launch {
            delay(SAFETY_MS)
            commit(listOf(download))
        }
    }

    /** Their Undo toast is gone: the ones not restored are removed now. */
    fun commit(downloads: List<DownloadState>) {
        for (id in undo.commit(downloads.map { it.id })) {
            pending.remove(id)?.cancel()
            removals.remove(id)?.invoke()
            _hidden.value = _hidden.value - id
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
        undo.cancel(download.id)
        removals.remove(download.id)
        pending.remove(download.id)?.cancel()
        _hidden.value = _hidden.value - download.id
    }

    /** Downloads hidden or deleted, waiting for their Undo window to pass. */
    val hidden: StateFlow<Set<String>> get() = _hidden
    private val _hidden = MutableStateFlow(emptySet<String>())
    private val pending = mutableMapOf<String, Job>()
    private val undo = PendingDeletes<String>()
    private val removals = mutableMapOf<String, () -> Unit>()
    private val scope = dev.nerzouille.koan.engine.KoanScopes.guarded("downloads", kotlinx.coroutines.Dispatchers.Main)

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
        /** If the Undo toast never says it's gone (it was never shown), the removal still happens. */
        const val SAFETY_MS = 30_000L

        /** The id extra the service's receiver reads (AbstractFetchDownloadService). */
        const val EXTRA_DOWNLOAD_ID = "downloadId"
    }
}
