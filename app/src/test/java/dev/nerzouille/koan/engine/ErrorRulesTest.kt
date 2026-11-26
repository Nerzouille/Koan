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

    @Test
    fun `a stopped load's stand-in address carries the failed address back`() {
        val failed = "https://www.example.com/a b?x=1&y=2#z"
        val standIn = ErrorPageUrl.standIn(failed)
        assertTrue(standIn.startsWith("http://127.0.0.1:4/"))
        assertTrue(ErrorRules.onPhone(standIn))
        assertEquals(failed, ErrorPageUrl.failedOf(standIn))
        assertEquals(failed, ErrorPageUrl.shown(standIn))
        assertEquals("https://example.org/", ErrorPageUrl.shown("https://example.org/"))
        assertTrue(ErrorPageUrl.build(ErrorKind.Offline, failed, private = false, waitMs = 0, standalone = true).endsWith("&s=1"))
        // Only the stand-in, and only a web address behind it.
        assertEquals(null, ErrorPageUrl.failedOf("https://example.org/?u=https%3A%2F%2Fexample.com"))
        assertEquals(null, ErrorPageUrl.failedOf("http://127.0.0.1:4/koan-offline?u=javascript%3Aalert(1)"))
    }
}
