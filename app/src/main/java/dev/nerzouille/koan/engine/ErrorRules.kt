package dev.nerzouille.koan.engine

import mozilla.components.concept.engine.request.ErrorType
import java.net.URI
import java.net.URLEncoder

/**
 * What Koan's error page says ([key] picks the icon and sentence in `assets/errorpages/error.html`).
 * [network]: the network may be to blame (offline override, grace, auto-reload). [leave]: the
 * page's button is Go back / Close tab, never a plain Try again (security and blocked pages).
 */
enum class ErrorKind(val key: String, val network: Boolean = false, val leave: Boolean = false) {
    Offline("offline", network = true),
    NotFound("notfound", network = true),
    Timeout("timeout", network = true),
    Refused("refused"),
    Cut("cut", network = true),
    Cert("cert", leave = true),
    NoHttps("nohttps", leave = true),
    Phishing("phishing", leave = true),
    Malware("malware", leave = true),
    Unwanted("unwanted", leave = true),
    FileType("filetype", leave = true),
    Port("port", leave = true),
    LocalNetwork("localnet", leave = true),
    Loop("loop"),
    Failed("failed"),
}

/** What to do with a failed load. */
sealed interface ErrorDecision {
    /** A loaded page's reload failed: the page stays, a toast says why. */
    data class Keep(val message: String) : ErrorDecision

    /** Koan's page for [kind]; its content waits [waitMs] (grace), then one retry if [retryAfterWait]. */
    data class Show(val kind: ErrorKind, val waitMs: Long, val retryAfterWait: Boolean) : ErrorDecision
}

/** Why the selected tab's error page might reload by itself. */
enum class AutoReloadTrigger { NetworkBack, TabShown }

/** The error pages' rules, pure (ErrorRulesTest). */
object ErrorRules {
    /** A network switch (Wi-Fi ↔ mobile, a VPN reconnecting) gets this long before an error shows. */
    const val GRACE_MS = 4000L

    /** Automatic reloads per failure, so a network that comes and goes never loops. */
    const val MAX_AUTO_RELOADS = 2

    /** After the network comes back, it settles this long before the reload. */
    const val SETTLE_MS = 500L

    fun kindOf(type: ErrorType): ErrorKind = when (type) {
        ErrorType.ERROR_OFFLINE, ErrorType.ERROR_NO_INTERNET -> ErrorKind.Offline
        ErrorType.ERROR_UNKNOWN_HOST, ErrorType.ERROR_UNKNOWN_PROXY_HOST -> ErrorKind.NotFound
        ErrorType.ERROR_NET_TIMEOUT -> ErrorKind.Timeout
        ErrorType.ERROR_CONNECTION_REFUSED, ErrorType.ERROR_PROXY_CONNECTION_REFUSED -> ErrorKind.Refused
        ErrorType.ERROR_NET_RESET, ErrorType.ERROR_NET_INTERRUPT -> ErrorKind.Cut
        ErrorType.ERROR_SECURITY_SSL, ErrorType.ERROR_SECURITY_BAD_CERT, ErrorType.ERROR_BAD_HSTS_CERT -> ErrorKind.Cert
        ErrorType.ERROR_HTTPS_ONLY -> ErrorKind.NoHttps
        ErrorType.ERROR_SAFEBROWSING_PHISHING_URI -> ErrorKind.Phishing
        ErrorType.ERROR_SAFEBROWSING_MALWARE_URI, ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI,
        ErrorType.ERROR_HARMFULADDON_URI -> ErrorKind.Malware
        ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI -> ErrorKind.Unwanted
        ErrorType.ERROR_UNSAFE_CONTENT_TYPE -> ErrorKind.FileType
        ErrorType.ERROR_PORT_BLOCKED -> ErrorKind.Port
        ErrorType.ERROR_LOCAL_NETWORK_ACCESS_DENIED -> ErrorKind.LocalNetwork
        ErrorType.ERROR_REDIRECT_LOOP -> ErrorKind.Loop
        else -> ErrorKind.Failed
    }

