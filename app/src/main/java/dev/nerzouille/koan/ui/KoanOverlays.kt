package dev.nerzouille.koan.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.menu.LoginChips
import dev.nerzouille.koan.ui.menu.UBlockPage
import dev.nerzouille.koan.ui.menu.UBlockUi
import dev.nerzouille.koan.engine.UBlock
import mozilla.components.browser.state.action.WebExtensionAction
import dev.nerzouille.koan.ui.menu.LoginSheet
import dev.nerzouille.koan.ui.menu.SaveLoginCard
import mozilla.components.concept.engine.prompt.PromptRequest
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.produceState
import dev.nerzouille.koan.engine.SiteAsk
import dev.nerzouille.koan.engine.SiteDecision
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.menu.PermissionPrompt
import dev.nerzouille.koan.ui.menu.SiteSettingsPage
import dev.nerzouille.koan.ui.menu.SiteSettingsUi
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.SecurityInfo
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest
import android.net.Uri
import android.provider.DocumentsContract
import dev.nerzouille.koan.ui.downloads.DownloadsScreen
import dev.nerzouille.koan.ui.history.ClearDataCard
import dev.nerzouille.koan.ui.history.HistoryScreen
import dev.nerzouille.koan.ui.menu.FilePrompt
import dev.nerzouille.koan.ui.menu.Confirm
import dev.nerzouille.koan.ui.kit.Toast
import mozilla.components.browser.state.state.content.DownloadState
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.data.AccountState
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.data.ThemeMode
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.bar.FindUi
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.menu.EssentialsAction
import dev.nerzouille.koan.ui.menu.LinkActions
import dev.nerzouille.koan.ui.menu.LinkMenu
import dev.nerzouille.koan.ui.menu.LinkTarget
import dev.nerzouille.koan.ui.menu.MenuActions
import dev.nerzouille.koan.ui.menu.MenuPage
import dev.nerzouille.koan.ui.menu.MenuRoot
import dev.nerzouille.koan.ui.menu.MenuState
import dev.nerzouille.koan.ui.menu.SignOutConfirm
import dev.nerzouille.koan.ui.settings.SettingsPage
import dev.nerzouille.koan.ui.settings.PasswordsUi
import dev.nerzouille.koan.ui.settings.authenticate
import dev.nerzouille.koan.data.Login
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.ui.settings.SettingsScreen
import dev.nerzouille.koan.ui.settings.SettingsUi
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.KoanSheet
import dev.nerzouille.koan.ui.sheet.SheetNav
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.signin.FxAWeb
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.update.UpdateCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.findTab

/** What opens over the page: the ⋯ menu, the long-press menu, the sign-out card, find, Settings. */
@Stable
class Overlays(scope: CoroutineScope, density: Density) {
    val menu = SheetState(scope, splineBasedDecay(density))
    val menuNav = SheetNav<MenuPage>(scope, MenuPage.Root)
    val linkCard = SheetState(scope, splineBasedDecay(density))
    var link by mutableStateOf<LinkTarget?>(null)
    val signOutCard = SheetState(scope, splineBasedDecay(density))
    var settingsOpen by mutableStateOf(false)
    var downloadsOpen by mutableStateOf(false)
    var historyOpen by mutableStateOf(false)
    val clearDataCard = SheetState(scope, splineBasedDecay(density))
    val clearDownloadsCard = SheetState(scope, splineBasedDecay(density))
    val clearSiteCard = SheetState(scope, splineBasedDecay(density))

    /** Logins: the full "Sign in with" list (7e), and "Save in Koan". */
    val loginCard = SheetState(scope, splineBasedDecay(density))
    val saveLoginCard = SheetState(scope, splineBasedDecay(density))

    /** A site's request waiting on the card, and what it asks for. */
    val permissionCard = SheetState(scope, splineBasedDecay(density))
    var permission by mutableStateOf<Pair<PermissionRequest, SiteAsk>?>(null)
    val settingsNav = SheetNav<SettingsPage>(scope, SettingsPage.Root)

