package dev.nerzouille.koan.ui.root

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import dev.nerzouille.koan.privacy.PrivateTabs
import org.junit.Assert.assertEquals
import org.junit.Test

/** Killed while Private showed: the new process opens normal mode, on the last normal space. */
class RootStatePrivateRestoreTest {
    private fun state(current: String?, normal: String?) = RootState(
        signingIn = mutableStateOf(false),
        currentTabs = HashMap(),
        currentSpace = mutableIntStateOf(0),
        currentSpaceUuid = mutableStateOf(current),
        overviewOpen = mutableStateOf(false),
        editingState = mutableStateOf(false),
        landing = mutableStateOf(null),
        startPage = mutableStateOf(false),
        overviewSettled = mutableStateOf(false),
        placeRestored = mutableStateOf(true),
        focusZenTop = mutableStateOf(false),
        heroPick = mutableStateOf(null),
        closedFrom = mutableStateOf(null),
        normalSpaceUuid = mutableStateOf(normal),
        normalSpace = mutableIntStateOf(1),
        firstRunHandled = mutableStateOf(false),
    )

    @Test fun `restored on Private in normal mode goes back to the last normal space`() {
        val root = state(current = PrivateTabs.SPACE.uuid, normal = "space-b")
        root.leavePrivatePlace(privateOn = false)
        assertEquals("space-b", root.currentSpaceUuid)
        assertEquals(1, root.currentSpace)
    }

    @Test fun `still in Private (configuration change) stays on Private`() {
        val root = state(current = PrivateTabs.SPACE.uuid, normal = "space-b")
        root.leavePrivatePlace(privateOn = true)
        assertEquals(PrivateTabs.SPACE.uuid, root.currentSpaceUuid)
        assertEquals(0, root.currentSpace)
    }

    @Test fun `a normal space is left as it is`() {
        val root = state(current = "space-c", normal = "space-b")
        root.leavePrivatePlace(privateOn = false)
        assertEquals("space-c", root.currentSpaceUuid)
    }
}
