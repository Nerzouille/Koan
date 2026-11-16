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

    @Test fun `a pre-release user is offered the final release`() {
        assertEquals(
            UpdateStatus.Available("0.2.2", "https://github.com/Nerzouille/Koan/releases/tag/v0.2.2"),
            UpdateCheck.status("0.2.2-preview.1", list(release("v0.2.2"))),
        )
        assertEquals(
            UpdateStatus.Available("1.0.0", "https://github.com/Nerzouille/Koan/releases/tag/v1.0.0"),
            UpdateCheck.status("1.0.0-beta.1", list(release("v1.0.0"), release("v1.0.0-beta.2"), release("v1.0.0-beta.1"))),
        )
    }

    @Test fun `a beta user is offered the next beta, with its suffix`() {
        assertEquals(
            UpdateStatus.Available("1.0.0-beta.2", "https://github.com/Nerzouille/Koan/releases/tag/v1.0.0-beta.2"),
            UpdateCheck.status("1.0.0-beta.1", list(release("v1.0.0-beta.2"), release("v1.0.0-beta.1"), release("v0.4.0"))),
        )
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("1.0.0-beta.2", list(release("v1.0.0-beta.2"), release("v1.0.0-beta.1"))))
    }

    @Test fun `an 0_x user is offered the 1_0 beta`() {
        assertEquals(
            UpdateStatus.Available("1.0.0-beta.1", "https://github.com/Nerzouille/Koan/releases/tag/v1.0.0-beta.1"),
            UpdateCheck.status("0.4.0", list(release("v1.0.0-beta.1"), release("v0.4.0"))),
        )
    }

    @Test fun `a stable 1_x user is never offered a pre-release`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("1.0.0", list(release("v1.1.0-beta.1"), release("v1.0.0"))))
        assertEquals(
            UpdateStatus.Available("1.0.1", "https://github.com/Nerzouille/Koan/releases/tag/v1.0.1"),
            UpdateCheck.status("1.0.0", list(release("v1.1.0-rc.1"), release("v1.0.1"), release("v1.0.0"))),
        )
    }

    @Test fun `semver order of pre-releases`() {
        val order = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0-beta.1")
        assertEquals(order, order.shuffled(java.util.Random(4)).map { UpdateCheck.parse(it)!! }.sorted().map { it.toString() })
    }

    @Test fun `newer than every release is up to date`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.3.0", list(release("v0.2.0"))))
    }

    @Test fun `no releases is up to date`() {
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.status("0.2.0", "<feed></feed>"))
    }

    @Test fun `parse keeps the numbers and the suffix`() {
        assertEquals(UpdateCheck.Version(listOf(0, 2, 0)), UpdateCheck.parse("v0.2.0"))
        assertEquals(UpdateCheck.Version(listOf(1, 0, 0), listOf("beta", "1")), UpdateCheck.parse("v1.0.0-beta.1"))
        assertEquals(UpdateCheck.Version(listOf(1, 0), listOf("rc1")), UpdateCheck.parse("1.0-rc1"))
        assertEquals(UpdateCheck.Version(listOf(1, 0, 0)), UpdateCheck.parse("1.0.0+build.5"))
        assertNull(UpdateCheck.parse("m3"))
        assertNull(UpdateCheck.parse(""))
        assertNull(UpdateCheck.parse("1.0.0-"))
    }

    @Test fun `an unreadable own version fails quietly`() {
        assertEquals(UpdateStatus.Failed, UpdateCheck.status("dev", list(release("v0.2.0"))))
    }
}
