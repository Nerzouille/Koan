package dev.nerzouille.koan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.ui.bar.AddressRow
import dev.nerzouille.koan.ui.bar.BarStyle
import dev.nerzouille.koan.ui.bar.KoanBar
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.bar.WorkspaceSwitcher
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.overview.OverviewScreen
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.lib.state.ext.observeAsComposableState

/**
 * Page view and Space overview, with the one bottom bar on top of both: it morphs into the
 * workspace bar as the overview opens. M1: Room holds fake spaces; opening a card loads its URL in
 * the single live engine tab (real per-card tabs arrive in M3).
 */
@Composable
fun KoanRoot(components: Components) {
    val spaces by components.spaces.spaces.collectAsStateWithLifecycle()
    var currentSpace by rememberSaveable { mutableIntStateOf(1) }
    var activeTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var overviewOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    // Dev toggle while the owner compares both morphs.
    var barStyle by rememberSaveable { mutableStateOf(BarStyle.Grow) }

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

    fun loadInLiveTab(url: String) = components.sessionUseCases.loadUrl(url)
    fun openOverview() {
        editing = false
        pager.snapTo(currentSpace.coerceIn(0, pager.count - 1))
        overviewOpen = true
    }
    fun newTab(index: Int) {
        currentSpace = index
        activeTabId = components.spaces.addTab(spaces[index].space.uuid, "about:blank", "New tab")
        loadInLiveTab("about:blank")
        overviewOpen = false
        editing = true
    }

    Box(Modifier.fillMaxSize()) {
        BrowserScreen(
            components = components,
            editing = editing,
            bottomPadding = barZone(barStyle, overview = false),
        )
        if (overviewVisible) {
            OverviewScreen(
                spaces = spaces,
                activeTabId = activeTabId,
                pager = pager,
                dock = barZone(barStyle, overview = true),
                onOpenTab = { index, tab ->
                    currentSpace = index
                    if (tab.koanTabId != activeTabId) {
                        activeTabId = tab.koanTabId
                        loadInLiveTab(tab.url)
                    }
                    overviewOpen = false
                },
                onNewTab = ::newTab,
                onCloseTab = { components.spaces.closeTab(it) },
                onReorder = { components.spaces.reorderStack(it) },
                onDismiss = { overviewOpen = false },
                modifier = Modifier.graphicsLayer {
                    val p = progress.value
                    alpha = p
                    val s = 1f + Motion.OVERVIEW_ZOOM * (1f - p)
                    scaleX = s
                    scaleY = s
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
            style = barStyle,
            overviewOpen = overviewOpen,
            overview = { progress.value },
            onSwipeUp = ::openOverview,
            switcher = { modifier ->
                WorkspaceSwitcher(spaces.map { it.space }, pager, enabled = overviewOpen, modifier = modifier)
            },
            address = { modifier ->
                AddressRow(
                    page = currentPage,
                    tabCount = spaces.getOrNull(currentSpace)?.stack?.size ?: 0,
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
                        "Bar: ${if (barStyle == BarStyle.Grow) "replace" else "grow"} style (dev)" to {
                            barStyle = if (barStyle == BarStyle.Grow) BarStyle.Replace else BarStyle.Grow
                        },
                    ),
                    modifier = modifier,
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        )
    }
}
