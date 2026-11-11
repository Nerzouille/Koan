package dev.nerzouille.koan.ui.root

import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.undoToast
import dev.nerzouille.koan.ui.player.downloadVideo

/** Sites whose fullscreen player keeps its own controls over the video (checked on the phone). */
internal fun ownPlayer(url: String?): Boolean {
    val host = url?.let { android.net.Uri.parse(it).host }?.removePrefix("www.")?.removePrefix("m.") ?: return false
    return OWN_PLAYER_SITES.any { host == it || host.endsWith(".$it") }
}

private val OWN_PLAYER_SITES = listOf("youtube.com", "youtube-nocookie.com")

/** The standalone player's ⋯ menu, outside the player: share, open with, delete, download. */
internal fun standaloneActions(
    components: Components,
    context: android.content.Context,
    video: dev.nerzouille.koan.ui.player.StandaloneVideo,
    activeTabId: String?,
    toast: (Toast) -> Unit,
): dev.nerzouille.koan.ui.player.StandaloneActions {
    val download = video.download
    return dev.nerzouille.koan.ui.player.StandaloneActions(
        share = {
            val ok = if (download != null) components.downloads.share(download) else runCatching {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("video/*")
                    .putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(video.uri))
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (!ok) toast(Toast("Couldn't share this file"))
        },
        openWith = {
            val ok = if (download != null) components.downloads.open(download) else runCatching {
                val view = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(android.net.Uri.parse(video.uri), "video/*")
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(android.content.Intent.createChooser(view, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (!ok) toast(Toast("Couldn't open this file", detail = "It may have been moved or deleted"))
        },
        delete = {
            if (download != null) {
                // As in Downloads: hidden at once, deleted once the Undo toast is gone.
                components.downloads.removeLater(download, deleteFile = true)
                toast(undoToast("File deleted", onGone = { components.downloads.commit(listOf(download)) }) { components.downloads.restore(download) })
            }
        },
        download = {
            val url = video.uri
            val tab = activeTabId
            if (tab != null) {
                downloadVideo(components, tab, url)
            } else {
                components.store.dispatch(
                    mozilla.components.browser.state.action.DownloadAction.AddDownloadAction(
                        mozilla.components.browser.state.state.content.DownloadState(url, skipConfirmation = true, referrerUrl = video.pageUrl),
                    ),
                )
            }
            toast(Toast("Downloading the video"))
        },
        shareLink = {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, video.uri)
            context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        pip = { components.pip.value?.enterNow() },
    )
}
