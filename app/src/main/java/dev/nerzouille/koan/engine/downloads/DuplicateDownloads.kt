package dev.nerzouille.koan.engine.downloads

import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.support.utils.DownloadFileUtils

/**
 * A download that matches a file already downloaded: same address (a finished download whose file
 * is still there), or a file of the same name still in the folder it would go to. A file that was
 * deleted since doesn't count: that download just starts.
 */
class DuplicateDownload(
    val download: DownloadState,
    /** The earlier download of it, when the list has one (Open uses it, Replace drops it). */
    val earlier: DownloadState?,
    /** The name and folder of the file that is already there. */
    val existingName: String,
    val existingFolder: String,
)

object DuplicateDownloads {
    fun find(download: DownloadState, all: Collection<DownloadState>, files: DownloadFileUtils): DuplicateDownload? {
        val folder = download.directoryPath.ifBlank { files.currentDownloadLocation }
        // The engine may already have made the name unique ("photo(1).jpg"): the file it would
        // duplicate is the one without the number.
        val name = download.fileName?.takeIf { it.isNotBlank() }?.let(::withoutCounter)
        // Several copies already: the one the others were numbered after (no counter), else the oldest.
        val earlier = all.filter { d ->
            d.id != download.id && d.status == DownloadState.Status.COMPLETED && !d.private && d.url == download.url &&
                d.fileName != null && exists(files, d.fileName!!, d.directoryPath)
        }.minWithOrNull(compareBy<DownloadState>({ withoutCounter(it.fileName!!) != it.fileName }, { it.createdTime }))
        if (earlier != null) return DuplicateDownload(download, earlier, earlier.fileName!!, earlier.directoryPath)
        if (name != null && exists(files, name, folder)) {
            val sameFile = all.firstOrNull { it.id != download.id && it.fileName == name && it.directoryPath == folder && it.status == DownloadState.Status.COMPLETED }
            return DuplicateDownload(download, sameFile, name, folder)
        }
        return null
    }

    /** "photo(2).jpg" or "photo (2).jpg" → "photo.jpg"; other names as they are. */
    fun withoutCounter(name: String): String = COUNTER.replace(name, "$1$2")

    private val COUNTER = Regex("""^(.+?) ?\(\d+\)(\.[^.]+)?$""")

    private fun exists(files: DownloadFileUtils, name: String, folder: String) =
        runCatching { files.fileExists(folder, name) }.getOrDefault(false)

    /** "photo.jpg" → "photo (1).jpg" (then (2)…), the first name not taken in [folder]. */
    fun keepBothName(name: String, taken: (String) -> Boolean): String {
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        var n = 1
        while (n < 1000) {
            val candidate = "$base ($n)$ext"
            if (!taken(candidate)) return candidate
            n++
        }
        return "$base (${System.currentTimeMillis()})$ext"
    }
}
