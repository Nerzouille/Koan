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
fun rememberFullscreen(components: Components, tabId: String?, hideStatus: Boolean): Boolean {
    val fullScreen by components.store.observeSlice(tabId) { s -> tabId?.let { s.findTab(it) }?.content?.fullScreen == true }
    // The video's shape, when the page told it (a landscape video unless it says otherwise).
    val portrait by components.store.observeSlice(tabId) { s ->
        tabId?.let { s.findTab(it) }?.mediaSessionState?.elementMetadata?.let { m ->
            m.portrait ?: (m.height > m.width)
        } == true
    }
    val activity = LocalContext.current as? Activity
    LaunchedEffect(fullScreen, portrait) {
        activity?.requestedOrientation = when {
            !fullScreen -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    LaunchedEffect(fullScreen, hideStatus) {
        val window = activity?.window ?: return@LaunchedEffect
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (fullScreen) {
            bars.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            bars.show(WindowInsetsCompat.Type.navigationBars())
            if (hideStatus) bars.hide(WindowInsetsCompat.Type.statusBars()) else bars.show(WindowInsetsCompat.Type.statusBars())
        }
    }
    // Koan never stays turned or without its bars when this goes away.
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
    BackHandler(enabled = fullScreen && tabId != null) { tabId?.let { components.sessionUseCases.exitFullscreen(it) } }
    return fullScreen
}
