package dev.nerzouille.koan.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class GeckoPrefsTest {
    @Test
    fun `light theme find colours, under both of Gecko's names`() {
        val p = GeckoPrefs.findPrefs(dark = false)
        assertEquals(8, p.size)
        for (suffix in listOf("", ".dark")) {
            assertEquals("#1554CB", p["ui.textSelectAttentionBackground$suffix"])
            assertEquals("#FFFFFF", p["ui.textSelectAttentionForeground$suffix"])
            assertEquals("#D0DDF5", p["ui.textHighlightBackground$suffix"])
            assertEquals("#000000", p["ui.textHighlightForeground$suffix"])
        }
    }

    @Test
    fun `dark theme find colours`() {
        val p = GeckoPrefs.findPrefs(dark = true)
        assertEquals("#1554CB", p["ui.textSelectAttentionBackground"])
        assertEquals("#FFFFFF", p["ui.textSelectAttentionForeground.dark"])
        assertEquals("#1C396E", p["ui.textHighlightBackground"])
        assertEquals("#FFFFFF", p["ui.textHighlightForeground.dark"])
    }
}
