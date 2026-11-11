package dev.nerzouille.koan.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.SiteAsk
import dev.nerzouille.koan.engine.UBlock
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.bar.FindUi
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.kit.KoanClipboard
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.menu.LinkTarget
import dev.nerzouille.koan.ui.menu.MenuPage
import dev.nerzouille.koan.ui.menu.TranslateActions
import dev.nerzouille.koan.ui.menu.UBlockUi
import dev.nerzouille.koan.ui.menu.languageName
import dev.nerzouille.koan.ui.menu.translateUiOf
import dev.nerzouille.koan.ui.overlays.DockMenus
import dev.nerzouille.koan.ui.overlays.LibraryOverlays
import dev.nerzouille.koan.ui.overlays.LinkCardOverlay
import dev.nerzouille.koan.ui.overlays.LoginOverlays
import dev.nerzouille.koan.ui.overlays.MenuOverlays
import dev.nerzouille.koan.ui.overlays.PermissionOverlays
import dev.nerzouille.koan.ui.overlays.SettingsOverlays
import dev.nerzouille.koan.ui.overlays.SignOutOverlay
import dev.nerzouille.koan.ui.overlays.TourOverlays
import dev.nerzouille.koan.ui.settings.SettingsPage
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.SheetNav
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.tour.TourSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.concept.engine.translate.Language
import mozilla.components.concept.engine.translate.TranslationOptions
import mozilla.components.concept.engine.translate.TranslationPageSettingOperation

/** What opens over the page: the ⋯ menu, the long-press menu, the sign-out card, find, Settings. */
@Stable
class Overlays(scope: CoroutineScope, density: Density) {
    val menu = SheetState(scope, splineBasedDecay(density))
    val menuNav = SheetNav<MenuPage>(scope, MenuPage.Root)
    val linkCard = SheetState(scope, splineBasedDecay(density))
    var link by mutableStateOf<LinkTarget?>(null)
    val signOutCard = SheetState(scope, splineBasedDecay(density))

    /** The tab counter's long-press menu: New tab, Close tab. */
    var tabMenuOpen by mutableStateOf(false)

    /** Back's long-press list: the pages before this one in the tab. */
    var backMenuOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var downloadsOpen by mutableStateOf(false)
    var historyOpen by mutableStateOf(false)

    /** History opened on its whole "Recently closed" list (long press on the dock's History). */
    var historyShowsClosed by mutableStateOf(false)

    /** History was opened from a page (not the overview): a tap opens the entry in that tab. */
    var historyFromPage by mutableStateOf(false)

    /** History from anywhere: the same page, its "Recently closed" on top (all of it when [allClosed]). */
    fun openHistory(fromPage: Boolean, allClosed: Boolean = false) {
        historyShowsClosed = allClosed
        historyFromPage = fromPage
        historyOpen = true
    }

    /** A library page asks first (deleting passwords, several files), on its floating card. */
    val askCard = SheetState(scope, splineBasedDecay(density))
    var ask by mutableStateOf<Ask?>(null)

    /** [destructive]: the action deletes or loses something (red); otherwise the primary button. */
    fun ask(title: String, text: String, confirm: String, destructive: Boolean = true, action: () -> Unit) {
        ask = Ask(title, text, confirm, destructive, action)
        askCard.animateTo(Detent.Compact)
    }

    class Ask(val title: String, val text: String, val confirm: String, val destructive: Boolean, val action: () -> Unit)
    val clearDataCard = SheetState(scope, splineBasedDecay(density))

    /** Logins: the full "Sign in with" list (7e), and "Save in Koan". */
    val loginCard = SheetState(scope, splineBasedDecay(density))
    val saveLoginCard = SheetState(scope, splineBasedDecay(density))

    /** A site's request waiting on the card, and what it asks for. */
    val permissionCard = SheetState(scope, splineBasedDecay(density))
    var permission by mutableStateOf<Pair<PermissionRequest, SiteAsk>?>(null)

    /** A page wants an app: "Open in YouTube?" ([ExternalLinks.ask]). */
    val appLinkCard = SheetState(scope, splineBasedDecay(density))

    /** The link card is also asking about a download (KoanOverlays): a tab change doesn't close it. */
    var actionBusy by mutableStateOf(false)

    /** Save image on a small preview: waiting for the page's full image. */
    var imageWaiting by mutableStateOf(false)

    /** What the page asks on the card: a <select>, a date, alert/confirm, HTTP sign-in… */
    val promptCard = SheetState(scope, splineBasedDecay(density))
    val settingsNav = SheetNav<SettingsPage>(scope, SettingsPage.Root)

    /** The tour or a "What's new", full screen over everything (Settings too: About replays it). */
    var tourSheet by mutableStateOf<TourSheet?>(null)
    var tourOpen by mutableStateOf(false)

    fun showTour(sheet: TourSheet) {
        tourSheet = sheet
        tourOpen = true
    }

