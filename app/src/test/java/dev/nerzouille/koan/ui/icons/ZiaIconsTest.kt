package dev.nerzouille.koan.ui.icons

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The Zia mod's icon strings, as Zen stores them on a space, and the bundled icons. */
class ZiaIconsTest {
    @Test fun parsesEveryZiaForm() {
        assertEquals("outline" to "home", ZiaIcons.parse("resource://zia-tabler/outline/home.svg"))
        assertEquals("filled" to "star", ZiaIcons.parse("resource://zia-tabler/filled/star.svg"))
        assertEquals("outline" to "book", ZiaIcons.parse("chrome://sine/content/zia/icons/tabler/outline/book.svg"))
        assertEquals("phosphor" to "airplane", ZiaIcons.parse("chrome://sine/content/zia/icons/phosphor/airplane.svg"))
        assertNull(ZiaIcons.parse("chrome://browser/skin/zen-icons/selectable/baseball.svg"))
        assertNull(ZiaIcons.parse("🎨"))
        assertTrue(ZiaIcons.isZia("resource://zia-own-icons/mine.svg"))
    }

    @Test fun closestNameBySpelling() {
        val names = listOf("home", "home-2", "house", "book", "books")
        assertEquals("home", ZiaIcons.closest("hom", names))
        assertEquals("books", ZiaIcons.closest("bookss", names))
        assertNull(ZiaIcons.closest("zzzzzzzzzz", names))
        assertEquals(3, ZiaIcons.levenshtein("kitten", "sitting"))
    }

    @Test fun phosphorNamesLandOnBundledIcons() {
        val assets = File("src/main/assets/zia")
        val outline = JSONObject(File(assets, "tabler.json").readText()).getJSONObject("icons").getJSONObject("outline")
        val map = JSONObject(File(assets, "phosphor-to-tabler.json").readText())
        val missing = map.keys().asSequence().map { map.getString(it) }.filterNot { outline.has(it) }.toList()
        assertTrue("Phosphor names mapped to icons the bundle lacks: ${missing.take(5)}", missing.size < map.length() / 20)
    }
}
