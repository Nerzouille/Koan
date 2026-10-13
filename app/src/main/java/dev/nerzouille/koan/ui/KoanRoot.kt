package dev.nerzouille.koan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.browser.BrowserScreen
import dev.nerzouille.koan.ui.overview.OverviewScreen

/**
 * Page view with the Space overview on top. M1: Room holds fake spaces; opening a card loads its URL
 * in the single live engine tab (real per-card tabs arrive in M3).
 */
@Composable
fun KoanRoot(components: Components) {
    val spaces by components.spaces.spaces.collectAsStateWithLifecycle()
    var currentSpace by rememberSaveable { mutableIntStateOf(1) }
    var activeTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var overviewOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    // Simple fade/scale for now; the hero morph is M4.
    val progress = remember { Animatable(if (overviewOpen) 1f else 0f) }
    LaunchedEffect(overviewOpen) {
        progress.animateTo(if (overviewOpen) 1f else 0f, spring(dampingRatio = 1f, stiffness = 600f))
    }
    val overviewVisible by remember { derivedStateOf { overviewOpen || progress.value > 0f } }

    fun loadInLiveTab(url: String) = components.sessionUseCases.loadUrl(url)

    Box(Modifier.fillMaxSize()) {
        BrowserScreen(
            components = components,
            tabCount = spaces.getOrNull(currentSpace)?.stack?.size ?: 0,
            editing = editing,
            onEditingChange = { editing = it },
            onTabs = {
                editing = false
                overviewOpen = true
            },
            onDebugFill = { spaces.getOrNull(currentSpace)?.let { components.spaces.fillWithFakeTabs(it.space.uuid) } },
        )
        if (overviewVisible) {
            OverviewScreen(
                spaces = spaces,
                activeTabId = activeTabId,
                initialSpace = currentSpace,
                onSpaceSettled = { currentSpace = it },
                onOpenTab = { index, tab ->
                    currentSpace = index
                    if (tab.koanTabId != activeTabId) {
                        activeTabId = tab.koanTabId
                        loadInLiveTab(tab.url)
                    }
                    overviewOpen = false
                },
                onNewTab = { index ->
                    currentSpace = index
                    activeTabId = components.spaces.addTab(spaces[index].space.uuid, "about:blank", "New tab")
                    loadInLiveTab("about:blank")
                    overviewOpen = false
                    editing = true
                },
                onCloseTab = { components.spaces.closeTab(it) },
                onReorder = { components.spaces.reorderStack(it) },
                onDismiss = { overviewOpen = false },
                modifier = Modifier.graphicsLayer {
                    val p = progress.value
                    alpha = p
                    val s = 1.04f - 0.04f * p
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}
