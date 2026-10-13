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
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.ui.bar.AddressRow
import dev.nerzouille.koan.ui.bar.DrawerContent
import dev.nerzouille.koan.ui.bar.DrawerState
import dev.nerzouille.koan.ui.bar.KoanBar
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.bar.WorkspaceSwitcher
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.overview.OverviewScreen
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.overview.findTab
import dev.nerzouille.koan.ui.overview.toModel
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
    var currentSpace by rememberSaveable { mutableIntStateOf(1) }
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
    LaunchedEffect(pager) {
        snapshotFlow { pager.settled }.collect { currentSpace = it }
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
        if (tab.koanTabId != activeTabId) {
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
    fun isPinned(id: String) = spaces.any { s -> (s.pinned + s.essentials).any { it.koanTabId == id } }

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
                    // A live pinned/essential card is only unloaded back to its row, never deleted.
                    if (isPinned(id)) {
                        if (id == activeTabId) activeTabId = null
                    } else {
                        components.spaces.closeTab(id)
                    }
                },
                onReorder = { ids -> components.spaces.reorderStack(ids.filterNot(::isPinned)) },
                onDismiss = { overviewOpen = false },
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
                    onOpenTab = { index, id -> spaces.getOrNull(index)?.findTab(id)?.let { openTab(index, it) } },
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
                    menuExtras = listOf(
                        "Fill space to 50 tabs (dev)" to {
                            spaces.getOrNull(currentSpace)?.let { components.spaces.fillWithFakeTabs(it.space.uuid) }
                        },
                    ),
                    modifier = modifier,
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
