package dev.nerzouille.koan.engine

import android.content.Context
import dev.nerzouille.koan.data.AccountStore
import dev.nerzouille.koan.data.Bookmarks
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
import kotlinx.coroutines.flow.MutableStateFlow
import mozilla.components.feature.readerview.ReaderViewMiddleware
import mozilla.components.browser.engine.gecko.GeckoEngine
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
        GeckoRuntime.create(appContext, settings)
    }

    val engine by lazy {
        GeckoEngine(
            appContext,
            DefaultSettings(
                trackingProtectionPolicy = TrackingProtectionPolicy.recommended(),
                preferredColorScheme = PreferredColorScheme.System,
            ),
            runtime,
        )
    }

    val store by lazy { BrowserStore(middleware = EngineMiddleware.create(engine) + ReaderViewMiddleware()) }

    /** Reader view, find in page, the long-press result. */
    val pageTools by lazy { PageTools(appContext, engine, store) }

    /** A link or text another app sent to Koan (default browser, share): opened as a new tab. */
    val incoming = MutableStateFlow<Incoming?>(null)

    val sessionUseCases by lazy { SessionUseCases(store) }
    val tabsUseCases by lazy { TabsUseCases(store) }

    /** Lives as long as the process; for writes that must not be cancelled with a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { KoanDatabase.build(appContext) }
    val spaces by lazy { SpacesRepository(database.dao(), appScope) }
    val accounts by lazy { AccountStore(DataStoreBlob(appContext), KeystoreBox()) }
    val sync by lazy { SpacesSync(accounts, database.dao(), appScope, liveTabs = { tabs.liveIds() }) }
    val lastPlace by lazy { LastPlace(appContext) }
    val settings by lazy { Settings(appContext) }
    val bookmarks by lazy { Bookmarks(database.dao(), appScope) }

    /** Engine tabs (history, scroll, form data) on disk, so they survive the process dying. */
    val sessionStorage by lazy { SessionStorage(appContext, engine, applicationScope = appScope) }

    /** Saves on tab changes; MainActivity adds "every few seconds in the foreground" and "on stop". */
    val autoSave by lazy { sessionStorage.autoSave(store).whenSessionsChange(appScope) }

    val tabs by lazy {
        TabManager(store, tabsUseCases, sessionUseCases, database.dao(), spaces.spaces, lastPlace, appScope)
    }
}

/** Something another app handed to Koan: a url to open, or text to open or search ([text]). */
data class Incoming(val text: String, val isUrl: Boolean, val at: Long = System.currentTimeMillis())
