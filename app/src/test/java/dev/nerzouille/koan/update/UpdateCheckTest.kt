package dev.nerzouille.koan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckTest {
    private fun release(tag: String) =
        """<entry><link rel="alternate" type="text/html" href="https://github.com/Nerzouille/Koan/releases/tag/$tag"/><title>$tag</title></entry>"""

    private fun list(vararg r: String) = r.joinToString("\n", "<feed>", "</feed>")

    @Test fun `same version is up to date`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.0", list(release("v0.2.0"), release("v0.1.0"))))
    }

    @Test fun `newer release is offered with its page`() {
        assertEquals(
            UpdateStatus.Available("0.10.0", "https://github.com/Nerzouille/Koan/releases/tag/v0.10.0"),
            UpdateCheck.status("0.2.0", list(release("v0.2.0"), release("v0.10.0"), release("v0.9.1"))),
        )
    }

    @Test fun `milestone tags are ignored`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.0", list(release("m3"), release("v0.2.0"))))
    }

    @Test fun `a non-version pre-release tag is ignored`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.1", list(release("preview-android11"), release("v0.2.1"))))
    }

    @Test fun `a preview counts as its release version`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.2-preview.1", list(release("v0.2.2"))))
        assertEquals(
            UpdateStatus.Available("0.2.3", "https://github.com/Nerzouille/Koan/releases/tag/v0.2.3"),
            UpdateCheck.status("0.2.2-preview.1", list(release("v0.2.3"))),
        )
    }

    @Test fun `newer than every release is up to date`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.3.0", list(release("v0.2.0"))))
    }

    @Test fun `no releases is up to date`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.0", "<feed></feed>"))
    }

    @Test fun `parse keeps the numbers`() {
        assertEquals(listOf(0, 2, 0), UpdateCheck.parse("v0.2.0"))
        assertEquals(listOf(1, 0), UpdateCheck.parse("1.0-rc1"))
        assertNull(UpdateCheck.parse("m3"))
        assertNull(UpdateCheck.parse(""))
    }
}
