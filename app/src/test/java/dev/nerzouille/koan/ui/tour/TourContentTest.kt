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
    fun `feature version drops the patch`() {
        assertEquals("0.3", featureVersion("0.3.0"))
        assertEquals("1.12", featureVersion("1.12.4"))
    }
}
