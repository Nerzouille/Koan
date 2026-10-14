package dev.nerzouille.koan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.AccountState
import dev.nerzouille.koan.data.SyncStatus
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.ui.bar.AddressRow
import dev.nerzouille.koan.ui.bar.DrawerContent
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.bar.DrawerState
import dev.nerzouille.koan.ui.bar.KoanBar
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.bar.WorkspaceSwitcher
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.overview.OverviewScreen
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.overview.SyncBadge
import dev.nerzouille.koan.ui.overview.SyncUi
import dev.nerzouille.koan.ui.overview.findTab
import kotlinx.coroutines.flow.first
import dev.nerzouille.koan.ui.overview.toModel
import dev.nerzouille.koan.ui.signin.FxAWeb
import dev.nerzouille.koan.ui.signin.SignInScreen
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.lib.state.ext.observeAsComposableState
import kotlin.math.roundToInt

/**
 * Page view and Space overview, with the one bottom bar on top of both: it morphs into the
 * workspace bar as the overview opens, and into the essentials/pinned drawer from there. M1: Room
 * holds fake spaces; opening a card loads its URL in the single live engine tab (real per-card tabs
 * arrive in M3).
 */
@Composable
fun KoanRoot(components: Components) {
    val spaces by components.spaces.spaces.collectAsStateWithLifecycle()
    val account by components.accounts.state.collectAsStateWithLifecycle()
    val syncStatus by components.sync.status.collectAsStateWithLifecycle()
    var signingIn by rememberSaveable { mutableStateOf(false) }
    // Focused stack card per space (uuid → card id): restored after process death too.
    val stackFocus = rememberSaveable { HashMap<String, String>() }
    var currentSpace by rememberSaveable { mutableIntStateOf(0) }
    // The space the user is on, by id: spaces added/removed on the desktop never move them.
    var currentSpaceUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var overviewOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val models = remember(spaces, activeTabId) { spaces.map { it.toModel(activeTabId) } }

    // Simple fade/scale for now; the hero morph is M4. Also drives the bar morph.
    val progress = remember { Animatable(if (overviewOpen) 1f else 0f) }
    LaunchedEffect(overviewOpen) {
        progress.animateTo(if (overviewOpen) 1f else 0f, Motion.Overview)
    }
    val overviewVisible by remember { derivedStateOf { overviewOpen || progress.value > 0f } }

    val scope = rememberCoroutineScope()
    val pager = remember { SpacePager(currentSpace, scope) }
    pager.count = spaces.size.coerceAtLeast(1)
    val density = LocalDensity.current
    pager.pageWidthPx = LocalWindowInfo.current.containerSize.width + with(density) { PagerTokens.PageGap.toPx() }
    // Cold start (e.g. after swiping Koan away): reopen on the space and tab saved on disk. After a
    // configuration change or a background kill, the saved instance state already has them.
    /** The live engine tab shows nothing yet (cold start). */
    fun engineIsBlank() = components.store.state.selectedTab?.content?.url.let { it.isNullOrEmpty() || it == "about:blank" }
    var placeRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (placeRestored) return@LaunchedEffect
        val place = components.lastPlace.read()
        place.spaceUuid?.let { currentSpaceUuid = it }
        val tabId = place.tabId
        if (tabId != null) {
            val tab = snapshotFlow { spaces }.first { it.isNotEmpty() }.firstNotNullOfOrNull { it.findTab(tabId) }
            if (tab != null) {
                activeTabId = tab.koanTabId
                // The engine starts on a blank tab after a cold start; the live page itself
                // (history, scroll) is restored by the engine from M3 on.
                // A loadUrl sent now could reach the start-up blank tab before its engine session
                // exists and get lost, so open the page as a fresh engine tab instead.
                if (engineIsBlank()) {
                    components.tabsUseCases.removeAllTabs()
                    components.tabsUseCases.addTab(tab.url, selectTab = true)
                }
            }
        }
        placeRestored = true
    }
    LaunchedEffect(activeTabId, placeRestored) {
        if (placeRestored) components.lastPlace.saveTab(activeTabId)
    }

    LaunchedEffect(pager) {
        snapshotFlow { pager.settled }.collect {
            currentSpace = it
            spaces.getOrNull(it)?.let { s -> currentSpaceUuid = s.space.uuid }
        }
    }
    LaunchedEffect(currentSpaceUuid, placeRestored) {
        val uuid = currentSpaceUuid
        if (placeRestored && uuid != null) components.lastPlace.saveSpace(uuid)
    }
    val spaceIds = spaces.map { it.space.uuid }
    LaunchedEffect(spaceIds, placeRestored) {
        // Wait for the saved place, so the first Room emission doesn't anchor on the first space.
        if (!placeRestored) return@LaunchedEffect
        val anchored = currentSpaceUuid?.let(spaceIds::indexOf) ?: -1
        val target = if (anchored >= 0) anchored else currentSpace.coerceIn(0, (spaceIds.size - 1).coerceAtLeast(0))
        if (target != pager.settled) pager.snapTo(target)
        currentSpace = target
        spaceIds.getOrNull(target)?.let { currentSpaceUuid = it }
    }
    val drawer = remember { DrawerState(scope) }
    LaunchedEffect(overviewOpen) { if (!overviewOpen) drawer.snapClosed() }

    // Blur steps for the cards behind the open drawer; RenderEffects are immutable and reusable.
    // Clamp, not Decal: Decal fades the edges to transparent and the page underneath (often a white
    // site) showed through as light strips on the left and right.
    val drawerBlurs = remember(density) {
        val steps = DrawerTokens.Blur.value.roundToInt()
        Array(steps + 1) { step -> if (step == 0) null else BlurEffect(step * density.density, step * density.density, TileMode.Clamp) }
    }

    fun loadInLiveTab(url: String) = components.sessionUseCases.loadUrl(url)
    fun openOverview() {
        editing = false
        pager.snapTo(currentSpace.coerceIn(0, pager.count - 1))
        overviewOpen = true
    }
    fun openTab(index: Int, tab: TabEntity) {
        currentSpace = index
        // Also load when the engine shows nothing (blank after a cold start), even for the current card.
        if (tab.koanTabId != activeTabId || engineIsBlank()) {
            activeTabId = tab.koanTabId
            loadInLiveTab(tab.url)
        }
        overviewOpen = false
    }
    fun newTab(index: Int) {
        currentSpace = index
        activeTabId = components.spaces.addTab(spaces[index].space.uuid, "about:blank", "New tab")
        loadInLiveTab("about:blank")
        overviewOpen = false
        editing = true
    }
    fun isPinned(id: String) = spaces.any { s -> (s.pinnedTabs + s.essentials).any { it.koanTabId == id } }
    fun stackItem(id: String) = spaces.firstNotNullOfOrNull { s -> s.stack.firstOrNull { it.id == id } }

    val signedIn = account is AccountState.SignedIn
    val sync = SyncUi(
        connect = account == AccountState.SignedOut,
        firstSync = syncStatus.let { it is SyncStatus.Syncing && it.first },
        badge = when (syncStatus) {
            is SyncStatus.Syncing -> SyncBadge.Syncing
            is SyncStatus.Failed, SyncStatus.NeedsSignIn -> SyncBadge.Attention
            is SyncStatus.Idle -> SyncBadge.None
        },
    )
    val drawerStatus = if (account == AccountState.SignedOut) {
        DrawerStatus.Connect
    } else if (!signedIn) {
        DrawerStatus.None
    } else {
        when (val st = syncStatus) {
            SyncStatus.NeedsSignIn -> DrawerStatus.SignInAgain
            is SyncStatus.Syncing -> DrawerStatus.Syncing
            is SyncStatus.Failed -> DrawerStatus.Synced(st.lastSyncAt, failed = true, offline = st.offline)
            is SyncStatus.Idle -> DrawerStatus.Synced(st.lastSyncAt)
        }
    }
    // First run (no account, nothing open yet): start in the overview, on the Connect card.
    var firstRunHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(account, spaces) {
        if (firstRunHandled || account == AccountState.Loading || spaces.isEmpty()) return@LaunchedEffect
        firstRunHandled = true
        if (account == AccountState.SignedOut && spaces.none { it.space.zenOrigin || it.stack.isNotEmpty() }) openOverview()
    }

    fun onSyncAction() {
        when {
            account == AccountState.SignedOut || syncStatus == SyncStatus.NeedsSignIn -> signingIn = true
            else -> components.sync.syncNow()
        }
    }

    Box(Modifier.fillMaxSize()) {
        BrowserScreen(
            components = components,
            editing = editing,
            bottomPadding = barZone(overview = false),
        )
        if (overviewVisible) {
            OverviewScreen(
                spaces = spaces,
                models = models,
                pager = pager,
                dock = barZone(overview = true),
                onOpenTab = ::openTab,
                onNewTab = ::newTab,
                onCloseTab = { id ->
                    val members = stackItem(id)?.tabs?.map { it.koanTabId } ?: listOf(id)
                    if (activeTabId in members) activeTabId = null
                    // A live pinned/essential card is only unloaded back to its row, never closed.
                    if (!isPinned(id)) components.spaces.closeTabs(members)
                },
                onReorder = { ids -> components.spaces.reorderStack(ids.mapNotNull(::stackItem)) },
                onDismiss = { overviewOpen = false },
                sync = sync,
                focus = stackFocus,
                onBadge = ::onSyncAction,
                onConnect = { signingIn = true },
                modifier = Modifier
                    .graphicsLayer {
                        val p = progress.value
                        alpha = p
                        val s = 1f + Motion.OVERVIEW_ZOOM * (1f - p)
                        scaleX = s
                        scaleY = s
                        renderEffect = drawerBlurs[(drawer.progress * (drawerBlurs.size - 1)).roundToInt()]
                    }
                    .drawWithContent {
                        drawContent()
                        val d = drawer.progress
                        if (d > 0.01f) drawRect(Color.Black, alpha = d * DrawerTokens.SCRIM)
                    },
            )
        }
        if (drawer.open) {
            // Tap outside the drawer closes it; nothing behind it takes input meanwhile.
            BackHandler { drawer.animateTo(false) }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            drawer.animateTo(false)
                        }
                    },
            )
        }

        val store = components.store
        val session = components.sessionUseCases
        val page by store.observeAsComposableState { state ->
            state.selectedTab?.content.let { c ->
                PageInfo(
                    url = c?.url.orEmpty(),
                    loading = c?.loading == true,
                    progress = (c?.progress ?: 0) / 100f,
                    canGoBack = c?.canGoBack == true,
                    canGoForward = c?.canGoForward == true,
                )
            }
        }
        val currentPage = page ?: PageInfo("", false, 0f, false, false)
        KoanBar(
            overviewOpen = overviewOpen,
            overview = { progress.value },
            pager = pager,
            drawer = drawer,
            loading = currentPage.loading,
            loadProgress = currentPage.progress,
            onSwipeUp = ::openOverview,
            drawerContent = { modifier ->
                DrawerContent(
                    spaces = models,
                    pager = pager,
                    drawer = drawer,
                    status = drawerStatus,
                    onOpenTab = { index, id -> spaces.getOrNull(index)?.findTab(id)?.let { openTab(index, it) } },
                    onToggleFolder = components.spaces::setFolderCollapsed,
                    onStatus = ::onSyncAction,
                    modifier = modifier,
                )
            },
            switcher = { modifier ->
                WorkspaceSwitcher(spaces.map { it.space }, pager, enabled = overviewOpen, modifier = modifier)
            },
            address = { modifier ->
                AddressRow(
                    page = currentPage,
                    tabCount = models.getOrNull(currentSpace)?.stack?.size ?: 0,
                    editing = editing,
                    overviewOpen = overviewOpen,
                    overview = { progress.value },
                    onEditingChange = { editing = it },
                    onSubmit = { input ->
                        editing = false
                        session.loadUrl(resolveInput(input))
                    },
                    onSearchFromOverview = { newTab(pager.settled) },
                    onBack = { session.goBack(store.state.selectedTabId) },
                    onForward = { session.goForward(store.state.selectedTabId) },
                    onReload = { session.reload() },
                    onStop = { session.stopLoading() },
                    onTabs = { if (overviewOpen) overviewOpen = false else openOverview() },
                    menuExtras = buildList {
                        when (val a = account) {
                            is AccountState.SignedIn -> {
                                add("Sync now" to { components.sync.syncNow() })
                                add("Sign out (${a.email})" to {
                                    FxAWeb.wipeWebSession()
                                    components.sync.signOut()
                                })
                            }
                            AccountState.SignedOut -> add("Sign in to Mozilla account" to { signingIn = true })
                            AccountState.Loading -> Unit
                        }
                        for (n in listOf(50, 150)) {
                            add("Fill space to $n tabs (dev)" to {
                                spaces.getOrNull(currentSpace)?.let { components.spaces.fillWithFakeTabs(it.space.uuid, n) }
                            })
                        }
                        add("Remove fake tabs (dev)" to { components.spaces.removeFakeTabs() })
                    },
                    modifier = modifier,
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Sign-in slides up over everything and back down when done.
        val signIn = remember { Animatable(0f) }
        LaunchedEffect(signingIn) { signIn.animateTo(if (signingIn) 1f else 0f, Motion.Sheet) }
        val signInVisible by remember { derivedStateOf { signingIn || signIn.value > 0f } }
        if (signInVisible) {
            SignInScreen(
                onLogin = { login ->
                    components.accounts.completeWebLogin(login.email, login.uid, login.sessionToken, login.keyFetchToken, login.unwrapBKey)
                    signingIn = false
                    components.sync.syncNow()
                    openOverview()
                },
                onClose = { signingIn = false },
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - signIn.value) * size.height
                },
            )
        }
    }
}
