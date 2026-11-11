package dev.nerzouille.koan.ui.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TourContentTest {
    private val one = listOf(TourCard("A highlight", "How to use it."))

    @Test
    fun `nothing seen yet gets the whole tour`() {
        assertEquals(TourSheet.Tour, dueSheet(null, "0.3"))
        assertEquals(TourSheet.Tour, dueSheet(null, "0.5", mapOf("0.4" to one)))
    }

    @Test
    fun `seen this version gets nothing`() {
        assertNull(dueSheet("0.3", "0.3"))
        assertNull(dueSheet("0.4", "0.4", mapOf("0.4" to one)))
    }

    @Test
    fun `a newer version with highlights gets its What's new once`() {
        assertEquals(TourSheet.WhatsNew("0.4"), dueSheet("0.3", "0.4", mapOf("0.4" to one)))
        assertNull(dueSheet("0.3", "0.4", emptyMap()))
    }

    @Test
    fun `skipped versions show the newest highlights up to this one`() {
        val highlights = mapOf("0.4" to one, "0.6" to one, "0.10" to one)
        assertEquals(TourSheet.WhatsNew("0.6"), dueSheet("0.3", "0.7", highlights))
        assertEquals(TourSheet.WhatsNew("0.10"), dueSheet("0.6", "0.10", highlights))
    }

    @Test
    fun `0_4 has 3 to 5 highlights and shows after 0_3`() {
        assertEquals(TourSheet.WhatsNew("0.4"), dueSheet("0.3", "0.4"))
        assert(Highlights.getValue("0.4").size in 3..5)
    }

    @Test
    fun `feature version drops the patch`() {
        assertEquals("0.3", featureVersion("0.3.0"))
        assertEquals("1.12", featureVersion("1.12.4"))
    }

    @Test
    fun `the 1_0 beta shows What's new 1_0 once after 0_4, and a later beta nothing`() {
        val beta1 = featureVersion("1.0.0-beta.1")
        assertEquals("1.0", beta1)
        assertEquals(TourSheet.WhatsNew("1.0"), dueSheet("0.4", beta1))
        assert(Highlights.getValue("1.0").size in 3..5)
        assertNull(dueSheet(beta1, beta1))
        assertNull(dueSheet(beta1, featureVersion("1.0.0-beta.2")))
        assertNull(dueSheet(beta1, featureVersion("1.0.0")))
    }

    @Test
    fun `the changelog link is the installed version's release page, pre-releases too`() {
        assertEquals("https://github.com/Nerzouille/Koan/releases/tag/v1.0.0-beta.1", releaseUrl("1.0.0-beta.1"))
        assertEquals("https://github.com/Nerzouille/Koan/releases/tag/v1.0.0", releaseUrl("1.0.0"))
    }
}
