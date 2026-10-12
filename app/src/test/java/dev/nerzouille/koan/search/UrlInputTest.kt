package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlInputTest {
    @Test fun `full urls pass through`() {
        assertEquals("https://example.com/a?b=1", resolveInput("https://example.com/a?b=1"))
        assertEquals("http://neverssl.com", resolveInput("  http://neverssl.com "))
        assertEquals("about:config", resolveInput("about:config"))
    }

    @Test fun `bare hosts get https`() {
        assertEquals("https://example.com", resolveInput("example.com"))
        assertEquals("https://en.wikipedia.org/wiki/Koan", resolveInput("en.wikipedia.org/wiki/Koan"))
        assertEquals("https://localhost:8080", resolveInput("localhost:8080"))
        assertEquals("https://192.168.1.10/admin", resolveInput("192.168.1.10/admin"))
    }

    @Test fun `everything else is a DuckDuckGo search`() {
        assertEquals("https://duckduckgo.com/?q=zen+browser", resolveInput("zen browser"))
        assertEquals("https://duckduckgo.com/?q=kotlin", resolveInput("kotlin"))
        assertEquals("https://duckduckgo.com/?q=what+is+example.com", resolveInput("what is example.com"))
        assertEquals("https://duckduckgo.com/?q=c%2B%2B", resolveInput("c++"))
    }

    @Test fun `empty input is blank page`() {
        assertEquals("about:blank", resolveInput("   "))
    }

    @Test fun `display host strips scheme and www`() {
        assertEquals("github.com", displayHost("https://www.github.com/Kjell6/Z-Sync"))
        assertEquals("localhost", displayHost("http://localhost:8080/x"))
        assertEquals("", displayHost("about:blank"))
        assertEquals("about:config", displayHost("about:config"))
    }
}
