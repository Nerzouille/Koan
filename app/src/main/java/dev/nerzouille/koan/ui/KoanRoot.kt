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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.data.AccountState
import dev.nerzouille.koan.data.SyncStatus
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.search.resolveShared
import dev.nerzouille.koan.search.Hit
import dev.nerzouille.koan.search.completion
import dev.nerzouille.koan.search.parseQuery
import dev.nerzouille.koan.ui.search.SearchActions
import dev.nerzouille.koan.ui.search.SearchField
import dev.nerzouille.koan.ui.search.SearchSheet
import dev.nerzouille.koan.ui.search.SearchBackdrop
import dev.nerzouille.koan.ui.theme.SearchTokens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import dev.nerzouille.koan.ui.search.TabTarget
import dev.nerzouille.koan.ui.search.rememberSearch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.thumbs.BakedBlur
import dev.nerzouille.koan.ui.bar.AddressRow
import dev.nerzouille.koan.ui.bar.DrawerContent
import dev.nerzouille.koan.ui.bar.drawerBackdrop
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.bar.KoanBar
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.bar.WorkspaceSwitcher
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.kit.KoanToastHost
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.downloads.host
import dev.nerzouille.koan.ui.downloads.sizeText
import mozilla.components.browser.state.state.content.DownloadState
import dev.nerzouille.koan.ui.kit.ToastState
import dev.nerzouille.koan.ui.bar.SwipeTarget
import dev.nerzouille.koan.ui.bar.BarGestures
import dev.nerzouille.koan.ui.bar.SpaceScrub
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.browser.TabSwipe
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.theme.BarTokens
import androidx.compose.animation.splineBasedDecay
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.browser.CoverTarget
import dev.nerzouille.koan.ui.browser.DockState
import dev.nerzouille.koan.ui.browser.observeSlice
import androidx.compose.runtime.SideEffect
import mozilla.components.browser.state.selector.findTab
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
import dev.nerzouille.koan.ui.theme.KoanTheme
import kotlinx.coroutines.launch
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens

/**
 * Page view and Space overview, with the one bottom bar on top of both: it morphs into the
 * workspace bar as the overview opens, and into the essentials/pinned drawer from there. Every card
 * is a real engine tab, managed by [dev.nerzouille.koan.engine.TabManager].
 */