    /**
     * A load failed with [kind]. [online]: Android has a network now; [sinceChangeMs]: since the
     * default network last changed or went; [autoReloads]: Koan's own reloads of this failure so
     * far; [reloadOfLoaded]: Koan reloaded a page that had loaded (pull, ⋯ Reload).
     */
    fun decide(kind: ErrorKind, online: Boolean, sinceChangeMs: Long, autoReloads: Int, reloadOfLoaded: Boolean): ErrorDecision {
        val shown = if (kind.network && !online) ErrorKind.Offline else kind
        if (reloadOfLoaded) return ErrorDecision.Keep(keepMessage(shown))
        val wait = if (kind.network && autoReloads < MAX_AUTO_RELOADS && sinceChangeMs in 0 until GRACE_MS) GRACE_MS - sinceChangeMs else 0L
        return ErrorDecision.Show(shown, wait, retryAfterWait = wait > 0 && online)
    }

    /** Whether the selected tab, showing [kind]'s page, reloads by itself now. */
    fun autoReload(trigger: AutoReloadTrigger, kind: ErrorKind?, online: Boolean, autoReloads: Int): Boolean {
        if (kind == null || !online || autoReloads >= MAX_AUTO_RELOADS) return false
        return when (trigger) {
            AutoReloadTrigger.NetworkBack -> kind.network
            AutoReloadTrigger.TabShown -> kind == ErrorKind.Offline
        }
    }

    /**
     * A load still waiting while Android has no real network ([online] false): Koan stops it and
     * shows the offline page itself instead of waiting for the engine (a VPN that stays up with
     * nothing under it holds the connection ~35 s). Only once the grace has passed, counted both
     * from the network's last change and from the load's start, only while nothing new has
     * [painted] since the load began, and never for an address on the phone itself.
     */
    fun stopHungLoad(online: Boolean, sinceChangeMs: Long, loadingMs: Long, painted: Boolean, url: String): Boolean =
        !online && !painted && sinceChangeMs >= GRACE_MS && loadingMs >= GRACE_MS && !onPhone(url)

    /** localhost and loopback addresses (a dev server through adb, a local app) need no network. */
    fun onPhone(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return true
        val host = ErrorPageUrl.hostOf(url).lowercase()
        return host == "localhost" || host.endsWith(".localhost") || host.startsWith("127.") || host == "[::1]" || host == "::1"
    }

    /** A Koan reload of a loaded page while offline: skipped, the page stays. */
    const val OFFLINE_KEEP = "You're offline. The page stays as it is."

    fun keepMessage(kind: ErrorKind): String = when (kind) {
        ErrorKind.Offline -> OFFLINE_KEEP
        ErrorKind.NotFound -> "Couldn't reload: site not found"
        ErrorKind.Timeout -> "Couldn't reload: the site took too long"
        ErrorKind.Refused, ErrorKind.Cut -> "Couldn't reload: the connection failed"
        ErrorKind.Cert -> "Couldn't reload: certificate problem"
        else -> "Couldn't reload this page"
    }
}

/** Koan's error page address for a failed load. */
object ErrorPageUrl {
    const val PAGE = "resource://android/assets/errorpages/error.html"

    /** What the page's Close tab button opens; acted on only from Koan's own error page. */
    const val CLOSE = "koan-error:close"

    /**
     * [standalone]: the page stands for a load Koan stopped while offline ([standIn]), so the tab's
     * address isn't the failed one: Try again goes to [url] (`s=1`).
     */
    fun build(kind: ErrorKind, url: String, private: Boolean, waitMs: Long, standalone: Boolean = false): String =
        PAGE + "?k=" + kind.key + "&h=" + enc(hostOf(url)) + "&u=" + enc(url) + "&p=" + (if (private) 1 else 0) + "&w=" + waitMs +
            (if (standalone) "&s=1" else "")

    /**
     * Where Koan sends a load it stopped while offline: an address on the phone where nothing
     * listens (port 4, not one the engine blocks), so it fails at once and the engine shows Koan's
     * offline page for it (the engine won't load Koan's page directly). [token] is Koan's one-time
     * proof ([StandIns]); the failed address rides along for the engine's error only.
     */
    fun standIn(failed: String, token: String): String = "$STAND_IN_BASE?t=$token&u=" + enc(failed)