    /** Find in page is open, and what was typed in it. */
    var finding by mutableStateOf(false)
    var findQuery by mutableStateOf("")

    /** Something covers the page or the overview (a sheet, a card, a full page): toasts make way. */
    val anyOpen: Boolean
        get() = menu.open || linkCard.open || signOutCard.open || tabMenuOpen || backMenuOpen || settingsOpen || downloadsOpen ||
            historyOpen || askCard.open || clearDataCard.open || loginCard.open ||
            saveLoginCard.open || permissionCard.open || appLinkCard.open || promptCard.open || tourOpen

    /** The overview opened, or the tab changed: nothing stays open over the old page. */
    fun dismissAll() {
        menu.snapClosed()
        if (!actionBusy) linkCard.snapClosed()
        signOutCard.snapClosed()
        tabMenuOpen = false
        backMenuOpen = false
        finding = false
        findQuery = ""
    }
}

/**
 * The overlays, drawn over the page view and the bar. [onOpenNew]: a url as a new tab of the
 * current space ([select] = shown now, next to the current tab when [nextToCurrent]).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KoanOverlays(
    components: Components,
    overlays: Overlays,
    page: PageInfo,
    activeTabId: String?,
    activeTab: TabEntity?,
    drawerStatus: DrawerStatus,
    onEditAddress: () -> Unit,
    onNewTab: () -> Unit,
    /** Closes the current tab (the tab counter's menu). */
    onCloseTab: () -> Unit,
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
    onSignIn: () -> Unit,
    /** Quiet confirmations (the shared bottom toast). */
    onToast: (Toast) -> Unit,
    /** A recently closed tab picked in History: back in its space and place. */
    onRestoreClosed: (String) -> Unit,
    /** The space on screen (the tab counter's "Reopen closed tab"). */
    currentSpaceUuid: String?,
    /** Private shown: copies are marked sensitive, no Essentials, the site's Protections switch. */
    private: Boolean = false,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    // A copy from a private tab is marked sensitive: Android 13+ hides it in its clipboard preview.
    val clipboard = remember(private) { KoanClipboard(context, sensitive = private) }

    val engines by components.settings.searchEngines.collectAsStateWithLifecycle(SearchEngine.builtIns)
    val downloads by components.store.observeSlice(Unit) { s -> s.downloads.values.toList() }
    // uBlock Origin on this page: on or off, and its badge (the tab's own, else the default).
    val ublockUi by components.store.observeSlice(activeTabId) { s ->
        s.extensions[UBlock.ID]?.let { ext ->
            val badge = activeTabId?.let { s.findTab(it) }?.extensionState?.get(UBlock.ID)?.browserAction?.badgeText
                ?: ext.browserAction?.badgeText
            UBlockUi(ext.enabled, UBlock.count(badge))
        }
    }
    val downloadLocation by components.settings.downloadLocation.collectAsStateWithLifecycle(null)
    val folderName = downloadLocation?.let(::folderLabel)

    fun share(url: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        if (subject.isNotBlank()) send.putExtra(Intent.EXTRA_SUBJECT, subject)
        context.startActivity(Intent.createChooser(send, null))
    }

    // ---- Firefox Translations, per tab (engine: TranslationsMiddleware) ----
    val tabTranslations by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.translationsState }
    val translateEngine by components.store.observeSlice(Unit) { s -> s.translationEngine }
    val phoneLanguage = remember { java.util.Locale.getDefault().language }
    // The last languages picked on each side (Recent in the lists), on the phone only.
    val translatePrefs = remember { context.getSharedPreferences("translate", Context.MODE_PRIVATE) }
    fun recentOf(side: String) = translatePrefs.getString("recent_$side", null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
    var recentFrom by remember { mutableStateOf(recentOf("from")) }
    var recentTo by remember { mutableStateOf(recentOf("to")) }
    val translateUi = tabTranslations?.let { t -> translateUiOf(t, translateEngine).copy(recentFrom = recentFrom, recentTo = recentTo) }
    fun dispatchTranslate(action: mozilla.components.browser.state.action.BrowserAction) = components.store.dispatch(action)
    val translateActions = TranslateActions(
        translate = { from, to ->
            activeTabId?.let { dispatchTranslate(TranslationsAction.TranslateAction(it, from, to, TranslationOptions(downloadModel = true))) }
        },
        showOriginal = { activeTabId?.let { dispatchTranslate(TranslationsAction.TranslateRestoreAction(it)) } },
        neverLanguage = { on -> activeTabId?.let { dispatchTranslate(TranslationsAction.UpdatePageSettingAction(it, TranslationPageSettingOperation.UPDATE_NEVER_TRANSLATE_LANGUAGE, on)) } },
        neverSite = { on -> activeTabId?.let { dispatchTranslate(TranslationsAction.UpdatePageSettingAction(it, TranslationPageSettingOperation.UPDATE_NEVER_TRANSLATE_SITE, on)) } },
        deleteLanguage = { code ->
            val name = languageName(code)
            val bytes = translateEngine.languageModels.orEmpty().firstOrNull { it.language?.code == code }?.size
            overlays.ask(
                "Delete $name?",
                listOfNotNull(
                    bytes?.let { "Frees %.0f MB on this phone.".format(it / (1L shl 20).toDouble()) },
                    "It downloads again the next time you translate $name.",
                ).joinToString(" "),
                "Delete",
            ) {
                components.store.dispatch(
                    TranslationsAction.ManageLanguageModelsAction(
                        mozilla.components.concept.engine.translate.ModelManagementOptions(
                            code,
                            mozilla.components.concept.engine.translate.ModelOperation.DELETE,
                            mozilla.components.concept.engine.translate.OperationLevel.LANGUAGE,
                        ),
                    ),
                )
                onToast(Toast("$name deleted from this phone"))
            }
        },
        alwaysLanguage = { on -> activeTabId?.let { dispatchTranslate(TranslationsAction.UpdatePageSettingAction(it, TranslationPageSettingOperation.UPDATE_ALWAYS_TRANSLATE_LANGUAGE, on)) } },
        picked = { side, code ->
            val list = (listOf(code) + recentOf(side).filter { it != code }).take(dev.nerzouille.koan.ui.theme.TranslateTokens.RECENT)
            translatePrefs.edit().putString("recent_$side", list.joinToString(",")).apply()
            if (side == "from") recentFrom = list else recentTo = list
        },
        pairChanged = { from, to ->
            activeTabId?.let { if (from.isNotEmpty() && from != to) dispatchTranslate(TranslationsAction.FetchTranslationDownloadSizeAction(it, Language(from), Language(to))) }
        },
    )

    // ---- the ⋯ menu ----
    MenuOverlays(
        components, overlays, scope, page, activeTabId, activeTab, private, engines, downloads, ublockUi,
        translateUi, translateActions, phoneLanguage, clipboard, onEditAddress, onNewTab, onToast, ::share,
    )

    // ---- a site asks for something (location, camera…); a page wants an app ----
    PermissionOverlays(components, overlays, scope, activeTabId, onToast)

    // ---- logins: fill from a chip (7f) or the full list (7e); save a new one; a page's other prompts ----
    LoginOverlays(components, overlays, scope, activeTabId)

    // ---- long press on a link or an image; the one card for it, a download, a duplicate ----
    LinkCardOverlay(components, overlays, scope, page, activeTabId, clipboard, onOpenNew, onToast, ::share)

    // ---- the tab counter's long-press menu; back's long-press list ----
    DockMenus(components, overlays, activeTabId, currentSpaceUuid, onNewTab, onCloseTab, onRestoreClosed)

    // ---- Settings, full screen, sliding up over everything ----
    SettingsOverlays(
        components, overlays, scope, engines, drawerStatus, folderName, ublockUi, translateEngine, translateActions,
        activeTabId, onSignIn, onOpenNew, onToast,
    )

    // ---- Downloads and History, full screen, over Settings too; a library page's ask card ----
    LibraryOverlays(components, overlays, scope, downloads, folderName, clipboard, activeTabId, onToast, onOpenNew, onRestoreClosed, ::share)

    // ---- sign out asks first ----
    SignOutOverlay(components, overlays)

    // ---- the tour / "What's new": full screen, on top of everything; leaving it counts as seen ----
    TourOverlays(components, overlays, scope, onOpenNew)
}

