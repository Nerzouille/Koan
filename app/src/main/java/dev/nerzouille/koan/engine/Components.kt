package dev.nerzouille.koan.engine

import dev.nerzouille.koan.data.SyncWrite
import dev.nerzouille.koan.data.HistoryDeletions
import mozilla.components.browser.state.selector.selectedTab
import dev.nerzouille.koan.engine.downloads.DuplicateMiddleware
import mozilla.components.browser.state.engine.middleware.TranslationsMiddleware
import mozilla.components.browser.state.action.TranslationsAction
import android.content.Context
import dev.nerzouille.koan.data.AccountStore
import dev.nerzouille.koan.data.Bookmarks
import dev.nerzouille.koan.data.LibrarySync
import kotlinx.coroutines.flow.first
import dev.nerzouille.koan.data.LoginStore
import dev.nerzouille.koan.data.HiddenLogins
import dev.nerzouille.koan.data.DataStoreBlob
import dev.nerzouille.koan.data.KeystoreBox
import dev.nerzouille.koan.data.KoanDatabase
import dev.nerzouille.koan.data.LastPlace
import dev.nerzouille.koan.data.Settings
import dev.nerzouille.koan.data.SpacesRepository
import dev.nerzouille.koan.data.SpacesSync
import kotlinx.coroutines.Dispatchers
import dev.nerzouille.koan.ui.tour.TourSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import mozilla.components.browser.state.state.content.DownloadState
import kotlinx.coroutines.launch
import dev.nerzouille.koan.sync.SpacesWriteGate
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
        GeckoPrefs.removeOldConfig(appContext)
        val settings = GeckoRuntimeSettings.Builder().build()
        GeckoRuntime.create(appContext, settings).also {
            // Pages' login forms are filled from Koan's sealed logins.
            it.autocompleteStorageDelegate = LoginAutofill(logins, appScope)
        }
    }

    /** tel:, intent://, apps' own schemes; the "Open in YouTube?" card and the menu's "Open in …". */
    val externalLinks: ExternalLinks by lazy {
        ExternalLinks(appContext, pageUrl = { store.state.selectedTab?.content?.url }, rewrite = ::rewriteLoad)
    }

    /** Google AMP links open the article: always in private tabs, in normal tabs if the setting says so. */
    @Volatile private var ampNormalTabs = false

    private fun rewriteLoad(session: mozilla.components.concept.engine.EngineSession, url: String): String? {
        val private = store.state.tabs.any { it.engineState.engineSession === session && it.content.private }
        val amp = dev.nerzouille.koan.privacy.AmpLinks.original(url)?.takeIf { private || ampNormalTabs }
        // Private: tracking parameters go from every page load, typed ones too.
        val cleaned = if (private) dev.nerzouille.koan.privacy.LinkCleaner.clean(amp ?: url) else null
        return cleaned ?: amp
    }

    val engine: GeckoEngine by lazy {
        GeckoEngine(
            appContext,
            DefaultSettings(
                // Bounce tracking follows its setting (privacy/PrivacyEngine).
                trackingProtectionPolicy = dev.nerzouille.koan.privacy.PrivacyEngine.policy(bounce = true),
                // Private tabs (owner's decisions 2026-11-11, docs/FEATURES.md "Private browsing").
                httpsOnlyMode = mozilla.components.concept.engine.Engine.HttpsOnlyMode.ENABLED_PRIVATE_ONLY,
                fingerprintingProtectionPrivateBrowsing = true,
                queryParameterStrippingPrivateBrowsing = true,
                queryParameterStrippingStripList = dev.nerzouille.koan.privacy.PrivacyEngine.STRIP_LIST.joinToString(" "),
                emailTrackerBlockingPrivateBrowsing = true,
                preferredColorScheme = PreferredColorScheme.System,
                // Visited links, and page titles for history.
                historyTrackingDelegate = history,
                // tel:, mailto:, intent:// … open in their app.
                requestInterceptor = externalLinks,
                // A download's name: the server's (Content-Disposition, validated by Gecko since
                // 160), else the URL's. Without a delegate AC only had the URL ("file.php").
                downloadDelegate = object : mozilla.components.concept.engine.DownloadDelegate {
                    override fun guessFileName(contentDisposition: String?, url: String?, mimeType: String?) =
                        downloadFiles.guessFileName(contentDisposition, url, mimeType)
                },
            ),
            runtime,
        )
    }

    val store: BrowserStore by lazy {
        BrowserStore(
            middleware = EngineMiddleware.create(engine) + ReaderViewMiddleware() +
                DuplicateMiddleware({ downloadFiles }, duplicateAsk) +
                DownloadMiddleware(
                    appContext, KoanDownloadService::class.java,
                    // Removing a download from the list never deletes the file (Delete does that explicitly).
                    deleteFileFromStorage = { false },
                    downloadFileUtils = downloadFiles, downloadStorage = downloadStorage,
                ) +
                // Firefox Translations, on the phone. GeckoView wants its calls on the main thread.
                TranslationsMiddleware(engine = engine, scope = KoanScopes.guarded("translations", Dispatchers.Main), isTranslationsEnabled = { true }),
        ).also { it.dispatch(TranslationsAction.InitTranslationsBrowserState) }
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

    // A download from a private tab always goes to the phone's Download folder.
    val downloadFiles by lazy {
        DefaultDownloadFileUtils(appContext) {
            if (store.state.selectedTab?.content?.private == true) defaultDownloadFolder else pickedDownloadFolder ?: defaultDownloadFolder
        }
    }
    val downloadStorage by lazy { DownloadStorage(appContext) }
    val downloadsUseCases by lazy { DownloadsUseCases(store, downloadFiles) }
    val downloads by lazy { Downloads(appContext, downloadFiles, downloadsUseCases) }

    /** The activity's picture-in-picture (MainActivity): whether Koan is in its PiP window. */
    val pip = MutableStateFlow<PictureInPicture?>(null)

    /** A download Koan didn't see a tap for, waiting on its card (KoanOverlays). */
    val downloadAsk = MutableStateFlow<DownloadAsk?>(null)

    /** A download of a file that is already there, waiting on the duplicate card (KoanOverlays). */
    val duplicateAsk = MutableStateFlow<DuplicateAsk?>(null)

    /** When the owner last tapped or pressed a key in Koan (uptime ms): downloads right after one start at once. */
    @Volatile var lastInputAt = 0L

    /** A download stopped (completed, failed, cancelled): KoanRoot shows the "downloaded" toast. */
    val downloadDone = MutableSharedFlow<Pair<DownloadState, DownloadState.Status>>(extraBufferCapacity = 4)

    fun watchDownloadLocation() = appScope.launch {
        settings.downloadLocation.collect { pickedDownloadFolder = it }
    }

    /** Gecko's own HTTP client (the engine's network, cookies and private session): Koan's requests on a tab's behalf. */
    val fetchClient by lazy { mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient(appContext, runtime) }

    // ---- private mode ----

    /** The private tabs (privacy/PrivateTabs): their own tab manager, saved sealed. */
    val privateTabs by lazy {
        dev.nerzouille.koan.privacy.PrivateTabs(appContext, store, tabsUseCases, sessionUseCases, { runtime }, { json ->
            engine.createSessionStateFrom(JsonReader(StringReader(json)))
        }, appScope)
    }

    /** Private tabs' per-site Protections switch (the ⋯ menu). */
    val trackingProtection by lazy { mozilla.components.feature.session.TrackingProtectionUseCases(store, engine) }

    /** The tab manager of what is shown: the private tabs while Private is on. */
    fun shownTabs(): TabManager = if (privateMode.on.value) privateTabs.tabs else tabs

    /** Private shown or not, and its lock. */
    val privateMode by lazy { dev.nerzouille.koan.privacy.PrivateMode(settings, privateTabs.count, bookmarks = { privateBookmarks.count() }) }

    /** Bookmarks made in private tabs: sealed with their own Keystore key, never in Room, never synced. */
    val privateBookmarks by lazy {
        dev.nerzouille.koan.privacy.PrivateBookmarks(
            dev.nerzouille.koan.privacy.SealedFile(
                java.io.File(appContext.filesDir, "private/bookmarks.sealed"),
                dev.nerzouille.koan.data.KeystoreBox("koan_private_bookmarks"),
            ),
            appScope,
        )
    }

    private var privacyStarted = false

    /** The engine's privacy settings, the private tabs, the lock. Once per process. */
    fun startPrivacy() {
        if (privacyStarted) return
        privacyStarted = true
        dev.nerzouille.koan.privacy.PrivacyEngine.start(engine, settings, appScope)
        appScope.launch { settings.ampNormalTabs.collect { ampNormalTabs = it } }
        privateMode.start(appContext)
        privateTabs.start()
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
                // Never for a private tab: its site must not be fetched or recorded outside the private session.
                .map { s -> s.tabs.filter { it.mediaSessionState != null && it.content.icon == null && !it.content.private }.map { it.id to it.content.url } }
                .distinctUntilChanged()
                .collect { playing ->
                    playing.forEach { (id, url) ->
                        val icon = FaviconLoader.image(url) ?: return@forEach
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
    val pageImages by lazy { PageImages(engine, store, appScope) }

    /** Koan's player over a page's fullscreen video (assets/extensions/video). */
    val pageVideo by lazy { PageVideo(engine, store, appScope) }

    /** Koan's video player: the minimised web video, the standalone player (ui/player). */
    val player by lazy { dev.nerzouille.koan.ui.player.PlayerHost() }

    /** A link or text another app sent to Koan (default browser, share): opened as a new tab. */
    val incoming = MutableStateFlow<Incoming?>(null)

    /**
     * A "Connect another device" pairing link: one another app opened Koan with (Koan asks first),
     * or one pasted in the address field. It goes to the sign-in, never to a tab or the history.
     */
    val pairLink = MutableStateFlow<PairLinkRequest?>(null)

    /** Koan came back after being unused a while: the UI shows the start page (then clears it). */
    val startPage = MutableStateFlow(false)

    /** A normal launch found the tour or a "What's new" due: the UI shows it (then clears it). */
    val tour = MutableStateFlow<TourSheet?>(null)

    val sessionUseCases by lazy { SessionUseCases(store) }
    val tabsUseCases by lazy { TabsUseCases(store) }

    /** Lives as long as the process; for writes that must not be cancelled with a screen. */
    val appScope = KoanScopes.guarded("app", Dispatchers.Default)

    val database by lazy { KoanDatabase.build(appContext) }
    val spaces by lazy { SpacesRepository(database.dao(), appScope) }
    val accounts by lazy { AccountStore(DataStoreBlob(appContext), KeystoreBox()) }
    /** Saved logins, sealed with their own Keystore key. */
    val hiddenLogins by lazy { HiddenLogins(appContext) }
    val logins by lazy { LoginStore(database.dao(), KeystoreBox("koan_logins"), hiddenLogins) }

    /** History removed on the phone, waiting to be deleted from the account (two-way sync). */
    val historyDeletions by lazy { HistoryDeletions(appContext) }

    /** Two-way sync: what the phone sends to the Mozilla account, for the switches turned on. */
    val syncWrite by lazy { SyncWrite(appContext, settings, database.dao(), logins, hiddenLogins, historyDeletions) }
    val sync by lazy {
        SpacesSync(accounts, database.dao(), appScope, liveTabs = { tabs.liveIds() }, library = LibrarySync(database.dao(), logins, historyDeletions),
            syncWrite = syncWrite)
    }
    val lastPlace by lazy { LastPlace(appContext) }
    val settings by lazy { Settings(appContext) }

    init {
        // Rule 4: the Zen spaces collection stays read-only unless writeBackEnabled is on (off).
        appScope.launch { settings.writeBackEnabled.collect { SpacesWriteGate.writeBackEnabled = it } }
    }

    val bookmarks by lazy { Bookmarks(database.dao(), appScope) }

    /** Engine tabs (history, scroll, form data) on disk, so they survive the process dying. */
    val sessionStorage by lazy { SessionStorage(appContext, engine, applicationScope = appScope) }

    /** Saves on tab changes; MainActivity adds "every few seconds in the foreground" and "on stop". */
    val autoSave by lazy { sessionStorage.autoSave(store).whenSessionsChange(appScope) }

    /** History: the pages tabs settle on, redirects left out. */
    val history by lazy { HistoryRecorder(database.dao(), appScope) }

    val tabs by lazy {
        TabManager(store, tabsUseCases, sessionUseCases, database.dao(), spaces.spaces, lastPlace, history, appScope, readState = { json ->
            engine.createSessionStateFrom(JsonReader(StringReader(json)))
        })
    }
}

/** A download of a file already downloaded: [start] with the (maybe renamed) download, or [cancel]. */
class DuplicateAsk(val dup: dev.nerzouille.koan.engine.downloads.DuplicateDownload, val start: (DownloadState) -> Unit, val cancel: () -> Unit)

/** A download waiting for "Download" or "Cancel" on its card. */
class DownloadAsk(val download: DownloadState, val start: () -> Unit, val cancel: () -> Unit)

/** Something another app handed to Koan: a url to open, or text to open or search ([text]). */
data class Incoming(
    val text: String,
    val kind: Kind,
    /** The sending app's package, when Android says (Back returns there). */
    val referrer: String? = null,
    val at: Long = System.currentTimeMillis(),
) {
    enum class Kind {
        /** A link (default browser, Custom Tabs): opened as is. */
        Url,
        /** Shared text or a web search: a link inside it opens, anything else is searched. */
        Shared,
        /** Text selected in another app ("Search in Koan"): opened if it is a url, else searched. */
        Selected,
    }
}

/** A pairing link for the sign-in; [fromOtherApp]: not typed or pasted by the owner, so Koan asks first. */
data class PairLinkRequest(val link: String, val fromOtherApp: Boolean)
