package dev.nerzouille.koan.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.engine.AppTarget
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.SiteDecision
import dev.nerzouille.koan.engine.UBlock
import dev.nerzouille.koan.search.EngineDraft
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.search.origin
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.downloads.host
import dev.nerzouille.koan.ui.kit.KoanClipboard
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.copiedToast
import dev.nerzouille.koan.ui.menu.BackMenu
import dev.nerzouille.koan.ui.menu.Confirm
import dev.nerzouille.koan.ui.menu.EssentialsAction
import dev.nerzouille.koan.ui.menu.MenuActions
import dev.nerzouille.koan.ui.menu.MenuPage
import dev.nerzouille.koan.ui.menu.MenuRoot
import dev.nerzouille.koan.ui.menu.MenuState
import dev.nerzouille.koan.ui.menu.SiteSettingsPage
import dev.nerzouille.koan.ui.menu.SiteSettingsUi
import dev.nerzouille.koan.ui.menu.TabMenu
import dev.nerzouille.koan.ui.menu.TranslateActions
import dev.nerzouille.koan.ui.menu.TranslatePage
import dev.nerzouille.koan.ui.menu.TranslateUi
import dev.nerzouille.koan.ui.menu.UBlockPage
import dev.nerzouille.koan.ui.menu.UBlockUi
import dev.nerzouille.koan.ui.menu.languageName
import dev.nerzouille.koan.ui.settings.SettingsPage
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.KoanSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.SecurityInfo
import mozilla.components.browser.state.state.TranslationsBrowserState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.concept.engine.translate.TranslationOperation

