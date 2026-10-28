package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {
    @Test fun `every engine's answer reads the same way`() {
        // Google adds more arrays after the list; Wikipedia adds descriptions and links.
        assertEquals(listOf("koan sound", "koans"), Suggestions.parse("""["koan",["koan sound","koans"],[],{"x":1}]"""))
        assertEquals(listOf("Koan"), Suggestions.parse("""["koan",["Koan"],[""],["https://en.wikipedia.org/wiki/Koan"]]"""))
        assertTrue(Suggestions.parse("""["koan"]""").isEmpty())
    }

    @Test fun `each engine asks its own endpoint`() {
        assertTrue(SearchEngine.Google.suggestUrl("a b")!!.startsWith("https://www.google.com/complete/search?client=firefox&q=a+b"))
        assertTrue(SearchEngine.DuckDuckGo.suggestUrl("x")!!.startsWith("https://ac.duckduckgo.com/"))
        assertTrue(Shortcut.YouTube.suggestUrl("x")!!.contains("ds=yt"))
        assertTrue(Shortcut.Wikipedia.suggestUrl("x")!!.contains("action=opensearch"))
        // Baidu answers in UTF-8 only when asked (ie=utf-8), and reads the query the same way.
        assertEquals("https://www.baidu.com/su?ie=utf-8&action=opensearch&wd=%E5%8C%97%E4%BA%AC", SearchEngine.Baidu.suggestUrl("北京"))
        assertEquals("https://www.baidu.com/s?ie=utf-8&wd=%E5%8C%97%E4%BA%AC", Shortcut.Baidu.searchUrl("北京"))
    }

    @Test fun `bd searches Baidu once`() {
        val parsed = parseQuery("@bd 北京")
        assertEquals(Shortcut.Baidu, parsed.shortcut)
        assertEquals("北京", parsed.rest)
    }
}
