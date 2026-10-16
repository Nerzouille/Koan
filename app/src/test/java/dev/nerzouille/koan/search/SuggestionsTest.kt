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
    }
}
