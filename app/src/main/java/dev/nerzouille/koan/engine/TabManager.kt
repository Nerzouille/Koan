package dev.nerzouille.koan.engine

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.LastPlace
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.search.loadsOnPhone
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.theme.ThumbTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import mozilla.components.lib.state.ext.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.action.CrashAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Koan's tabs ↔ the engine's (docs/ARCHITECTURE.md "Tab lifecycle and memory").
 *
 * Every Room tab can have a BrowserStore tab with the same id. It is created the first time the tab
 * is opened on the phone (a Zen tab never opened here has none: first open = fresh load) and is
 * kept, suspended or live, until the card is closed. At most [MAX_LIVE] of them hold a live engine
 * session; the others keep only their saved state and are restored on open.
 *
 * Also: the current tab (persisted, so Koan reopens on it), navigation written back to Room, tabs
 * opened by pages (target=_blank, window.open) placed next to their opener, and page screenshots.
 */
class TabManager(
    private val store: BrowserStore,
    private val tabsUseCases: TabsUseCases,
    private val sessionUseCases: SessionUseCases,
    private val dao: KoanDao,
    private val spaces: StateFlow<List<SpaceWithTabs>>,
    private val lastPlace: LastPlace,
    /** For Room writes and file work that must outlive any screen. */
    private val appScope: CoroutineScope,
) {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _current = MutableStateFlow<String?>(null)

    /** The tab the page view shows (or will show once opened). Null = none, e.g. it was closed. */
    val current: StateFlow<String?> = _current

    /** A new tab waiting for its first url (search sheet open); it has no engine tab yet. */
    private val _blank = MutableStateFlow<String?>(null)
    val blank: StateFlow<String?> = _blank

    /** The tab to go back to when a blank new tab is cancelled. */
    private var beforeBlank: String? = null

    private val _started = MutableStateFlow(false)

    /** Restored from disk; the UI may now open tabs. */
    val started: StateFlow<Boolean> = _started

    /** Most recently opened first; decides which sessions stay live. */
    private val recent = ArrayDeque<String>()

    /** Engine tabs Koan created and whose Room row may not exist yet (never treated as orphans). */
    private val ownTabs = HashSet<String>()

    /** Tabs a page opened that the engine has not selected yet; selecting one makes it current. */
    private val windowTabs = HashSet<String>()

    private var engineView = WeakReference<EngineView>(null)

    // ---- start -------------------------------------------------------------------------------

    /**
     * Restores the engine tabs saved before the process died (history and scroll included), then
     * reopens on the last tab. Call once per process, before the UI opens anything.
     */
    fun start(storage: SessionStorage) = main.launch {
        if (_started.value) return@launch
        dao.deleteBlankTabs()
        tabsUseCases.restore(storage)
        val place = lastPlace.read()
        val rooms = spaces.first { it.isNotEmpty() }
        val byId = rooms.allTabs().associateBy { it.koanTabId }
        // Engine tabs whose Room row is gone (closed in Zen while Koan was away) go now.
        val orphans = store.state.tabs.map { it.id }.filter { it !in byId || !byId.getValue(it).hasCard() }
        if (orphans.isNotEmpty()) tabsUseCases.removeTabs(orphans)
        recent.addAll(store.state.tabs.sortedByDescending { it.lastAccess }.map { it.id })
        val tab = place.tabId?.let(byId::get)?.takeIf { it.hasCard() || it.pinned || it.essential }
        if (tab != null) open(tab, captureLeaving = false)
        _started.value = true
        appScope.launch { ThumbnailStore.retainOnly(rooms.allTabs().mapNotNullTo(HashSet()) { it.thumbnailKey }) }
        watchEngine()
        watchRoom()
    }

    // ---- opening / closing -------------------------------------------------------------------

    /**
     * Shows [tab] in the page view: selects its engine tab, restoring or creating it as needed.
     * A pinned/essential tab also gets its card. [captureLeaving]: screenshot the outgoing page
     * first (it is on screen; from the overview it was captured when the overview opened).
     */
    fun open(tab: TabEntity, captureLeaving: Boolean = true) = main.launch {
        val id = tab.koanTabId
        cancelBlank(restore = false)
        val leaving = _current.value
        if (captureLeaving && leaving != null && leaving != id) captureNow(leaving, ThumbTokens.LEAVE_CAPTURE_TIMEOUT_MS)

        val engineTab = store.state.findTab(id)
        when {
            // A desktop-only page (local file…): the page view keeps its placeholder.
            !loadsOnPhone(tab.url) && engineTab == null -> Unit
            engineTab == null -> addEngineTab(id, tab.url)
            // Zen moved this (never phone-navigated) tab elsewhere since it was last open here:
            // its saved page is stale, load Zen's.
            !tab.dirty && tab.url.isNotEmpty() && engineTab.content.url.isNotEmpty() && engineTab.content.url != tab.url -> {
                tabsUseCases.removeTab(id)
                addEngineTab(id, tab.url)
            }
            store.state.selectedTabId != id -> tabsUseCases.selectTab(id)
        }
        setCurrent(id)
        appScope.launch {
            if ((tab.pinned || tab.essential) && tab.cardOrder == null) {
                dao.setCardOrder(id, dao.maxStackOrder(tab.spaceUuid) + 1)
            }
            dao.setLastAccessed(id, System.currentTimeMillis())
        }
    }

    /** A new tab at the bottom of [spaceUuid]'s stack, waiting for a url (the search sheet opens). */
    fun newTab(spaceUuid: String): String {
        val id = UUID.randomUUID().toString()
        cancelBlank(restore = false)
        beforeBlank = _current.value
        _blank.value = id
        _current.value = id
        appScope.launch {
            dao.upsertTabs(
                listOf(
                    TabEntity(
                        id, spaceUuid = spaceUuid, url = "", title = "New tab",
                        order = dao.maxStackOrder(spaceUuid) + 1, lastAccessed = System.currentTimeMillis(),
                    ),
                ),
            )
        }
        return id
    }

    /** Loads [url] in the current tab; a blank new tab gets its engine tab now. */
    fun load(url: String) = main.launch {
        val id = _current.value ?: return@launch
        if (_blank.value == id) {
            _blank.value = null
            beforeBlank = null
            addEngineTab(id, url)
            // Now a real tab: saved as the place to reopen on.
            setCurrent(id)
        } else if (store.state.findTab(id) != null) {
            sessionUseCases.loadUrl(url, id)
        }
    }

    /** The search sheet closed without a url: a blank new tab is discarded, back to the tab before it. */
    fun cancelBlank(restore: Boolean = true) {
        val id = _blank.value ?: return
        _blank.value = null
        appScope.launch { dao.deleteTab(id) }
        if (restore && _current.value == id) setCurrent(beforeBlank)
        beforeBlank = null
    }

    /**
     * Closes a card ([ids] = its tabs; a split card has several). Normal tabs close; a pinned or
     * essential tab is only unloaded back to its drawer row, its screenshot kept so it reopens instantly.
     */
    fun close(ids: List<String>, rows: List<TabEntity>) = main.launch {
        val keep = rows.filter { it.pinned || it.essential }.mapTo(HashSet()) { it.koanTabId }
        if (_current.value in ids) setCurrent(null)
        if (_blank.value in ids) _blank.value = null
        removeEngineTabs(ids)
        appScope.launch {
            keep.forEach { dao.setCardOrder(it, null) }
            val gone = ids.filter { it !in keep }
            if (gone.isNotEmpty()) dao.closeTabs(gone)
            rows.filter { it.koanTabId in gone }.mapNotNull { it.thumbnailKey }.forEach { ThumbnailStore.delete(it) }
            gone.forEach { dao.setThumbnail(it, null) }
        }
    }

    /** Ids of every tab that exists in the engine (live or suspended): opened on the phone. */
    fun liveIds(): Set<String> = store.state.tabs.mapTo(HashSet()) { it.id }

    private fun addEngineTab(id: String, url: String) {
        ownTabs += id
        store.dispatch(TabListAction.AddTabAction(createTab(url = url, id = id), select = true))
    }

    private fun setCurrent(id: String?) {
        _current.value = id
        if (id != null) touch(id)
        appScope.launch { lastPlace.saveTab(id) }
    }

    private fun touch(id: String) {
        recent.remove(id)
        recent.addFirst(id)
        enforceBudget()
    }

    /**
     * Removes engine tabs. If the engine's selected tab goes, another live one is selected first,
     * so the engine never wakes a suspended tab nobody asked for.
     */
    private fun removeEngineTabs(ids: List<String>) {
        val present = ids.filter { store.state.findTab(it) != null }
        if (present.isEmpty()) return
        if (store.state.selectedTabId in present) {
            val fallback = recent.firstOrNull { it !in present && store.state.findTab(it)?.engineState?.engineSession != null }
            if (fallback != null) tabsUseCases.selectTab(fallback)
        }
        recent.removeAll(present.toSet())
        ownTabs.removeAll(present.toSet())
        tabsUseCases.removeTabs(present)
    }

    // ---- lifecycle and memory ----------------------------------------------------------------

    /** Keeps the [MAX_LIVE] most recently opened sessions; the others are saved and closed. */
    private fun enforceBudget(max: Int = MAX_LIVE) {
        val selected = store.state.selectedTabId
        val live = store.state.tabs.filter { it.engineState.engineSession != null }
        if (live.size <= max) return
        val rank = recent.withIndex().associate { (i, id) -> id to i }
        live.sortedBy { if (it.id == selected) -1 else rank[it.id] ?: Int.MAX_VALUE }
            .drop(max)
            .forEach { store.dispatch(EngineAction.SuspendEngineSessionAction(it.id)) }
    }

    /**
     * Android 14+ only sends UI_HIDDEN and BACKGROUND to apps. In the background (in the LRU list,
     * a kill may follow) only the current session stays live. (The screenshot for the next start is
     * taken in onPause, while the page is still on screen.)
     */
    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) main.launch { enforceBudget(max = 1) }
    }

    // ---- engine → Room -----------------------------------------------------------------------

    private val pageWrites = HashMap<String, Job>()
    private var settleJob: Job? = null
    private var lastCrashRestore = 0L

    private fun watchEngine() = main.launch {
        var known = store.state.tabs.associate { it.id to it.pageKey() }
        var loading = false
        var selected = store.state.selectedTabId
        store.flow().collect { state ->
            for (tab in state.tabs) {
                val key = tab.pageKey()
                if (known[tab.id] != key) schedulePageWrite(tab)
                // A page opened a new tab (target=_blank, window.open): it joins the stack next to its opener.
                if (tab.id !in known && tab.id !in ownTabs) adoptWindowTab(tab)
            }
            known = state.tabs.associate { it.id to it.pageKey() }
            windowTabs.retainAll(known.keys)

            if (state.selectedTabId != selected) {
                selected = state.selectedTabId
                // The engine switched to a tab a page just opened: that is now the current tab.
                if (selected in windowTabs) {
                    windowTabs.remove(selected)
                    setCurrent(selected)
                }
            }
            if (state.tabs.count { it.engineState.engineSession != null } > MAX_LIVE) enforceBudget()

            val current = state.selectedTabId?.let(state::findTab)
            // Content process died (HyperOS kills it in the background): bring the page back.
            if (current != null && current.engineState.crashed && System.currentTimeMillis() - lastCrashRestore > CRASH_RETRY_MS) {
                lastCrashRestore = System.currentTimeMillis()
                store.dispatch(CrashAction.RestoreCrashedSessionAction(current.id))
            }
            // Screenshot once the page has settled after loading.
            val nowLoading = current?.content?.loading == true
            if (loading && !nowLoading && current != null) {
                settleJob?.cancel()
                val id = current.id
                settleJob = main.launch {
                    delay(ThumbTokens.SETTLE_MS)
                    if (store.state.selectedTabId == id) captureNow(id, timeoutMs = 1000)
                }
            }
            loading = nowLoading
        }
    }

    private fun TabSessionState.pageKey() = content.url to content.title

    /** Url/title changes reach Room once they settle (titles change a lot while a page loads). */
    private fun schedulePageWrite(tab: TabSessionState) {
        val id = tab.id
        pageWrites[id]?.cancel()
        pageWrites[id] = main.launch {
            delay(PAGE_WRITE_DEBOUNCE_MS)
            val content = store.state.findTab(id)?.content ?: return@launch
            val url = content.url
            if (url.isEmpty() || url == "about:blank") return@launch
            val title = content.title
            appScope.launch {
                val row = dao.tab(id) ?: return@launch
                if (row.url == url && (title.isEmpty() || row.title == title)) return@launch
                val sameSite = displayHost(row.url) == displayHost(url)
                // Zen's static icons (emoji) belong to the tab; favicons belong to the site.
                val keepIcon = row.staticIcon || sameSite
                dao.setPage(
                    id,
                    url = url,
                    title = title.ifEmpty { if (sameSite) row.title else displayHost(url) },
                    iconUrl = if (keepIcon) row.iconUrl else null,
                    staticIcon = row.staticIcon,
                )
            }
        }
    }

    private fun adoptWindowTab(tab: TabSessionState) {
        ownTabs += tab.id
        windowTabs += tab.id
        val parentId = tab.parentId ?: _current.value
        appScope.launch {
            val parent = parentId?.let { dao.tab(it) }
            val spaceUuid = parent?.spaceUuid ?: spaces.value.firstOrNull()?.space?.uuid ?: return@launch
            val after = parent?.takeIf { it.inStack }?.stackOrder ?: dao.maxStackOrder(spaceUuid)
            val row = TabEntity(
                tab.id, spaceUuid = spaceUuid, url = tab.content.url, title = tab.content.title.ifEmpty { displayHost(tab.content.url) },
                order = 0, lastAccessed = System.currentTimeMillis(), origin = TabOrigin.LOCAL,
            )
            dao.insertInStack(row, after)
        }
    }

    /**
     * Room → engine: a tab that lost its card elsewhere (Zen closed it, a sync hid it) drops its
     * engine tab too.
     */
    private fun watchRoom() = main.launch {
        var before = emptySet<String>()
        spaces.map { list -> list.allTabs().filter { it.hasCard() }.mapTo(HashSet()) { it.koanTabId } }
            .distinctUntilChanged()
            .collect { withCards ->
                if (withCards.isEmpty()) return@collect
                val gone = store.state.tabs.map { it.id }.filter { it !in withCards && it !in ownTabs }
                if (gone.isNotEmpty()) removeEngineTabs(gone)
                // Once Room has a tab the engine made, it is a normal tab like any other.
                ownTabs.retainAll { it !in withCards }
                // The current tab lost its card (closed in Zen): no current tab.
                val cur = _current.value
                if (cur != null && cur in before && cur !in withCards) setCurrent(null)
                before = withCards
            }
    }

    // ---- screenshots -------------------------------------------------------------------------

    /** The page view's engine view, for screenshots. */
    fun attach(view: EngineView?) {
        engineView = WeakReference(view)
    }

    /** Screenshots the current tab (overview opening, app hidden). */
    fun captureCurrent() {
        val id = _current.value ?: return
        main.launch { captureNow(id, timeoutMs = 1000) }
    }

    /**
     * Screenshots tab [id] if it is the one on screen and has painted; stores it and points Room at
     * it. Waits at most [timeoutMs] for the engine (the encode runs in the background).
     */
    private suspend fun captureNow(id: String, timeoutMs: Long) {
        val view = engineView.get() ?: return
        val tab = store.state.findTab(id) ?: return
        if (store.state.selectedTabId != id || tab.engineState.engineSession == null || !tab.content.firstContentfulPaint) return
        val bitmap = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Bitmap?> { cont -> view.captureThumbnail { if (cont.isActive) cont.resume(it) } }
        } ?: return
        appScope.launch {
            val previous = dao.tab(id)?.thumbnailKey
            val (key, dropped) = ThumbnailStore.save(id, bitmap, previous) ?: return@launch
            if (dao.tab(id) == null) {
                ThumbnailStore.delete(key)
                return@launch
            }
            dao.setThumbnail(id, key)
            if (dropped.isNotEmpty()) dao.clearThumbnails(dropped)
        }
    }

    /** Decodes the screenshots of the cards around [index] in [space] before the overview shows them. */
    fun prefetchThumbnails(space: SpaceWithTabs, index: Int) {
        val keys = space.stack.map { it.first.thumbnailKey }
        val around = (index - PREFETCH_BEHIND..index + PREFETCH_FRONT).mapNotNull { keys.getOrNull(it) }
        appScope.launch { ThumbnailStore.prefetch(around) }
    }

    companion object {
        /** Live engine sessions kept: the current tab + the 3 most recently used. */
        const val MAX_LIVE = 4
        private const val PAGE_WRITE_DEBOUNCE_MS = 400L
        private const val CRASH_RETRY_MS = 5000L
        private const val PREFETCH_BEHIND = 6
        private const val PREFETCH_FRONT = 2
    }
}

private fun List<SpaceWithTabs>.allTabs(): List<TabEntity> =
    flatMap { s -> s.essentials + s.pinnedTabs + s.stack.flatMap { it.tabs } }.distinctBy { it.koanTabId }

/** Has (or should have) a card: normal tabs, and pinned/essential tabs opened on the phone. */
private fun TabEntity.hasCard() = !closed && inStack
