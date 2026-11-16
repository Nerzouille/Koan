package dev.nerzouille.koan.ui.root

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The start page goes with the address field, even when both change in the same frame. */
class RootStateEditingTest {
    private fun state() = RootState(
        signingIn = mutableStateOf(false),
        currentTabs = HashMap(),
        currentSpace = mutableIntStateOf(0),
        currentSpaceUuid = mutableStateOf(null),
        overviewOpen = mutableStateOf(false),
        editingState = mutableStateOf(false),
        landing = mutableStateOf(null),
        startPage = mutableStateOf(false),
        overviewSettled = mutableStateOf(false),
        placeRestored = mutableStateOf(false),
        focusZenTop = mutableStateOf(false),
        heroPick = mutableStateOf(null),
        closedFrom = mutableStateOf(null),
        normalSpaceUuid = mutableStateOf(null),
        firstRunHandled = mutableStateOf(false),
    )

    /** What a launch did with both the start page and What's new due: open, then the tour closes the field. */
    @Test fun `closing the field in the same frame as the start page opened leaves the start page`() {
        val s = state()
        s.startPage = true
        s.editing = true
        s.editing = false
        assertFalse(s.startPage)
    }

    @Test fun `the start page stays while the field is open`() {
        val s = state()
        s.startPage = true
        s.editing = true
        assertTrue(s.startPage)
        s.editing = true
        assertTrue(s.startPage)
    }

    @Test fun `closing a field that was already closed still leaves the start page`() {
        val s = state()
        s.startPage = true
        s.editing = false
        assertFalse(s.startPage)
    }
}
