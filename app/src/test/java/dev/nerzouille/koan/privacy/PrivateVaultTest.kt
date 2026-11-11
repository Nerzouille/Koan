package dev.nerzouille.koan.privacy

import dev.nerzouille.koan.data.AesGcmBox
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PrivateVaultTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val box = AesGcmBox { key }
    private val saved = SavedPrivateTabs(
        tabs = listOf(
            SavedPrivateTab("a", "https://example.com/secret-page", "A made-up page", null, 1, 42L, """{"history":[]}"""),
            SavedPrivateTab("b", "https://example.org/", "", "https://example.org/icon.png", 2, 43L, null),
        ),
        current = "b",
    )

    @Test fun roundTrip() {
        assertEquals(saved, PrivateVault.decode(PrivateVault.encode(saved)))
    }

    @Test fun fileIsSealedNeverPlainText() {
        val dir = Files.createTempDirectory("vault").toFile()
        val file = File(dir, "private/tabs.sealed")
        val vault = PrivateVault(file, box)
        vault.write(saved)
        val raw = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(raw.contains("example.com"))
        assertFalse(raw.contains("made-up"))
        assertEquals(saved, vault.read())
        vault.delete()
        assertFalse(file.exists())
        assertNull(vault.read())
    }

    @Test fun anotherKeyReadsNothing() {
        val dir = Files.createTempDirectory("vault").toFile()
        val file = File(dir, "tabs.sealed")
        PrivateVault(file, box).write(saved)
        val other = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertNull(PrivateVault(file, AesGcmBox { other }).read())
    }
}
