package dev.nerzouille.koan.ui.menu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** File upload never hands a page Koan's own files (audit 6.4). */
class UploadUrisTest {
    private val koan = "dev.nerzouille.koan"

    @Test fun otherAppsContentIsAllowed() {
        assertTrue(UploadUris.allowed("content", "com.android.providers.media.documents", koan))
        assertTrue(UploadUris.allowed("content", "media", koan))
    }

    @Test fun koansOwnProvidersAreRefused() {
        assertFalse(UploadUris.allowed("content", "dev.nerzouille.koan.files", koan))
        assertFalse(UploadUris.allowed("content", "dev.nerzouille.koan", koan))
        assertFalse(UploadUris.allowed("CONTENT", "Dev.Nerzouille.Koan.Files", koan))
        // A staging or debug install refuses its own the same way.
        assertFalse(UploadUris.allowed("content", "dev.nerzouille.koan.staging.files", "dev.nerzouille.koan.staging"))
    }

    @Test fun filePathsAndOddSchemesAreRefused() {
        assertFalse(UploadUris.allowed("file", null, koan))
        assertFalse(UploadUris.allowed("file", "", koan))
        assertFalse(UploadUris.allowed(null, null, koan))
        assertFalse(UploadUris.allowed("content", null, koan))
    }

    @Test fun aLookalikeAuthorityIsNotKoans() {
        assertTrue(UploadUris.allowed("content", "dev.nerzouille.koanx.files", koan))
    }
}
