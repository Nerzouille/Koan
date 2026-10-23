package dev.nerzouille.koan.ui.browser

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PageTokens
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.state.selector.findTab
import mozilla.components.lib.state.ext.flow
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.feature.session.SessionFeature
import kotlin.math.roundToInt

/** GeckoView's own keyboard listener (GeckoView.KEYBOARD_WINDOW_INSETS_LISTENER, GeckoView 157). */
private const val GECKO_KEYBOARD_LISTENER = "KEYBOARD_WINDOW_INSETS_LISTENER"

/**
 * Page view: the live engine view, full height under the dock (drawn by KoanRoot), which hides as
 * the page scrolls (GeckoView dynamic toolbar). The [cover] sits on top while a tab comes up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    components: Components,
    dock: DockState,
    /** A fullscreen video: the page takes the whole screen (no dock, no status bar inset). */
    fullScreen: Boolean,
    /** Find in page holds the dock in place: the page ends above it (its last lines reachable). */
    findOpen: Boolean,
    cover: CoverTarget?,
    zoomIn: () -> Boolean,
    swipe: TabSwipe,
) {
    val context = LocalContext.current
    val store = components.store
    val session = components.sessionUseCases

    val engineView = remember {
        GeckoEngineView(context).apply {
            // Logins are Koan's (its cards and chips): Android's autofill service (e.g. Google
            // Password Manager) would offer to save the same login a second time.
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            // window.print() goes through Android's print dialog, which needs the activity (held weakly).
            setActivityContext(context)
        }
    }
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

    // The page ends above the navigation bar (a solid strip below it, as Firefox) and above the
    // keyboard: it is exactly what can be seen. GeckoView would also take the keyboard off the
    // page itself (it reads it from the window), hiding the bottom of the page twice: its keyboard
    // listener goes (GeckoView adds it back each time its view attaches). The dock over the page
    // is GeckoView's dynamic toolbar: the page is laid out above it, so a fixed bottom bar sits on
    // the dock and follows it down as it slides away (the clipping is minus the part slid away).
    DisposableEffect(engineView) {
        val gecko = engineView.getChildAt(0)
        fun dropKeyboard() = engineView.removeWindowInsetsListener(GECKO_KEYBOARD_LISTENER)
        val onAttach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = dropKeyboard()
            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        gecko?.addOnAttachStateChangeListener(onAttach)
        if (gecko?.isAttachedToWindow == true) dropKeyboard()
        onDispose { gecko?.removeOnAttachStateChangeListener(onAttach) }
    }
    val density = LocalDensity.current
    val imeUp = WindowInsets.isImeVisible
    val navPx = WindowInsets.navigationBars.getBottom(density)
    val dockZonePx = with(density) { barZone(overview = false).roundToPx() }
    // The dock slides off the screen entirely, the strip under the page included.
    dock.heightPx = if (fullScreen) 0f else (dockZonePx + navPx).toFloat()
    // While find is open the dock can't slide away, so it is a plain bar the page ends above: as a
    // dynamic toolbar, the page's last lines would only show once it hides.
    val toolbarPx = if (fullScreen || findOpen) 0 else dockZonePx
    // A field in the page has the keyboard (not the address field, find, the overview): the dock
    // steps aside, as if slid away, so the field sits right on the keyboard. Decided as the
    // keyboard comes up: the address field closing with the keyboard still sliding away must not
    // flip the page into it.
    var typingInPage by remember { mutableStateOf(false) }
    LaunchedEffect(imeUp) { typingInPage = imeUp && !dock.locked && !fullScreen }
    LaunchedEffect(typingInPage) { dock.keyboardFor(typingInPage) }
    LaunchedEffect(toolbarPx) {
        engineView.setDynamicToolbarMaxHeight(toolbarPx)
        if (toolbarPx == 0) return@LaunchedEffect
        snapshotFlow { if (dock.keyboard) toolbarPx else dock.offset.coerceAtMost(toolbarPx.toFloat()).roundToInt() }
            .distinctUntilChanged()
            .collect { slid -> engineView.setVerticalClipping(-slid) }
    }
    // The dock follows a page's scroll only if the page (or the scroller under the finger) can scroll.
    dock.pageCanScroll = { engineView.getInputResultDetail().let { it.canScrollToTop() || it.canScrollToBottom() } }
    LaunchedEffect(engineView) {
        // Near the top of a page the dock always shows (once the finger is off: a fling, a jump).
        val nearTop = with(density) { PageTokens.DockNearTop.toPx() }
        engineView.verticalScrollPosition.collect { y ->
            if (y < nearTop && !dock.touching) dock.show()
            // Scrolling changes what the card should show: a screenshot follows once it stops.
            components.tabs.pageScrolled()
        }
    }

    // Pull to refresh: the indicator lets go once the reload has loaded (or another tab came up).
    val scope = rememberCoroutineScope()
    val pull = remember { PullRefresh(scope) }
    with(density) {
        pull.triggerPx = PullTokens.Trigger.toPx()
        pull.maxPx = PullTokens.Max.toPx()
    }
    val currentId by components.tabs.current.collectAsState()
    LaunchedEffect(currentId) { pull.done() }
    LaunchedEffect(pull.refreshing) {
        if (!pull.refreshing) return@LaunchedEffect
        val start = System.currentTimeMillis()
        val loading = store.flow().map { s -> s.selectedTabId?.let(s::findTab)?.content?.loading == true }
        withTimeoutOrNull(PullTokens.START_TIMEOUT_MS) { loading.first { it } }
        loading.first { !it }
        // A quick reload still shows that it happened.
        delay(PullTokens.MIN_SPIN_MS - (System.currentTimeMillis() - start))
        pull.done()
    }
    val view = LocalView.current

    Box(
        Modifier
            .fillMaxSize()
            .background(KoanTheme.colors.Base)
            .then(if (fullScreen) Modifier else Modifier.statusBarsPadding().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)))
            .then(if (findOpen && !fullScreen) Modifier.padding(bottom = barZone(overview = false)) else Modifier)
            .nestedScroll(dock.nestedScroll)
            .pullToRefresh(
                pull,
                atTop = { !fullScreen && !swipe.active && engineView.verticalScrollPosition.value < 1f },
                // Gecko's own "can overscroll top" is never set while the dock is on the page:
                // the page only has to keep its touches (maps, canvases, pages with their own pull).
                pageKeepsTouch = { engineView.getInputResultDetail().let { it.isTouchHandledByWebsite() || it.isTouchHandlingUnknown() } },
                onRefresh = { currentId?.let { session.reload(it) } },
                onTick = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
            )
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
            PullIndicator(pull, Modifier.align(Alignment.TopCenter))
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
