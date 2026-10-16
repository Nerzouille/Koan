package dev.nerzouille.koan.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class UBlockTest {
    @Test fun `badge text reads as a count`() {
        assertEquals(0, UBlock.count(null))
        assertEquals(0, UBlock.count(""))
        assertEquals(24, UBlock.count("24"))
        assertEquals(1200, UBlock.count("1.2k"))
        assertEquals(0, UBlock.count("?"))
    }
}