/** The ⋯ menu (its root page, uBlock, Translate, Site settings) and the clear-site-data card. */
@Composable
internal fun MenuOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    page: PageInfo,
    activeTabId: String?,
    activeTab: TabEntity?,
    private: Boolean,
    engines: List<SearchEngine>,
    downloads: List<DownloadState>,
    ublockUi: UBlockUi?,
    translateUi: TranslateUi?,
    translateActions: TranslateActions,
    phoneLanguage: String,
    clipboard: KoanClipboard,
    onEditAddress: () -> Unit,
    onNewTab: () -> Unit,
    onToast: (Toast) -> Unit,
    share: (url: String, subject: String) -> Unit,
) {
    val session = components.sessionUseCases
    val menu = overlays.menu
    fun dispatchTranslate(action: mozilla.components.browser.state.action.BrowserAction) = components.store.dispatch(action)
    // The current page as the menu needs it: title (bookmarks, share), reader view.
    val title by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.title.orEmpty() }
    val reader by components.store.observeSlice(activeTabId) { s ->
        activeTabId?.let { s.findTab(it) }?.readerState.let { (it?.readerable == true) to (it?.active == true) }
    }
    val bookmarks by components.bookmarks.urls.collectAsStateWithLifecycle()
    // A search engine the page declares (OpenSearch): asked each time the menu opens on a page (the
    // page may have added it since); hidden when Koan already has an engine on that site.
    val declared by produceState<Pair<String, EngineDraft?>?>(null, page.url, menu.open) {
        val tab = activeTabId
        if (menu.open && tab != null && page.url.isNotEmpty()) value = page.url to components.pageEngines.find(tab)
    }
    val newEngine = declared?.takeIf { it.first == page.url }?.second
        ?.takeIf { d -> engines.none { it.home.equals(origin(d.url), ignoreCase = true) } }
    val ublockPopup by components.store.observeSlice(Unit) { s -> s.extensions[UBlock.ID]?.popupSession }
    // Gecko's verdict; unknown (a download, a page still loading) falls back to the scheme.
    val secure by components.store.observeSlice(activeTabId) { s ->
        val content = activeTabId?.let { s.findTab(it) }?.content
        when (content?.securityInfo) {
            is SecurityInfo.Secure -> true
            is SecurityInfo.Insecure -> false
            else -> content?.url?.startsWith("https://") == true
        }
    }
    // "Open in YouTube": looked up when the menu opens on a page (installed apps can change).
    val openInApp by produceState<AppTarget?>(null, menu.open, page.url) {
        value = if (menu.open && page.url.isNotEmpty()) withContext(Dispatchers.Default) { components.externalLinks.appFor(page.url) } else null
    }
    // Site settings' list: loaded when the page shows, again after each change.
    var decisionsVersion by remember { mutableStateOf(0) }
    val showingSite = overlays.menuNav.depth > 0
    val siteDecisions by produceState<List<SiteDecision>?>(null, page.url, decisionsVersion, showingSite) {
        value = if (showingSite && page.url.isNotEmpty()) components.sitePermissions.decisions(page.url) else null
    }
    fun close() = menu.animateTo(Detent.Closed)
    // ---- the ⋯ menu ----
    // Private tabs: is tracking protection on for this site (no exception)?
    var protections by remember(activeTabId, page.url) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(activeTabId, page.url, private) {
        val id = activeTabId
        if (!private || id == null || page.url.isEmpty()) return@LaunchedEffect
        components.trackingProtection.containsException(id) { excepted -> protections = !excepted }
    }
    val state = MenuState(
        private = private,
        protections = protections,
        bookmarked = page.url in bookmarks,
        readerAvailable = reader.first,
        readerActive = reader.second,
        essentials = when {
            activeTab == null || page.url.isEmpty() -> EssentialsAction.None
            !activeTab.essential -> EssentialsAction.Add
            activeTab.origin == TabOrigin.LOCAL -> EssentialsAction.Remove
            else -> EssentialsAction.FromZen
        },
        activeDownloads = downloads.count { it.status in ACTIVE },
        secure = secure,
        ublock = ublockUi,
        searchEngine = newEngine,
        openInApp = openInApp,
        translate = translateUi?.takeIf { page.url.isNotEmpty() && (it.pageLanguage != null || it.translated) }?.let {
            when {
                it.translated -> "Translated · show original"
                it.working -> "Translating…"
                else -> "Translate page"
            }
        },
    )
    KoanSheet(menu, overlays.menuNav) { p, pageScope ->
        when (p) {
            MenuPage.Root -> MenuRoot(
                page = page,
                state = state,
                scope = pageScope,
                actions = MenuActions(
                    back = { session.goBack(activeTabId) },
                    forward = { session.goForward(activeTabId) },
                    reload = { session.reload(activeTabId) },
                    stop = { session.stopLoading(activeTabId) },
                    editAddress = { close(); onEditAddress() },
                    copyLink = { clipboard.setText(AnnotatedString(page.url)); copiedToast("Link")?.let(onToast); close() },
                    protections = { on ->
                        val id = activeTabId
                        if (id != null) {
                            // Kept for the private session (until the last private tab closes).
                            if (on) components.trackingProtection.removeException(id)
                            else components.trackingProtection.addException(id, persistInPrivateMode = true)
                            protections = on
                            session.reload(id)
                        }
                    },
                    newTab = { close(); onNewTab() },
                    bookmark = { components.bookmarks.toggle(page.url, title) },
                    share = { close(); share(page.url, title) },
                    find = {
                        close()
                        overlays.findQuery = ""
                        overlays.finding = true
                    },
                    desktopSite = { on -> activeTabId?.let { session.requestDesktopSite(on, it) } },
                    reader = { activeTabId?.let(components.pageTools::toggleReader); close() },
                    essentials = {
                        val tab = activeTab
                        if (tab != null) {
                            if (state.essentials == EssentialsAction.Remove) {
                                components.spaces.removeFromEssentials(tab)
                            } else {
                                components.spaces.addToEssentials(tab, page.url, title)
                            }
                        }
                        close()
                    },
                    settings = {
                        close()
                        overlays.settingsOpen = true
                    },
                    downloads = {
                        close()
                        overlays.downloadsOpen = true
                    },
                    history = {
                        close()
                        overlays.openHistory(fromPage = activeTabId != null)
                    },
                    siteSettings = { overlays.menuNav.push(MenuPage.SiteSettings) },
                    ublockToggle = components.ublock::setEnabled,
                    ublockOpen = {
                        overlays.menuNav.push(MenuPage.UBlock)
                        // Its button opens its panel; the panel arrives in the store as a session.
                        val s = components.store.state
                        val tabAction = activeTabId?.let { s.findTab(it) }?.extensionState?.get(UBlock.ID)?.browserAction
                        (tabAction ?: s.extensions[UBlock.ID]?.browserAction)?.onClick?.invoke()
                    },
                    ublockInstall = components.ublock::install,
                    openInApp = {
                        close()
                        openInApp?.let { app ->
                            if (!components.externalLinks.openIn(app, page.url)) onToast(Toast("Couldn't open ${app.label}"))
                        }
                    },
                    translate = {
                        activeTabId?.let { id ->
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_PAGE_SETTINGS))
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_SUPPORTED_LANGUAGES))
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_LANGUAGE_MODELS))
                            translateUi?.pageLanguage?.let { from -> translateActions.pairChanged(from, phoneLanguage) }
                        }
                        overlays.menuNav.push(MenuPage.Translate)
                    },
                    addSearchEngine = {
                        val draft = newEngine
                        close()
                        if (draft != null) {
                            overlays.settingsOpen = true
                            overlays.settingsNav.push(SettingsPage.SearchEngines)
                            overlays.settingsNav.push(SettingsPage.EditEngine(null, draft))
                        }
                    },
                ),
            )
            MenuPage.UBlock -> UBlockPage(ublockPopup, pageScope) {
                // The panel closed with its page: so does its session.
                components.store.state.extensions[UBlock.ID]?.popupSession?.close()
                components.store.dispatch(WebExtensionAction.UpdatePopupSessionAction(UBlock.ID, popupSession = null))
            }
            MenuPage.Translate -> TranslatePage(
                translateUi ?: TranslateUi(null, emptyList(), emptyList(), false, false, null, null, false, false, unsupported = false),
                target = phoneLanguage,
                actions = translateActions,
                scope = pageScope,
            )
            MenuPage.SiteSettings -> SiteSettingsPage(
                SiteSettingsUi(
                    url = page.url,
                    secure = secure,
                    decisions = siteDecisions,
                    onSet = { d, allow ->
                        scope.launch {
                            components.sitePermissions.set(page.url, d, allow)
                            decisionsVersion++
                        }
                    },
                    readerAvailable = reader.first,
                    readerActive = reader.second,
                    onReader = { activeTabId?.let(components.pageTools::toggleReader) },
                    onClearData = { overlays.clearSiteCard.animateTo(Detent.Compact) },
                ),
                pageScope,
            )
        }
    }
    val clearSite = overlays.clearSiteCard
    KoanCard(clearSite) { interactive ->
        Confirm(
            title = "Clear data for ${displayHost(page.url)}?",
            text = "Its cookies, stored data and permissions go: you'll be signed out of it, and it asks again next time.",
            confirm = "Clear site data",
            interactive = interactive,
            onConfirm = {
                clearSite.animateTo(Detent.Closed)
                val host = displayHost(page.url)
                scope.launch {
                    val done = components.sitePermissions.clearSiteData(page.url)
                    decisionsVersion++
                    session.reload(activeTabId)
                    // Success needs no toast: the card asked first and the list empties. Only a failure says so.
                    if (!done) onToast(Toast("Couldn't clear all of $host's data"))
                }
            },
            onCancel = { clearSite.animateTo(Detent.Closed) },
        )
    }
}

