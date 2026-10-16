package dev.nerzouille.koan.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.nerzouille.koan.ui.settings.SettingsScreen
import dev.nerzouille.koan.ui.settings.SettingsUi
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.KoanSheet
import dev.nerzouille.koan.ui.sheet.SheetNav
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.signin.FxAWeb
import dev.nerzouille.koan.ui.theme.Motion
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
                ),
            )
        }
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
                onOpenUrl = { url ->
                    overlays.settingsOpen = false
                    onOpenNew(url, true, false)
                },
                devActions = devActions,
            ),
            nav = overlays.settingsNav,
            onClose = { overlays.settingsOpen = false },
            modifier = Modifier.graphicsLayer { translationY = (1f - slide.value) * size.height },
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
