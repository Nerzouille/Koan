package dev.nerzouille.koan.engine

import android.content.Context
import dev.nerzouille.koan.data.AccountStore
import dev.nerzouille.koan.data.Bookmarks
import dev.nerzouille.koan.data.LibrarySync
import dev.nerzouille.koan.data.LoginStore
import dev.nerzouille.koan.data.DataStoreBlob
import dev.nerzouille.koan.data.KeystoreBox
import dev.nerzouille.koan.data.KoanDatabase
import dev.nerzouille.koan.data.LastPlace
import dev.nerzouille.koan.data.Settings
import dev.nerzouille.koan.data.SpacesRepository
import dev.nerzouille.koan.data.SpacesSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dev.nerzouille.koan.ui.tour.TourSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import mozilla.components.browser.state.state.content.DownloadState
import kotlinx.coroutines.launch
import mozilla.components.support.webextensions.WebExtensionSupport
import android.os.Environment
import dev.nerzouille.koan.engine.downloads.Downloads
import dev.nerzouille.koan.engine.downloads.KoanDownloadService
import mozilla.components.feature.downloads.DownloadMiddleware
import mozilla.components.feature.downloads.DownloadStorage
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.support.utils.DefaultDownloadFileUtils
import mozilla.components.feature.readerview.ReaderViewMiddleware
import android.util.JsonReader
import java.io.StringReader
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.feature.media.MediaSessionFeature
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.ext.flow
import mozilla.components.browser.state.action.ContentAction
import dev.nerzouille.koan.favicon.FaviconLoader
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/** Process-wide objects: engine (one GeckoRuntime per process) and data. */
class Components(context: Context) {
    private val appContext = context.applicationContext

    val runtime: GeckoRuntime by lazy {
        val settings = GeckoRuntimeSettings.Builder()
            .configFilePath(GeckoPrefs.write(appContext).absolutePath)
            .build()
        GeckoRuntime.create(appContext, settings).also {
            // Pages' login forms are filled from Koan's sealed logins.
            it.autocompleteStorageDelegate = LoginAutofill(logins, appScope)
        }
    }

    val engine by lazy {
        GeckoEngine(
            appContext,
            DefaultSettings(
                trackingProtectionPolicy = TrackingProtectionPolicy.recommended(),
                preferredColorScheme = PreferredColorScheme.System,
                // Visited links, and page titles for history.
                historyTrackingDelegate = history,
                // tel:, mailto:, intent:// … open in their app.
                requestInterceptor = ExternalLinks(appContext),
            ),
            runtime,
        )
    }

    val store by lazy {
        BrowserStore(
            middleware = EngineMiddleware.create(engine) + ReaderViewMiddleware() +
                DownloadMiddleware(
                    appContext, KoanDownloadService::class.java,
                    // Removing a download from the list never deletes the file (Delete does that explicitly).
                    deleteFileFromStorage = { false },
                    downloadFileUtils = downloadFiles, downloadStorage = downloadStorage,
                ),
        )
    }

    // ---- downloads ----

    /**
     * Where downloads go, read without waiting: the folder picked in Settings (a content:// tree)
     * or the phone's Download folder. Kept current by [watchDownloadLocation].
     */
    @Volatile
    private var pickedDownloadFolder: String? = null

    val defaultDownloadFolder: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

    val downloadFiles by lazy { DefaultDownloadFileUtils(appContext) { pickedDownloadFolder ?: defaultDownloadFolder } }
    val downloadStorage by lazy { DownloadStorage(appContext) }
    val downloadsUseCases by lazy { DownloadsUseCases(store, downloadFiles) }
    val downloads by lazy { Downloads(appContext, downloadFiles, downloadsUseCases) }

    /** A download stopped (completed, failed, cancelled): KoanRoot shows the "downloaded" toast. */
    val downloadDone = MutableSharedFlow<Pair<DownloadState, DownloadState.Status>>(extraBufferCapacity = 4)

    fun watchDownloadLocation() = appScope.launch {
        settings.downloadLocation.collect { pickedDownloadFolder = it }
    }

    /** uBlock Origin (C5). [startExtensions] once per process. */
    val ublock by lazy { UBlock(engine, store, settings, appScope) }