    /** Every stand-in address starts so (Koan's own or a site's copy). */
    const val STAND_IN_BASE = "http://127.0.0.1:4/koan-offline"

    /**
     * What Koan shows and keeps for tab [id]'s address [url]: the failed address behind Koan's own
     * stand-in in that tab ([StandIns.shown]), else [url] itself. Main thread.
     */
    fun shown(id: String?, url: String): String = if (id == null) url else standIns?.shown(id, url) ?: url

    /** Set by ErrorPages.start. */
    @Volatile var standIns: StandIns? = null

    /** The host to show: the address's host without "www.", else the address itself. */
    fun hostOf(url: String): String {
        val host = runCatching { URI(url).host }.getOrNull()
            // An address URI won't parse (a space, a stray character): the part after "://".
            ?: url.substringAfter("://", "").takeWhile { it != '/' && it != '?' && it != '#' }.substringAfterLast('@').substringBefore(':')
        return host.takeIf { it.isNotEmpty() }?.removePrefix("www.") ?: url
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

/**
 * Koan's stand-in loads ([ErrorPageUrl.standIn]), at most one per tab. Spoof-proof: a stand-in
 * address counts only when Koan started it in that tab with its random token; any other load of
 * such an address (a site's link) is shown as itself and gets the usual error page. Fail-safe:
 * the failed address replaces the stand-in's only once its load failed with "connection refused";
 * a response, or any other error, abandons the trick (the stand-in is never shown under the real
 * address). Pure; main thread.
 */
class StandIns(private val newToken: () -> String) {
    enum class State { Pending, Shown }

    data class Entry(val url: String, val failed: String, val state: State, val private: Boolean)

    sealed interface Outcome {
        /** Not Koan's stand-in for this tab: the usual handling. */
        data object NotOurs : Outcome

        /** Koan's offline page for [failed]. */
        data class Offline(val failed: String) : Outcome

        /** Koan's stand-in didn't fail as expected: keep the page, say offline, show nothing of it. */
        data object Abandon : Outcome
    }

    private val byTab = HashMap<String, Entry>()

    /** Persisted entries changed (normal tabs only; private ones are never written). */
    var onChange: (Map<String, Entry>) -> Unit = {}

    fun restore(saved: Map<String, Entry>) {
        byTab.putAll(saved.filterValues { !it.private })
    }

    /** Koan stops tab [id]'s hung load of [failed]: the stand-in address to load. */
    fun start(id: String, failed: String, private: Boolean): String {
        val url = ErrorPageUrl.standIn(failed, newToken())
        byTab[id] = Entry(url, failed, State.Pending, private)
        changed()
        return url
    }

    /** The engine's error for tab [id]'s load of [url]; [refused]: it was "connection refused". */
    fun onError(id: String, url: String, refused: Boolean): Outcome {
        val e = byTab[id]?.takeIf { it.url == url } ?: return Outcome.NotOurs
        if (!refused) {
            byTab.remove(id)
            changed()
            return Outcome.Abandon
        }
        if (e.state != State.Shown) {
            byTab[id] = e.copy(state = State.Shown)
            changed()
        }
        return Outcome.Offline(e.failed)
    }

    /**
     * Tab [id] now shows [url] (the engine committed it). True = Koan's stand-in got a real
     * response (it showed before its error): abandon it. Moving on to another address ends it.
     */
    fun onShown(id: String, url: String): Boolean {
        val e = byTab[id] ?: return false
        if (url == e.url) {
            if (e.state == State.Shown) return false
            byTab.remove(id)
            changed()
            return true
        }
        if (e.state == State.Shown) {
            byTab.remove(id)
            changed()
        }
        return false
    }

    /** The failed address for tab [id]'s [url] once Koan's stand-in failed as planned, else null. */
    fun shown(id: String, url: String): String? = byTab[id]?.takeIf { it.state == State.Shown && it.url == url }?.failed

    /** Whether tab [id]'s [url] is Koan's own stand-in, shown or on its way. */
    fun isOurs(id: String, url: String): Boolean = byTab[id]?.url == url

    fun closed(id: String) {
        if (byTab.remove(id) != null) changed()
    }

    private fun changed() = onChange(byTab.filterValues { !it.private })
}

