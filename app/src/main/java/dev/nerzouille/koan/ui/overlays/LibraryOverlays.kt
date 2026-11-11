package dev.nerzouille.koan.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.downloads.DownloadsScreen
import dev.nerzouille.koan.ui.history.ClearDataCard
import dev.nerzouille.koan.ui.history.HistoryActions
import dev.nerzouille.koan.ui.history.HistoryScreen
import dev.nerzouille.koan.ui.kit.KoanClipboard
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.copiedToast
import dev.nerzouille.koan.ui.kit.rememberPresence
import dev.nerzouille.koan.ui.kit.slideUpPage
import dev.nerzouille.koan.ui.menu.Confirm
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState

/**
 * The library over everything (Settings too): Downloads and History full screen, their clear cards,
 * and the card a library page asks on first (History, Downloads, Passwords).
 */
@Composable
internal fun LibraryOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    downloads: List<DownloadState>,
    private: Boolean,
    folderName: String?,
    clipboard: KoanClipboard,
    activeTabId: String?,
    onToast: (Toast) -> Unit,
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
    onRestoreClosed: (String) -> Unit,
    share: (url: String, subject: String) -> Unit,
) {
    val session = components.sessionUseCases
    // A delete that left the file on the phone: its row is back, and Koan says so.
    LaunchedEffect(Unit) { components.downloads.deleteFailed.collect { name -> onToast(Toast("Couldn't delete $name", detail = "It's still in the list")) } }
    // ---- Downloads, full screen, over Settings too ----
    val downloadsSlide = rememberPresence(overlays.downloadsOpen, Motion.Sheet)
    if (downloadsSlide.composed) {
        DownloadsScreen(
            downloads = downloads,
            private = private,
            folder = folderName ?: "Download",
            actions = components.downloads,
            onClear = {
                overlays.ask("Clear the download list?", "The files stay in ${folderName ?: "Download"}.", "Clear list") {
                    if (private) components.downloads.clearHistory() else components.downloads.clearRows(downloads)
                }
            },
            onClose = { overlays.downloadsOpen = false },
            onToast = onToast,
            onCopyLink = { url -> clipboard.setText(AnnotatedString(url)); copiedToast("Link")?.let(onToast) },
            onConfirm = { title, text, confirm, action -> overlays.ask(title, text, confirm, action = action) },
            modifier = Modifier.slideUpPage { downloadsSlide.progress },
            onPlayVideo = { d ->
                val video = dev.nerzouille.koan.ui.player.videoOfDownload(d, components.downloads)
                if (video != null) components.player.open(video)
                video != null
            },
        )
    }

    // ---- History, full screen like Downloads, and Clear browsing data ----
    val historySlide = rememberPresence(overlays.historyOpen, Motion.Sheet)
    if (historySlide.composed) {
        val dao = components.database.dao()
        val removeVisits = { visits: List<VisitEntity> ->
            val urls = visits.map { it.url }
            scope.launch {
                visits.forEach { dao.removeVisit(it.url) }
                // Two-way history on: the account deletes them too, at the next sync.
                if (components.settings.syncBack.first().history) components.historyDeletions.add(urls)
            }
            val what = if (visits.size == 1) "Removed from history" else "Removed ${visits.size} pages from history"
            onToast(Toast(what, "Undo") {
                components.historyDeletions.remove(urls)
                scope.launch { visits.forEach { dao.restoreVisit(it) } }
            })
        }
        HistoryScreen(
            dao = dao,
            onOpen = { url ->
                overlays.historyOpen = false
                // From a page: the entry opens in that tab (its menu has "Open in new tab").
                val here = activeTabId?.takeIf { overlays.historyFromPage }
                if (here != null) session.loadUrl(url, here) else onOpenNew(url, true, false)
            },
            onRemove = removeVisits,
            onClear = { overlays.clearDataCard.animateTo(Detent.Compact) },
            onClose = { overlays.historyOpen = false },
            onRestore = { id ->
                overlays.historyOpen = false
                onRestoreClosed(id)
            },
            actions = HistoryActions(
                openNew = { url -> overlays.historyOpen = false; onOpenNew(url, true, false) },
                copy = { url -> clipboard.setText(AnnotatedString(url)); copiedToast("Link")?.let(onToast) },
                share = { text -> share(text, "") },
                remove = removeVisits,
                restore = { id -> overlays.historyOpen = false; onRestoreClosed(id) },
            ),
            allClosed = overlays.historyShowsClosed,
            modifier = Modifier.slideUpPage { historySlide.progress },
        )
    }
    val clearData = overlays.clearDataCard
    KoanCard(clearData) { interactive ->
        ClearDataCard(
            interactive = interactive,
            onClear = { choice ->
                clearData.animateTo(Detent.Closed)
                scope.launch {
                    if (choice.history) components.database.dao().clearHistory()
                    val ok = components.sitePermissions.clearAll(cookies = choice.cookies, cache = choice.cache)
                    onToast(Toast(if (ok) "Browsing data cleared" else "Couldn't clear everything"))
                }
            },
            onCancel = { clearData.animateTo(Detent.Closed) },
        )
    }

    // ---- a library page asks first (History, Downloads, Passwords), over those pages ----
    val askCard = overlays.askCard
    KoanCard(askCard) { interactive ->
        val ask = overlays.ask ?: return@KoanCard
        Confirm(
            title = ask.title,
            text = ask.text,
            confirm = ask.confirm,
            interactive = interactive,
            destructive = ask.destructive,
            onConfirm = {
                askCard.animateTo(Detent.Closed)
                ask.action()
            },
            onCancel = { askCard.animateTo(Detent.Closed) },
        )
    }
}
