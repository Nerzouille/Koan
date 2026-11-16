package dev.nerzouille.koan.engine

import dev.nerzouille.koan.engine.downloads.PendingDeletes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Downloads' Undo follows its toast (audit 3.29): the removal happens when the toast leaves. */
class UndoWindowTest {
    @Test fun aReplacedToastCommitsAtOnce() {
        val p = PendingDeletes<String>()
        p.hold("a")
        // Another toast replaces the Undo toast: its onGone commits now, not 3 s later.
        assertEquals(listOf("a"), p.commit(listOf("a")))
        assertTrue(p.waiting.isEmpty())
    }

    @Test fun undoCancelsAndTheLaterCommitDoesNothing() {
        val p = PendingDeletes<String>()
        p.hold("a")
        assertTrue(p.cancel("a"))
        assertEquals(emptyList<String>(), p.commit(listOf("a")))
    }

    @Test fun eachKeyCommitsOnce() {
        val p = PendingDeletes<String>()
        p.hold("a"); p.hold("b")
        assertEquals(listOf("a", "b"), p.commit(listOf("a", "b")))
        assertEquals(emptyList<String>(), p.commit(listOf("a", "b")))
        assertFalse(p.cancel("a"))
    }

    @Test fun onlyThisToastsKeysCommit() {
        val p = PendingDeletes<String>()
        p.hold("a"); p.hold("b")
        assertEquals(listOf("a"), p.commit(listOf("a")))
        assertEquals(setOf("b"), p.waiting)
    }
}
