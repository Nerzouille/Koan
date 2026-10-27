package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomEnginesTest {
    private val mine = SearchEngine("custom:1", "Example Search", "https://search.example.com/?q=%s", "https://search.example.com/ac?q=%s", "ex", custom = true)

    @Test
    fun savedEnginesComeBack() {
        val other = SearchEngine("custom:2", "Other", "https://other.example.org/s?q=%s", custom = true)
        assertEquals(listOf(mine, other), CustomEngines.fromJson(CustomEngines.toJson(listOf(mine, other))))
    }

    @Test
    fun brokenSavedListGivesNone() {
        assertEquals(emptyList<SearchEngine>(), CustomEngines.fromJson("not json"))
        assertEquals(emptyList<SearchEngine>(), CustomEngines.fromJson(null))
    }

    @Test
    fun aValidEnginePasses() {
        val check = CustomEngines.check("Mine", "https://example.net/search?q=%s", "@mine", "", SearchEngine.builtIns)
        assertTrue(check.ok)
    }

    @Test
    fun searchUrlMustBeHttpsWithPlaceholder() {
        val others = SearchEngine.builtIns
        assertEquals("Must start with https://", CustomEngines.check("A", "http://example.net/?q=%s", "", "", others).url)
        assertEquals("Put %s where the search words go", CustomEngines.check("A", "https://example.net/?q=", "", "", others).url)
        assertTrue(CustomEngines.check("A", "", "", "", others).url != null)
        assertEquals("Must start with https://", CustomEngines.check("A", "https://example.net/?q=%s", "", "ftp://x/%s", others).suggest)
    }

    @Test
    fun shortcutMustBeFree() {
        val others = SearchEngine.builtIns + mine
        assertEquals("@wiki is used by Koan", CustomEngines.check("A", "https://a.example/?q=%s", "@wiki", "", others).shortcut)
        assertEquals("@tabs is used by Koan", CustomEngines.check("A", "https://a.example/?q=%s", "tabs", "", others).shortcut)
        assertEquals("@ex is used by another engine", CustomEngines.check("A", "https://a.example/?q=%s", "@EX", "", others).shortcut)
        assertTrue(CustomEngines.check("A", "https://a.example/?q=%s", "a b", "", others).shortcut != null)
        assertNull(CustomEngines.check("A", "https://a.example/?q=%s", "", "", others).shortcut)
    }

    @Test
    fun nameMustBeNewAndNotBlank() {
        assertTrue(CustomEngines.check(" ", "https://a.example/?q=%s", "", "", emptyList()).name != null)
        assertTrue(CustomEngines.check("google", "https://a.example/?q=%s", "", "", SearchEngine.builtIns).name != null)
    }

    @Test
    fun customEngineWorksLikeTheOthers() {
        assertEquals("https://search.example.com/?q=koan+app", mine.searchUrl("koan app"))
        assertEquals("https://search.example.com/ac?q=k", mine.suggestUrl("k"))
        assertEquals("https://search.example.com", mine.home)
        assertEquals("https://search.example.com/?q=hello+world", resolveInput("hello world", mine))
        val shortcuts = Shortcut.all(SearchEngine.builtIns + mine)
        val parsed = parseQuery("@ex koan", shortcuts)
        assertEquals("ex", parsed.shortcut?.key)
        assertEquals("https://search.example.com/?q=koan", parsed.shortcut?.searchUrl(parsed.rest))
        assertTrue(parsed.shortcut!!.isDefault(mine))
        // Without its shortcut, "@ex" is just text.
        assertNull(parseQuery("@ex koan").shortcut)
    }

    @Test
    fun originHandlesHostsWithoutPath() {
        assertEquals("https://example.net", origin("https://example.net?q=%s"))
        assertEquals("https://example.net:8443", origin("https://example.net:8443/search?q=%s"))
    }
}
