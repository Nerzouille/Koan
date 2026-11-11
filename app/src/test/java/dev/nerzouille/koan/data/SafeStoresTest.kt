package dev.nerzouille.koan.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** A corrupted settings file gives the defaults instead of crashing Koan at start (audit 8.2). */
class SafeStoresTest {
    @Test fun corruptedFileIsReplacedByDefaults() = runBlocking {
        val dir = Files.createTempDirectory("koan-store").toFile()
        val file = File(dir, "settings.preferences_pb").apply { writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(
                corruptionHandler = SafeStores.resetOnCorruption("settings"),
                scope = scope,
                produceFile = { file },
            )
            val prefs = store.safeData.first()
            assertNull(prefs[booleanPreferencesKey("any")])
            assertTrue(prefs.asMap().isEmpty())
        } finally {
            scope.cancel()
            dir.deleteRecursively()
        }
    }
}
