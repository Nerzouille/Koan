package dev.nerzouille.koan.ui.browser

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import dev.nerzouille.koan.ui.overview.LocalCardHero
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import android.view.HapticFeedbackConstants
import android.view.Choreographer
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
    /**
     * "Hide status bar while scrolling": once the page is scrolled down ([DockState.statusHidden])
     * it runs up to the top edge; at its top it ends below the status bar again.
     */
    hideStatus: Boolean,
    swipe: TabSwipe,
) {
    val hero = LocalCardHero.current
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
    // The status bar hid or showed: the page view's top moved, and the page is held where it was
    // ([DockState.statusShift]) while the engine draws it at the new size (it tells no one when:
    // a few frames, [PageTokens.STATUS_SETTLE_FRAMES]), then takes its new place in one frame.
    // Nothing else moves: not the dock, the toolbar height or the clipping.
    DisposableEffect(engineView) {
        val gecko = engineView.getChildAt(0)
        val frames = Choreographer.getInstance()
        var left = 0
        val tick = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (--left > 0) return frames.postFrameCallback(this)
                dock.statusSettled()
            }
        }
        val resized = View.OnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top == oldBottom - oldTop || dock.statusShift == 0f) return@OnLayoutChangeListener
            frames.removeFrameCallback(tick)
            left = PageTokens.STATUS_SETTLE_FRAMES
            frames.postFrameCallback(tick)
        }
        gecko?.addOnLayoutChangeListener(resized)
        onDispose {
            gecko?.removeOnLayoutChangeListener(resized)
            frames.removeFrameCallback(tick)
        }
    }
    val density = LocalDensity.current
    val imeUp = WindowInsets.isImeVisible
    val navPx = WindowInsets.navigationBars.getBottom(density)
    // The keyboard the page ends above, changed once per show or hide, never along the keyboard's
    // slide: each resize costs Gecko frames, and a page resized every frame flashes bands between
    // it and the keyboard. Showing: the keyboard slides up over the page, which then ends at it.
    // Hiding: the page takes its full height at once and the keyboard slides away over it.
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    var keyboardPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(density) {
        snapshotFlow { ime.getBottom(density) to imeTarget.getBottom(density) }.collect { (now, target) ->
            if (target <= now) keyboardPx = target
        }
    }
    val dockZonePx = with(density) { barZone(overview = false).roundToPx() }
    // The status bar's row, shown or not. The page starts below it while the status bar shows, at
    // the top edge once it's hidden: one resize each way, at the top of the page or well past it,
    // never along a scroll. Only the page's top moves; its bottom (dock, keyboard) never does.
    val statusPx = WindowInsets.statusBarsIgnoringVisibility.getTop(density)
    dock.statusPx = if (hideStatus && !fullScreen) statusPx.toFloat() else 0f
    val topPx = if (fullScreen || (hideStatus && dock.statusHidden)) 0 else statusPx
    // Never held for long, should the page view not change size after all.
    LaunchedEffect(dock.statusHidden) {
        delay(PageTokens.STATUS_SETTLE_MAX_MS)
        dock.statusSettled()
    }
    // The dock slides off the screen entirely, the strip under the page included.
    dock.heightPx = if (fullScreen) 0f else (dockZonePx + navPx).toFloat()
    // A field in the page has the keyboard (not the address field, find, the overview): the dock
    // steps aside and the page runs exactly down to the keyboard, no room kept for the dock.
    // Decided as the keyboard comes up: the address field closing with the keyboard still sliding
    // away must not flip the page into it.
    var typingInPage by remember { mutableStateOf(false) }
    LaunchedEffect(imeUp) { typingInPage = imeUp && !dock.locked && !fullScreen }
    LaunchedEffect(typingInPage) { dock.keyboardFor(typingInPage) }
    // No dynamic toolbar while the dock can't slide: with the keyboard (it is away; switched in the
    // same frame as the page's resize), and while find
    // is open (a plain bar the page ends above; as a dynamic toolbar, the page's last lines would
    // only show once it hides).
    val toolbarPx = if (fullScreen || findOpen || (typingInPage && keyboardPx > navPx)) 0 else dockZonePx
    LaunchedEffect(toolbarPx) {
        engineView.setDynamicToolbarMaxHeight(toolbarPx)
        if (toolbarPx == 0) return@LaunchedEffect
        snapshotFlow { dock.offset.coerceAtMost(toolbarPx.toFloat()).roundToInt() }
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
            dock.pageAt(y)
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
    val base = KoanTheme.colors.Base

    Box(
        Modifier
            .fillMaxSize()
            .background(base)
            .padding(top = with(density) { topPx.toDp() })
            .then(if (fullScreen) Modifier else Modifier.padding(bottom = with(density) { maxOf(navPx, keyboardPx).toDp() }))
            .then(if (findOpen && !fullScreen) Modifier.padding(bottom = barZone(overview = false)) else Modifier)
            // While the page is held below its new place (the status bar just hid), its bottom hangs
            // below the page view: covered, never seen in the navigation bar's strip.
            .drawWithContent {
                drawContent()
                val below = dock.statusShift
                if (below > 0f) drawRect(base, Offset(0f, size.height), Size(size.width, below))
            }
            // Where the card flying to and from the overview starts and ends.
            .onGloballyPositioned { hero.pageRect = it.boundsInRoot() }
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
                translationY = dock.statusShift
            },
        ) {
            AndroidView(factory = { engineView }, modifier = Modifier.fillMaxSize())
            PageCover(cover, store)
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