@Composable
fun KoanRoot(components: Components) {
    val spaces by components.spaces.spaces.collectAsStateWithLifecycle()
    val account by components.accounts.state.collectAsStateWithLifecycle()
    val syncStatus by components.sync.status.collectAsStateWithLifecycle()
    var signingIn by rememberSaveable { mutableStateOf(false) }
    // Each space's current tab (uuid → card id last opened there): the overview centres on it.
    val currentTabs = rememberSaveable { HashMap<String, String>() }
    var currentSpace by rememberSaveable { mutableIntStateOf(0) }
    // The space the user is on, by id: spaces added/removed on the desktop never move them.
    var currentSpaceUuid by rememberSaveable { mutableStateOf<String?>(null) }
    val tabs = components.tabs
    val activeTabId by tabs.current.collectAsStateWithLifecycle()
    val blankId by tabs.blank.collectAsStateWithLifecycle()
    val tabsStarted by tabs.started.collectAsStateWithLifecycle()
    var overviewOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    // A new tab always starts with the search sheet open.
    LaunchedEffect(blankId) { if (blankId != null) editing = true }
    val palette = KoanTheme.colors
    val searchEngine by components.settings.searchEngine.collectAsStateWithLifecycle(SearchEngine.DuckDuckGo)
    val models = remember(spaces, activeTabId, palette) { spaces.map { it.toModel(activeTabId, palette) } }

    // Simple fade/scale for now; the hero morph is M4. Also drives the bar morph. A swipe on the
    // dock drives it 1:1; otherwise it springs.
    val motion = remember { OverviewMotion(overviewOpen) }
    // The open motion has finished: the overview may now do the work it put off (thumbs/BakedBlur,
    // composing the neighbouring spaces).
    var overviewSettled by remember { mutableStateOf(false) }
    LaunchedEffect(overviewOpen, motion.dragging) {
        overviewSettled = false
        BakedBlur.onOverviewMotion(running = true, open = overviewOpen)
        if (motion.dragging) return@LaunchedEffect
        motion.settle(overviewOpen)
        BakedBlur.onOverviewMotion(running = false, open = overviewOpen)
        overviewSettled = overviewOpen
    }
    val overviewVisible by remember { derivedStateOf { overviewOpen || motion.progress > 0f } }

    val scope = rememberCoroutineScope()
    val pager = remember { SpacePager(currentSpace, scope) }
    pager.count = spaces.size.coerceAtLeast(1)
    val density = LocalDensity.current
    pager.pageWidthPx = LocalWindowInfo.current.containerSize.width + with(density) { PagerTokens.PageGap.toPx() }
    // Cold start (e.g. after swiping Koan away): reopen on the space saved on disk (TabManager
    // reopens the tab). After a configuration change or a background kill, the saved instance state
    // already has it.
    var placeRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (placeRestored) return@LaunchedEffect
        components.lastPlace.read().spaceUuid?.let { currentSpaceUuid = it }
        placeRestored = true
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
    motion.distancePx = with(density) { BarTokens.OverviewDrag.toPx() }
    val drawer = remember(density) { SheetState(scope, splineBasedDecay(density)) }
    LaunchedEffect(overviewOpen) { if (!overviewOpen) drawer.snapClosed() }

    // The ⋯ menu, the long-press menu, find, Settings (see KoanOverlays).
    val overlays = remember(density) { Overlays(scope, density) }
    // The search sheet's backdrop: fades with editing, its fully blurred part starts at searchTop (px).
    val searchShown = animateFloatAsState(if (editing && !overviewOpen) 1f else 0f, tween(SearchTokens.SHOW_MS), label = "search")
    val searchVisible by remember { derivedStateOf { searchShown.value > 0f } }
    val searchTop = remember { mutableFloatStateOf(Float.MAX_VALUE) }
    LaunchedEffect(editing) { if (editing) searchTop.floatValue = Float.MAX_VALUE }
    // The address field's text while editing, and what it finds.
    val searchField = remember { SearchField() }
    val suggestions by components.settings.searchSuggestions.collectAsStateWithLifecycle(true)
    val search = rememberSearch(searchField, components.database.dao(), spaces, models, activeTabId, searchEngine, suggestions)
    val toasts = remember { ToastState() }
    LaunchedEffect(overviewOpen, activeTabId) { overlays.dismissAll() }

    // Blur radii baked for the cards behind the open drawer (weakest first, see drawerBackdrop).
    val drawerBlurs = remember(density) {
        val levels = DrawerTokens.BLUR_LEVELS
        val max = DrawerTokens.Blur.value * density.density
        List(levels) { i -> max * (i + 1) / levels }
    }

    // The page cover zooms the tapped card's screenshot in (read when the cover starts).
    val zoomIn = remember { mutableStateOf(false) }
    val dock = remember { DockState(scope) }
    val tabSwipe = remember { TabSwipe(scope, tabs.current) }
    tabSwipe.pageWidthPx = LocalWindowInfo.current.containerSize.width + with(density) { BarTokens.TabSwipeGap.toPx() }
    SideEffect { dock.locked = overviewOpen || editing || overlays.finding }
    LaunchedEffect(activeTabId) { dock.snapShown() }

    fun openOverview() {
        // A chain of tab swipes still resting: the overview shows the live tab, nothing opens later.
        tabSwipe.cancel()
        editing = false
        tabs.cancelBlank()
        tabs.captureCurrent("overview opening")
        val index = currentSpace.coerceIn(0, pager.count - 1)
        pager.snapTo(index)
        spaces.getOrNull(index)?.let { s ->
            val current = s.stack.indexOfFirst { item -> item.tabs.any { it.koanTabId == activeTabId } }
            tabs.prefetchThumbnails(s, if (current >= 0) current else s.stack.lastIndex)
        }
        overviewOpen = true
    }
    fun openTab(index: Int, tab: TabEntity) {
        // From the page view (a scrub, a swipe to a card in another space) the pager follows too:
        // the next scrub starts from it and it saves the place.
        if (index != pager.settled) pager.snapTo(index)
        currentSpace = index
        spaces.getOrNull(index)?.let { s ->
            // A split view's card has its own id.
            val card = s.stack.firstOrNull { item -> item.tabs.any { it.koanTabId == tab.koanTabId } }?.id
            currentTabs[s.space.uuid] = card ?: tab.koanTabId
        }
        zoomIn.value = overviewOpen
        // From the overview the page was captured when it opened; from the page view, now.
        tabs.open(tab, captureLeaving = !overviewOpen)
        overviewOpen = false
    }
    fun newTab(index: Int) {
        val space = spaces.getOrNull(index) ?: return
        currentSpace = index
        currentTabs[space.space.uuid] = tabs.newTab(space.space.uuid)
        zoomIn.value = false
        overviewOpen = false
        editing = true
    }
    fun stackItem(id: String) = spaces.firstNotNullOfOrNull { s -> s.stack.firstOrNull { it.id == id } }
    fun tabById(id: String) = spaces.firstNotNullOfOrNull { s ->
        s.stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }
            ?: (s.pinnedTabs + s.essentials).firstOrNull { it.koanTabId == id }
    }
    /** A url as a new tab of the current space; [nextToCurrent]: right after the current card. */
    fun openNewTab(url: String, select: Boolean, nextToCurrent: Boolean) {
        val index = currentSpace.coerceIn(0, (spaces.size - 1).coerceAtLeast(0))
        val space = spaces.getOrNull(index) ?: return
        val opener = if (nextToCurrent) activeTabId?.let(::tabById) else null
        val id = tabs.openNew(space.space.uuid, url, opener, select)
        if (!select) {
            toasts.show(
                Toast("Opened in background", "Switch") {
                    spaces.withIndex().firstNotNullOfOrNull { (i, s) ->
                        s.stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }?.let { i to it }
                    }?.let { (i, tab) -> openTab(i, tab) }
                },
            )
            return
        }
        currentTabs[space.space.uuid] = id
        editing = false
        zoomIn.value = false
        overviewOpen = false
    }
    // Links and text other apps send (default browser, share): a new tab in the current space.
    LaunchedEffect(tabsStarted, spaces.isEmpty(), placeRestored) {
        if (!tabsStarted || spaces.isEmpty() || !placeRestored) return@LaunchedEffect
        components.incoming.collect { incoming ->
            if (incoming == null) return@collect
            components.incoming.value = null
            overlays.settingsOpen = false
            openNewTab(if (incoming.isUrl) incoming.text else resolveShared(incoming.text, searchEngine), select = true, nextToCurrent = false)
        }
    }
    /** A search result picked: an open tab is switched to (in its space), any other page loads here. */
    fun openHit(hit: Hit) {
        editing = false
        val target = hit.payload as? TabTarget
        val tab = target?.let { tabById(it.tabId) }
        if (target != null && tab != null) openTab(target.spaceIndex, tab) else tabs.load(hit.url)
    }
    /** Go on the keyboard: "@wiki koan" searches there, the completed top hit opens, else url or search. */
    fun submitSearch(input: String) {
        val parsed = parseQuery(input)
        val top = search.results.top
        when {
            parsed.shortcut != null -> {
                editing = false
                tabs.load(if (parsed.rest.isEmpty()) parsed.shortcut.home else parsed.shortcut.searchUrl(parsed.rest))
            }
            parsed.scope != null -> search.results.let { r -> (r.tabs + r.history + r.bookmarks).firstOrNull() }?.let(::openHit)
            top != null && input.equals(completion(searchField.typed, top) ?: "\u0000", ignoreCase = true) -> openHit(top)
            else -> {
                editing = false
                tabs.load(resolveInput(input, searchEngine))
            }
        }
    }
    /** The tab a space shows when switched to from the page view: its current card, else its newest. */
    fun lastTabOf(index: Int): TabEntity? {
        val s = spaces.getOrNull(index) ?: return null
        val item = currentTabs[s.space.uuid]?.let { id -> s.stack.firstOrNull { it.id == id } } ?: s.stack.lastOrNull()
        return item?.first
    }
    /**
     * The space holding the current tab's card (a live pinned tab or essential has one too, and an
     * essential's card may live in another space than the one it was opened from), and the card's
     * index in its stack.
     */
    fun currentCard(): Pair<Int, Int>? {
        for ((si, s) in spaces.withIndex()) {
            val i = s.stack.indexOfFirst { item -> item.tabs.any { it.koanTabId == activeTabId } }
            if (i >= 0) return si to i
        }
        return null
    }
    /** Leaving the overview without picking a card: back to the current tab, or the space's newest. */
    fun dismissOverview() {
        if (activeTabId != null && tabById(activeTabId!!) != null) {
            zoomIn.value = false
            overviewOpen = false
            return
        }
        val index = pager.settled
        lastTabOf(index)?.let { openTab(index, it) }
    }
    // Back on a page: its space is the current one again (the overview may have been left on
    // another), so the tab count and the next scrub match the page. Once the overview is gone,
    // so it never jumps to another space while it fades out. Also at launch, once the spaces and
    // the saved place are in.
    LaunchedEffect(overviewOpen, activeTabId, spaces.isEmpty(), placeRestored) {
        if (overviewOpen || spaces.isEmpty() || !placeRestored) return@LaunchedEffect
        snapshotFlow { motion.progress }.first { it <= 0f }
        currentCard()?.first?.let { index ->
            if (index != pager.settled) pager.snapTo(index)
            currentSpace = index
        }
    }
    /** The current tab's neighbours in its stack (previous, next), for a swipe on the dock. */
    fun tabNeighbours(): Pair<TabEntity?, TabEntity?> {
        val (si, i) = currentCard() ?: return null to null
        val stack = spaces[si].stack
        return stack.getOrNull(i - 1)?.first to stack.getOrNull(i + 1)?.first
    }
    // Their screenshots are decoded ahead, so a swipe shows them from its first frame.
    LaunchedEffect(activeTabId, spaces) {
        val (previous, next) = tabNeighbours()
        ThumbnailStore.prefetch(listOfNotNull(previous?.thumbnailKey, next?.thumbnailKey))
    }
    val bar = remember { BarGestures(pager, drawer, motion, tabSwipe, SpaceScrub(scope)) }
    bar.overviewOpen = { overviewOpen }
    bar.onOverviewDragStart = ::openOverview
    bar.onOverviewSettle = { open -> if (!open) dismissOverview() }
    bar.tabStrip = {
        currentCard()?.let { (si, i) -> spaces[si].stack.map { it.first } to i }
    }
    bar.onTabSwipe = { tab -> openTab(currentCard()?.first ?: currentSpace, tab) }
    bar.onSpaceSwipe = { index ->
        currentSpace = index
        val tab = lastTabOf(index)
        if (tab != null) openTab(index, tab) else openOverview()
    }

    // No tab to show (first run, or the last one was closed in Zen): the overview.
    LaunchedEffect(tabsStarted, activeTabId, spaces.isEmpty()) {
        if (tabsStarted && activeTabId == null && spaces.isNotEmpty() && !overviewOpen) openOverview()
    }

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

    val canGoBack by components.store.observeSlice(activeTabId) { state ->
        activeTabId?.let { state.findTab(it) }?.content?.canGoBack == true
    }
    // Page view: Back goes through the page's history, then to the overview.
    BackHandler(enabled = !overviewOpen && !editing) {
        val id = activeTabId
        if (canGoBack && id != null) components.sessionUseCases.goBack(id) else openOverview()
    }

    Box(Modifier.fillMaxSize()) {
        val activeTab = activeTabId?.let(::tabById)
        BrowserScreen(
            components = components,
            dock = dock,
            cover = activeTabId?.let { id ->
                when {
                    id == blankId -> CoverTarget(id, "", null, false, null, blank = true)
                    activeTab != null -> CoverTarget.of(activeTab, blank = false)
                    else -> CoverTarget(id, "", null, false, null, blank = false)
                }
            },
            zoomIn = { zoomIn.value },
            swipe = tabSwipe,
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
                    // A live pinned/essential card is only unloaded back to its row (TabManager).
                    val rows = stackItem(id)?.tabs.orEmpty()
                    tabs.close(rows.map { it.koanTabId }.ifEmpty { listOf(id) }, rows)
                },
                onReorder = { ids -> components.spaces.reorderStack(ids.mapNotNull(::stackItem)) },
                onDismiss = ::dismissOverview,
                sync = sync,
                open = overviewOpen,
                settled = overviewSettled,
                currentTabs = currentTabs,
                onBadge = ::onSyncAction,
                onConnect = { signingIn = true },
                modifier = Modifier
                    .graphicsLayer {
                        val p = motion.progress
                        alpha = p
                        val s = 1f + Motion.OVERVIEW_ZOOM * (1f - p)
                        scaleX = s
                        scaleY = s
                    }
                    .drawWithContent {
                        drawContent()
                        val d = drawer.progress
                        if (d > 0.01f) drawRect(palette.DrawerScrim, alpha = d)
                    }
                    .drawerBackdrop({ drawer.progress }, drawerBlurs),
            )
        }
        if (drawer.open) {
            // Tap outside the drawer closes it; nothing behind it takes input meanwhile.
            BackHandler { drawer.animateTo(Detent.Closed) }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            drawer.animateTo(Detent.Closed)
                        }
                    },
            )
        }

        val store = components.store
        val session = components.sessionUseCases
        // What the address row shows: the current tab (a blank new tab has no page yet).
        val page by store.observeSlice(activeTabId to blankId) { state ->
            activeTabId?.takeIf { it != blankId }?.let { state.findTab(it) }.let { tab ->
                val c = tab?.content
                // Reader view shows its own extension page: the address stays the article's.
                val reader = tab?.readerState?.takeIf { it.active }?.activeUrl
                PageInfo(
                    url = reader ?: c?.url.orEmpty(),
                    loading = c?.loading == true,
                    progress = (c?.progress ?: 0) / 100f,
                    canGoBack = c?.canGoBack == true,
                    canGoForward = c?.canGoForward == true,
                    desktopSite = c?.desktopMode == true,
                )
            }
        }
        val currentPage = page
        val findUi = rememberFindUi(components, overlays, activeTabId)
        // Downloads running: their combined progress for the ring in the pill (NaN = size unknown).
        val downloadProgress by store.observeSlice(Unit) { s ->
            val running = s.downloads.values.filter { !it.private && it.status == DownloadState.Status.DOWNLOADING }
            if (running.isEmpty()) {
                null
            } else {
                val total = running.sumOf { it.contentLength ?: 0L }
                if (running.any { it.contentLength == null } || total <= 0L) Float.NaN else running.sumOf { it.currentBytesCopied }.toFloat() / total
            }
        }
        // A download finished (or failed): say so, with Open (or Retry).
        LaunchedEffect(Unit) {
            components.downloadDone.collect { (download, status) ->
                val name = download.fileName ?: "File"
                when (status) {
                    DownloadState.Status.COMPLETED -> toasts.show(
                        Toast(
                            "$name downloaded", "Open",
                            detail = listOfNotNull(sizeText(download.contentLength ?: download.currentBytesCopied), host(download)).joinToString(" · "),
                            moment = true,
                        ) { components.downloads.open(download) },
                    )
                    DownloadState.Status.FAILED -> toasts.show(Toast("Couldn't download $name", "Retry") { components.downloads.retry(download) })
                    else -> Unit
                }
            }
        }
        // Page view space mode: the scrub, from the long press until the finger lifts.
        val spaceMode = !overviewOpen && bar.scrub.active
        fun spaceTarget(index: Int) = models.getOrNull(index)?.let { SwipeTarget(it.name, spaceIcon = it.icon) }
        fun tabTarget(tab: TabEntity) = SwipeTarget(displayHost(tab.url).ifEmpty { tab.title }, url = tab.url, favicon = tab.iconUrl, staticIcon = tab.staticIcon)
        // A new page load brings the dock back.
        LaunchedEffect(currentPage.loading) { if (currentPage.loading) dock.show() }
        // Typing in the address field: results above it, below the dock.
        // One surface while typing: the page blurred behind the results and the field (it fades
        // in and out with editing), the dock's own surface gone.
        val searchBottom = barZone(overview = false) + WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
        if (searchVisible) {
            SearchBackdrop(
                snapshot = { tabs.snapshot() },
                solidTop = { searchTop.floatValue },
                shown = { searchShown.value },
            )
        }
        if (editing) {
            SearchSheet(
                field = searchField,
                search = search,
                engine = searchEngine,
                bottom = searchBottom,
                currentSpace = currentSpace,
                onTop = { searchTop.floatValue = it },
                actions = SearchActions(
                    open = ::openHit,
                    search = { url ->
                        editing = false
                        tabs.load(url)
                    },
                    pick = searchField::set,
                    cancel = {
                        editing = false
                        tabs.cancelBlank()
                    },
                ),
            )
        }
        KoanBar(
            overviewOpen = overviewOpen,
            overview = { motion.progress },
            bar = bar,
            loading = currentPage.loading,
            loadProgress = currentPage.progress,
            dockHide = { dock.offset },
            surface = { 1f - searchShown.value },
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
                // Over the page view it is the scrub's row (nothing switches until release).
                WorkspaceSwitcher(
                    models, pager, enabled = overviewOpen, modifier = modifier,
                    // While the scrub row shows it follows the scrub (space by space under the
                    // finger, shared like a drag), otherwise the pager.
                    pos = { if (bar.scrub.shown > 0f) bar.scrub.pos else pager.pos },
                    from = { if (bar.scrub.shown > 0f) bar.scrub.index else pager.settled },
                    to = { if (bar.scrub.shown > 0f) bar.scrub.index else pager.target },
                )
            },
            address = { modifier ->
                AddressRow(
                    page = currentPage,
                    tabCount = models.getOrNull(currentSpace)?.stack?.size ?: 0,
                    editing = editing,
                    overviewOpen = overviewOpen,
                    overview = { motion.progress },
                    onEditingChange = { on ->
                        editing = on
                        // Closing the sheet of a new tab that never got a url discards it.
                        if (!on) tabs.cancelBlank()
                    },
                    onSubmit = ::submitSearch,
                    onSearchFromOverview = { newTab(pager.settled) },
                    onBack = { session.goBack(activeTabId) },
                    onTabs = { if (overviewOpen) overviewOpen = false else openOverview() },
                    onMenu = { overlays.menu.animateTo(Detent.Compact) },
                    modifier = modifier,
                    swipe = { if (overviewOpen) 0f else tabSwipe.fraction },
                    previous = tabSwipe.previous?.let(::tabTarget),
                    next = tabSwipe.next?.let(::tabTarget),
                    shownUrl = if (overviewOpen) null else tabSwipe.shown?.url,
                    space = if (spaceMode) spaceTarget(bar.scrub.index) else null,
                    find = findUi,
                    search = searchField,
                    complete = search::complete,
                    completedTitle = { search.results.top?.title },
                    download = downloadProgress,
                    onDownloads = { overlays.downloadsOpen = true },
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        KoanOverlays(
            components = components,
            overlays = overlays,
            page = currentPage,
            activeTabId = activeTabId,
            activeTab = activeTab,
            drawerStatus = drawerStatus,
            onEditAddress = { editing = true },
            onNewTab = { newTab(currentSpace) },
            onOpenNew = ::openNewTab,
            onSignIn = { signingIn = true },
            onToast = toasts::show,
            // Dev stress fills: debug builds only (R8 drops them and the fake data from release).
            devActions = if (!BuildConfig.DEBUG) emptyList() else buildList {
                for (n in listOf(50, 150)) {
                    add("Fill space to $n tabs" to {
                        spaces.getOrNull(currentSpace)?.let { components.spaces.fillWithFakeTabs(it.space.uuid, n) }
                        Unit
                    })
                }
                add("Remove fake tabs" to { components.spaces.removeFakeTabs(); Unit })
            },
        )

        KoanToastHost(toasts, bottom = barZone(overviewOpen), modifier = Modifier.align(Alignment.BottomCenter))

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
