package dev.nerzouille.koan.ui.browser

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PageTokens
import kotlinx.coroutines.flow.distinctUntilChanged
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.feature.session.SessionFeature
import kotlin.math.roundToInt

/**
 * Page view: the live engine view, full height under the dock (drawn by KoanRoot), which hides as
 * the page scrolls (GeckoView dynamic toolbar). The [cover] sits on top while a tab comes up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    components: Components,
    dock: DockState,
    cover: CoverTarget?,
    zoomIn: () -> Boolean,
    swipe: TabSwipe,
) {
    val context = LocalContext.current
    val store = components.store
    val session = components.sessionUseCases

    val engineView = remember { GeckoEngineView(context) }
    val sessionFeature = remember {
        SessionFeature(store, session.goBack, session.goForward, engineView)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> sessionFeature.start()
                Lifecycle.Event.ON_STOP -> sessionFeature.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        components.tabs.attach(engineView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sessionFeature.stop()
            components.tabs.attach(null)
        }
    }

    // The dock's height over the page: the engine lays the page out above it, and lets the page
    // grow under it as it slides away. (With the keyboard up, the dock sits on the keyboard.)
    val density = LocalDensity.current
    val imeUp = WindowInsets.isImeVisible
    val navPx = WindowInsets.navigationBars.getBottom(density)
    val dockPx = with(density) { barZone(overview = false).toPx() } + if (imeUp) 0 else navPx
    dock.heightPx = dockPx
    LaunchedEffect(dockPx) {
        engineView.setDynamicToolbarMaxHeight(dockPx.roundToInt())
        snapshotFlow { (dockPx - dock.offset).roundToInt() }.distinctUntilChanged().collect { visible ->
            engineView.setVerticalClipping(-visible)
        }
    }
    LaunchedEffect(engineView) {
        // Near the top of a page the dock always shows (once the finger is off: a fling, a jump).
        val nearTop = with(density) { PageTokens.DockNearTop.toPx() }
        engineView.verticalScrollPosition.collect { y ->
            if (y < nearTop && !dock.touching) dock.show()
            // Scrolling changes what the card should show: a screenshot follows once it stops.
            components.tabs.pageScrolled()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KoanTheme.colors.Base)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.ime)
            .nestedScroll(dock.nestedScroll)
            .pointerInput(dock) {
                // Watch (never consume) the fingers on the page: the dock settles when they lift.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when {
                            event.changes.all { !it.pressed } -> {
                                dock.settle(viewConfiguration.touchSlop)
                                if (engineView.verticalScrollPosition.value < with(density) { PageTokens.DockNearTop.toPx() }) dock.show()
                            }
                            event.changes.count { it.pressed && !it.previousPressed } > 0 -> dock.onTouchDown()
                        }
                    }
                }
            },
    ) {
        // A swipe on the dock: the tabs of the stack form a strip that follows the finger. The live
        // page (and its cover) sits in its slot, the tabs around the screen show their screenshots.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationX = if (swipe.active) (swipe.live - swipe.pos) * swipe.pageWidthPx else 0f
            },
        ) {
            AndroidView(factory = { engineView }, modifier = Modifier.fillMaxSize())
            PageCover(cover, store, zoomIn)
        }
        if (swipe.active) {
            val strip = swipe.strip
            for (i in (swipe.anchor - 1)..(swipe.anchor + 1)) {
                if (i == swipe.live) continue
                val tab = strip.getOrNull(i) ?: continue
                key(tab.koanTabId) {
                    PagePreview(
                        CoverTarget.of(tab, blank = false),
                        Modifier.graphicsLayer { translationX = (i - swipe.pos) * swipe.pageWidthPx },
                    )
                }
            }
        }
    }
    // The screenshots one more tab out are decoded ahead, so the next swipe of a chain shows them
    // from its first frame.
    LaunchedEffect(swipe.anchor, swipe.strip) {
        val strip = swipe.strip
        val a = swipe.anchor
        ThumbnailStore.prefetch(((a - 2)..(a + 2)).mapNotNull { strip.getOrNull(it)?.thumbnailKey })
    }
}
