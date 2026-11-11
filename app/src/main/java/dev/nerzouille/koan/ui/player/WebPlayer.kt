package dev.nerzouille.koan.ui.player

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
 * where it was and the video keeps playing there. [onDownload]: Download video in the ⋯ menu,
 * for a plain video file. (No mini-player for now: docs/ROADMAP.md.)
 */
@Composable
fun WebPlayerLayer(
    components: Components,
    tabId: String?,
    fullScreen: Boolean,
    inPip: Boolean,
    onDownload: (url: String) -> Unit,
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

    val save = videoSave(video.src, video.drm, video.kind, video.live)
    val saveScope = rememberCoroutineScope()
    val pipOnLeave by components.settings.pictureInPicture.collectAsStateWithLifecycle(true)
    val rows = buildList {
        add(pipOnLeaveRow(pipOnLeave) { on -> saveScope.launch { components.settings.setPictureInPicture(on) } })
        // Always there (F6): greyed out, with why, when the video isn't a file Koan can save.
        add(
            when (save) {
                is VideoSave.File -> MenuRow(KoanIcons.Download, "Download video") { leave(); onDownload(save.url) }
                // No video ending: asked the server only now, on the tap.
                is VideoSave.Check -> MenuRow(KoanIcons.Download, "Download video") {
                    saveScope.launch {
                        if (isVideoFile(save.url)) {
                            leave()
                            onDownload(save.url)
                        } else {
                            onToast("This video can't be downloaded", "It isn't a file the site lets Koan save")
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
    )
}

private fun resetDrag(components: Components) {
    components.player.webDragY.floatValue = 0f
    components.player.webScale.floatValue = 1f
}

/** "example.com" for a page's address. */
internal fun siteOf(url: String?): String =
    url?.let { runCatching { android.net.Uri.parse(it).host }.getOrNull() }?.removePrefix("www.").orEmpty()

/** Starts a download of a page's video file (Download video). */
fun downloadVideo(components: Components, tabId: String?, url: String) {
    val id = tabId ?: return
    val page = components.store.state.findTab(id)?.content?.url
    components.store.dispatch(ContentAction.UpdateDownloadAction(id, DownloadState(url, skipConfirmation = true, referrerUrl = page)))
}
