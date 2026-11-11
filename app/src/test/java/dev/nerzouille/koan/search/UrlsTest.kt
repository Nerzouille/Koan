package dev.nerzouille.koan.search

import dev.nerzouille.koan.data.LoginStore
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What Koan derives from a web address today (audit 3.28), pinned before the helpers merge. The
 * three known bugs are asserted as they behave today; the commit that fixes each flips its line.
 */
class UrlsTest {
    @Test fun displayHostToday() {
        assertEquals("example.com", displayHost("https://example.com/a?b#c"))
        assertEquals("example.com", displayHost("https://www.example.com/"))
        assertEquals("m.example.com", displayHost("https://m.example.com/"))
        assertEquals("example.com", displayHost("http://user@example.com:8080/x"))
        assertEquals("192.168.1.10", displayHost("http://192.168.1.10:8000/"))
        assertEquals("alice.github.io", displayHost("https://alice.github.io/blog"))
        assertEquals("example.co.uk", displayHost("https://www.example.co.uk/"))
        assertEquals("", displayHost("about:blank"))
        assertEquals("about:config", displayHost("about:config"))
        assertEquals("", displayHost("file:///sdcard/a.html"))
        // Fixed (3.28 c): an IPv6 literal was cut at its first ':'.
        assertEquals("[::1]", displayHost("http://[::1]:8080/"))
        assertEquals("[2001:db8::1]", displayHost("https://[2001:db8::1]/"))
    }

    @Test fun siteHostToday() {
        assertEquals("example.com", siteHost("www.example.com"))
        assertEquals("example.com", siteHost("m.example.com"))
        assertEquals("en.example.org", siteHost("en.m.example.org"))
        assertEquals("example.com", siteHost("mobile.example.com"))
        assertEquals("m.com", siteHost("m.com"))
        assertEquals("example.com", siteHost("Example.COM"))
    }

    @Test fun baseDomainToday() {
        assertEquals("example.com", baseDomain("accounts.example.com"))
        assertEquals("example.com", baseDomain("www.example.com"))
        assertEquals("example.co.uk", baseDomain("accounts.example.co.uk"))
        assertEquals("example.com", baseDomain("example.com"))
        assertEquals("localhost", baseDomain("localhost"))
        assertEquals(baseDomain("x.example.com"), LoginStore.baseDomain("x.example.com"))
        // Fixed (3.28 d): one *.github.io site's login was offered on every other one.
        assertEquals("alice.github.io", baseDomain("alice.github.io"))
        assertEquals("alice.github.io", baseDomain("www.alice.github.io"))
        assertEquals("alice.github.io", baseDomain("docs.alice.github.io"))
        assertEquals("shop.pages.dev", baseDomain("shop.pages.dev"))
        assertEquals("github.io", baseDomain("github.io"))
    }

    @Test fun webUrls() {
        assertEquals(true, isWebUrl("https://example.com"))
        assertEquals(true, isWebUrl("HTTP://example.com"))
        assertEquals(false, isWebUrl("about:blank"))
        assertEquals(false, isWebUrl("file:///a"))
    }
}
