package dev.nerzouille.koan.engine

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.ErrorType
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.lib.state.ext.flow

/**
 * Koan's own error pages (`assets/errorpages/error.html`), per tab: which tab shows one, the grace
 * wait during a network switch, the reload guard (a loaded page is never replaced by an error) and
 * the auto-reload when the network comes back. Rules in [ErrorRules]. Main thread only.
 */
class ErrorPages(
    private val store: BrowserStore,
    private val net: NetworkWatch,
    /** Main-thread scope. */
    private val scope: CoroutineScope,
    private val reload: (String) -> Unit,
    /** Loads an address in tab id; replace = in place of the current history entry. */
    private val load: (id: String, url: String, replace: Boolean) -> Unit,
    private val stop: (String) -> Unit,
) {
    /** [url]: the failed address. [page]: Koan's own page address when Koan loaded it itself (a stopped load). */
    private class Shown(val kind: ErrorKind, val url: String, val at: Long, val page: String? = null)

    private val shown = HashMap<String, Shown>()
    /** Tabs Koan is reloading while they show a loaded page: the url and when. */
    private val reloadingLoaded = HashMap<String, Pair<String, Long>>()
    private val autoReloads = HashMap<String, Int>()
    /** The address each tab's last top-level load asked for, and when (onLoadRequest). */
    private val pending = HashMap<String, Pair<String, Long>>()
    /** When each tab's current load began. */
    private val loadStart = HashMap<String, Long>()
    /** Tabs whose current load had nothing painted at some point (a new document came, or none yet). */
    private val unpainted = HashSet<String>()

    private val _waiting = MutableStateFlow<Set<String>>(emptySet())
    /** Tabs whose error page waits (grace): Koan's load line keeps going for them. */
    val waiting: StateFlow<Set<String>> = _waiting

    /** Short notes for the toast ("You're offline. The page stays as it is."). */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** The page's Close tab button, for this tab (KoanRoot closes it like Back on its first page). */
    val closeRequests = MutableSharedFlow<String>(extraBufferCapacity = 2)

    private var started = false

    /** Once per process, on the main thread. */
    fun start() {
        if (started) return
        started = true
        net.start()
        watchLoads()
        watchNetwork()
        watchHungLoads()
    }

    /** A top-level load of a web address in [session]'s tab (ExternalLinks.onLoadRequest). */
    fun noteLoad(session: EngineSession, uri: String) {
        val id = tabOf(session)?.id ?: return
        pending[id] = uri to SystemClock.elapsedRealtime()
    }

    private fun tabOf(session: EngineSession) =
        store.state.let { s -> (s.tabs + s.customTabs).firstOrNull { it.engineState.engineSession === session } }

    /** The engine's error for [session]'s load of [uri] (RequestInterceptor.onErrorRequest). */
    fun onError(session: EngineSession, type: ErrorType, uri: String?): RequestInterceptor.ErrorResponse? {
        val url = uri ?: return null
        val tab = tabOf(session)
        val id = tab?.id
        val now = SystemClock.elapsedRealtime()
        // The stand-in for a load Koan stopped while offline: the offline page for the failed address.
        ErrorPageUrl.failedOf(url)?.let { failed ->
            if (id != null && shown[id]?.page != url) shown[id] = Shown(ErrorKind.Offline, failed, now, url)
            log("offline page for a stopped load")
            return RequestInterceptor.ErrorResponse(ErrorPageUrl.build(ErrorKind.Offline, failed, tab?.content?.private == true, 0, standalone = true))
        }
        val reloadOfLoaded = id != null && reloadingLoaded.remove(id)?.let { (u, at) -> u == url && now - at < RELOAD_MARK_MS } == true
        val online = net.state.value.online
        val decision = ErrorRules.decide(preview(url) ?: ErrorRules.kindOf(type), online, net.sinceChangeMs(), id?.let { autoReloads[it] } ?: 0, reloadOfLoaded)
        log("error $type online=$online since=${net.sinceChangeMs()} → $decision")
        return when (decision) {
            is ErrorDecision.Keep -> {
                messages.tryEmit(decision.message)
                null
            }
            is ErrorDecision.Show -> {
                if (id != null) {
                    shown[id] = Shown(decision.kind, url, now)
                    if (decision.waitMs > 0) wait(id, url, decision)
                }
                val private = tab?.content?.private == true || (preview(url) != null && "koan_error_private=1" in url)
                RequestInterceptor.ErrorResponse(ErrorPageUrl.build(decision.kind, url, private, decision.waitMs))
            }
        }
    }

    /** Whether tab [id] shows Koan's error page now. */
    fun isShowing(id: String): Boolean {
        val s = shown[id] ?: return false
        val url = store.state.findTabOrCustomTab(id)?.content?.url
        return url == s.url || (s.page != null && url == s.page)
    }

    /**
     * Pull or ⋯ Reload on an error page Koan loaded itself: its address is Koan's page, so load the
     * failed address over it instead. False = not such a page (reload as usual).
     */
    fun retryOwnPage(id: String): Boolean {
        val s = shown[id]?.takeIf { it.page != null && isShowing(id) } ?: return false
        load(id, s.url, true)
        return true
    }

    /** Whether tab [id] shows Koan's error page for [url] (not history). Main thread. */
    fun isShowingUrl(id: String, url: String): Boolean = shown[id]?.url == url && isShowing(id)

    /**
     * Koan is about to reload tab [id] (pull, ⋯ Reload). False = don't: it shows a loaded page and
     * the phone is offline, so the page stays (a toast says so).
     */
    fun beforeReload(id: String): Boolean {
        if (isShowing(id)) {
            autoReloads.remove(id)
            return true
        }
        if (!net.state.value.online) {
            messages.tryEmit(ErrorRules.OFFLINE_KEEP)
            return false
        }
        val url = store.state.findTabOrCustomTab(id)?.content?.url ?: return true
        reloadingLoaded[id] = url to SystemClock.elapsedRealtime()
        return true
    }

    /** The error page's Close tab button ([ErrorPageUrl.CLOSE]): only from Koan's own error page. */
    fun closeFrom(session: EngineSession, hasUserGesture: Boolean) {
        val tab = tabOf(session) ?: return
        log("close asked: showing=${isShowing(tab.id)} gesture=$hasUserGesture")
        if (hasUserGesture && isShowing(tab.id)) closeRequests.tryEmit(tab.id)
    }

    /** A tab's error page during the grace: the load line runs, then the page shows, or one retry. */
    private fun wait(id: String, url: String, decision: ErrorDecision.Show) {
        _waiting.value = _waiting.value + id
        scope.launch {
            delay(decision.waitMs)
            _waiting.value = _waiting.value - id
            val still = shown[id]?.url == url && isShowing(id)
            if (still && decision.retryAfterWait && net.state.value.online && store.state.selectedTabId == id) autoReload(id, "after the grace")
        }
    }

    private fun autoReload(id: String, why: String) {
        autoReloads[id] = (autoReloads[id] ?: 0) + 1
        _waiting.value = _waiting.value - id
        log("auto-reload ($why), #${autoReloads[id]}")
        val own = shown[id]?.takeIf { it.page != null }
        if (own != null) load(id, own.url, true) else reload(id)
    }

    /**
     * A tab's load that ends without an error since it began (not the error page's own load, which
     * starts right after the error) clears its error state and its auto-reload count. Not on a
     * load's start: the page's own Close tab button starts one.
     */
    private fun watchLoads() = scope.launch {
        var loading = emptyMap<String, Boolean>()
        val started = HashMap<String, Long>()
        store.flow().map { s -> (s.tabs + s.customTabs).associate { it.id to (it.content.loading to it.content.firstContentfulPaint) } }
            .distinctUntilChanged().collect { tabs ->
                val t = SystemClock.elapsedRealtime()
                tabs.forEach { (id, state) ->
                    val (l, painted) = state
                    val was = loading[id] ?: false
                    if (l && !was) {
                        loadStart[id] = t
                        unpainted.remove(id)
                        if (shown[id].let { it == null || t - it.at > OWN_LOAD_MS }) started[id] = t
                    }
                    if (l && !painted) unpainted += id
                    if (!l && was) {
                        val start = started.remove(id)
                        if (start != null && shown[id].let { it != null && it.at < start }) shown.remove(id)
                        reloadingLoaded[id]?.let { (_, at) -> if (t - at > OWN_LOAD_MS) reloadingLoaded.remove(id) }
                        if (shown[id] == null) autoReloads.remove(id)
                        loadStart.remove(id)
                        unpainted.remove(id)
                    }
                }
                (loading.keys - tabs.keys).forEach { id ->
                    shown.remove(id); reloadingLoaded.remove(id); autoReloads.remove(id); started.remove(id)
                    pending.remove(id); loadStart.remove(id); unpainted.remove(id)
                    _waiting.value = _waiting.value - id
                }
                loading = tabs.mapValues { it.value.first }
            }
    }

    /**
     * While Android has no real network, a load that is still waiting after the grace is stopped
     * and the offline page shown by Koan itself ([ErrorRules.stopHungLoad]): a VPN that stays up
     * with nothing under it makes the engine wait ~35 s for its own error. A reload of a loaded
     * page just stops (the page stays).
     */
    private fun watchHungLoads() = scope.launch {
        net.state.map { it.online }.distinctUntilChanged().collectLatest { online ->
            if (online) return@collectLatest
            while (true) {
                delay(HUNG_CHECK_MS)
                stopHungLoads()
            }
        }
    }

    private fun stopHungLoads() {
        val t = SystemClock.elapsedRealtime()
        val online = net.state.value.online
        store.state.let { it.tabs + it.customTabs }.forEach { tab ->
            val id = tab.id
            val c = tab.content
            val start = loadStart[id] ?: return@forEach
            if (!c.loading) return@forEach
            // The address this load asked for (a link, a typed address); a reload asks for none.
            val url = pending[id]?.takeIf { (_, at) -> at >= start - PENDING_MS }?.first ?: ErrorPageUrl.shown(c.url)
            val painted = id in unpainted && c.firstContentfulPaint
            if (!ErrorRules.stopHungLoad(online, net.sinceChangeMs(), t - start, painted, url)) return@forEach
            loadStart.remove(id)
            if (reloadingLoaded.remove(id) != null) {
                log("hung reload stopped while offline, the page stays")
                stop(id)
                messages.tryEmit(ErrorRules.OFFLINE_KEEP)
                return@forEach
            }
            val page = ErrorPageUrl.standIn(url)
            // Recorded before the stop: the stopped load's end must not clear it.
            shown[id] = Shown(ErrorKind.Offline, url, t, page)
            log("hung load stopped while offline after ${t - start}ms → offline page")
            stop(id)
            // Already on the offline page (its Try again went nowhere): in its place, not a second entry.
            val replace = ErrorPageUrl.failedOf(c.url) != null
            scope.launch {
                delay(STOP_SETTLE_MS)
                load(id, page, replace)
            }
        }
    }

    /** The network came back, or a tab came up: the selected tab's error page may reload. */
    private fun watchNetwork() = scope.launch {
        var wasOnline = net.state.value.online
        var lastChange = net.state.value.changedAt
        val selected = store.flow().map { it.selectedTabId }.distinctUntilChanged()
        combine(net.state, selected) { n, id -> n to id }.collect { (n, id) ->
            val trigger = when {
                n.online && (!wasOnline || n.changedAt != lastChange) -> AutoReloadTrigger.NetworkBack
                else -> AutoReloadTrigger.TabShown
            }
            wasOnline = n.online
            lastChange = n.changedAt
            id ?: return@collect
            if (!ErrorRules.autoReload(trigger, shown[id]?.takeIf { isShowing(id) }?.kind, n.online, autoReloads[id] ?: 0)) return@collect
            delay(ErrorRules.SETTLE_MS)
            if (store.state.selectedTabId == id && net.state.value.online && isShowing(id)) autoReload(id, trigger.name)
        }
    }

    /**
     * Dev, debug and staging builds: a failed test address with `koan_error_preview=<key>` shows
     * that kind's page (and `koan_error_private=1` the Private colours), for checking every page.
     */
    private fun preview(url: String): ErrorKind? {
        if (!dev.nerzouille.koan.ui.overview.StackLab.available) return null
        val key = url.substringAfter("koan_error_preview=", "").takeWhile { it.isLetter() }
        return ErrorKind.entries.firstOrNull { it.key == key }
    }

    private fun log(s: String) {
        // No addresses: dev, debug and staging builds only (like KoanPull).
        if (dev.nerzouille.koan.ui.overview.StackLab.available) Log.i("KoanError", s)
    }

    private companion object {
        /** A reload mark older than this is stale. */
        const val RELOAD_MARK_MS = 60_000L
        /** Load starts this soon after an error are the error page's own. */
        const val OWN_LOAD_MS = 1500L
        /** How often loads are checked while offline. */
        const val HUNG_CHECK_MS = 500L
        /** The stop reaches the engine before the stand-in's load. */
        const val STOP_SETTLE_MS = 300L
        /** A load request this long before its load began still belongs to it. */
        const val PENDING_MS = 1000L
    }
}
