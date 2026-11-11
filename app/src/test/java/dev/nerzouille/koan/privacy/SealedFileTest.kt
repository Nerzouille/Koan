package dev.nerzouille.koan.privacy

import dev.nerzouille.koan.data.AesGcmBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator

/** Private's sealed files never crash Koan and never leave plain text behind. */
class SealedFileTest {
    private val box = AesGcmBox(KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().let { k -> { k } })
    private val dir: File = Files.createTempDirectory("sealed").toFile()
    private val file = File(dir, "private/tabs.sealed")
    private val sealed = SealedFile(file, box)

    @Test
    fun aCorruptedFileReadsAsMissingAndTheNextSaveWorks() {
        sealed.write("https://example.com/made-up")
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 0x01).toByte()
        file.writeBytes(bytes)
        assertNull(sealed.read())

        sealed.write("https://example.org/again")
        assertEquals("https://example.org/again", sealed.read())
    }

    @Test
    fun aTruncatedOrEmptyFileReadsAsMissing() {
        sealed.write("https://example.com/made-up")
        file.writeBytes(file.readBytes().copyOf(5))
        assertNull(sealed.read())
        file.writeBytes(ByteArray(0))
        assertNull(sealed.read())
    }

    /** A kill mid-write leaves the .tmp: the previous version still reads, and the next write replaces both. */
    @Test
    fun aLeftoverTmpFromAKillKeepsThePreviousVersion() {
        sealed.write("first")
        File(file.path + ".tmp").writeBytes(byteArrayOf(1, 2, 3))
        assertEquals("first", sealed.read())
        sealed.write("second")
        assertEquals("second", sealed.read())
        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test
    fun deleteRemovesTheTmpToo() {
        sealed.write("first")
        File(file.path + ".tmp").writeBytes(byteArrayOf(1))
        sealed.delete()
        assertFalse(file.exists())
        assertFalse(File(file.path + ".tmp").exists())
        assertNull(sealed.read())
    }

    @Test
    fun nothingReadableOnDisk() {
        sealed.write("""{"url":"https://example.com/private-page","title":"A made-up title"}""")
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            val raw = String(f.readBytes(), Charsets.ISO_8859_1)
            assertFalse(raw.contains("example.com"))
            assertFalse(raw.contains("made-up"))
        }
    }
}
