package dev.nerzouille.koan.ui.browser

import dev.nerzouille.koan.search.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateCueTest {
    private val phone = listOf("en", "fr")
    private val article = TranslateSignals(pageLanguage = "de", geckoOffers = true, readable = true, translated = false, processing = false)

    private fun cue(s: TranslateSignals = article, offerOn: Boolean = true, search: Boolean = false, dismissed: Boolean = false) =
        translateCue(s, phone, offerOn, search, dismissed)

    @Test fun `a readable article in another language gets the card`() {
        assertEquals(TranslateCue.Card, cue())
    }

    @Test fun `by default only the icon`() {
        // A short page or one that is mostly links, forms or media (no reader view).
        assertEquals(TranslateCue.Icon, cue(article.copy(readable = false)))
        // Gecko isn't confident enough to offer it.
        assertEquals(TranslateCue.Icon, cue(article.copy(geckoOffers = false)))
        // A search engine's results.
        assertEquals(TranslateCue.Icon, cue(search = true))
        // The card was dismissed on this site since Koan started: the icon stays.
        assertEquals(TranslateCue.Icon, cue(dismissed = true))
        // Already translating.
        assertEquals(TranslateCue.Icon, cue(article.copy(processing = true)))
    }

    @Test fun `nothing for a phone language, an unknown one, a translated page or the switch off`() {
        assertEquals(TranslateCue.None, cue(article.copy(pageLanguage = "fr-CA")))
        assertEquals(TranslateCue.None, cue(article.copy(pageLanguage = null)))
        assertEquals(TranslateCue.None, cue(article.copy(translated = true)))
        assertEquals(TranslateCue.None, cue(offerOn = false))
        assertEquals(TranslateCue.None, cue(article.copy(always = true)))
        assertEquals(TranslateCue.None, cue(article.copy(never = true)))
    }

    private val engines = SearchEngine.builtIns + SearchEngine("custom-1", "Kagi", "https://kagi.com/search?q=%s", custom = true) +
        SearchEngine("custom-2", "Wiki", "https://example.org/find/%s", custom = true)

    @Test fun `results pages of every engine in the list`() {
        assertTrue(isSearchResults("https://www.google.com/search?q=hallo+welt&hl=de", engines))
        assertTrue(isSearchResults("https://www.google.fr/search?q=bonjour", engines))
        assertTrue(isSearchResults("https://www.google.co.uk/search?client=x&q=hello", engines))
        assertTrue(isSearchResults("https://duckduckgo.com/?q=hallo&ia=web", engines))
        assertTrue(isSearchResults("https://search.brave.com/search?q=hallo", engines))
        assertTrue(isSearchResults("https://www.baidu.com/s?ie=utf-8&wd=ni+hao", engines))
        assertTrue(isSearchResults("https://kagi.com/search?q=hallo", engines))
        assertTrue(isSearchResults("https://example.org/find/hallo", engines))
    }

    @Test fun `other pages of those sites, and other sites, are not results`() {
        assertFalse(isSearchResults("https://www.google.com/maps/place/Berlin", engines))
        assertFalse(isSearchResults("https://www.google.com/search", engines))
        assertFalse(isSearchResults("https://duckduckgo.com/about", engines))
        assertFalse(isSearchResults("https://example.org/find/", engines))
        assertFalse(isSearchResults("https://de.wikipedia.org/wiki/Hallo?q=x", engines))
        assertFalse(isSearchResults("https://news.example.com/search?q=x", engines))
        assertFalse(isSearchResults("not a url", engines))
    }

    @Test fun `site cores ignore www and the country domain`() {
        assertEquals("google", siteCore("www.google.co.uk"))
        assertEquals("google", siteCore("google.fr"))
        assertEquals("search.brave", siteCore("search.brave.com"))
    }
}