    /** Find in page is open, and what was typed in it. */
    var finding by mutableStateOf(false)
    var findQuery by mutableStateOf("")

    /** The overview opened, or the tab changed: nothing stays open over the old page. */
    fun dismissAll() {
        menu.snapClosed()
        linkCard.snapClosed()
        signOutCard.snapClosed()
        finding = false
        findQuery = ""
    }
}

/**
 * The overlays, drawn over the page view and the bar. [onOpenNew]: a url as a new tab of the
 * current space ([select] = shown now, next to the current tab when [nextToCurrent]).
 */
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
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
    onSignIn: () -> Unit,
    devActions: List<Pair<String, () -> Unit>>,
    /** Quiet confirmations (the shared bottom toast). */
    onToast: (Toast) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val clipboard = LocalClipboardManager.current
    val session = components.sessionUseCases
    val menu = overlays.menu

    // The current page as the menu needs it: title (bookmarks, share), reader view.
    val title by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.title.orEmpty() }
    val reader by components.store.observeSlice(activeTabId) { s ->
        activeTabId?.let { s.findTab(it) }?.readerState.let { (it?.readerable == true) to (it?.active == true) }
    }
    val bookmarks by components.bookmarks.urls.collectAsStateWithLifecycle()
    val account by components.accounts.state.collectAsStateWithLifecycle()
    val theme by components.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
    val engine by components.settings.searchEngine.collectAsStateWithLifecycle(SearchEngine.DuckDuckGo)
    val suggestionsOn by components.settings.searchSuggestions.collectAsStateWithLifecycle(true)
    val downloads by components.store.observeSlice(Unit) { s -> s.downloads.values.toList() }
    // uBlock Origin on this page: on or off, and its badge (the tab's own, else the default).
    val ublockUi by components.store.observeSlice(activeTabId) { s ->
        s.extensions[UBlock.ID]?.let { ext ->
            val badge = activeTabId?.let { s.findTab(it) }?.extensionState?.get(UBlock.ID)?.browserAction?.badgeText
                ?: ext.browserAction?.badgeText
            UBlockUi(ext.enabled, UBlock.count(badge))
        }
    }
    val ublockPopup by components.store.observeSlice(Unit) { s -> s.extensions[UBlock.ID]?.popupSession }
    val blockedWeek by components.settings.blockedByDay.collectAsStateWithLifecycle(emptyMap())
    // Gecko's verdict; unknown (a download, a page still loading) falls back to the scheme.
    val secure by components.store.observeSlice(activeTabId) { s ->
        val content = activeTabId?.let { s.findTab(it) }?.content
        when (content?.securityInfo) {
            is SecurityInfo.Secure -> true
            is SecurityInfo.Insecure -> false
            else -> content?.url?.startsWith("https://") == true
        }
    }
    // Site settings' list: loaded when the page shows, again after each change.
    var decisionsVersion by remember { mutableStateOf(0) }
    val showingSite = overlays.menuNav.depth > 0
    val siteDecisions by produceState<List<SiteDecision>?>(null, page.url, decisionsVersion, showingSite) {
        value = if (showingSite && page.url.isNotEmpty()) components.sitePermissions.decisions(page.url) else null
    }
    val downloadLocation by components.settings.downloadLocation.collectAsStateWithLifecycle(null)
    val folderName = downloadLocation?.let(::folderLabel)
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        scope.launch { components.settings.setDownloadLocation(uri.toString()) }
    }

    fun share(url: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        if (subject.isNotBlank()) send.putExtra(Intent.EXTRA_SUBJECT, subject)
        context.startActivity(Intent.createChooser(send, null))
    }
    fun close() = menu.animateTo(Detent.Closed)

    // ---- the ⋯ menu ----
    val state = MenuState(
        bookmarked = page.url in bookmarks,
        readerAvailable = reader.first,
        readerActive = reader.second,
        essentials = when {
            activeTab == null || page.url.isEmpty() -> EssentialsAction.None
            !activeTab.essential -> EssentialsAction.Add
            activeTab.origin == TabOrigin.LOCAL -> EssentialsAction.Remove
            else -> EssentialsAction.FromZen
        },
        activeDownloads = downloads.count { !it.private && it.status in ACTIVE },
        secure = secure,
        ublock = ublockUi,
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
                    copyLink = { clipboard.setText(AnnotatedString(page.url)); close() },
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
                        overlays.historyOpen = true
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
                ),
            )
            MenuPage.UBlock -> UBlockPage(ublockPopup, pageScope) {
                // The panel closed with its page: so does its session.
                components.store.state.extensions[UBlock.ID]?.popupSession?.close()
                components.store.dispatch(WebExtensionAction.UpdatePopupSessionAction(UBlock.ID, popupSession = null))
            }
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

    // ---- a site asks for something (location, camera…) ----
    val request by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.permissionRequestsList?.firstOrNull() }
    val appRequest by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.appPermissionRequestsList?.firstOrNull() }
    val sites = components.sitePermissions
    fun consume(r: PermissionRequest) { activeTabId?.let { components.store.dispatch(ContentAction.ConsumePermissionsRequest(it, r)) } }
    LaunchedEffect(request) {
        val r = request ?: return@LaunchedEffect
        sites.autoAnswer(r)?.let { allow ->
            if (allow) r.grant() else r.reject()
            consume(r)
            return@LaunchedEffect
        }
        val ask = sites.askOf(r)
        if (ask == null) {
            r.reject()
            consume(r)
            return@LaunchedEffect
        }
        val remembered = r.uri?.let { sites.remembered(it, ask) }
        if (remembered != null) {
            if (remembered) r.grant() else r.reject()
            consume(r)
            return@LaunchedEffect
        }
        overlays.permission = r to ask
        overlays.permissionCard.animateTo(Detent.Compact)
    }
    val permissionCard = overlays.permissionCard
    fun answer(allow: Boolean, always: Boolean) {
        val (r, ask) = overlays.permission ?: return
        overlays.permission = null
        permissionCard.animateTo(Detent.Closed)
        scope.launch { sites.answer(r, ask, allow, always) }
        consume(r)
    }
    // Dismissed (tap outside, Back): blocked this time, nothing remembered.
    LaunchedEffect(permissionCard.open) { if (!permissionCard.open && overlays.permission != null) answer(allow = false, always = false) }
    KoanCard(permissionCard) { interactive ->
        val (r, ask) = overlays.permission ?: return@KoanCard
        PermissionPrompt(
            host = displayHost(r.uri.orEmpty()).ifEmpty { "This site" },
            ask = ask,
            interactive = interactive,
            onBlock = { answer(allow = false, always = true) },
            onAlways = { answer(allow = true, always = true) },
            onOnce = { answer(allow = true, always = false) },
        )
    }
    // Gecko needs Android's own permission too (location, camera, microphone); it asks for it itself.
    var pendingApp by remember { mutableStateOf<PermissionRequest?>(null) }
    val askAndroid = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val r = pendingApp ?: return@rememberLauncherForActivityResult
        pendingApp = null
        if (results.values.all { it }) r.grant() else r.reject()
        activeTabId?.let { components.store.dispatch(ContentAction.ConsumeAppPermissionsRequest(it, r)) }
    }
    LaunchedEffect(appRequest) {
        val r = appRequest ?: return@LaunchedEffect
        val wanted = r.permissions.flatMap { androidPermissions(it) }.distinct()
        if (wanted.isEmpty() || wanted.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            if (wanted.isEmpty()) r.reject() else r.grant()
            activeTabId?.let { components.store.dispatch(ContentAction.ConsumeAppPermissionsRequest(it, r)) }
        } else {
            pendingApp = r
            askAndroid.launch(wanted.toTypedArray())
        }
    }

    // ---- logins: fill from a chip (7f) or the full list (7e); save a new one on the phone ----
    val loginPrompt by components.store.observeSlice(activeTabId) { s ->
        activeTabId?.let { s.findTab(it) }?.content?.promptRequests
            ?.firstOrNull { it is PromptRequest.SelectLoginPrompt || it is PromptRequest.SaveLoginPrompt }
    }
    // A page's file field: Android's picker (and the camera).
    FilePrompt(components, activeTabId)
    fun consumePrompt(p: PromptRequest) { activeTabId?.let { components.store.dispatch(ContentAction.ConsumePromptRequestAction(it, p)) } }
    val select = loginPrompt as? PromptRequest.SelectLoginPrompt
    if (select != null && select.logins.isNotEmpty()) {
        fun use(login: mozilla.components.concept.storage.Login) {
            overlays.loginCard.animateTo(Detent.Closed)
            select.onConfirm(login)
            consumePrompt(select)
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            LoginChips(
                select.logins, onPick = ::use,
                onMore = { overlays.loginCard.animateTo(Detent.Compact) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(bottom = barZone(overview = false)),
            )
        }
        KoanCard(overlays.loginCard) { interactive ->
            LoginSheet(
                origin = select.logins.first().origin, logins = select.logins, interactive = interactive,
                onUse = ::use,
                onNotNow = {
                    overlays.loginCard.animateTo(Detent.Closed)
                    select.onDismiss()
                    consumePrompt(select)
                },
            )
        }
    }
    val save = loginPrompt as? PromptRequest.SaveLoginPrompt
    val entry = save?.logins?.firstOrNull()
    var savedBefore by remember { mutableStateOf(false) }
    LaunchedEffect(save) {
        if (save == null || entry == null) return@LaunchedEffect
        savedBefore = components.logins.forOrigin(entry.origin).any { it.username == entry.username }
        overlays.saveLoginCard.animateTo(Detent.Compact)
    }
    KoanCard(overlays.saveLoginCard) { interactive ->
        if (save == null || entry == null) return@KoanCard
        SaveLoginCard(
            origin = entry.origin, username = entry.username, update = savedBefore, interactive = interactive,
            onSave = {
                overlays.saveLoginCard.animateTo(Detent.Closed)
                scope.launch {
                    components.logins.saveLocal(
                        entry.origin, entry.formActionOrigin, entry.httpRealm, entry.usernameField, entry.passwordField,
                        entry.username, entry.password,
                    )
                }
                save.onConfirm(entry)
                consumePrompt(save)
            },
            onNotNow = {
                overlays.saveLoginCard.animateTo(Detent.Closed)
                save.onDismiss()
                consumePrompt(save)
            },
        )
    }

    // ---- long press on a link or an image ----
    val hit by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.hitResult }
    LaunchedEffect(hit) {
        val result = hit ?: return@LaunchedEffect
        activeTabId?.let(components.pageTools::consumeHitResult)
        val target = LinkTarget.of(result) ?: return@LaunchedEffect
        // A light tick as the card comes up: the press landed.
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        overlays.link = target
        overlays.linkCard.animateTo(Detent.Compact)
    }
    val linkCard = overlays.linkCard
    KoanCard(linkCard) { interactive ->
        val target = overlays.link ?: return@KoanCard
        LinkMenu(
            target, interactive,
            LinkActions(
                openNew = { url -> linkCard.animateTo(Detent.Closed); onOpenNew(url, true, true) },
                openBackground = { url -> linkCard.animateTo(Detent.Closed); onOpenNew(url, false, true) },
                copy = { url -> clipboard.setText(AnnotatedString(url)); linkCard.animateTo(Detent.Closed) },
                share = { url -> linkCard.animateTo(Detent.Closed); share(url, "") },
                saveImage = { url ->
                    linkCard.animateTo(Detent.Closed)
                    // Goes through the same downloads as a page's: no dialog, the toast says when it's saved.
                    activeTabId?.let { id ->
                        components.store.dispatch(
                            ContentAction.UpdateDownloadAction(id, DownloadState(url, skipConfirmation = true, referrerUrl = page.url)),
                        )
                    }
                },
            ),
        )
    }

    // ---- Settings, full screen, sliding up over everything ----
    val roles = remember { context.getSystemService(Context.ROLE_SERVICE) as RoleManager }
    var isDefault by remember { mutableStateOf(roles.isRoleHeld(RoleManager.ROLE_BROWSER)) }
    LifecycleResumeEffect(Unit) {
        isDefault = roles.isRoleHeld(RoleManager.ROLE_BROWSER)
        onPauseOrDispose {}
    }
    val askDefault = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = roles.isRoleHeld(RoleManager.ROLE_BROWSER)
    }
    // Passwords: the sites are listed freely; usernames and passwords only once unlocked, and only
    // while the Passwords pages are open.
    val loginSummaries by components.logins.summaries.collectAsStateWithLifecycle(emptyList())
    var unlockedLogins by remember { mutableStateOf<Map<String, Login>?>(null) }
    val inPasswords = overlays.settingsOpen && overlays.settingsNav.pages.any { it == SettingsPage.Passwords }
    LaunchedEffect(inPasswords) { if (!inPasswords) unlockedLogins = null }
    val slide = remember { Animatable(0f) }
    LaunchedEffect(overlays.settingsOpen) {
        slide.animateTo(if (overlays.settingsOpen) 1f else 0f, Motion.Sheet)
        if (!overlays.settingsOpen) overlays.settingsNav.reset()
    }
    val settingsVisible by remember { derivedStateOf { overlays.settingsOpen || slide.value > 0f } }
    if (settingsVisible) {
        val signedIn = account as? AccountState.SignedIn
        SettingsScreen(
            ui = SettingsUi(
                theme = theme,
                onTheme = { mode -> scope.launch { components.settings.setThemeMode(mode) } },
                engine = engine,
                onEngine = { e -> scope.launch { components.settings.setSearchEngine(e) } },
                suggestions = suggestionsOn,
                onSuggestions = { on -> scope.launch { components.settings.setSearchSuggestions(on) } },
                email = signedIn?.email,
                status = drawerStatus,
                onSyncNow = { components.sync.syncNow() },
                onSignIn = {
                    overlays.settingsOpen = false
                    onSignIn()
                },
                onSignOut = { overlays.signOutCard.animateTo(Detent.Compact) },
                isDefault = isDefault,
                onMakeDefault = { askDefault.launch(roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER)) },
                version = BuildConfig.VERSION_NAME,
                checkUpdate = { UpdateCheck.check(BuildConfig.VERSION_NAME) },
                onOpenUrl = { url ->
                    overlays.settingsOpen = false
                    onOpenNew(url, true, false)
                },
                devActions = devActions,
                downloadFolder = folderName,
                onPickDownloadFolder = { pickFolder.launch(null) },
                onDefaultDownloadFolder = { scope.launch { components.settings.setDownloadLocation(null) } },
                onShowDownloads = { overlays.downloadsOpen = true },
                ublock = ublockUi,
                ublockWeek = blockedWeek.values.sum(),
                onUblock = components.ublock::setEnabled,
                onInstallUblock = components.ublock::install,
                passwords = PasswordsUi(
                    summaries = loginSummaries,
                    unlocked = unlockedLogins,
                    onUnlock = { then ->
                        (context as? FragmentActivity)?.let { activity ->
                            authenticate(activity, "Unlock your passwords") {
                                // Unlocked at once; the logins fill in as they're opened (the tapped one first).
                                unlockedLogins = emptyMap()
                                if (then != null) overlays.settingsNav.push(SettingsPage.PasswordDetail(then))
                                scope.launch {
                                    fun add(logins: List<Login>) {
                                        val current = unlockedLogins ?: return
                                        unlockedLogins = current + logins.associateBy { it.guid }
                                    }
                                    then?.let { components.logins.openOne(it) }?.let { add(listOf(it)) }
                                    components.logins.openAll(::add)
                                }
                            }
                        }
                    },
                ),
            ),
            nav = overlays.settingsNav,
            onClose = { overlays.settingsOpen = false },
            modifier = Modifier.graphicsLayer { translationY = (1f - slide.value) * size.height },
        )
    }

    // ---- Downloads, full screen, over Settings too ----
    val downloadsSlide = remember { Animatable(0f) }
    LaunchedEffect(overlays.downloadsOpen) { downloadsSlide.animateTo(if (overlays.downloadsOpen) 1f else 0f, Motion.Sheet) }
    val downloadsVisible by remember { derivedStateOf { overlays.downloadsOpen || downloadsSlide.value > 0f } }
    if (downloadsVisible) {
        DownloadsScreen(
            downloads = downloads,
            folder = folderName ?: "Download",
            actions = components.downloads,
            onClear = { overlays.clearDownloadsCard.animateTo(Detent.Compact) },
            onClose = { overlays.downloadsOpen = false },
            modifier = Modifier.graphicsLayer { translationY = (1f - downloadsSlide.value) * size.height },
        )
    }
    val clearCard = overlays.clearDownloadsCard
    KoanCard(clearCard) { interactive ->
        Confirm(
            title = "Clear the download list?",
            text = "The files stay in ${folderName ?: "Download"}.",
            confirm = "Clear list",
            interactive = interactive,
            onConfirm = {
                clearCard.animateTo(Detent.Closed)
                components.downloads.clearHistory()
            },
            onCancel = { clearCard.animateTo(Detent.Closed) },
        )
    }

    // ---- History, full screen like Downloads, and Clear browsing data ----
    val historySlide = remember { Animatable(0f) }
    LaunchedEffect(overlays.historyOpen) { historySlide.animateTo(if (overlays.historyOpen) 1f else 0f, Motion.Sheet) }
    val historyVisible by remember { derivedStateOf { overlays.historyOpen || historySlide.value > 0f } }
    if (historyVisible) {
        val dao = components.database.dao()
        HistoryScreen(
            dao = dao,
            onOpen = { url ->
                overlays.historyOpen = false
                onOpenNew(url, true, false)
            },
            onRemove = { v ->
                scope.launch { dao.removeVisit(v.url) }
                onToast(Toast("Removed from history", "Undo") { scope.launch { dao.restoreVisit(v) } })
            },
            onClear = { overlays.clearDataCard.animateTo(Detent.Compact) },
            onClose = { overlays.historyOpen = false },
            modifier = Modifier.graphicsLayer { translationY = (1f - historySlide.value) * size.height },
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

    // ---- sign out asks first ----
    val signOutCard = overlays.signOutCard
    KoanCard(signOutCard) { interactive ->
        SignOutConfirm(
            interactive = interactive,
            onSignOut = {
                signOutCard.animateTo(Detent.Closed)
                overlays.settingsNav.pop()
                FxAWeb.wipeWebSession()
                components.sync.signOut()
            },
            onCancel = { signOutCard.animateTo(Detent.Closed) },
        )
    }
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

/** Downloads still going (for the menu's "2 active"). */
private val ACTIVE = setOf(DownloadState.Status.INITIATED, DownloadState.Status.DOWNLOADING, DownloadState.Status.PAUSED)

/** A picked folder's name, from its tree uri ("primary:Documents/Books" → "Books"). */
private fun folderLabel(uri: String): String =
    runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)) }.getOrNull()
        ?.substringAfter(':')?.substringAfterLast('/')?.ifEmpty { "Phone storage" }
        ?: "Picked folder"

/** The Android permissions Gecko needs for a page's request. */
private fun androidPermissions(p: Permission): List<String> = when (p) {
    is Permission.AppLocationCoarse -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    is Permission.AppLocationFine -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    is Permission.AppCamera -> listOf(Manifest.permission.CAMERA)
    is Permission.AppAudio -> listOf(Manifest.permission.RECORD_AUDIO)
    else -> emptyList()
}
