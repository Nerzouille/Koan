package dev.nerzouille.koan.privacy

import dev.nerzouille.koan.data.AesGcmBox
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivateBookmarksTest {
    private fun box() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().let { k -> AesGcmBox { k } }
    private val box = box()
    private val file = File(Files.createTempDirectory("marks").toFile(), "private/bookmarks.sealed")

    private fun TestScope.marks(sealed: SealedFile = SealedFile(file, box)) =
        PrivateBookmarks(sealed, this, StandardTestDispatcher(testScheduler), now = { 7L })

    @Test fun roundTrip() {
        val list = listOf(PrivateBookmark("https://example.com/a", "A made-up page", 2L), PrivateBookmark("https://example.org/", "", 1L))
        assertEquals(list, PrivateBookmarks.decode(PrivateBookmarks.encode(list)))
    }

    @Test fun `a toggle is sealed on disk and comes back after a restart`() = runTest {
        val marks = marks()
        marks.toggle("https://example.com/secret", "A made-up title")
        advanceUntilIdle()
        val raw = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(raw.contains("example.com"))
        assertFalse(raw.contains("made-up"))
        val again = marks()
        assertEquals(1, again.count())
        assertEquals(PrivateBookmark("https://example.com/secret", "A made-up title", 7L), again.list.value.single())
    }

    @Test fun `toggling twice removes it and the file`() = runTest {
        val marks = marks()
        marks.toggle("https://example.com/", "")
        advanceUntilIdle()
        assertEquals("https://example.com/", marks.list.value.single().title)
        marks.toggle("https://example.com/", "")
        advanceUntilIdle()
        assertTrue(marks.list.value.isEmpty())
        assertFalse(file.exists())
    }

    @Test fun `clear empties the set and deletes the file`() = runTest {
        val marks = marks()
        marks.toggle("https://example.com/a", "a")
        marks.toggle("https://example.com/b", "b")
        advanceUntilIdle()
        assertEquals(2, marks.count())
        marks.clear()
        advanceUntilIdle()
        assertEquals(0, marks.count())
        assertFalse(file.exists())
    }

    @Test fun `a file sealed with another key reads as empty`() = runTest {
        val marks = marks()
        marks.toggle("https://example.com/a", "a")
        advanceUntilIdle()
        val other = SealedFile(file, box())
        assertNull(other.read())
        assertEquals(0, marks(other).count())
    }

    @Test fun `search matches url or title, any case, newest first`() {
        val list = listOf(
            PrivateBookmark("https://example.com/recipes", "Soup", 3L),
            PrivateBookmark("https://example.org/", "Example RECIPES", 2L),
            PrivateBookmark("https://example.net/", "Other", 1L),
        )
        assertEquals(list.take(2), PrivateBookmarks.search(list, "recipes", 10))
        assertEquals(list.take(1), PrivateBookmarks.search(list, "", 1))
    }
}
