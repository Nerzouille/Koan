package dev.nerzouille.koan

import android.app.SearchManager
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.downloads.KoanDownloadService
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.support.base.android.NotificationsDelegate
import android.content.Intent
import android.graphics.Color
import dev.nerzouille.koan.engine.Incoming
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
import dev.nerzouille.koan.thumbs.BakedBlur
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.KoanRoot
import dev.nerzouille.koan.ui.RefreshBooster
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.tour.dueSheet
import dev.nerzouille.koan.ui.tour.featureVersion
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.feature.tabs.WindowFeature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val refresh = RefreshBooster(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        DeviceProfile.initialize(this)
        val app = application as KoanApp
        val components = app.components
        app.componentsReady = true
        IconStore.initialize(this)
        ThumbnailStore.initialize(this)
        BakedBlur.initialize(this)
        components.spaces.ensureLocalSpace()
        // Engine tabs come back from disk (no-op if this process already has them), then the last tab.
        components.tabs.start(components.sessionStorage)
        components.autoSave
            .periodicallyInForeground(lifecycle = lifecycle)
            .whenGoingToBackground(lifecycle)
        // Pages opening tabs (target=_blank, window.open); TabManager files them next to their opener.
        lifecycle.addObserver(WindowFeature(components.store, components.tabsUseCases))
        components.sync.start()
        components.pageTools.start()
        components.startExtensions()
        components.startMedia()
        components.watchDownloadLocation()
        startDownloads(components)
        // A fresh start from a link (default browser, share); a recreated activity already had it.
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            // Nothing is drawn until the setting is read (a few ms), so the wrong theme never flashes.
            // Likewise "Hide status bar": the page never jumps up a few ms after the first frame.
            val mode by components.settings.themeMode.collectAsState(initial = null)
            val hideStatus by components.settings.hideStatusBar.collectAsState(initial = null)
            val resolved = mode ?: return@setContent
            val hide = hideStatus ?: return@setContent
            val dark = when (resolved) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(dark) {
                components.pageTools.setReaderDark(dark)
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
                KoanRoot(components, hideStatus = hide)
            }
        }
    }

    private var downloads: DownloadsFeature? = null

    private companion object {
        /** Per process, not per activity (a recreated activity must not restore twice). */
        val savedDownloadsRestored = java.util.concurrent.atomic.AtomicBoolean(false)

        /** When Koan last went to the background; null in a new process (then read from disk). */
        @Volatile var hiddenAt: Long? = null
    }

    // Downloads need the notification permission for their progress notification (Android 13+).
    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val names = results.keys.toTypedArray()
        val grants = IntArray(names.size) { if (results.getValue(names[it])) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }
        downloads?.onPermissionsResult(names, grants)
    }

    /**
     * Pages start downloads; the service runs them. No confirmation dialog: a download starts at
     * once (design 7g) and the end of it shows in a toast (KoanRoot, via [Components.downloadDone]).
     */
    private fun startDownloads(components: Components) {
        // The download list lives on disk (DownloadStorage): back into the store once per process.
        if (savedDownloadsRestored.compareAndSet(false, true)) components.downloadsUseCases.restoreDownloads()
        val feature = DownloadsFeature(
            applicationContext,
            store = components.store,
            useCases = components.downloadsUseCases,
            onNeedToRequestPermissions = { askPermissions.launch(it) },
            onDownloadStopped = { download, _, status -> components.downloadDone.tryEmit(download to status) },
            downloadFileUtils = components.downloadFiles,
            downloadManager = FetchDownloadManager(
                applicationContext, components.store, KoanDownloadService::class,
                notificationsDelegate = NotificationsDelegate(NotificationManagerCompat.from(applicationContext)),
            ),
            customFirstPartyDownloadDialog = { current, _, positive, _, _ -> positive.value(current.value) },
            customThirdPartyDownloadDialog = { _, _, negative -> negative.value() },
        )
        downloads = feature
        lifecycle.addObserver(feature)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Links and text other apps send: they open as a new tab in the current space (KoanRoot). */
    private fun handleIntent(intent: Intent?) {
        val incoming = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let { Incoming(it, isUrl = true) }
            Intent.ACTION_WEB_SEARCH -> intent.getStringExtra(SearchManager.QUERY)?.let { Incoming(it, isUrl = false) }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { Incoming(it, isUrl = false) }
            else -> null
        } ?: return
        if (incoming.text.isBlank()) return
        openedFromLink = true
        (application as KoanApp).components.incoming.value = incoming
    }

    /** This time Koan came to the front for a link or a share: never the start page. */
    private var openedFromLink = false

    /**
     * Back after being unused longer than the setting (not for a link): the start page. Checked
     * once per return to the front, after any new intent has arrived.
     */
    private fun checkStartPage() {
        val components = (application as KoanApp).components
        val fromLink = openedFromLink
        openedFromLink = false
        val known = hiddenAt
        hiddenAt = Long.MAX_VALUE
        if (fromLink || known == Long.MAX_VALUE) return
        // The tour (first launch) or a version's "What's new": on a normal launch only.
        components.appScope.launch {
            val sheet = dueSheet(components.settings.tourSeen(), featureVersion(BuildConfig.VERSION_NAME))
            if (sheet != null) components.tour.value = sheet
        }
        components.appScope.launch {
            val at = known ?: components.lastPlace.hiddenAt() ?: return@launch
            val after = components.settings.startPageAfter.first().millis ?: return@launch
            if (System.currentTimeMillis() - at >= after) components.startPage.value = true
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

    override fun onResume() {
        super.onResume()
        checkStartPage()
    }

    override fun onStop() {
        val components = (application as KoanApp).components
        // Not for a rotation: the activity comes straight back.
        if (!isChangingConfigurations) {
            val now = System.currentTimeMillis()
            hiddenAt = now
            components.appScope.launch { components.lastPlace.saveHiddenAt(now) }
        }
        components.sync.onBackground()
        super.onStop()
    }

    override fun onPause() {
        // Leaving Koan: screenshot the page, so the next start (or a restore) shows it at once.
        (application as KoanApp).components.tabs.captureCurrent("app hidden")
        refresh.stop()
        super.onPause()
    }
}
