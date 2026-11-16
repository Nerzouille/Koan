package dev.nerzouille.koan.ui.player

import androidx.compose.runtime.Immutable
import dev.nerzouille.koan.ui.downloads.sizeText
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status

/** A short note inside the player (it draws over Koan's own toasts): a download started or failed. */
@Immutable
data class PlayerNotice(val title: String, val detail: String? = null, val error: Boolean = false, val serial: Int = 0)

/**
 * What the player says about the download it started ([d], null until the engine has it):
 * nothing yet while its size is unknown, then "Downloading · 1.2 GB", or that it failed.
 */
fun downloadNotice(d: DownloadState?, name: String): PlayerNotice? {
    d ?: return null
    val file = d.fileName?.takeIf { it.isNotBlank() } ?: name
    val size = (d.contentLength ?: 0L).takeIf { it > 0 }?.let(::sizeText)
    return when (d.status) {
        Status.FAILED -> PlayerNotice("Couldn't download the video", file, error = true)
        Status.CANCELLED -> null
        Status.COMPLETED -> PlayerNotice(file, listOfNotNull("Downloaded", size).joinToString(" · "))
        else -> size?.let { PlayerNotice(file, "Downloading · $it") }
    }
}

/** "clip.webm" for a video's address (its last path segment, decoded); "Video" when it has none. */
fun videoFileName(url: String): String =
    runCatching { java.net.URLDecoder.decode(url.substringBefore('?').substringBefore('#').substringAfterLast('/'), "UTF-8") }
        .getOrNull()?.takeIf { it.isNotBlank() } ?: "Video"