/** Downloads still going (for the menu's "2 active"). */
private val ACTIVE = setOf(DownloadState.Status.INITIATED, DownloadState.Status.DOWNLOADING, DownloadState.Status.PAUSED)

/** The tab counter's long-press menu, and back's long-press list (the pages before this one). */
@Composable
internal fun DockMenus(
    components: Components,
    overlays: Overlays,
    activeTabId: String?,
    currentSpaceUuid: String?,
    onNewTab: () -> Unit,
    onCloseTab: () -> Unit,
    onRestoreClosed: (String) -> Unit,
) {
    // ---- the tab counter's long-press menu ----
    val lastClosed by remember(currentSpaceUuid) {
        currentSpaceUuid?.let { components.database.dao().observeLastClosed(it) } ?: flowOf(null)
    }.collectAsStateWithLifecycle(null)
    TabMenu(
        open = overlays.tabMenuOpen,
        onDismiss = { overlays.tabMenuOpen = false },
        onNewTab = { overlays.tabMenuOpen = false; onNewTab() },
        onCloseTab = { overlays.tabMenuOpen = false; onCloseTab() },
        onReopenClosed = lastClosed?.let { closed -> { overlays.tabMenuOpen = false; onRestoreClosed(closed.koanTabId) } },
    )

    // ---- back's long-press list: the pages before this one ----
    val backHistory by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.history }
    BackMenu(
        open = overlays.backMenuOpen,
        history = backHistory,
        onDismiss = { overlays.backMenuOpen = false },
        onGoTo = { index ->
            overlays.backMenuOpen = false
            activeTabId?.let { components.sessionUseCases.goToHistoryIndex(index, it) }
        },
    )
}

/** Deleting a downloaded translation language asks first. */
@Composable
internal fun LanguageDeleteOverlay(components: Components, overlays: Overlays, translateEngine: TranslationsBrowserState, onToast: (Toast) -> Unit) {
    val languageDeleteCard = overlays.languageDeleteCard
    KoanCard(languageDeleteCard) { interactive ->
        val code = overlays.languageToDelete ?: return@KoanCard
        val name = languageName(code)
        val bytes = translateEngine.languageModels.orEmpty().firstOrNull { it.language?.code == code }?.size
        Confirm(
            title = "Delete $name?",
            text = listOfNotNull(
                bytes?.let { "Frees %.0f MB on this phone.".format(it / (1L shl 20).toDouble()) },
                "It downloads again the next time you translate $name.",
            ).joinToString(" "),
            confirm = "Delete",
            interactive = interactive,
            onConfirm = {
                languageDeleteCard.animateTo(Detent.Closed)
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
            },
            onCancel = { languageDeleteCard.animateTo(Detent.Closed) },
        )
    }
}
