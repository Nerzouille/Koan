package dev.nerzouille.koan.data

import dev.nerzouille.koan.engine.downloads.DuplicateDownloads
import org.junit.Assert.assertEquals
import org.junit.Test

/** The duplicate-download card's names (fake file names). */
class DuplicateNameTest {
    @Test fun counterIsDropped() {
        assertEquals("photo.jpg", DuplicateDownloads.withoutCounter("photo(2).jpg"))
        assertEquals("photo.jpg", DuplicateDownloads.withoutCounter("photo (12).jpg"))
        assertEquals("notes", DuplicateDownloads.withoutCounter("notes(1)"))
        assertEquals("photo.jpg", DuplicateDownloads.withoutCounter("photo.jpg"))
        assertEquals("(1).jpg", DuplicateDownloads.withoutCounter("(1).jpg"))
    }

    @Test fun keepBothTakesTheFirstFreeNumber() {
        val taken = setOf("photo (1).jpg", "photo (2).jpg")
        assertEquals("photo (3).jpg", DuplicateDownloads.keepBothName("photo.jpg") { it in taken })
        assertEquals("archive (1)", DuplicateDownloads.keepBothName("archive") { false })
        assertEquals("app.v2 (1).apk", DuplicateDownloads.keepBothName("app.v2.apk") { false })
    }
}
