package dev.nerzouille.koan.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindCountTest {
    private fun hit(query: String = "word", found: Boolean = true, current: Int = 1, total: Int = 3, tab: String = "t") =
        FindHit(tab, query, found, current, total)

    @Test
    fun countedMatch() = assertEquals("2/3", findCount("t", "word", hit(current = 2)))

    @Test
    fun noMatch() = assertEquals("0/0", findCount("t", "word", hit(found = false, current = 0, total = 0)))

    @Test
    fun foundButNotCountedIsNotZero() {
        // Gecko highlighted a match its counter skipped: never "0/0".
        assertEquals("", findCount("t", "word", hit(current = 0, total = 0)))
        assertEquals("", findCount("t", "word", hit(current = 1, total = -1)))
    }

    @Test
    fun anOlderQueryOrAnotherTabIsNotShown() {
        assertEquals("", findCount("t", "words", hit(query = "word")))
        assertEquals("", findCount("t", "word", hit(tab = "other")))
    }

    @Test
    fun emptyFieldOrNoAnswerYet() {
        assertEquals("", findCount("t", "", hit()))
        assertEquals("", findCount("t", "word", null))
    }

    @Test fun `an empty field clears again only if nothing was typed since`() {
        val q = FindQueries()
        assertEquals(null, q.typed("a", "jazz"))
        val ticket = q.typed("a", "")!!
        assertTrue(q.stillEmpty(ticket))
        q.typed("b", "other tab")
        assertTrue(q.stillEmpty(ticket))
        q.typed("a", "j")
        assertFalse(q.stillEmpty(ticket))
    }
}
