package dev.nerzouille.koan.ui.browser

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.nerzouille.koan.engine.Components
import mozilla.components.browser.state.selector.findTab

/**
 * A page went fullscreen (a video's ⛶): the system bars hide (a swipe from the edge shows them for
 * a moment), the screen turns to the video's orientation, and Back leaves fullscreen first.
 * Outside fullscreen, the status bar follows [hideStatus] ("Hide status bar", the same swipe
 * shows it for a moment). Returns whether the page is fullscreen, for the page view (no dock, no
 * status bar inset) and the bar (hidden).
 */
@Composable
fun rememberFullscreen(components: Components, tabId: String?, hideStatus: Boolean, rotated: Boolean = false): Boolean {
    val fullScreen by components.store.observeSlice(tabId) { s -> tabId?.let { s.findTab(it) }?.content?.fullScreen == true }
    // The video's shape, when the page told it (a landscape video unless it says otherwise).
    val portrait by components.store.observeSlice(tabId) { s ->
        tabId?.let { s.findTab(it) }?.mediaSessionState?.elementMetadata?.let { m ->
            m.portrait ?: (m.height > m.width)
        } == true
    }
    val activity = LocalContext.current as? Activity
    // [rotated]: the video layer's rotate button turns it the other way. The video's shape first,
    // then (auto-rotate on, once the phone is held that way) the phone's rotation, portrait included.
    // Auto-rotate off: the player's rotate pill turns it the other way, until fullscreen ends.
    LaunchedEffect(fullScreen) { if (!fullScreen) components.player.fullscreenRotated = false }
    val shapePortrait = portrait != (rotated || components.player.fullscreenRotated)
    val rotationLocked = dev.nerzouille.koan.ui.player.rememberRotationLocked()
    val turned = dev.nerzouille.koan.ui.player.rememberTurned(fullScreen && !rotationLocked, shapePortrait)
    LaunchedEffect(fullScreen, shapePortrait, rotationLocked, turned) {
        activity?.requestedOrientation = dev.nerzouille.koan.ui.player.fullscreenOrientation(fullScreen, shapePortrait, rotationLocked, turned)
    }
    // Again after each rotation: HyperOS brings the bars back (grey, with their contrast scrim)
    // when a fullscreen video turns the screen.
    val orientation = androidx.compose.ui.platform.LocalConfiguration.current.orientation
    LaunchedEffect(fullScreen, hideStatus, orientation) {
        val window = activity?.window ?: return@LaunchedEffect
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        dev.nerzouille.koan.ui.player.immersive(window, fullScreen)
        if (fullScreen) {
            dev.nerzouille.koan.ui.player.playerBars(window, orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
        } else {
            bars.show(WindowInsetsCompat.Type.navigationBars())
            if (hideStatus) bars.hide(WindowInsetsCompat.Type.statusBars()) else bars.show(WindowInsetsCompat.Type.statusBars())
        }
    }
    // Koan never stays turned or without its bars when this goes away.
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { dev.nerzouille.koan.ui.player.immersive(it, false) }
            activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
    BackHandler(enabled = fullScreen && tabId != null) { tabId?.let { components.sessionUseCases.exitFullscreen(it) } }
    return fullScreen
}

/**
 * Every page that is fullscreen leaves it, the way the player's own ✕ does (its video port let go
 * first). [only] narrows it (the private tabs). Fullscreen is per tab in the engine: a tab left in
 * it would turn the screen again the next time it shows (fixes F1: Private back in landscape).
 */
fun leaveFullscreen(components: Components, only: (mozilla.components.browser.state.state.TabSessionState) -> Boolean = { true }) {
    components.store.state.tabs.filter { it.content.fullScreen && only(it) }.forEach { tab ->
        components.pageVideo.release(tab.id)
        components.sessionUseCases.exitFullscreen(tab.id)
    }
}
