package dev.nerzouille.koan.privacy

import android.content.Context
import android.util.JsonWriter
import androidx.room.Room
import dev.nerzouille.koan.data.KeystoreBox
import dev.nerzouille.koan.data.KoanDatabase
import dev.nerzouille.koan.data.SpaceEntity
import dev.nerzouille.koan.data.SpacesRepository
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.engine.TabManager
import dev.nerzouille.koan.thumbs.ThumbnailStore
import java.io.File
import java.io.StringWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.state.ext.flow
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/**
 * The private tabs (OVERVIEW_PRIVATE.md §4, owner's decisions 2026-11-11). One set, apart from
 * every space: their rows live in an in-memory database (never on disk, never synced, never in
 * history or Recently closed), and a second [TabManager] runs their engine tabs, which are
 * GeckoView private sessions.
 *
 * They stay open until closed, across restarts: their pages and page history are saved sealed with
 * a Keystore key ([PrivateVault]). GeckoView keeps private cookies and site data in memory only, so
 * those survive while Koan runs and are gone after a restart (sites sign you out). While private
 * tabs exist, a hidden private session ([keeper]) keeps that private context alive even when every
 * tab's session is suspended for memory. When the last private tab closes, the keeper closes too:
 * GeckoView then deletes the private cookies, cache and site data; the saved file goes, and so do
 * the screenshots (memory only).
 */
class PrivateTabs(
    context: Context,
    private val store: BrowserStore,
    tabsUseCases: TabsUseCases,
    sessionUseCases: SessionUseCases,
    private val runtime: () -> GeckoRuntime,
    private val readState: (String) -> EngineSessionState,
    private val appScope: CoroutineScope,
) {
    private val database = Room.inMemoryDatabaseBuilder(context.applicationContext, KoanDatabase::class.java).build()
    val dao = database.dao()
    val spaces = SpacesRepository(dao, appScope)
    val tabs = TabManager(
        store, tabsUseCases, sessionUseCases, dao, spaces.spaces,
        lastPlace = null, history = null, appScope = appScope, readState = readState, isPrivate = true,
    )
    private val vault = PrivateVault(File(context.filesDir, "private/tabs.sealed"), KeystoreBox(KEY_ALIAS))

    /** How many private tabs have a card. */
    val count: StateFlow<Int> = spaces.spaces
        .map { list -> list.sumOf { s -> s.stack.sumOf { it.tabs.size } } }
        .stateIn(appScope, SharingStarted.Eagerly, 0)

    private var started = false
    private var keeper: GeckoSession? = null

    /** Restores the saved private tabs (not opened: Koan starts in normal mode). Once per process. */
    fun start() {
        if (started) return
        started = true
        appScope.launch(Dispatchers.Main) {
            dao.upsertSpaces(listOf(SPACE))
            tabs.start { restore() }.join()
            watch()
        }
    }

    private suspend fun restore(): String? {
        val saved = withContext(Dispatchers.IO) { vault.read() } ?: return null
        if (saved.tabs.isEmpty()) return null
        dao.upsertTabs(
            saved.tabs.map { t ->
                TabEntity(
                    t.id, spaceUuid = SPACE.uuid, url = t.url, title = t.title.ifEmpty { t.url }, iconUrl = t.iconUrl,
                    order = t.order, lastAccessed = t.lastAccessed, origin = TabOrigin.LOCAL,
                )
            },
        )
        // TabManager drops engine tabs that have no row: wait until the rows show.
        spaces.spaces.first { list -> list.sumOf { s -> s.stack.sumOf { it.tabs.size } } >= saved.tabs.size }
        for (t in saved.tabs) {
            val state = t.engineState?.let { json -> runCatching { readState(json) }.getOrNull() }
            store.dispatch(
                TabListAction.AddTabAction(
                    createTab(url = t.url, id = t.id, title = t.title, private = true, engineSessionState = state),
                    select = false,
                ),
            )
        }
        return saved.current
    }

    @OptIn(FlowPreview::class)
    private fun watch() {
        // Saved a moment after any change: a tab opened, closed, navigated, or its page history.
        appScope.launch {
            val engine = store.flow().map { s ->
                s.tabs.filter { it.content.private }.map { Triple(it.id, it.content.url, it.engineState.engineSessionState) }
            }.distinctUntilChanged()
            combine(spaces.spaces, engine, tabs.current) { _, _, _ -> }
                .drop(1)
                .debounce(SAVE_DEBOUNCE_MS)
                .collect { save() }
        }
        // The last one closed: everything private goes.
        appScope.launch(Dispatchers.Main) {
            var before = count.value
            keepAlive(before > 0)
            count.collect { n ->
                if (n > 0) keepAlive(true)
                if (before > 0 && n == 0) wipe()
                before = n
            }
        }
    }

    private var saveJob: Job? = null

    /** Saves now (Koan going to the background): live tabs report their page history first. */
    fun saveNow() {
        if (!started) return
        appScope.launch(Dispatchers.Main) {
            store.state.tabs.filter { it.content.private }.forEach { it.engineState.engineSession?.flushSessionState() }
            delay(FLUSH_WAIT_MS)
            save()
        }
    }

    private suspend fun save() = withContext(Dispatchers.Main) {
        val rows = spaces.spaces.value.flatMap { s -> s.stack.flatMap { it.tabs } }
        if (rows.isEmpty()) {
            withContext(Dispatchers.IO) { vault.delete() }
            return@withContext
        }
        val saved = SavedPrivateTabs(
            tabs = rows.map { row ->
                val tab = store.state.findTab(row.koanTabId)
                val state = tab?.engineState?.engineSessionState?.let { s ->
                    runCatching { StringWriter().also { s.writeTo(JsonWriter(it)) }.toString() }.getOrNull()
                }
                SavedPrivateTab(
                    id = row.koanTabId,
                    url = tab?.content?.url?.takeIf { it.isNotEmpty() } ?: row.url,
                    title = row.title,
                    iconUrl = row.iconUrl,
                    order = row.order,
                    lastAccessed = row.lastAccessed,
                    engineState = state,
                )
            },
            current = tabs.current.value,
        )
        saveJob?.join()
        saveJob = appScope.launch(Dispatchers.IO) { runCatching { vault.write(saved) } }
    }

    /** A hidden private session keeps the private cookies and site data alive while tabs exist. */
    private fun keepAlive(on: Boolean) {
        if (on && keeper == null) {
            keeper = GeckoSession(GeckoSessionSettings.Builder().usePrivateMode(true).build()).also { it.open(runtime()) }
        } else if (!on) {
            keeper?.close()
            keeper = null
        }
    }

    private suspend fun wipe() {
        keepAlive(false)
        ThumbnailStore.forgetAllInMemory()
        dao.deleteClosedTabs()
        withContext(Dispatchers.IO) { vault.delete() }
    }

    companion object {
        private const val KEY_ALIAS = "koan_private"
        private const val SAVE_DEBOUNCE_MS = 1_000L
        private const val FLUSH_WAIT_MS = 300L

        /** The private tabs' only space: never shown as a space, never synced. */
        val SPACE = SpaceEntity(uuid = "koan-private", name = "Private", icon = "", gradientColors = emptyList(), order = 0)
    }
}
