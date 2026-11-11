package dev.nerzouille.koan.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {
    @Test fun `a day is added to what is already checked`() {
        val s = Selection<String>()
        s.start("other")
        s.addAll(listOf("a", "b"))
        assertEquals(setOf("other", "a", "b"), s.keys)
        assertTrue(s.hasAll(listOf("a", "b")))
    }

    @Test fun `a day is removed without touching other days`() {
        val s = Selection<String>()
        s.set(setOf("a", "b", "c"))
        s.removeAll(listOf("a", "b"))
        assertEquals(setOf("c"), s.keys)
        assertFalse(s.hasAll(listOf("a", "b")))
    }

    @Test fun `removing the last day leaves selection mode`() {
        val s = Selection<String>()
        s.addAll(listOf("a"))
        s.removeAll(listOf("a"))
        assertFalse(s.active)
    }

    @Test fun `an empty day is never all checked`() {
        val s = Selection<String>()
        s.start("a")
        assertFalse(s.hasAll(emptyList()))
    }

    @Test fun `half a day is not all checked`() {
        val s = Selection<String>()
        s.start("a")
        assertFalse(s.hasAll(listOf("a", "b")))
        s.keepOnly(setOf("b"))
        assertFalse(s.active)
    }
}