/** Find in page for the address row, while it is open. */
@Composable
fun rememberFindUi(components: Components, overlays: Overlays, activeTabId: String?): FindUi? {
    if (!overlays.finding || activeTabId == null) return null
    val result by components.store.observeSlice(activeTabId) { s -> s.findTab(activeTabId)?.content?.findResults?.lastOrNull() }
    val count = when {
        overlays.findQuery.isEmpty() -> ""
        result == null -> ""
        result!!.numberOfMatches == 0 -> "0/0"
        else -> "${result!!.activeMatchOrdinal + 1}/${result!!.numberOfMatches}"
    }
    return FindUi(
        count = count,
        onQuery = { q ->
            overlays.findQuery = q
            components.pageTools.find(activeTabId, q)
        },
        onNext = { forward -> if (overlays.findQuery.isNotEmpty()) components.pageTools.findNext(activeTabId, forward) },
        onClose = {
            overlays.finding = false
            overlays.findQuery = ""
            components.pageTools.stopFind(activeTabId)
        },
    )
}

/** A picked folder's name, from its tree uri ("primary:Documents/Books" → "Books"). */
internal fun folderLabel(uri: String): String =
    runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)) }.getOrNull()
        ?.substringAfter(':')?.substringAfterLast('/')?.ifEmpty { "Phone storage" }
        ?: "Picked folder"
