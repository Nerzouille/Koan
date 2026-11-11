package dev.nerzouille.koan.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTitleTest {
    @Test fun dropsSiteSuffixThatRepeatsTheHost() {
        assertEquals("Koan", cardTitle("Koan - Wikipedia", "https://en.wikipedia.org/wiki/Koan"))
        assertEquals("A page", cardTitle("A page — Example", "https://www.example.com/a"))
    }

    @Test fun keepsOtherSuffixes() {
        assertEquals("Koan - a story", cardTitle("Koan - a story", "https://en.wikipedia.org/wiki/Koan"))
        assertEquals("Wikipedia", cardTitle("Wikipedia", "https://en.wikipedia.org/"))
        assertEquals(" - Wikipedia", cardTitle(" - Wikipedia", "https://en.wikipedia.org/"))
    }

    @Test fun multiWordSiteName() {
        assertEquals("Docs", cardTitle("Docs - Example Shop", "https://exampleshop.com/docs"))
    }
}
