package dev.nerzouille.koan.ui.player

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import dev.nerzouille.koan.engine.WebVideo
import mozilla.components.browser.state.state.content.DownloadState

/** What the player shows, from either a page's video or Koan's own (standalone) one. */
@Immutable
data class PlayerState(
    val paused: Boolean = true,
    val ended: Boolean = false,
    val waiting: Boolean = false,
    /** Seconds as of [at] (uptime ms), moving at [rate] while playing. */
    val time: Double = 0.0,
    val at: Long = 0L,
    val rate: Double = 1.0,
    val duration: Double = 0.0,
    val buffered: Double = 0.0,
    val muted: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
) {
    val playing: Boolean get() = !paused && !ended

    /** The position now, between two updates. */
    fun positionAt(now: Long = SystemClock.uptimeMillis()): Double {
        if (paused || ended || waiting) return if (ended && duration > 0) duration else time
        val t = time + (now - at) / 1000.0 * rate
        return if (duration > 0) t.coerceAtMost(duration) else t
    }

    companion object {
        fun of(v: WebVideo) = PlayerState(v.paused, v.ended, v.waiting, v.time, v.at, v.rate, v.duration, v.buffered, v.muted, v.width, v.height)
    }
}

/** The player's buttons, sent to whichever video it drives. */
interface PlayerControls {
    fun play()
    fun pause()
    fun seek(seconds: Double)
    fun rate(rate: Double)
    fun mute(muted: Boolean)
}

/** Web: in place of a page's player. File / Link: Koan's own, over the current screen. */
enum class PlayerMode { Web, File, Link }

/** A video Koan plays without a page (Downloads, "Play video", a file from another app). */
@Immutable
data class StandaloneVideo(
    /** A content:// or http(s) address. */
    val uri: String,
    val title: String,
    /** "Downloads · 64.2 MB", or the host. */
    val subtitle: String,
    val mode: PlayerMode,
    /** The download it is, for Share / Open with / Delete. */
    val download: DownloadState? = null,
    /** The page it was found on (Download video's referrer). */
    val pageUrl: String? = null,
    /** Where it grows from (a Downloads row), in root coordinates; null = from the centre. */
    val from: Rect? = null,
)

/**
 * The player's app-wide state (Components.player): the standalone player, and the page view's
 * swipe-down offset. Main thread only.
 */
class PlayerHost {
    var standalone by mutableStateOf<StandaloneVideo?>(null)
    /** The page view follows a swipe down on a fullscreen web video (BrowserScreen's layer). */
    val webDragY = mutableFloatStateOf(0f)
    val webScale = mutableFloatStateOf(1f)

    /** Koan's controls give way to the site's until the next fullscreen. */
    var siteControls by mutableStateOf(false)

    /** The standalone player's video size and play state, for picture-in-picture. */
    var standaloneShape by mutableStateOf<Pair<Int, Int>?>(null)
    var standalonePlaying by mutableStateOf(false)

    fun open(video: StandaloneVideo) {
        standalone = video
    }
}

/** A video Koan can download or play as a file: http(s), ending in .mp4 / .webm (not a stream, a blob, a manifest). */
fun downloadableVideo(source: String?): String? {
    val url = source?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return null
    if (isFilePage(url)) return null
    val path = url.substringBefore('#').substringBefore('?').lowercase()
    return url.takeIf { VIDEO_EXTENSIONS.any { e -> path.endsWith(e) } }
}

/** Whether a page's video can be saved, for the ⋯ menu's "Download video" (fixes F6). */
sealed interface VideoSave {
    /** A video file by its address: download it. */
    data class File(val url: String) : VideoSave

    /** An http address with no video ending: ask the server on tap ([isVideoFile]), never before. */
    data class Check(val url: String) : VideoSave

    /** Not a file Koan can save: the row is greyed out, with [reason] under it. */
    data class No(val reason: String) : VideoSave
}

fun videoSave(src: String, drm: Boolean, kind: String, live: Boolean): VideoSave {
    val http = src.startsWith("https://") || src.startsWith("http://")
    return when {
        drm -> VideoSave.No("Protected video")
        live -> VideoSave.No("Live video, can't be saved")
        kind == "stream" -> VideoSave.No("Streamed video, can't be saved")
        downloadableVideo(src) != null -> VideoSave.File(src)
        http && !isFilePage(src) -> VideoSave.Check(src)
        else -> VideoSave.No("This video can't be saved")
    }
}

/** A wiki's page about a file ("…/File:Clip.webm") is a page, not the file. */
private fun isFilePage(url: String): Boolean {
    val path = url.substringBefore('#').substringBefore('?').lowercase()
    return path.substringAfterLast('/').contains(':') || path.contains("/wiki/")
}

/** The server says [url] is a video (a HEAD request, 2 s at most); false when unsure. */
suspend fun isVideoFile(url: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    runCatching {
        val c = dev.nerzouille.koan.engine.KoanHttp.open(url, 2000)
        c.requestMethod = "HEAD"
        c.instanceFollowRedirects = true
        try {
            c.responseCode in 200..399 && c.contentType.orEmpty().lowercase().let { it.startsWith("video/") || it == "application/octet-stream" && downloadableVideo(url) != null }
        } finally {
            c.disconnect()
        }
    }.getOrDefault(false)
}

private val VIDEO_EXTENSIONS = listOf(".mp4", ".webm", ".m4v", ".mov", ".mkv")

/** A finished download that is a video, as the standalone player opens it; null otherwise. */
fun videoOfDownload(d: DownloadState, downloads: dev.nerzouille.koan.engine.downloads.Downloads): StandaloneVideo? {
    if (d.status != DownloadState.Status.COMPLETED) return null
    val name = d.fileName.orEmpty()
    val ext = name.substringAfterLast('.', "").lowercase()
    val video = d.contentType.orEmpty().lowercase().startsWith("video/") || ".$ext" in VIDEO_EXTENSIONS
    if (!video) return null
    val uri = downloads.uri(d) ?: return null
    val size = (d.contentLength ?: d.currentBytesCopied).takeIf { it > 0 }?.let { dev.nerzouille.koan.ui.downloads.sizeText(it) }
    return StandaloneVideo(uri, name, listOfNotNull("Downloads", size).joinToString(" · "), PlayerMode.File, download = d)
}

/** A video file's address (a long-pressed link, a link from another app). */
fun videoOfLink(url: String, label: String = "", pageUrl: String? = null): StandaloneVideo {
    val file = android.net.Uri.parse(url).lastPathSegment.orEmpty()
    val host = android.net.Uri.parse(url).host?.removePrefix("www.").orEmpty()
    return StandaloneVideo(url, label.trim().ifEmpty { file.ifEmpty { host } }, host, PlayerMode.Link, pageUrl = pageUrl)
}

/** A video another app opened with Koan (content:// or file), by its name if it says one. */
fun videoOfFile(uri: String, name: String?): StandaloneVideo =
    StandaloneVideo(uri, name ?: android.net.Uri.parse(uri).lastPathSegment.orEmpty().ifEmpty { "Video" }, "From another app", PlayerMode.File)
