package dev.nerzouille.koan.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.DeviceProfile
import dev.nerzouille.koan.data.AccountState
import dev.nerzouille.koan.data.SyncStatus
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.engine.ClosedTab
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.Opener
import dev.nerzouille.koan.engine.returnToApp
import dev.nerzouille.koan.privacy.PrivateTabs
import dev.nerzouille.koan.search.Hit
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.Shortcut
import dev.nerzouille.koan.search.completion
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.search.parseQuery
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.thumbs.BakedBlur
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.bar.AddressRow
import dev.nerzouille.koan.ui.bar.BarGestures
import dev.nerzouille.koan.ui.bar.DrawerContent
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.bar.KoanBar
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.bar.SpaceScrub
import dev.nerzouille.koan.ui.bar.SwipeTarget
import dev.nerzouille.koan.ui.bar.WorkspaceSwitcher
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.bar.drawerBackdrop
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.browser.ClosingPage
import dev.nerzouille.koan.ui.browser.CoverTarget
import dev.nerzouille.koan.ui.browser.DockState
import dev.nerzouille.koan.ui.browser.TabSwipe
import dev.nerzouille.koan.ui.browser.TranslateOfferHost
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.browser.rememberFullscreen
import dev.nerzouille.koan.ui.downloads.visibleIn
import dev.nerzouille.koan.ui.downloads.host
import dev.nerzouille.koan.ui.downloads.sizeText
import dev.nerzouille.koan.ui.kit.KoanToastHost
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.ToastState
import dev.nerzouille.koan.ui.overview.CardHero
import dev.nerzouille.koan.ui.overview.LocalCardHero
import dev.nerzouille.koan.ui.overview.OverviewScreen
import dev.nerzouille.koan.ui.overview.PageHeroLayer
import dev.nerzouille.koan.ui.overview.PrivateOverview
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.overview.SyncBadge
import dev.nerzouille.koan.ui.overview.SyncUi
import dev.nerzouille.koan.ui.overview.findTab
import dev.nerzouille.koan.ui.overview.toModel
import dev.nerzouille.koan.ui.player.WebPlayerLayer
import dev.nerzouille.koan.ui.player.downloadVideo
import dev.nerzouille.koan.ui.privacy.NoPersonalizedLearning
import dev.nerzouille.koan.ui.privacy.PrivateDockPill
import dev.nerzouille.koan.ui.privacy.PrivateFlip
import dev.nerzouille.koan.ui.privacy.PrivateLockCover
import dev.nerzouille.koan.ui.privacy.flipTextDip
import dev.nerzouille.koan.ui.privacy.unlockPrivate
import dev.nerzouille.koan.ui.root.IncomingLinks
import dev.nerzouille.koan.ui.root.PrivateSwitch
import dev.nerzouille.koan.ui.root.ownPlayer
import dev.nerzouille.koan.ui.root.rememberRootState
import dev.nerzouille.koan.ui.root.standaloneActions
import dev.nerzouille.koan.ui.search.SearchActions
import dev.nerzouille.koan.ui.search.SearchField
import dev.nerzouille.koan.ui.search.SearchSheet
import dev.nerzouille.koan.ui.search.TabTarget
import dev.nerzouille.koan.ui.search.rememberSearch
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.signin.FxAWeb
import dev.nerzouille.koan.ui.signin.SignInScreen
import dev.nerzouille.koan.ui.start.ContinueTab
import dev.nerzouille.koan.ui.start.StartPage
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanNormalTheme
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.KoanPrivateTheme
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PageTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.content.DownloadState

/**
 * Page view and Space overview, with the one bottom bar on top of both: it morphs into the
 * workspace bar as the overview opens, and into the essentials/pinned drawer from there. Every card
 * is a real engine tab, managed by [dev.nerzouille.koan.engine.TabManager].
 * [hideStatus]: the "Hide status bar" setting (see SystemBars.kt).
 */
@Composable
fun KoanRoot(components: Components, hideStatus: Boolean) {
    val hero = remember { CardHero() }
    // Private shown: its palette (Dusk / Night), and keyboards don't learn from what is typed.
    val privateOn by components.privateMode.on.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalCardHero provides hero) {
        KoanPrivateTheme(privateOn) {
            NoPersonalizedLearning(privateOn) { KoanRootContent(components, hideStatus, hero) }
        }
    }
}

