package dev.nerzouille.koan.ui.player

import mozilla.components.lib.state.ext.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.icons.KoanIcons
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.content.DownloadState

/** Koan's controls sent to a page's video (assets/extensions/video). */
class WebControls(private val components: Components, private val tabId: String) : PlayerControls {
    override fun play() = components.pageVideo.play(tabId)
    override fun pause() = components.pageVideo.pause(tabId)
    override fun seek(seconds: Double) = components.pageVideo.seek(tabId, seconds)
    override fun rate(rate: Double) = components.pageVideo.rate(tabId, rate)
    override fun mute(muted: Boolean) = components.pageVideo.mute(tabId, muted)
}

/**
 * The player in a page (§9 web): over the current tab's fullscreen video, in place of the site's
 * own player. A swipe down, the fullscreen button or Back leave fullscreen: the page comes back
 * where it was and the video keeps playing there. Download video in the ⋯ menu stays in the
 * player and says what happens there (Koan's own toasts are under it). (No mini-player for now:
 * docs/ROADMAP.md.)
 */
@Composable
fun WebPlayerLayer(
    components: Components,
    tabId: String?,
    fullScreen: Boolean,
    inPip: Boolean,
    onToast: (String, String?) -> Unit,
) {
    val host = components.player
    val videos by components.pageVideo.videos.collectAsStateWithLifecycle()
    // Each new fullscreen: Koan's controls again.
    LaunchedEffect(fullScreen) { if (fullScreen) host.siteControls = false }
    val video = tabId?.let { videos[it] }?.takeIf { it.fullscreen }
    if (!fullScreen || video == null || host.siteControls || tabId == null || host.standalone != null) {
        // Not ours (any more): the page view is never left moved.
        LaunchedEffect(Unit) { resetDrag(components) }
        return
    }
    val controls = remember(tabId) { WebControls(components, tabId) }

    fun leave() {
        components.pageVideo.release(tabId)
        components.sessionUseCases.exitFullscreen(tabId)
        resetDrag(components)
    }

    val save = videoSave(video.src, video.drm, video.kind, video.live, video.file)
    val saveScope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<PlayerNotice?>(null) }
    var serial by remember { mutableIntStateOf(0) }
    fun say(n: PlayerNotice) {
        notice = n.copy(serial = ++serial)
    }

    /** Starts the download and follows it: its size once known, or why it failed (F6 recheck). */
    fun startDownload(url: String) {
        val before = components.duplicateAsk.value
        val id = downloadVideo(components, tabId, url) ?: return
        // A file already there: the duplicate card answers, and it shows outside fullscreen.
        if (components.duplicateAsk.value.let { it != null && it !== before }) {
            leave()
            return
        }
        val name = videoFileName(url)
        saveScope.launch {
            val first = kotlinx.coroutines.withTimeoutOrNull(PlayerTokens.DOWNLOAD_START_MS) {
                components.store.flow().map { downloadNotice(it.downloads[id], name) }.first { it != null }
            }
            when {
                first != null -> say(first)
                components.store.state.downloads[id] != null -> say(PlayerNotice(name, "Downloading"))
                else -> say(PlayerNotice("Couldn't start the download", name, error = true))
            }
            if (first?.error == true) return@launch
            // Still here when it fails later: say so.
            val end = components.store.flow().map { it.downloads[id]?.status }
                .first { it == null || it == DownloadState.Status.FAILED || it == DownloadState.Status.COMPLETED || it == DownloadState.Status.CANCELLED }
            if (end == DownloadState.Status.FAILED) say(PlayerNotice("Couldn't download the video", components.store.state.downloads[id]?.fileName ?: name, error = true))
        }
    }
    val pipOnLeave by components.settings.pictureInPicture.collectAsStateWithLifecycle(true)
    val rows = buildList {
        add(pipOnLeaveRow(pipOnLeave) { on -> saveScope.launch { components.settings.setPictureInPicture(on) } })
        // Always there (F6): greyed out, with why, when the video isn't a file Koan can save.
        add(
            when (save) {
                is VideoSave.File -> MenuRow(KoanIcons.Download, "Download video") { startDownload(save.url) }
                // No video ending: asked the server only now, on the tap.
                is VideoSave.Check -> MenuRow(KoanIcons.Download, "Download video") {
                    saveScope.launch {
                        val private = components.store.state.findTab(tabId)?.content?.private == true
                        if (isVideoFile(components.fetchClient, save.url, private)) {
                            startDownload(save.url)
                        } else {
                            say(PlayerNotice("This video can't be downloaded", "It isn't a file the site lets Koan save", error = true))
                        }
                    }
                }
                is VideoSave.No -> MenuRow(KoanIcons.Download, "Download video", enabled = false, subtitle = save.reason) {}
            },
        )
        add(
            MenuRow(KoanIcons.SiteControls, "Use the site's controls") {
                components.pageVideo.release(tabId)
                host.siteControls = true
                onToast("The site's controls", "Koan's come back next time the video goes fullscreen")
            },
        )
    }
    val page = components.store.state.findTab(tabId)?.content
    VideoPlayer(
        state = PlayerState.of(video),
        controls = controls,
        mode = PlayerMode.Web,
        title = video.title.ifBlank { page?.title.orEmpty() },
        subtitle = siteOf(page?.url),
        menuRows = rows,
        onLeading = null,
        onSwipeAway = ::leave,
        onFullscreenButton = ::leave,
        onPip = { components.pip.value?.enterNow() },
        onBack = ::leave,
        onDrag = { dy, s ->
            host.webDragY.floatValue = dy
            host.webScale.floatValue = s
        },
        hidden = inPip,
        notice = notice,
        onRotate = { host.fullscreenRotated = !host.fullscreenRotated },
    )
}

private fun resetDrag(components: Components) {
    components.player.webDragY.floatValue = 0f
    components.player.webScale.floatValue = 1f
}

/** "example.com" for a page's address. */
internal fun siteOf(url: String?): String =
    url?.let { runCatching { android.net.Uri.parse(it).host }.getOrNull() }?.removePrefix("www.").orEmpty()

/** Starts a download of a page's video file (Download video); its download's id, or null with no tab. */
fun downloadVideo(components: Components, tabId: String?, url: String): String? {
    val id = tabId ?: return null
    val page = components.store.state.findTab(id)?.content?.url
    val private = components.store.state.findTab(id)?.content?.private == true
    // A private tab's file downloads in Gecko's private session and stays a private download.
    val download = DownloadState(url, skipConfirmation = true, referrerUrl = page, private = private)
    components.store.dispatch(ContentAction.UpdateDownloadAction(id, download))
    return download.id
}
