package dev.nerzouille.koan.privacy

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.nerzouille.koan.data.SafeStores
import dev.nerzouille.koan.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Private's lock (OVERVIEW_PRIVATE.md §4.1): 1 min away or the screen off. The activity calls
 * onHidden from onStop and onShown from onStart; a PiP window on screen keeps Koan started (no
 * onStop), so it never starts the timer. These tests replay those lifecycle sequences on a fake clock.
 */
class PrivateModeLockTest {
    private val dir: File = Files.createTempDirectory("koan-private").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settings = Settings(
        PreferenceDataStoreFactory.create(SafeStores.resetOnCorruption("settings"), scope = scope) { File(dir, "settings.preferences_pb") },
    )
    private var clock = 1_000_000L
    private val tabs = MutableStateFlow(1)
    private var marks = 0
    private val mode = PrivateMode(settings, tabs, { marks }, now = { clock })

    @After
    fun cleanUp() {
        scope.cancel()
        dir.deleteRecursively()
    }

    /** Unlocked with the fingerprint, as after the prompt. */
    private fun unlocked() = mode.also { it.locked.value = false }

    private fun away(ms: Long) {
        mode.onHidden()
        clock += ms
        mode.onShown()
    }

    @Test
    fun aColdStartIsLocked() = runBlocking {
        assertTrue(mode.needsUnlock())
    }

    @Test
    fun underAMinuteAwayStaysUnlocked() = runBlocking {
        unlocked()
        away(59_999)
        assertFalse(mode.needsUnlock())
    }

    @Test
    fun aMinuteAwayLocks() = runBlocking {
        unlocked()
        away(60_000)
        assertTrue(mode.needsUnlock())
    }

    @Test
    fun shortTripsDoNotAddUp() = runBlocking {
        unlocked()
        repeat(5) { away(30_000); clock += 1_000 }
        assertFalse(mode.needsUnlock())
    }

    @Test
    fun theScreenOffLocksAtOnce() = runBlocking {
        unlocked()
        mode.onScreenOff()
        assertTrue(mode.needsUnlock())
    }

    /** PiP on screen: Koan is paused, never stopped, so no timer, however long the video plays. */
    @Test
    fun aPipWindowOnScreenNeverStartsTheTimer() = runBlocking {
        unlocked()
        clock += 30 * 60_000L
        mode.onShown() // back to full screen from the PiP window: onStart with no onStop before it
        assertFalse(mode.needsUnlock())
    }

    /** The PiP window closed (or Audio only): onStop runs, and the minute counts from then. */
    @Test
    fun closingThePipWindowStartsTheMinute() = runBlocking {
        unlocked()
        clock += 10 * 60_000L // watched in PiP
        away(61_000)
        assertTrue(mode.needsUnlock())
    }

    @Test
    fun theScreenOffDuringPipLocks() = runBlocking {
        unlocked()
        mode.onScreenOff()
        mode.onShown()
        assertTrue(mode.needsUnlock())
    }

    @Test
    fun theLockSwitchOffNeverAsks() = runBlocking {
        settings.setPrivateLock(false)
        away(10 * 60_000L)
        mode.onScreenOff()
        assertFalse(mode.needsUnlock())
    }

    @Test
    fun nothingPrivateLeftNeedsNoFingerprint() = runBlocking {
        tabs.value = 0
        marks = 0
        mode.onScreenOff()
        assertFalse(mode.needsUnlock())
    }

    @Test
    fun privateBookmarksAloneAreLockedToo() = runBlocking {
        tabs.value = 0
        marks = 2
        mode.onScreenOff()
        assertTrue(mode.needsUnlock())
    }
}