@Composable
private fun KoanRootContent(components: Components, hideStatus: Boolean, hero: CardHero) {
    // Private shown (privacy/PrivateMode): the private tabs' one set and their own tab manager take
    // the place of the spaces. Read through states, so long-lived lambdas always see the current mode.
    val privateMode = components.privateMode
    val privateTabs = components.privateTabs
    // The navigation state (ui/root/RootState.kt), read below through its properties.
    val root = rememberRootState()
    // Set together with privateMode.on (showPrivate), so code right after a switch sees the new mode.
    val privateOnState = remember { mutableStateOf(privateMode.on.value) }
    val privateOn by privateOnState
    val normalSpaces = components.spaces.spaces.collectAsStateWithLifecycle()
    val privateSpaces = privateTabs.spaces.spaces.collectAsStateWithLifecycle()
    val spaces by remember { derivedStateOf { if (privateOnState.value) privateSpaces.value else normalSpaces.value } }
    val repo by remember { derivedStateOf { if (privateOnState.value) privateTabs.spaces else components.spaces } }
    val account by components.accounts.state.collectAsStateWithLifecycle()
    val syncStatus by components.sync.status.collectAsStateWithLifecycle()
    var signingIn by root::signingIn
    // Each space's current tab (uuid → card id last opened there): the overview centres on it.
    val currentTabs = root.currentTabs
    var currentSpace by root::currentSpace
    // The space the user is on, by id: spaces added/removed on the desktop never move them.
    var currentSpaceUuid by root::currentSpaceUuid
    val tabs by remember { derivedStateOf { if (privateOnState.value) privateTabs.tabs else components.tabs } }
    val normalCurrent = components.tabs.current.collectAsStateWithLifecycle()
    val privateCurrent = privateTabs.tabs.current.collectAsStateWithLifecycle()
    val activeTabId by remember { derivedStateOf { if (privateOnState.value) privateCurrent.value else normalCurrent.value } }
    val normalBlank = components.tabs.blank.collectAsStateWithLifecycle()
    val privateBlank = privateTabs.tabs.blank.collectAsStateWithLifecycle()
    val blankId by remember { derivedStateOf { if (privateOnState.value) privateBlank.value else normalBlank.value } }
    val normalStarted = components.tabs.started.collectAsStateWithLifecycle()
    val privateStarted = privateTabs.tabs.started.collectAsStateWithLifecycle()
    val tabsStarted by remember { derivedStateOf { if (privateOnState.value) privateStarted.value else normalStarted.value } }
    var overviewOpen by root::overviewOpen
    var editing by root::editing
    /** A tab another app just opened, until it is the current one (see the incoming links). */
    var landing by root::landing
    // The start page is up (with the field): not a tab until something opens from it. It goes
    // with the field.
    var startPage by root::startPage
    LaunchedEffect(editing) { if (!editing) startPage = false }
    // A new tab always starts with the search sheet open.
    LaunchedEffect(blankId) { if (blankId != null) editing = true }
    val palette = KoanTheme.colors
    val searchEngine by components.settings.searchEngine.collectAsStateWithLifecycle(SearchEngine.DuckDuckGo)
    val searchEngines by components.settings.searchEngines.collectAsStateWithLifecycle(SearchEngine.builtIns)
    val shortcuts = remember(searchEngines) { Shortcut.all(searchEngines) }
    // Both modes' cards, each in its own palette: the overview shows Private's page beside the
    // normal space during a flip; [models] is the mode shown.
    val normalPalette = if (palette.isDark) KoanPalette.Dark else KoanPalette.Light
    val privatePalette = if (palette.isDark) KoanPalette.Night else KoanPalette.Dusk
    val normalModels = remember(normalSpaces.value, normalCurrent.value, normalPalette) { normalSpaces.value.map { it.toModel(normalCurrent.value, normalPalette) } }
    val privateModels = remember(privateSpaces.value, privateCurrent.value, privatePalette) { privateSpaces.value.map { it.toModel(privateCurrent.value, privatePalette) } }
    val models = if (privateOn) privateModels else normalModels

    // The tab's card flies between the page and the opaque overview (CardHero). Also drives the
    // bar morph. A swipe on the dock drives it 1:1; otherwise it springs.
    val motion = remember { OverviewMotion(overviewOpen) }
    // The open motion has finished: the overview may now do the work it put off (thumbs/BakedBlur,
    // composing the neighbouring spaces).
    var overviewSettled by root::overviewSettled
    LaunchedEffect(overviewOpen, motion.dragging) {
        overviewSettled = false
        BakedBlur.onOverviewMotion(running = true, open = overviewOpen)
        if (motion.dragging) return@LaunchedEffect
        motion.settle(overviewOpen)
        BakedBlur.onOverviewMotion(running = false, open = overviewOpen)
        overviewSettled = overviewOpen
    }
    val overviewVisible by remember { derivedStateOf { overviewOpen || motion.progress > 0f } }
    hero.progress = { motion.progress }
    hero.zoom = { 1f + Motion.OVERVIEW_ZOOM * (1f - motion.progress) }
    hero.overviewShown = { overviewOpen || motion.progress > 0f }

    val scope = rememberCoroutineScope()
    val pager = remember { SpacePager(currentSpace, scope) }
    pager.count = spaces.size.coerceAtLeast(1)
    val density = LocalDensity.current
    pager.pageWidthPx = LocalWindowInfo.current.containerSize.width + with(density) { PagerTokens.PageGap.toPx() }
    // Cold start (e.g. after swiping Koan away): reopen on the space saved on disk (TabManager
    // reopens the tab). After a configuration change or a background kill, the saved instance state
    // already has it.
    var placeRestored by root::placeRestored
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
        // Private has no place on disk.
        if (placeRestored && uuid != null && uuid != PrivateTabs.SPACE.uuid) components.lastPlace.saveSpace(uuid)
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
    // The address field's text while editing, and what it finds.
    val searchField = remember { SearchField() }
    val suggestions by components.settings.searchSuggestions.collectAsStateWithLifecycle(true)
    val privateSuggestions by components.settings.privateSuggestions.collectAsStateWithLifecycle(false)
    val searchPrivateMarks by components.privateBookmarks.list.collectAsStateWithLifecycle()
    val search = rememberSearch(
        searchField, components.database.dao(), spaces, models, activeTabId, searchEngine, shortcuts,
        suggest = if (privateOn) privateSuggestions else suggestions,
        private = privateOn,
        privateMarks = if (privateOn) searchPrivateMarks else emptyList(),
    )
    val toasts = remember { ToastState() }
    // (space uuid, card id) the overview brings into focus (a card put back by Undo).
    val overviewFocus = remember { mutableStateOf<Pair<String, String>?>(null) }
    // Just signed in: once the first sync has brought Zen's tabs, the overview shows Zen's top tab
    // (the front-most Zen card; tabs opened on the phone sit in front of it).
    var focusZenTop by root::focusZenTop
    LaunchedEffect(focusZenTop) {
        if (!focusZenTop) return@LaunchedEffect
        val synced = withTimeoutOrNull(ZEN_TOP_WAIT_MS) {
            snapshotFlow { syncStatus to spaces }.first { (status, all) ->
                status is SyncStatus.NeedsSignIn || status is SyncStatus.Failed ||
                    status is SyncStatus.Idle && all.any { it.space.zenOrigin }
            }
        }
        focusZenTop = false
        val all = synced?.second?.takeIf { synced.first is SyncStatus.Idle } ?: return@LaunchedEffect
        val space = all.getOrNull(currentSpace.coerceIn(0, (all.size - 1).coerceAtLeast(0))) ?: return@LaunchedEffect
        val top = space.stack.lastOrNull { item -> item.tabs.any { it.origin == TabOrigin.ZEN && !it.pinned && !it.essential } }
        if (top != null) overviewFocus.value = space.space.uuid to top.id
    }
    LaunchedEffect(overviewOpen, activeTabId) { overlays.dismissAll() }
    // A sheet, the drawer or a full page opening sends the toast away: nothing pops above them.
    // Toasts shown while one is open (History's Undo) still show.
    val covered by remember { derivedStateOf { overlays.anyOpen || drawer.open || signingIn } }
    LaunchedEffect(covered) { if (covered) toasts.dismiss() }

    // Blur radii baked for the cards behind the open drawer (weakest first, see drawerBackdrop).
    val drawerBlurs = remember(density) {
        val levels = DeviceProfile.tier.drawerBlurLevels
        val max = DrawerTokens.Blur.value * density.density
        List(levels) { i -> max * (i + 1) / levels }
    }

    // The card the overview flies to or from: the current tab's, unless a tap just picked another
    // (until the current tab follows) or a tab with no card (NO_CARD).
    var heroPick by root::heroPick

    /** Back closing a tab: its page on its way out (ClosingPage), over the next tab coming up. */
    var closingShot by remember { mutableStateOf<ImageBitmap?>(null) }
    val closingProgress = remember { Animatable(0f) }
    LaunchedEffect(activeTabId) { heroPick = null }
    // Tabs closed in the overview that the spaces may still list for a moment (Room is async):
    // never opened again unless Undo brings them back.
    val gone = remember { mutableStateSetOf<String>() }
    // The current tab was closed in the overview: the space it was in, where back to the page
    // opens the card that came forward (its current card, currentTabs), or the start page.
    var closedFrom by root::closedFrom
    LaunchedEffect(activeTabId) { if (activeTabId != null) closedFrom = null }
    val dock = remember { DockState(scope) }
    // The shown mode's current tab, for the tab swipe (it waits for the tab it lands on).
    val shownCurrent = remember { kotlinx.coroutines.flow.MutableStateFlow<String?>(null) }
    SideEffect { shownCurrent.value = activeTabId }
    val tabSwipe = remember { TabSwipe(scope, shownCurrent) }
    tabSwipe.pageWidthPx = LocalWindowInfo.current.containerSize.width + with(density) { BarTokens.TabSwipeGap.toPx() }
    SideEffect { dock.locked = overviewOpen || editing || overlays.finding }
    LaunchedEffect(activeTabId) { dock.snapShown() }

    fun openOverview() {
        // A chain of tab swipes still resting: the overview shows the live tab, nothing opens later.
        tabSwipe.cancel()
        heroPick = null
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
    fun openTab(index: Int, tab: TabEntity, captureLeaving: Boolean = !overviewOpen) {
        // From the page view (a scrub, a swipe to a card in another space) the pager follows too:
        // the next scrub starts from it and it saves the place.
        if (index != pager.settled) pager.snapTo(index)
        currentSpace = index
        spaces.getOrNull(index)?.let { s ->
            // A split view's card has its own id.
            val card = s.stack.firstOrNull { item -> item.tabs.any { it.koanTabId == tab.koanTabId } }?.id
            currentTabs[s.space.uuid] = card ?: tab.koanTabId
            if (overviewOpen) heroPick = card ?: tab.koanTabId
        }
        // From the overview the page was captured when it opened; from the page view, now.
        tabs.open(tab, captureLeaving = captureLeaving)
        overviewOpen = false
    }
    fun newTab(index: Int) {
        val space = spaces.getOrNull(index) ?: return
        currentSpace = index
        currentTabs[space.space.uuid] = tabs.newTab(space.space.uuid)
        heroPick = NO_CARD
        overviewOpen = false
        editing = true
    }
    fun stackItem(id: String) = spaces.firstNotNullOfOrNull { s -> s.stack.firstOrNull { it.id == id } }
    fun tabById(id: String) = spaces.firstNotNullOfOrNull { s ->
        s.stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }
            ?: (s.pinnedTabs + s.essentials).firstOrNull { it.koanTabId == id }
    }
    /** A url as a new tab of the current space; [nextToCurrent]: right after the current card. */
    fun openNewTab(url: String, select: Boolean, nextToCurrent: Boolean, fromApp: Opener.App? = null): String? {
        val index = currentSpace.coerceIn(0, (spaces.size - 1).coerceAtLeast(0))
        val space = spaces.getOrNull(index) ?: return null
        val opener = if (nextToCurrent) activeTabId?.let(::tabById) else null
        val id = tabs.openNew(space.space.uuid, url, opener, select, fromApp)
        if (!select) {
            toasts.show(
                Toast("Opened in background", "Switch") {
                    spaces.withIndex().firstNotNullOfOrNull { (i, s) ->
                        s.stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }?.let { i to it }
                    }?.let { (i, tab) -> openTab(i, tab) }
                },
            )
            return id
        }
        currentTabs[space.space.uuid] = id
        editing = false
        heroPick = id
        overviewOpen = false
        return id
    }
    // ---- Private mode (OVERVIEW_PRIVATE.md §4, §6; owner's decisions 2026-11-11) ----
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val flip = remember { PrivateFlip(scope, privateMode.on.value) }
    // The normal space to come back to.
    var normalSpaceUuid by root::normalSpaceUuid
    val privateSwitch = PrivateSwitch(components, root, privateOnState, { tabs }, tabSwipe, overlays, drawer, flip, scope, context)
    fun showPrivate(on: Boolean) = privateSwitch.show(on)
    fun togglePrivate() = privateSwitch.toggle()
    fun leavePrivate() = privateSwitch.leave()
    val latestToggle = androidx.compose.runtime.rememberUpdatedState<() -> Unit> { togglePrivate() }
    val privateOverview = remember(flip) { PrivateOverview(flip) { latestToggle.value() } }

    // Links and text other apps send (default browser, share): a new tab in the current space.
    IncomingLinks(
        components, root, tabsStarted, spaces.isEmpty(), searchEngine, { activeTabId }, tabSwipe, { tabs }, overlays,
        leavePrivate = ::leavePrivate, openNewTab = ::openNewTab,
    )
    /** A url from the field: loads in the current tab, or in a new one from the start page. */
    fun loadHere(url: String) {
        if (startPage) openNewTab(url, select = true, nextToCurrent = false) else tabs.load(url)
    }
    /** A search result picked: an open tab is switched to (in its space), any other page loads here. */
    fun openHit(hit: Hit) {
        editing = false
        val target = hit.payload as? TabTarget
        val tab = target?.let { tabById(it.tabId) }
        if (target != null && tab != null) openTab(target.spaceIndex, tab) else loadHere(hit.url)
    }
    /** Go on the keyboard: "@wiki koan" searches there, the completed top hit opens, else url or search. */
    fun submitSearch(input: String) {
        // A "Connect another device" link pasted here: the sign-in, on its pairing page.
        if (dev.nerzouille.koan.sync.PairingFlow.isPairingLink(input)) {
            editing = false
            components.pairLink.value = dev.nerzouille.koan.engine.PairLinkRequest(input.trim(), fromOtherApp = false)
            return
        }
        val parsed = parseQuery(input, shortcuts)
        val top = search.results.top
        when {
            parsed.shortcut != null -> {
                editing = false
                loadHere(if (parsed.rest.isEmpty()) parsed.shortcut.home else parsed.shortcut.searchUrl(parsed.rest))
            }
            parsed.scope != null -> search.results.let { r -> (r.tabs + r.pages).firstOrNull() }?.let(::openHit)
            top != null && input.equals(completion(searchField.typed, top) ?: "\u0000", ignoreCase = true) -> openHit(top)
            else -> {
                editing = false
                val url = resolveInput(input, searchEngine)
                val links = components.externalLinks
                if (links.isAppLink(url)) links.openTyped(url)?.let(::loadHere) else loadHere(url)
            }
        }
    }
    /** The tab a space shows when switched to from the page view: its current card, else its newest. */
    fun lastTabOf(index: Int): TabEntity? {
        val s = spaces.getOrNull(index) ?: return null
        val live = s.stack.filter { item -> item.tabs.none { it.koanTabId in gone } }
        val item = currentTabs[s.space.uuid]?.let { id -> live.firstOrNull { it.id == id } } ?: live.lastOrNull()
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
    /** The start page in space [index], its search field focused (a space with no tab left…). */
    fun openStartPage(index: Int) {
        if (index != pager.settled) pager.snapTo(index)
        currentSpace = index
        heroPick = NO_CARD
        overviewOpen = false
        searchField.start("")
        startPage = true
        editing = true
    }
    /**
     * Leaving the overview without picking a card (tab counter, Back, swipe down): back to the
     * current tab; if it was closed, to the card that came forward in its place.
     */
    fun dismissOverview() {
        val id = activeTabId
        if (id != null && id !in gone && tabById(id) != null) {
            heroPick = null
            overviewOpen = false
            return
        }
        // The current tab is gone: the card that came forward in its space, else the start page
        // there. Never a page for a tab that no longer exists.
        val index = closedFrom?.let { uuid -> spaces.indexOfFirst { it.space.uuid == uuid } }?.takeIf { it >= 0 } ?: pager.settled
        val tab = lastTabOf(index)
        if (tab != null) openTab(index, tab) else openStartPage(index)
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
    bar.privateMode = { privateOnState.value }
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

    // Back after a while unused (MainActivity decides): the start page over the last tab. A link
    // from another app wins.
    val startRequested by components.startPage.collectAsStateWithLifecycle()
    LaunchedEffect(startRequested, tabsStarted, spaces.isEmpty(), placeRestored) {
        if (!startRequested || !tabsStarted || spaces.isEmpty() || !placeRestored) return@LaunchedEffect
        components.startPage.value = false
        if (components.incoming.value != null || signingIn) return@LaunchedEffect
        tabSwipe.cancel()
        tabs.cancelBlank()
        overlays.dismissAll()
        overlays.menu.snapClosed()
        overlays.settingsOpen = false
        overlays.historyOpen = false
        overlays.downloadsOpen = false
        openStartPage(currentSpace)
    }

    // The tour or a "What's new" (MainActivity: normal launches only), once the place is back and
    // not over the sign-in. The start page's keyboard would cover the sheet: the tab shows instead.
    val tourDue by components.tour.collectAsStateWithLifecycle()
    LaunchedEffect(tourDue, tabsStarted, placeRestored, signingIn) {
        val sheet = tourDue ?: return@LaunchedEffect
        if (!tabsStarted || !placeRestored || signingIn) return@LaunchedEffect
        components.tour.value = null
        editing = false
        overlays.showTour(sheet)
    }

    // No tab to show (first run, or the last one was closed in Zen): the overview (once the start
    // page, if up, is left).
    LaunchedEffect(tabsStarted, activeTabId, spaces.isEmpty(), startPage) {
        if (tabsStarted && activeTabId == null && spaces.isNotEmpty() && !overviewOpen && !startPage && landing == null) openOverview()
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
    var firstRunHandled by root::firstRunHandled
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
    val activity = LocalContext.current as? Activity
    /** Undo of a tab closed by Back: back in its place, and shown. */
    fun undoClose(closed: ClosedTab) {
        scope.launch {
            tabs.reopen(closed)
            gone -= closed.row.koanTabId
            val index = spaces.indexOfFirst { it.space.uuid == closed.row.spaceUuid }
            openTab(if (index >= 0) index else currentSpace, closed.row)
        }
    }
    /** A tab from History's "Recently closed": back in its space and place, and opened. */
    fun restoreClosed(id: String) {
        val fallback = spaces.getOrNull(currentSpace)?.space?.uuid ?: return
        scope.launch {
            val closed = tabs.restoreClosed(id, fallback) ?: return@launch
            gone -= closed.row.koanTabId
            val index = spaces.indexOfFirst { it.space.uuid == closed.row.spaceUuid }.let { if (it >= 0) it else currentSpace }
            openTab(index, closed.row)
        }
    }
    /**
     * A card closed in the overview: Undo puts it back in its place, in focus. Once the overview
     * is gone, Undo opens it like after Back.
     */
    fun closeCard(id: String, next: String?) {
        val rows = stackItem(id)?.tabs.orEmpty()
        val tab = rows.singleOrNull()
        val space = spaces.firstOrNull { s -> s.stack.any { it.id == id } }?.space?.uuid
        // The current tab (or, after closing it, the card that took its place): the card that
        // comes forward becomes the current one.
        val wasCurrent = rows.any { it.koanTabId == activeTabId } || (activeTabId == null && space != null && currentTabs[space] == id)
        if (space != null && currentTabs[space] == id) {
            if (next != null) currentTabs[space] = next else currentTabs.remove(space)
        }
        if (wasCurrent && space != null) {
            closedFrom = space
            if (next != null) currentTabs[space] = next else currentTabs.remove(space)
        }
        // A live pinned/essential card is only unloaded back to its row (TabManager); a split view closes whole.
        if (tab == null || tab.pinned || tab.essential) {
            tabs.close(rows.map { it.koanTabId }.ifEmpty { listOf(id) }, rows)
            return
        }
        gone += id
        scope.launch {
            val closed = tabs.closeForUndo(id) ?: return@launch
            toasts.show(
                Toast("Tab closed", "Undo") {
                    if (!overviewOpen) return@Toast undoClose(closed)
                    scope.launch {
                        tabs.reopen(closed)
                        gone -= id
                        // It was the current tab: it is again.
                        if (wasCurrent && activeTabId == null) {
                            closedFrom = closed.row.spaceUuid
                            currentTabs[closed.row.spaceUuid] = id
                        }
                        overviewFocus.value = closed.row.spaceUuid to closed.row.koanTabId
                    }
                },
            )
        }
    }
    /**
     * Close tab from the tab counter's menu: onto its neighbour in the stack (the overview after
     * the space's last one), with Undo. A pinned tab or an essential goes back to its row, a split
     * view closes whole.
     */
    fun closeCurrentTab() {
        val id = activeTabId ?: return
        val (si, i) = currentCard() ?: return
        val stack = spaces[si].stack
        val rows = stackItem(stack[i].id)?.tabs.orEmpty()
        val next = stack.getOrNull(i + 1)?.first ?: stack.getOrNull(i - 1)?.first
        val tab = tabById(id)
        if (tab == null || tab.pinned || tab.essential || rows.size > 1) {
            if (next != null) openTab(si, next, captureLeaving = false)
            tabs.close(rows.map { it.koanTabId }.ifEmpty { listOf(id) }, rows)
            if (next == null) openOverview()
            return
        }
        scope.launch {
            if (next != null) openTab(si, next, captureLeaving = false)
            val closed = tabs.closeForUndo(id) ?: return@launch
            if (next == null) openOverview()
            toasts.show(Toast("Tab closed", "Undo") { undoClose(closed) })
        }
    }

    /**
     * Back on a tab's first page: a tab another app opened closes and Back returns to that app; a
     * tab opened from a link closes back to its opener; any other tab closes onto its neighbour in
     * the stack (the overview after the space's last one), with Undo. A pinned tab, an essential or
     * a split view never closes: the overview, as before.
     */
    fun backFromFirstPage(id: String) {
        val tab = tabById(id)
        val card = currentCard()
        if (tab == null || card == null || tab.pinned || tab.essential || stackItem(spaces[card.first].stack[card.second].id)?.tabs.orEmpty().size > 1) {
            openOverview()
            return
        }
        val (si, i) = card
        val opener = tabs.openerOf(id)
        val parent = (opener as? Opener.Tab)?.let { tabById(it.id) }
        val stack = spaces[si].stack
        val next = stack.getOrNull(i + 1)?.first ?: stack.getOrNull(i - 1)?.first
        scope.launch {
            // Onto another tab: the page shrinks and slides away first (the overview and another
            // app have their own motion).
            if (opener !is Opener.App && (parent != null || next != null)) {
                val shot = tabs.grabScreen(id, PageTokens.CLOSE_GRAB_MS)?.asImageBitmap()
                    ?: tab.thumbnailKey?.let { ThumbnailStore.cached(it) ?: ThumbnailStore.load(it) }
                if (shot != null) {
                    closingShot = shot
                    closingProgress.snapTo(0f)
                    launch {
                        closingProgress.animateTo(1f, tween(PageTokens.CLOSE_MS, easing = FastOutSlowInEasing))
                        closingShot = null
                    }
                }
            }
            when {
                opener is Opener.App -> {
                    // Coming back to Koan later shows the tab next to it.
                    if (next != null) openTab(si, next, captureLeaving = false)
                    tabs.closeForUndo(id)
                    activity?.returnToApp(opener)
                    return@launch
                }
                parent != null -> {
                    val index = spaces.indexOfFirst { s -> s.space.uuid == parent.spaceUuid }
                    openTab(if (index >= 0) index else si, parent, captureLeaving = false)
                    tabs.closeForUndo(id)
                    return@launch
                }
                next != null -> openTab(si, next, captureLeaving = false)
            }
            val closed = tabs.closeForUndo(id) ?: return@launch
            if (next == null) openOverview()
            toasts.show(Toast("Tab closed", "Undo") { undoClose(closed) })
        }
    }
    // Page view: Back goes through the page's history, then closes the tab (see backFromFirstPage).
    BackHandler(enabled = !overviewOpen && !editing) {
        val id = activeTabId
        when {
            id == null -> openOverview()
            canGoBack -> components.sessionUseCases.goBack(id)
            id == blankId -> openOverview()
            else -> backFromFirstPage(id)
        }
    }
    // A fullscreen video: no bars, the video's orientation; Back leaves fullscreen first.
    // "Hide status bar" holds in the page view only: anything over it (the search sheet, the
    // overview, the menu and other sheets, Settings, the tour…) brings the status bar back. The
    // page keeps its size meanwhile (BrowserScreen's toTop): the bar draws over its top row.
    val pageOnly = !overviewVisible && !covered && !editing
    val fullScreen = rememberFullscreen(components, activeTabId, hideStatus && pageOnly)
    val pipHost by components.pip.collectAsStateWithLifecycle()
    val noPip = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val inPip by (pipHost?.inPip ?: noPip).collectAsStateWithLifecycle()
    val heroCard = when (val pick = heroPick) {
        NO_CARD -> null
        null -> currentCard()?.let { (si, i) -> spaces[si].stack[i].id }
        else -> pick
    }
    SideEffect { hero.cardId = heroCard }

    Box(Modifier.fillMaxSize()) {
        val activeTab = activeTabId?.let(::tabById)
        val cover = activeTabId?.let { id ->
            when {
                id == blankId -> CoverTarget(id, "", null, false, null, blank = true)
                activeTab != null -> CoverTarget.of(activeTab, blank = false)
                else -> CoverTarget(id, "", null, false, null, blank = false)
            }
        }
        BrowserScreen(
            components = components,
            dock = dock,
            fullScreen = fullScreen,
            findOpen = overlays.finding,
            cover = cover,
            toTop = hideStatus,
            swipe = tabSwipe,
        )
        closingShot?.let { ClosingPage(it, closingProgress, toTop = hideStatus) }
        // A fullscreen video: Koan's own player in place of the site's (ui/player); YouTube keeps
        // its own (the extension leaves it out).
        if (fullScreen && !ownPlayer(activeTab?.url ?: components.store.state.findTab(activeTabId.orEmpty())?.content?.url)) {
            WebPlayerLayer(
                components = components,
                tabId = activeTabId,
                fullScreen = fullScreen,
                inPip = inPip,
                onDownload = { url ->
                    downloadVideo(components, activeTabId, url)
                    toasts.show(Toast("Downloading the video"))
                },
                onToast = { title, detail -> toasts.show(Toast(title, detail = detail)) },
            )
        }
        // "Hide status bar", with the status bar back over the page (search sheet, menus): a strip
        // of the page view's colour fades in behind it, so its icons never sit on the page. Under
        // the sheets' scrim, so it dims with the page.
        // The fade is read in the layer only: no recomposition per frame.
        val statusStrip = animateFloatAsState(
            if (hideStatus && !pageOnly && !overviewVisible && !fullScreen) 1f else 0f,
            Motion.SearchScrim,
            label = "statusStrip",
        )
        if (hideStatus && !overviewVisible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .graphicsLayer { alpha = statusStrip.value }
                    .background(palette.Base),
            )
        }
        if (overviewVisible) {
            OverviewScreen(
                spaces = spaces,
                models = models,
                pager = pager,
                dock = barZone(overview = true),
                onOpenTab = ::openTab,
                onNewTab = ::newTab,
                onCloseTab = ::closeCard,
                focusRequest = overviewFocus,
                onReorder = { ids -> repo.reorderStack(ids.mapNotNull(::stackItem)) },
                private = privateOverview,
                normalModels = normalModels,
                privateModels = privateModels,
                onDismiss = ::dismissOverview,
                sync = sync,
                open = overviewOpen,
                settled = overviewSettled,
                currentTabs = currentTabs,
                onBadge = ::onSyncAction,
                onConnect = { signingIn = true },
                modifier = Modifier
                    .graphicsLayer {
                        // Opaque: the card flying over it (CardHero) is all that changes between
                        // page and overview, never two layers at partial alpha.
                        val p = motion.progress
                        val s = 1f + Motion.OVERVIEW_ZOOM * (1f - p)
                        scaleX = s
                        scaleY = s
                    }
                    .drawWithContent {
                        drawContent()
                        val d = drawer.progress
                        val scrim = if (DeviceProfile.liveBlur) palette.DrawerScrim else palette.DrawerScrimNoBlur
                        if (d > 0.01f) drawRect(scrim, alpha = d)
                    }
                    // No blur without RenderEffect (Android 11): the stronger scrim alone, and the
                    // sheet's taller shadow (KoanBar).
                    .then(if (DeviceProfile.liveBlur) Modifier.drawerBackdrop({ drawer.progress }, drawerBlurs) else Modifier),
            )
        }
        // Between page and overview: the tab's card in flight over the opaque overview. Composed
        // ahead (hidden at rest), so the first frame of a transition only moves a layer.
        PageHeroLayer(
            hero = hero,
            card = heroCard?.let { id -> models.firstNotNullOfOrNull { m -> m.stack.firstOrNull { it.id == id } } },
            fallback = cover,
        )
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
                    canGoBack = c?.canGoBack == true,
                    canGoForward = c?.canGoForward == true,
                    desktopSite = c?.desktopMode == true,
                )
            }
        }
        val currentPage = page
        // The load's progress on its own: read only by the load line's animation, never here.
        val pageProgress = store.observeSlice(activeTabId) { state ->
            activeTabId?.let { state.findTab(it) }?.content?.progress?.let { it / 100f } ?: 0f
        }
        val findUi = rememberFindUi(components, overlays, activeTabId)
        // Downloads running: their combined progress for the ring in the pill (NaN = size unknown).
        val downloadProgress by store.observeSlice(privateOn) { s ->
            val running = s.downloads.values.filter { it.status == DownloadState.Status.DOWNLOADING && visibleIn(it, privateOn) }
            if (running.isEmpty()) {
                null
            } else {
                val total = running.sumOf { it.contentLength ?: 0L }
                if (running.any { it.contentLength == null } || total <= 0L) Float.NaN else running.sumOf { it.currentBytesCopied }.toFloat() / total
            }
        }
        // A download finished: the ring's check for DONE_MS (restarted by the next one), unless
        // another download still runs (the ring then keeps showing progress).
        var ringDoneTick by remember { mutableIntStateOf(0) }
        var ringDone by remember { mutableStateOf(false) }
        LaunchedEffect(ringDoneTick) {
            if (ringDoneTick == 0) return@LaunchedEffect
            ringDone = true
            kotlinx.coroutines.delay(DownloadTokens.DONE_MS.toLong())
            ringDone = false
        }
        // A download finished (or failed): say so, with Open (or Retry).
        LaunchedEffect(Unit) {
            components.downloadDone.collect { (download, status) ->
                // A private download that ends while normal mode is shown says nothing there.
                if (!visibleIn(download, components.privateMode.on.value)) return@collect
                val name = download.fileName ?: "File"
                when (status) {
                    DownloadState.Status.COMPLETED -> {
                        ringDoneTick++
                        toasts.show(
                            Toast(
                                "$name downloaded", "Open",
                                detail = listOfNotNull(sizeText(download.contentLength ?: download.currentBytesCopied), host(download)).joinToString(" · "),
                                moment = true,
                            ) { components.downloads.open(download) },
                        )
                    }
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
        // Typing in the address field: results above the dock. The keyboard's inset is added in
        // layout by the sheet and the start page, never read here (it moves every frame).
        val searchBottom = barZone(overview = false)
        StartPage(
            open = startPage,
            space = models.getOrNull(currentSpace),
            // The tab under the start page; none once it's gone (closed in Zen meanwhile).
            last = activeTab?.takeIf { activeTabId != blankId }?.let { ContinueTab(it.url, it.title, it.iconUrl, it.staticIcon) },
            bottom = searchBottom,
            onOpenTab = { id ->
                editing = false
                spaces.getOrNull(currentSpace)?.findTab(id)?.let { openTab(currentSpace, it) }
            },
            onDismiss = { editing = false },
        )
        SearchSheet(
            // On the start page the essentials stay usable until something is typed.
            open = editing && !(startPage && searchField.typed.isEmpty()),
            field = searchField,
            search = search,
            engine = searchEngine,
            shortcuts = shortcuts,
            bottom = searchBottom,
            currentSpace = currentSpace,
            private = privateOn,
            actions = SearchActions(
                open = ::openHit,
                search = { url ->
                    editing = false
                    loadHere(url)
                },
                pick = searchField::set,
                cancel = {
                    // The start page stays: a tap beside the results only clears the field.
                    if (startPage) {
                        searchField.set("")
                    } else {
                        editing = false
                        tabs.cancelBlank()
                    }
                },
            ),
        )
        // Fullscreen: the page alone.
        if (!fullScreen) KoanBar(
            overviewOpen = overviewOpen,
            overview = { motion.progress },
            bar = bar,
            loading = currentPage.loading && !startPage,
            loadProgress = { pageProgress.value },
            dockHide = { dock.offset },
            dockAway = { dock.keyboard },
            drawerContent = { modifier ->
                // Private has no spaces: no drawer (its gestures are off, BarGestures).
                if (!privateOn) DrawerContent(
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
                // Private: one "Private" pill instead (a label, no action for now), on the same
                // line as the space pills. It dips out and back in with the flip.
                Box(modifier.flipTextDip(flip), contentAlignment = Alignment.BottomCenter) {
                    if (flip.labels) {
                        PrivateDockPill(Modifier.padding(bottom = BarTokens.SwitcherBottom).height(BarTokens.SwitcherItem))
                    } else {
                        // Over the page view it is the scrub's row (nothing switches until release).
                        WorkspaceSwitcher(
                            models, pager, enabled = overviewOpen, modifier = Modifier.fillMaxSize(),
                            // While the scrub row shows it follows the scrub (space by space under the
                            // finger, shared like a drag), otherwise the pager.
                            pos = { if (bar.scrub.shown > 0f) bar.scrub.pos else pager.pos },
                            from = { if (bar.scrub.shown > 0f) bar.scrub.index else pager.settled },
                            to = { if (bar.scrub.shown > 0f) bar.scrub.index else pager.target },
                        )
                    }
                }
            },
            address = { modifier ->
                AddressRow(
                    page = if (startPage) PageInfo("", loading = false, canGoBack = false, canGoForward = false) else currentPage,
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
                    onTabs = { if (overviewOpen) dismissOverview() else openOverview() },
                    onMenu = { overlays.menu.animateTo(Detent.Compact) },
                    onHistory = { overlays.openHistory(fromPage = !overviewOpen && activeTabId != null) },
                    onRecentlyClosed = { overlays.openHistory(fromPage = !overviewOpen && activeTabId != null, allClosed = true) },
                    onSettings = { overlays.settingsOpen = true },
                    onTabsMenu = { overlays.tabMenuOpen = true },
                    onBackMenu = { overlays.backMenuOpen = true },
                    pillPressed = { bar.pressOnPill = true },
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
                    downloadDone = ringDone && downloadProgress == null,
                    onDownloads = { overlays.downloadsOpen = true },
                    private = flip.labels,
                    flip = flip,
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            privateMix = { if (flip.running) flip.colour() else -1f },
        )

        // Settings, History, Downloads and the ⋯ sheet keep the normal palette in Private.
        KoanNormalTheme { KoanOverlays(
            components = components,
            private = privateOn,
            overlays = overlays,
            page = currentPage,
            activeTabId = activeTabId,
            activeTab = activeTab,
            drawerStatus = drawerStatus,
            onEditAddress = { editing = true },
            onNewTab = { newTab(currentSpace) },
            onCloseTab = ::closeCurrentTab,
            onOpenNew = ::openNewTab,
            onSignIn = { signingIn = true },
            onToast = toasts::show,
            onRestoreClosed = ::restoreClosed,
            currentSpaceUuid = spaces.getOrNull(currentSpace)?.space?.uuid,
        ) }

        TranslateOfferHost(
            components, activeTabId,
            allowed = !overviewVisible && !editing && !covered && !fullScreen,
            onToast = toasts::show,
        )
        // ---- Koan's own video player (Downloads, "Play video", files from other apps) ----
        val player = components.player
        player.standalone?.let { video ->
            val pipNow = components.pip.collectAsStateWithLifecycle().value
            dev.nerzouille.koan.ui.player.StandalonePlayer(
                video = video,
                actions = standaloneActions(components, androidx.compose.ui.platform.LocalContext.current, video, activeTabId, toasts::show),
                inPip = inPip,
                onClosed = {
                    player.standalone = null
                    pipNow?.setStandalone(false, false, null)
                },
                onPlaying = { playing, shape -> pipNow?.setStandalone(true, playing, shape) },
                onControls = { c, st ->
                    pipNow?.standaloneControls = c
                    pipNow?.standaloneState = st
                },
            )
        }
        KoanToastHost(toasts, bottom = barZone(overviewOpen), modifier = Modifier.align(Alignment.BottomCenter))

        // Sign-in slides up over everything and back down when done.
        val signIn = remember { Animatable(0f) }
        LaunchedEffect(signingIn) { signIn.animateTo(if (signingIn) 1f else 0f, Motion.Sheet) }
        val signInVisible by remember { derivedStateOf { signingIn || signIn.value > 0f } }
        // A pairing link: pasted in the address field, the sign-in opens on its pairing page, as on
        // first run. Opened by another app, Koan asks first, and says so when it would replace the
        // signed-in account. The sign-in screen itself warns too (SignInScreen's replacing).
        val incomingPairLink by components.pairLink.collectAsStateWithLifecycle()
        var pairLink by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(incomingPairLink) {
            val request = incomingPairLink ?: return@LaunchedEffect
            components.pairLink.value = null
            if (!request.fromOtherApp) {
                pairLink = request.link
                signingIn = true
                return@LaunchedEffect
            }
            val signedIn = (components.accounts.state.value as? dev.nerzouille.koan.data.AccountState.SignedIn)?.email
            val start = {
                pairLink = request.link
                signingIn = true
            }
            if (signedIn == null) {
                overlays.ask(
                    title = "Sign in from another app?",
                    text = "An app opened a Mozilla sign-in link in Koan. Continue only if you just scanned the " +
                        "code on your own computer.",
                    confirm = "Sign in",
                    destructive = false,
                    action = start,
                )
            } else {
                overlays.ask(
                    title = "Replace your Mozilla account?",
                    text = "An app opened a Mozilla sign-in link in Koan. Koan is signed in as " +
                        "${dev.nerzouille.koan.data.maskedEmail(signedIn)}: a code for another account replaces it " +
                        "on this phone. Continue only if you just scanned the code on your own computer.",
                    confirm = "Replace",
                    action = start,
                )
            }
        }
        if (signInVisible) {
            val accountState by components.accounts.state.collectAsStateWithLifecycle()
            SignInScreen(
                onLogin = { login ->
                    // The sign-in screen said it replaces the signed-in account (replacing).
                    components.accounts.completeWebLogin(login.email, login.uid, login.sessionToken, login.keyFetchToken, login.unwrapBKey, allowReplace = true)
                    // The session is sealed in Koan now: none of it stays in the sign-in page's storage.
                    dev.nerzouille.koan.ui.signin.FxAWeb.wipeWebSession()
                    signingIn = false
                    focusZenTop = true
                    components.sync.syncNow()
                    openOverview()
                },
                onPairing = { flow, code ->
                    // The sign-in screen said it replaces the signed-in account (replacing).
                    val sameKey = components.accounts.completePairing(flow, code, allowReplace = true)
                    // Both ways in must open the same Sync data (true/false only, never the keys).
                    if (sameKey != null) android.util.Log.i("KoanSync", "QR sign-in key matches the password sign-in: $sameKey")
                    pairLink = null
                    dev.nerzouille.koan.ui.signin.FxAWeb.wipeWebSession()
                    signingIn = false
                    focusZenTop = true
                    components.sync.syncNow()
                    openOverview()
                },
                pairLink = pairLink,
                replacing = (accountState as? dev.nerzouille.koan.data.AccountState.SignedIn)?.email?.let { dev.nerzouille.koan.data.maskedEmail(it) },
                onClose = {
                    pairLink = null
                    // Left half-way: whatever the page kept goes too.
                    dev.nerzouille.koan.ui.signin.FxAWeb.wipeWebSession()
                    signingIn = false
                },
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - signIn.value) * size.height
                },
            )
        }

        // Private tabs locked (after 1 min away or the screen off): nothing of them shows until
        // the fingerprint or PIN; the prompt comes up by itself.
        val locked by privateMode.locked.collectAsStateWithLifecycle()
        val lockOn by components.settings.privateLock.collectAsStateWithLifecycle(true)
        val privateCount by privateTabs.count.collectAsStateWithLifecycle()
        val privateMarks by components.privateBookmarks.list.collectAsStateWithLifecycle()
        val showLock = privateOn && locked && lockOn && (privateCount > 0 || privateMarks.isNotEmpty())
        fun unlock() {
            scope.launch { if (fragmentActivity != null && unlockPrivate(fragmentActivity)) privateMode.locked.value = false }
        }
        LaunchedEffect(showLock) { if (showLock) unlock() }
        if (showLock) {
            BackHandler { leavePrivate() }
            PrivateLockCover(onUnlock = ::unlock, onLeave = ::leavePrivate)
        }
    }
}

/** [heroPick]: the tab shown next has no card to fly from (a new tab, a link from another app). */
internal const val NO_CARD = ""

/** After sign-in, how long the overview waits for the first sync before giving up on showing Zen's top tab. */
private const val ZEN_TOP_WAIT_MS = 120_000L

