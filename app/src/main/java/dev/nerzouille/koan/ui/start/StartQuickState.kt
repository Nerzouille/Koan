package dev.nerzouille.koan.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.search.CopiedLink
import dev.nerzouille.koan.ui.search.CopiedLinks
import dev.nerzouille.koan.ui.theme.StartPageTokens
import mozilla.components.browser.state.state.content.DownloadState

/**
 * The link to offer while the search field is open ([editing]), checked once each time it opens
 * (and again when the window gets focus: Android hides the clipboard from an app without it).
 * [current]: the open page's address, never offered.
 */
@Composable
fun rememberCopiedLink(editing: Boolean, current: String?): CopiedLink? {
    val context = LocalContext.current
    val links = remember { CopiedLinks(context.applicationContext) }
    val focused = LocalWindowInfo.current.isWindowFocused
    var link by remember { mutableStateOf<CopiedLink?>(null) }
    val page by rememberUpdatedState(current)
    LaunchedEffect(editing, focused) {
        if (!editing) link = null else if (focused && link == null) link = links.check(page)
    }
    return link
}

/**
 * The start page's quick rows while it is [open]: the latest pages visited (not in Private, not
 * [skip], the tab under it) and the last download if it finished within the hour.
 */
@Composable
fun rememberStartQuick(
    components: Components,
    open: Boolean,
    private: Boolean,
    skip: String?,
    copied: CopiedLink?,
    onOpenUrl: (String) -> Unit,
): StartQuick {
    var pages by remember { mutableStateOf(emptyList<RecentPage>()) }
    var download by remember { mutableStateOf<DownloadState?>(null) }
    LaunchedEffect(open, private) {
        if (!open || private) {
            pages = emptyList()
            download = null
            return@LaunchedEffect
        }
        val visits = components.database.dao().recentVisits(StartPageTokens.RECENT_PAGES * 2).map { RecentPage(it.url, it.title) }
        pages = recentPagesToShow(visits, skip, StartPageTokens.RECENT_PAGES)
        download = lastRecentDownload(components.store.state.downloads.values, System.currentTimeMillis())
    }
    val open by rememberUpdatedState(onOpenUrl)
    return StartQuick(
        pages = pages,
        download = download?.let { RecentDownload(it.fileName ?: it.url, it.url) },
        copied = copied,
        onOpenUrl = { open(it) },
        onOpenDownload = { download?.let { components.downloads.open(it) } },
    )
}

/** The newest finished, non-private download, if it finished less than an hour before [now]. */
fun lastRecentDownload(all: Collection<DownloadState>, now: Long): DownloadState? =
    all.filter { it.status == DownloadState.Status.COMPLETED && !it.private }
        .maxByOrNull { it.createdTime }
        ?.takeIf { now - it.createdTime in 0..StartPageTokens.RECENT_DOWNLOAD_MAX_AGE_MS }
