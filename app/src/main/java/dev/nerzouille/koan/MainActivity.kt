package dev.nerzouille.koan

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.data.ThemeMode
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.KoanRoot
import dev.nerzouille.koan.ui.RefreshBooster
import dev.nerzouille.koan.ui.theme.KoanTheme
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.feature.tabs.WindowFeature

class MainActivity : FragmentActivity() {
    private val refresh = RefreshBooster(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val app = application as KoanApp
        val components = app.components
        app.componentsReady = true
        IconStore.initialize(this)
        ThumbnailStore.initialize(this)
        components.spaces.ensureLocalSpace()
        // Engine tabs come back from disk (no-op if this process already has them), then the last tab.
        components.tabs.start(components.sessionStorage)
        components.autoSave
            .periodicallyInForeground(lifecycle = lifecycle)
            .whenGoingToBackground(lifecycle)
        // Pages opening tabs (target=_blank, window.open); TabManager files them next to their opener.
        lifecycle.addObserver(WindowFeature(components.store, components.tabsUseCases))
        components.sync.start()

        setContent {
            // Nothing is drawn until the setting is read (a few ms), so the wrong theme never flashes.
            val mode by components.settings.themeMode.collectAsState(initial = null)
            val resolved = mode ?: return@setContent
            val dark = when (resolved) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            LaunchedEffect(resolved) {
                // Pages follow Koan's theme too (prefers-color-scheme).
                components.engine.settings.preferredColorScheme = when (resolved) {
                    ThemeMode.System -> PreferredColorScheme.System
                    ThemeMode.Light -> PreferredColorScheme.Light
                    ThemeMode.Dark -> PreferredColorScheme.Dark
                }
            }
            KoanTheme(dark) {
                KoanRoot(components)
            }
        }
    }

    // High refresh only while the user interacts (and the animations that follow); see RefreshBooster.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        refresh.onInput()
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        refresh.onInput()
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        (application as KoanApp).components.sync.onForeground()
    }

    override fun onStop() {
        (application as KoanApp).components.sync.onBackground()
        super.onStop()
    }

    override fun onPause() {
        // Leaving Koan: screenshot the page, so the next start (or a restore) shows it at once.
        (application as KoanApp).components.tabs.captureCurrent()
        refresh.stop()
        super.onPause()
    }
}
