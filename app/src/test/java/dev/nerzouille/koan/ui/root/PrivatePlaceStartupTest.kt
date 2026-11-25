package dev.nerzouille.koan.ui.root

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import dev.nerzouille.koan.privacy.PrivateTabs
import dev.nerzouille.koan.ui.overview.SpacePager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Killed while Private showed, on a space other than the first: the new process opens normal mode
 * on that space. Runs the root's startup in the order of `KoanRootContent` (state, space pager,
 * place restore, pager → space id, anchoring by id) with the spaces already loaded, through a real
 * save and restore of the instance state.
 */
@RunWith(RobolectricTestRunner::class)
class PrivatePlaceStartupTest {

    @Test fun `killed in Private on the second space reopens on the second space`() {
        // Before the kill: Private on, entered from space "b" (index 1).
        Host.privateOn = true
        val saved = Bundle()
        Robolectric.buildActivity(Host::class.java).setup().use { before ->
            idle()
            val root = Host.root!!
            root.placeRestored = true
            root.currentSpace = 1
            root.currentSpaceUuid = "b"
            Host.pager!!.snapTo(1)
            idle()
            root.enterPrivatePlace()
            // Private's own pager has one page: index 0, Private's id.
            root.currentSpace = 0
            root.currentSpaceUuid = PrivateTabs.SPACE.uuid
            idle()
            before.pause().stop().saveInstanceState(saved)
        }

        // The new process: normal mode, the spaces already loaded.
        Host.privateOn = false
        Robolectric.buildActivity(Host::class.java).setup(saved).use {
            idle()
            assertEquals("b", Host.root!!.currentSpaceUuid)
            assertEquals(1, Host.pager!!.settled)
            assertEquals(1, Host.root!!.currentSpace)
        }
    }

    private fun idle() = repeat(3) { shadowOf(Looper.getMainLooper()).idle() }

    class Host : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { Startup(privateOn) }
        }

        companion object {
            var privateOn = false
            var root: RootState? = null
            var pager: SpacePager? = null
        }
    }
}

private val SPACES = listOf("a", "b", "c")

/** `KoanRootContent`'s startup, in its order, with the spaces already loaded. */
@Composable
private fun Startup(privateOn: Boolean) {
    val root = rememberRootState(privateOn)
    PrivatePlaceStartupTest.Host.root = root
    val spaceIds = if (privateOn) listOf(PrivateTabs.SPACE.uuid) else SPACES
    val scope = rememberCoroutineScope()
    val pager = remember { SpacePager(root.currentSpace, scope) }
    PrivatePlaceStartupTest.Host.pager = pager
    LaunchedEffect(Unit) {
        if (!root.placeRestored) root.placeRestored = true
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settled }.collect {
            root.currentSpace = it
            spaceIds.getOrNull(it)?.let { s -> root.currentSpaceUuid = s }
        }
    }
    LaunchedEffect(spaceIds, root.placeRestored) {
        if (!root.placeRestored) return@LaunchedEffect
        val anchored = root.currentSpaceUuid?.let(spaceIds::indexOf) ?: -1
        val target = if (anchored >= 0) anchored else root.currentSpace.coerceIn(0, spaceIds.size - 1)
        if (target != pager.settled) pager.snapTo(target)
        root.currentSpace = target
        spaceIds.getOrNull(target)?.let { root.currentSpaceUuid = it }
    }
}