    private var extensionsStarted = false

    /** Add-on support (installed list, badges, popups into the store), then uBlock. */
    fun startExtensions() {
        if (extensionsStarted) return
        extensionsStarted = true
        WebExtensionSupport.initialize(engine, store, openPopupInTab = false)
        ublock.start()
    }

    private var mediaStarted = false

    /** Media notification and lock screen controls for pages playing (KoanMediaService). Once per process. */
    fun startMedia() {
        if (mediaStarted) return
        mediaStarted = true
        MediaSessionFeature(appContext, KoanMediaService::class.java, store).start()
        // The notification shows the tab's icon: Koan's favicons, given to a tab once it plays.
        appScope.launch {
            store.flow()
                .map { s -> s.tabs.filter { it.mediaSessionState != null && it.content.icon == null }.map { it.id to it.content.url } }
                .distinctUntilChanged()
                .collect { playing ->
                    playing.forEach { (id, url) ->
                        val icon = FaviconLoader.image(url, null) ?: return@forEach
                        withContext(Dispatchers.Main) { store.dispatch(ContentAction.UpdateIconAction(id, url, icon)) }
                    }
                }
        }
    }

    /** What sites may do: answers to their requests, and Site settings. */
    val sitePermissions by lazy { SitePermissions(appContext, runtime) }

    /** Reader view, find in page, the long-press result. */
    val pageTools by lazy { PageTools(appContext, engine, store) }

    /** The search engine a page declares, for "Add this search engine". */
    val pageEngines by lazy { PageEngines(engine, store, appScope) }

    /** A link or text another app sent to Koan (default browser, share): opened as a new tab. */
    val incoming = MutableStateFlow<Incoming?>(null)

    /**
     * A "Connect another device" pairing link Koan was opened with (the system camera, when Koan
     * is the default browser): it goes to the sign-in, never to a tab or the history.
     */
    val pairLink = MutableStateFlow<String?>(null)

    /** Koan came back after being unused a while: the UI shows the start page (then clears it). */
    val startPage = MutableStateFlow(false)

    /** A normal launch found the tour or a "What's new" due: the UI shows it (then clears it). */
    val tour = MutableStateFlow<TourSheet?>(null)

    val sessionUseCases by lazy { SessionUseCases(store) }
    val tabsUseCases by lazy { TabsUseCases(store) }

    /** Lives as long as the process; for writes that must not be cancelled with a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { KoanDatabase.build(appContext) }
    val spaces by lazy { SpacesRepository(database.dao(), appScope) }
    val accounts by lazy { AccountStore(DataStoreBlob(appContext), KeystoreBox()) }
    /** Saved logins, sealed with their own Keystore key. */
    val logins by lazy { LoginStore(database.dao(), KeystoreBox("koan_logins")) }
    val sync by lazy {
        SpacesSync(accounts, database.dao(), appScope, liveTabs = { tabs.liveIds() }, library = LibrarySync(database.dao(), logins))
    }
    val lastPlace by lazy { LastPlace(appContext) }
    val settings by lazy { Settings(appContext) }
    val bookmarks by lazy { Bookmarks(database.dao(), appScope) }

    /** Engine tabs (history, scroll, form data) on disk, so they survive the process dying. */
    val sessionStorage by lazy { SessionStorage(appContext, engine, applicationScope = appScope) }

    /** Saves on tab changes; MainActivity adds "every few seconds in the foreground" and "on stop". */
    val autoSave by lazy { sessionStorage.autoSave(store).whenSessionsChange(appScope) }

    /** History: the pages tabs settle on, redirects left out. */
    val history by lazy { HistoryRecorder(database.dao(), appScope) }

    val tabs by lazy {
        TabManager(store, tabsUseCases, sessionUseCases, database.dao(), spaces.spaces, lastPlace, history, appScope) { json ->
            engine.createSessionStateFrom(JsonReader(StringReader(json)))
        }
    }
}

/** Something another app handed to Koan: a url to open, or text to open or search ([text]). */
data class Incoming(val text: String, val isUrl: Boolean, val at: Long = System.currentTimeMillis())
