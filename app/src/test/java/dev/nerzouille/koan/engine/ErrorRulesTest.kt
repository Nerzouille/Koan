package dev.nerzouille.koan.engine

import mozilla.components.concept.engine.request.ErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorRulesTest {
    private val long = Long.MAX_VALUE

    @Test
    fun `every engine error has a page, the owner's list mapped as decided`() {
        val expected = mapOf(
            ErrorType.ERROR_OFFLINE to ErrorKind.Offline,
            ErrorType.ERROR_NO_INTERNET to ErrorKind.Offline,
            ErrorType.ERROR_UNKNOWN_HOST to ErrorKind.NotFound,
            ErrorType.ERROR_NET_TIMEOUT to ErrorKind.Timeout,
            ErrorType.ERROR_CONNECTION_REFUSED to ErrorKind.Refused,
            ErrorType.ERROR_NET_RESET to ErrorKind.Cut,
            ErrorType.ERROR_NET_INTERRUPT to ErrorKind.Cut,
            ErrorType.ERROR_SECURITY_SSL to ErrorKind.Cert,
            ErrorType.ERROR_SECURITY_BAD_CERT to ErrorKind.Cert,
            ErrorType.ERROR_BAD_HSTS_CERT to ErrorKind.Cert,
            ErrorType.ERROR_HTTPS_ONLY to ErrorKind.NoHttps,
            ErrorType.ERROR_SAFEBROWSING_PHISHING_URI to ErrorKind.Phishing,
            ErrorType.ERROR_SAFEBROWSING_MALWARE_URI to ErrorKind.Malware,
            ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI to ErrorKind.Unwanted,
            ErrorType.ERROR_UNSAFE_CONTENT_TYPE to ErrorKind.FileType,
            ErrorType.ERROR_PORT_BLOCKED to ErrorKind.Port,
            ErrorType.ERROR_REDIRECT_LOOP to ErrorKind.Loop,
            ErrorType.ERROR_CORRUPTED_CONTENT to ErrorKind.Failed,
            ErrorType.UNKNOWN to ErrorKind.Failed,
        )
        expected.forEach { (type, kind) -> assertEquals(type.name, kind, ErrorRules.kindOf(type)) }
        ErrorType.entries.forEach { ErrorRules.kindOf(it) }
    }

    @Test
    fun `security and blocked pages never offer a plain try again`() {
        listOf(ErrorKind.Cert, ErrorKind.NoHttps, ErrorKind.Phishing, ErrorKind.Malware, ErrorKind.Unwanted).forEach { assertTrue(it.leave) }
        listOf(ErrorKind.Offline, ErrorKind.NotFound, ErrorKind.Timeout, ErrorKind.Failed).forEach { assertFalse(it.leave) }
    }

    @Test
    fun `a network error while offline shows the offline page`() {
        val d = ErrorRules.decide(ErrorKind.NotFound, online = false, sinceChangeMs = long, autoReloads = 0, reloadOfLoaded = false)
        assertEquals(ErrorDecision.Show(ErrorKind.Offline, 0, false), d)
        // A site's own failure stays itself.
        val cert = ErrorRules.decide(ErrorKind.Cert, online = false, sinceChangeMs = long, autoReloads = 0, reloadOfLoaded = false)
        assertEquals(ErrorKind.Cert, (cert as ErrorDecision.Show).kind)
    }

    @Test
    fun `a failed reload of a loaded page keeps the page`() {
        val d = ErrorRules.decide(ErrorKind.Timeout, online = true, sinceChangeMs = long, autoReloads = 0, reloadOfLoaded = true)
        assertTrue(d is ErrorDecision.Keep)
        val off = ErrorRules.decide(ErrorKind.NotFound, online = false, sinceChangeMs = long, autoReloads = 0, reloadOfLoaded = true)
        assertEquals(ErrorDecision.Keep(ErrorRules.OFFLINE_KEEP), off)
    }

    @Test
    fun `a network switch gets the rest of the grace, then one retry when online`() {
        val online = ErrorRules.decide(ErrorKind.Cut, online = true, sinceChangeMs = 1000, autoReloads = 0, reloadOfLoaded = false)
        assertEquals(ErrorDecision.Show(ErrorKind.Cut, 3000, true), online)
        val offline = ErrorRules.decide(ErrorKind.NotFound, online = false, sinceChangeMs = 500, autoReloads = 0, reloadOfLoaded = false)
        assertEquals(ErrorDecision.Show(ErrorKind.Offline, 3500, false), offline)
    }

    @Test
    fun `no grace when the network is settled, for a site's own error, or after the automatic reloads`() {
        assertEquals(0L, (ErrorRules.decide(ErrorKind.Timeout, true, long, 0, false) as ErrorDecision.Show).waitMs)
        assertEquals(0L, (ErrorRules.decide(ErrorKind.Cert, true, 100, 0, false) as ErrorDecision.Show).waitMs)
        assertEquals(0L, (ErrorRules.decide(ErrorKind.Refused, true, 100, 0, false) as ErrorDecision.Show).waitMs)
        assertEquals(0L, (ErrorRules.decide(ErrorKind.Cut, true, 100, ErrorRules.MAX_AUTO_RELOADS, false) as ErrorDecision.Show).waitMs)
    }

    @Test
    fun `auto-reload only for network pages, online, a bounded number of times`() {
        assertTrue(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, ErrorKind.Offline, online = true, autoReloads = 0))
        assertTrue(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, ErrorKind.Timeout, online = true, autoReloads = 1))
        assertFalse(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, ErrorKind.Offline, online = false, autoReloads = 0))
        assertFalse(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, ErrorKind.Cert, online = true, autoReloads = 0))
        assertFalse(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, ErrorKind.Offline, online = true, autoReloads = ErrorRules.MAX_AUTO_RELOADS))
        assertFalse(ErrorRules.autoReload(AutoReloadTrigger.NetworkBack, null, online = true, autoReloads = 0))
    }

    @Test
    fun `a tab coming up reloads only an offline page`() {
        assertTrue(ErrorRules.autoReload(AutoReloadTrigger.TabShown, ErrorKind.Offline, online = true, autoReloads = 0))
        assertFalse(ErrorRules.autoReload(AutoReloadTrigger.TabShown, ErrorKind.NotFound, online = true, autoReloads = 0))
    }

    @Test
    fun `the page address carries the kind, host, failed address, private flag and wait`() {
        val url = ErrorPageUrl.build(ErrorKind.NotFound, "https://www.example.com/a b?x=1&y=2#z", private = true, waitMs = 1200)
        assertEquals(
            ErrorPageUrl.PAGE + "?k=notfound&h=example.com&u=https%3A%2F%2Fwww.example.com%2Fa+b%3Fx%3D1%26y%3D2%23z&p=1&w=1200",
            url,
        )
    }

    @Test
    fun `host falls back to the address when it has none`() {
        assertEquals("example.org", ErrorPageUrl.hostOf("http://example.org:8080/x"))
        assertEquals("not a url", ErrorPageUrl.hostOf("not a url"))
        assertEquals("<b>x</b>", ErrorPageUrl.hostOf("<b>x</b>"))
    }

    @Test
    fun `a hung load while offline stops only after the grace, before anything new painted, never for the phone itself`() {
        val url = "https://example.com/"
        assertTrue(ErrorRules.stopHungLoad(online = false, sinceChangeMs = 5000, loadingMs = 4000, painted = false, url = url))
        assertFalse(ErrorRules.stopHungLoad(online = true, sinceChangeMs = 5000, loadingMs = 9000, painted = false, url = url))
        // Within the grace, from the network's change or from the load's start.
        assertFalse(ErrorRules.stopHungLoad(online = false, sinceChangeMs = 3000, loadingMs = 9000, painted = false, url = url))
        assertFalse(ErrorRules.stopHungLoad(online = false, sinceChangeMs = long, loadingMs = 3999, painted = false, url = url))
        // The new page already shows: it stays.
        assertFalse(ErrorRules.stopHungLoad(online = false, sinceChangeMs = long, loadingMs = 9000, painted = true, url = url))
        listOf("http://localhost:8765/a", "http://127.0.0.1/", "about:blank", ErrorPageUrl.PAGE).forEach {
            assertFalse(it, ErrorRules.stopHungLoad(online = false, sinceChangeMs = long, loadingMs = 9000, painted = false, url = it))
        }
    }

    private fun standIns(): StandIns {
        var n = 0
        return StandIns(newToken = { "token${++n}" })
    }

    private val failed = "https://www.example.com/a b?x=1&y=2#z"

    @Test
    fun `Koan's own stand-in, refused, shows the offline page and maps back to the failed address`() {
        val s = standIns()
        val url = s.start("tab1", failed, private = false)
        assertTrue(url.startsWith("http://127.0.0.1:4/koan-offline?t=token1&u="))
        assertTrue(ErrorRules.onPhone(url))
        // Not mapped before its load failed as planned.
        assertEquals(null, s.shown("tab1", url))
        assertEquals(StandIns.Outcome.Offline(failed), s.onError("tab1", url, refused = true))
        assertFalse(s.onShown("tab1", url))
        assertEquals(failed, s.shown("tab1", url))
        // Moving on ends it: the same address later is just itself.
        assertFalse(s.onShown("tab1", "https://example.org/"))
        assertEquals(null, s.shown("tab1", url))
        assertEquals(StandIns.Outcome.NotOurs, s.onError("tab1", url, refused = true))
    }

    @Test
    fun `a site's stand-in address is never mapped - no token, a guessed token, another tab's token`() {
        val s = standIns()
        val ours = s.start("tab1", failed, private = false)
        val noToken = ErrorPageUrl.STAND_IN_BASE + "?u=https%3A%2F%2Fexample.org%2F"
        val guessed = ErrorPageUrl.standIn("https://example.org/", "token1")
        listOf(noToken, guessed).forEach {
            assertEquals(it, StandIns.Outcome.NotOurs, s.onError("tab1", it, refused = true))
            assertEquals(null, s.shown("tab1", it))
        }
        // Koan's own address, opened in another tab (a link copied from it).
        assertEquals(StandIns.Outcome.NotOurs, s.onError("tab2", ours, refused = true))
        assertEquals(null, s.shown("tab2", ours))
        // A different failed address with tab1's token isn't tab1's stand-in either.
        assertEquals(StandIns.Outcome.NotOurs, s.onError("tab1", ErrorPageUrl.standIn("https://example.net/", "token1"), refused = true))
        // Each stop gets a new token: the old address stops counting.
        val again = s.start("tab1", failed, private = false)
        assertEquals(StandIns.Outcome.NotOurs, s.onError("tab1", ours, refused = true))
        assertEquals(StandIns.Outcome.Offline(failed), s.onError("tab1", again, refused = true))
    }

    @Test
    fun `fail-safe - a stand-in that answers or fails otherwise is abandoned and never mapped`() {
        val s = standIns()
        // A response: the engine shows the stand-in's page before any error.
        val url = s.start("tab1", failed, private = false)
        assertTrue(s.onShown("tab1", url))
        assertEquals(null, s.shown("tab1", url))
        assertEquals(StandIns.Outcome.NotOurs, s.onError("tab1", url, refused = true))
        // Another error than "connection refused" (an engine update blocking the port, say).
        val other = s.start("tab1", failed, private = false)
        assertEquals(StandIns.Outcome.Abandon, s.onError("tab1", other, refused = false))
        assertEquals(null, s.shown("tab1", other))
        assertFalse(s.onShown("tab1", other))
    }

    @Test
    fun `only the engine's connection refused counts as the stand-in failing as planned`() {
        // ErrorPages passes refused = (type == ERROR_CONNECTION_REFUSED); the port must stay one
        // the engine doesn't block, else the error would be ERROR_PORT_BLOCKED (and Back breaks).
        assertTrue(ErrorPageUrl.STAND_IN_BASE.startsWith("http://127.0.0.1:4/"))
        assertEquals(ErrorKind.Refused, ErrorRules.kindOf(ErrorType.ERROR_CONNECTION_REFUSED))
    }

    @Test
    fun `stand-ins survive a restart for normal tabs only`() {
        val saved = HashMap<String, StandIns.Entry>()
        val s = standIns()
        s.onChange = { saved.clear(); saved.putAll(it) }
        val normal = s.start("tab1", failed, private = false)
        val private = s.start("tab2", "https://example.org/", private = true)
        s.onError("tab1", normal, refused = true)
        s.onError("tab2", private, refused = true)
        assertEquals(setOf("tab1"), saved.keys)
        val after = standIns().also { it.restore(saved) }
        assertEquals(failed, after.shown("tab1", normal))
        assertEquals(StandIns.Outcome.Offline(failed), after.onError("tab1", normal, refused = true))
        assertEquals(StandIns.Outcome.NotOurs, after.onError("tab2", private, refused = true))
        s.closed("tab1")
        assertTrue(saved.isEmpty())
    }
}
