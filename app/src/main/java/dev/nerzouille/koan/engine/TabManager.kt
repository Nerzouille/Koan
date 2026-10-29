package dev.nerzouille.koan.engine

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import dev.nerzouille.koan.DeviceProfile
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.LastPlace
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.search.loadsOnPhone
import dev.nerzouille.koan.thumbs.BakedBlur
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.theme.ThumbTokens
import android.util.JsonWriter
import dev.nerzouille.koan.data.ClosedTabEntity
import java.io.StringWriter
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.action.CrashAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSessionState
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
 * kept, suspended or live, until the card is closed. At most [maxLive] of them hold a live engine
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
    private val history: HistoryRecorder,
    /** For Room writes and file work that must outlive any screen. */
    private val appScope: CoroutineScope,
    /** The engine's saved state back from its JSON (recently closed tabs). */
    private val readState: (String) -> EngineSessionState,
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

    /** Tabs opened from a link in another tab: that tab, for Back on their first page. */
    private val openers = HashMap<String, String>()

    /** The tab another app opened (a link, a share), while it is the current one: Back goes back there. */
    private var fromApp: String? = null

    /** That app ([fromApp]'s), when Android told us, and the link it sent. */
    private var fromAppSource: Opener.App = Opener.App(null, null)

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
        appScope.launch {
            ThumbnailStore.retainOnly(rooms.allTabs().mapNotNullTo(HashSet()) { it.thumbnailKey } + dao.closedTabThumbnails())
            // Blank screenshots saved by older builds go once, later, so startup isn't slowed.
            delay(BLANK_SCAN_DELAY_MS)
            val blank = ThumbnailStore.dropBlank()
            if (blank.isNotEmpty()) dao.clearThumbnails(blank)
        }
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
        if (captureLeaving && leaving != null && leaving != id) captureNow(leaving, ThumbTokens.LEAVE_CAPTURE_TIMEOUT_MS, "leaving")
        // Any screenshot still in flight now would show this tab's page, not its own.
        switches++

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

    /**
     * Opens [url] in a new tab of [spaceUuid]: right after the [opener]'s card (a link's "open in
     * new tab"), else at the bottom of the stack. [select]: shown now; otherwise it waits in the
     * stack and loads when opened. Returns the new tab's id.
     */
    fun openNew(spaceUuid: String, url: String, opener: TabEntity? = null, select: Boolean = true, fromApp: Opener.App? = null): String {
        val id = UUID.randomUUID().toString()
        if (opener != null) openers[id] = opener.koanTabId
        main.launch {
            openNew(id, spaceUuid, url, opener, select)
            if (fromApp != null) {
                this@TabManager.fromApp = id
                fromAppSource = fromApp
            }
        }
        return id
    }

    /** Who opened tab [id], for Back on its first page; null = nobody in particular. */
    fun openerOf(id: String): Opener? = when {
        fromApp == id -> fromAppSource
        else -> openers[id]?.let { Opener.Tab(it) }
    }

    private suspend fun openNew(id: String, spaceUuid: String, url: String, opener: TabEntity?, select: Boolean) {
        val after = opener?.takeIf { it.spaceUuid == spaceUuid && it.inStack }?.stackOrder ?: dao.maxStackOrder(spaceUuid)
        val row = TabEntity(
            id, spaceUuid = spaceUuid, url = url, title = displayHost(url).ifEmpty { url },
            order = 0, lastAccessed = System.currentTimeMillis(), origin = TabOrigin.LOCAL,
        )
        // The engine tab first, so the Room row never shows a card the engine doesn't know.
        if (select) {
            open(row).join()
        } else {
            ownTabs += id
            store.dispatch(TabListAction.AddTabAction(createTab(url = url, id = id), select = false))
        }
        appScope.launch { dao.insertInStack(row, after) }
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
    fun close(ids: List<String>, rows: List<TabEntity>, keepThumbnails: Boolean = false) = main.launch {
        val keep = rows.filter { it.pinned || it.essential }.mapTo(HashSet()) { it.koanTabId }
        ids.forEach {
            openers.remove(it)
            history.forget(it)
        }
        if (_current.value in ids) setCurrent(null)
        if (_blank.value in ids) _blank.value = null
        removeEngineTabs(ids)
        appScope.launch {
            keep.forEach { dao.setCardOrder(it, null) }
            val gone = ids.filter { it !in keep }
            if (gone.isNotEmpty()) dao.closeTabs(gone)
            // Kept for Undo: if the tab doesn't come back, the next start's cleanup drops it.
            if (!keepThumbnails) {
                rows.filter { it.koanTabId in gone }.mapNotNull { it.thumbnailKey }.forEach { ThumbnailStore.delete(it) }
                gone.forEach { dao.setThumbnail(it, null) }
            }
        }
    }

    /**
     * Closes normal tab [id] like [close], keeping what [reopen] needs to bring it back, now (Undo)
     * and later (Recently closed, persisted).
     */
    suspend fun closeForUndo(id: String): ClosedTab? {
        val row = dao.tab(id) ?: return null
        val closed = ClosedTab(row, freshState(id))
        val json = closed.state?.let { state -> runCatching { StringWriter().also { state.writeTo(JsonWriter(it)) }.toString() }.getOrNull() }
        close(listOf(id), listOf(row), keepThumbnails = true).join()
        appScope.launch {
            val dropped = dao.addClosedTab(ClosedTabEntity(id, row, System.currentTimeMillis(), json), ClosedTabEntity.KEEP)
            // Their screenshots go with them, unless the tab is open again meanwhile.
            dropped.filter { dao.tab(it.koanTabId)?.closed != false }.mapNotNull { it.tab.thumbnailKey }.forEach { ThumbnailStore.delete(it) }
        }.join() // Recorded before an Undo can take it back.
        return closed
    }

    /**
     * [id]'s engine state (page history, scroll) as of now. GeckoView only reports it now and then,
     * so a live tab is asked for it first (waiting a moment at most).
     */
    private suspend fun freshState(id: String): EngineSessionState? {
        val engineState = store.state.findTab(id)?.engineState ?: return null
        val session = engineState.engineSession ?: return engineState.engineSessionState
        val before = engineState.engineSessionState
        withContext(Dispatchers.Main) { session.flushSessionState() }
        return withTimeoutOrNull(STATE_FLUSH_TIMEOUT_MS) {
            store.flow().map { it.findTab(id)?.engineState?.engineSessionState }.first { it != null && it !== before }
        } ?: store.state.findTab(id)?.engineState?.engineSessionState
    }

    /** Undo: [closed] is back in its place in the stack, its page history and scroll too (not opened yet). */
    suspend fun reopen(closed: ClosedTab) {
        val row = closed.row
        ownTabs += row.koanTabId
        withContext(Dispatchers.Main) {
            store.dispatch(TabListAction.AddTabAction(createTab(url = row.url, id = row.koanTabId, engineSessionState = closed.state), select = false))
        }
        appScope.launch {
            dao.deleteClosedTab(row.koanTabId)
            // Back in its place; if another card took that place meanwhile, just behind it.
            val taken = dao.stackTabs(row.spaceUuid).any { it.order == row.order && it.koanTabId != row.koanTabId }
            if (taken) dao.insertInStack(row, after = row.order - 1) else dao.upsertTabs(listOf(row))
        }.join()
    }

    /**
     * A recently closed tab back in the stack, like [reopen]. Its space may be gone ([fallbackSpace]
     * then), and Zen may have closed it meanwhile (it comes back as a phone tab). Null if unknown.
     */
    suspend fun restoreClosed(id: String, fallbackSpace: String): ClosedTab? {
        val entry = dao.closedTab(id) ?: return null
        val existing = dao.tab(id)
        if (existing != null && !existing.closed) {
            // Already back (an Undo raced it).
            dao.deleteClosedTab(id)
            return ClosedTab(existing, null)
        }
        val spaceKnown = dao.spaces().any { it.uuid == entry.tab.spaceUuid }
        var row = entry.tab.copy(closed = false, spaceUuid = if (spaceKnown) entry.tab.spaceUuid else fallbackSpace, folderId = null, splitId = null)
        if (row.origin == TabOrigin.ZEN && existing == null) {
            row = row.copy(origin = TabOrigin.LOCAL, zenTabId = null, zenOrder = -1, zenUrl = null)
        }
        if (!spaceKnown) row = row.copy(order = dao.maxStackOrder(row.spaceUuid) + 1)
        val state = entry.engineState?.let { json -> runCatching { readState(json) }.getOrNull() }
        return ClosedTab(row, state).also { reopen(it) }
    }

    /** Closes a tab that only held a download, back to the tab used before it. */
    private fun dropDownloadTab(id: String) = main.launch {
        val row = dao.tab(id)
        if (_current.value == id) {
            val back = recent.firstOrNull { it != id && store.state.findTab(it) != null }?.let { dao.tab(it) }
            if (back != null) open(back, captureLeaving = false).join()
        }
        close(listOf(id), listOfNotNull(row))
    }

    /** Ids of every tab that exists in the engine (live or suspended): opened on the phone. */
    fun liveIds(): Set<String> = store.state.tabs.mapTo(HashSet()) { it.id }

    private fun addEngineTab(id: String, url: String) {
        ownTabs += id
        store.dispatch(TabListAction.AddTabAction(createTab(url = url, id = id), select = true))
    }

    private fun setCurrent(id: String?) {
        if (_current.value != id) switches++
        // Another tab now: Back from the app's tab no longer goes back to that app.
        if (id != fromApp) fromApp = null
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

    /** Live engine sessions kept: the memory tier's (DeviceProfile), 4 on the owner's phones. */
    private val maxLive get() = DeviceProfile.tier.liveSessions

    /** Keeps the [maxLive] most recently opened sessions; the others are saved and closed. */
    private fun enforceBudget(max: Int = maxLive) {
        val selected = store.state.selectedTabId
        val live = store.state.tabs.filter { it.engineState.engineSession != null }
        if (live.size <= max) return
        val rank = recent.withIndex().associate { (i, id) -> id to i }
        live.sortedBy { if (it.id == selected) -1 else rank[it.id] ?: Int.MAX_VALUE }
            .drop(max)
            .forEach { store.dispatch(EngineAction.SuspendEngineSessionAction(it.id)) }
    }

    /**
     * In the background (in the LRU list, a kill may follow) only the current session stays live.
     * (The screenshot for the next start is taken in onPause, while the page is still on screen.)
     * Android 14+ only sends UI_HIDDEN and BACKGROUND to apps; older ones also warn while Koan is in
     * the foreground (RUNNING_LOW, RUNNING_CRITICAL), with the same answer, and critical also drops
     * the decoded screenshots.
     */
    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int) {
        val foregroundLow = level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || foregroundLow) main.launch { enforceBudget(max = 1) }
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            ThumbnailStore.trimMemory()
            BakedBlur.trimMemory()
        }
    }

    // ---- engine → Room -----------------------------------------------------------------------

    private val pageWrites = HashMap<String, Job>()
    private var captureJob: Job? = null
    private var scrollJob: Job? = null
    private var lastScrollAt = 0L

    /** When each tab was last screenshotted: background captures are throttled per tab. */
    private val lastCapture = HashMap<String, Long>()

    /** Counts tab switches: a screenshot taken across one belongs to no tab and is dropped. */
    private var switches = 0L
    private var lastCrashRestore = 0L

    private fun watchEngine() = main.launch {
        var known = store.state.tabs.associate { it.id to it.pageKey() }
        var knownDownloads = store.state.downloads.keys
        var loading = false
        var painted: String? = null
        var selected = store.state.selectedTabId
        store.flow().collect { state ->
            for (tab in state.tabs) {
                val key = tab.pageKey()
                if (known[tab.id] != key) {
                    schedulePageWrite(tab)
                    if (tab.id == state.selectedTabId && known[tab.id] != null) captureSoon(tab.id, "content changed", ThumbTokens.SETTLE_MS)
                }
                // A page opened a new tab (target=_blank, window.open): it joins the stack next to its opener.
                if (tab.id !in known && tab.id !in ownTabs) adoptWindowTab(tab)
            }
            // A tab opened straight onto a file (a link from another app, a download link in a
            // new tab) shows nothing and would download it again on every restore: it goes.
            if (state.downloads.keys != knownDownloads) {
                state.downloads.values.filter { it.id !in knownDownloads }.forEach { d ->
                    val tab = d.sessionId?.let { state.findTab(it) }
                    if (tab != null && tab.content.url == d.url && !tab.content.firstContentfulPaint) dropDownloadTab(tab.id)
                }
                knownDownloads = state.downloads.keys
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
            if (state.tabs.count { it.engineState.engineSession != null } > maxLive) enforceBudget()

            val current = state.selectedTabId?.let(state::findTab)
            // Content process died (HyperOS kills it in the background): bring the page back.
            if (current != null && current.engineState.crashed && System.currentTimeMillis() - lastCrashRestore > CRASH_RETRY_MS) {
                lastCrashRestore = System.currentTimeMillis()
                store.dispatch(CrashAction.RestoreCrashedSessionAction(current.id))
            }
            // Screenshots taken ahead of the overview: soon after the first paint, then once the
            // page has settled after loading.
            val nowPainted = current?.takeIf { it.content.firstContentfulPaint }?.id
            if (nowPainted != null && nowPainted != painted) captureSoon(nowPainted, "first paint", ThumbTokens.PAINT_SETTLE_MS)
            painted = nowPainted
            val nowLoading = current?.content?.loading == true
            if (loading && !nowLoading && current != null) captureSoon(current.id, "loaded", ThumbTokens.SETTLE_MS)
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
            val tab = store.state.findTab(id) ?: return@launch
            // Reader view shows its own extension page: the tab keeps the article's url.
            if (tab.readerState.active) return@launch
            val content = tab.content
            val url = content.url
            if (url.isEmpty() || url == "about:blank") return@launch
            val title = content.title
            history.pageShown(id, url, title)
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
        if (parentId != null) openers[tab.id] = parentId
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
    fun captureCurrent(reason: String) {
        val id = _current.value ?: return
        main.launch { captureNow(id, timeoutMs = 1000, reason) }
    }

    /** The page view scrolled: the current tab is screenshotted once it has been still a moment. */
    fun pageScrolled() {
        lastScrollAt = System.currentTimeMillis()
        if (scrollJob?.isActive == true) return
        scrollJob = main.launch {
            while (true) {
                val wait = lastScrollAt + ThumbTokens.SCROLL_SETTLE_MS - System.currentTimeMillis()
                if (wait <= 0) break
                delay(wait)
            }
            _current.value?.let { captureSoon(it, "scroll stopped", 0) }
        }
    }

    /**
     * Screenshots tab [id] in the background [delayMs] from now, so the overview finds a recent
     * screenshot ready. A newer request replaces a pending one; at most one capture per tab every
     * [ThumbTokens.CAPTURE_MIN_INTERVAL_MS].
     */
    private fun captureSoon(id: String, reason: String, delayMs: Long) {
        captureJob?.cancel()
        captureJob = main.launch {
            delay(delayMs)
            val wait = (lastCapture[id] ?: 0L) + ThumbTokens.CAPTURE_MIN_INTERVAL_MS - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            if (_current.value == id) captureNow(id, timeoutMs = 1000, reason)
        }
    }

    /**
     * Screenshots tab [id] if it is the one on screen and has painted; stores it and points Room at
     * it. Waits at most [timeoutMs] for the engine (the encode runs in the background).
     */
    private suspend fun captureNow(id: String, timeoutMs: Long, reason: String) {
        capLog(id, "requested ($reason)")
        val view = engineView.get() ?: return capLog(id, "skipped: no page view")
        val tab = store.state.findTab(id) ?: return capLog(id, "skipped: no engine tab")
        if (store.state.selectedTabId != id) return capLog(id, "skipped: not on screen")
        if (tab.engineState.engineSession == null) return capLog(id, "skipped: not live")
        if (!tab.content.firstContentfulPaint) return capLog(id, "skipped: not painted yet")
        val before = switches
        val url = tab.content.url
        lastCapture[id] = System.currentTimeMillis()
        val bitmap = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Bitmap?> { cont -> view.captureThumbnail { if (cont.isActive) cont.resume(it) } }
        } ?: return capLog(id, "no bitmap within ${timeoutMs}ms")
        // The engine sends the pixels the page view shows when it gets to it, not the tab's: if the
        // view switched tabs or this tab navigated meanwhile, they may be another page's.
        val why = when {
            switches != before || store.state.selectedTabId != id -> "tab switched"
            store.state.findTab(id)?.content?.url != url -> "navigated"
            else -> null
        }
        if (why != null) {
            bitmap.recycle()
            return capLog(id, "discarded: $why while capturing")
        }
        appScope.launch {
            val previous = dao.tab(id)?.thumbnailKey
            val (key, dropped) = ThumbnailStore.save(id, bitmap, previous)
                ?: return@launch capLog(id, "not saved: blank (or write failed)")
            capLog(id, "saved $key")
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

    // TEMPORARY (capture debugging): debug builds only; remove once captures have proven stable.
    private fun capLog(id: String, event: String) {
        if (dev.nerzouille.koan.BuildConfig.DEBUG) android.util.Log.i("KoanCapture", "${id.take(8)} $event")
    }

    companion object {
        private const val PAGE_WRITE_DEBOUNCE_MS = 400L
        private const val CRASH_RETRY_MS = 5000L
        private const val PREFETCH_BEHIND = 6
        private const val PREFETCH_FRONT = 2
        private const val BLANK_SCAN_DELAY_MS = 15_000L

        /** How long a close waits for a live tab's fresh engine state. */
        private const val STATE_FLUSH_TIMEOUT_MS = 500L
    }
}

private fun List<SpaceWithTabs>.allTabs(): List<TabEntity> =
    flatMap { s -> s.essentials + s.pinnedTabs + s.stack.flatMap { it.tabs } }.distinctBy { it.koanTabId }

/** Has (or should have) a card: normal tabs, and pinned/essential tabs opened on the phone. */
private fun TabEntity.hasCard() = !closed && inStack

/** Who opened a tab: another tab (a link), or another app. */
sealed interface Opener {
    data class Tab(val id: String) : Opener

    /** [referrer]: the app's package, when Android says; [link]: what it sent. */
    data class App(val referrer: String?, val link: String?) : Opener
}

/** A closed tab, kept for Undo: its Room row and its engine state (page history, scroll). */
class ClosedTab(val row: TabEntity, val state: EngineSessionState?)
