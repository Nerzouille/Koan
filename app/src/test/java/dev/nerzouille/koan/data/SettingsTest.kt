package dev.nerzouille.koan.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Settings on a real DataStore file. Koan has no DataStore migrations (no SharedPreferences
 * import, no schema key): what matters is that the defaults that protect the owner's data hold on
 * a new or reset file, and that what is written is read back after a restart.
 */
class SettingsTest {
    private val dir: File = Files.createTempDirectory("koan-settings").toFile()
    private val file = File(dir, "settings.preferences_pb")
    private val scopes = mutableListOf<CoroutineScope>()

    /** A fresh store on [file], as a new process would open it. */
    private fun open(): Settings {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return Settings(
            PreferenceDataStoreFactory.create(
                corruptionHandler = SafeStores.resetOnCorruption("settings"),
                scope = scope,
                produceFile = { file },
            ),
        )
    }

    /** Closes the previous store (only one may own a file at a time). */
    private fun restart(): Settings {
        scopes.forEach { it.cancel() }
        scopes.clear()
        Thread.sleep(50)
        return open()
    }

    @After
    fun cleanUp() {
        scopes.forEach { it.cancel() }
        dir.deleteRecursively()
    }

    @Test
    fun safetyDefaultsOnANewFile() = runBlocking {
        val s = open()
        assertFalse("never write Zen spaces by default (rule 4)", s.writeBackEnabled.first())
        assertEquals(SyncBackChoice(passwords = false, history = false), s.syncBack.first())
        assertFalse(s.syncBackPasswordEdits.first())
        assertTrue("Private locks by default", s.privateLock.first())
        assertFalse("no screenshots of private tabs by default", s.privateScreenshots.first())
        assertFalse(s.devFlagSecureOff.first())
        assertNull("no tour seen: the tour shows", s.tourSeen())
        assertEquals(ThemeMode.System, s.themeMode.first())
    }

    @Test
    fun theSameDefaultsAfterACorruptedFile() = runBlocking {
        file.writeBytes(byteArrayOf(0x0a, 0x7f, 0x13, 0x37))
        val s = open()
        assertFalse(s.writeBackEnabled.first())
        assertEquals(SyncBackChoice(), s.syncBack.first())
        assertTrue(s.privateLock.first())
    }

    @Test
    fun valuesSurviveARestart() = runBlocking {
        val s = open()
        s.setSyncBack(SyncBackChoice(passwords = true, history = false))
        s.setSyncBackPasswordEdits(true)
        s.setPrivateLock(false)
        s.setTourSeen("0.4")
        s.setThemeMode(ThemeMode.Dark)

        val again = restart()
        assertEquals(SyncBackChoice(passwords = true, history = false), again.syncBack.first())
        assertTrue(again.syncBackPasswordEdits.first())
        assertFalse(again.privateLock.first())
        assertEquals("0.4", again.tourSeen())
        assertEquals(ThemeMode.Dark, again.themeMode.first())
        assertFalse(again.writeBackEnabled.first())
    }
}
