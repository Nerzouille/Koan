package dev.nerzouille.koan.ui.player

import dev.nerzouille.koan.engine.WebFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFilesTest {
    private val base = "https://media.example.com/transcoded/a/ab/Clip.webm"
    private val p240 = WebFile("$base/Clip.webm.240p.vp9.webm", 240, "video/webm; codecs=\"vp9\"", false)
    private val p1080 = WebFile("$base/Clip.webm.1080p.vp9.webm", 1080, "video/webm", false)
    private val p360mp4 = WebFile("$base/Clip.webm.360p.mp4", 360, "video/mp4", false)
    private val original = WebFile("https://media.example.com/a/ab/Clip.webm", 0, "", true)
    private val files = listOf(p240, p360mp4, p1080, original)

    @Test fun `quality lists the playing file's family, original then largest first`() {
        var picked: WebFile? = null
        val q = qualityChoices(files, p240.src, kind = "") { picked = it }
        assertEquals(listOf("Original", "1080p", "240p"), q.map { it.label })
        assertEquals(listOf(false, false, true), q.map { it.selected })
        q[1].onPick()
        assertEquals(p1080, picked)
    }

    @Test fun `no quality row for a stream, an unknown file or a single one`() {
        assertTrue(qualityChoices(files, p240.src, kind = "stream") {}.isEmpty())
        assertTrue(qualityChoices(files, "https://other.example.com/x.webm", kind = "") {}.isEmpty())
        assertTrue(qualityChoices(files, p360mp4.src, kind = "") {}.isEmpty())
        assertTrue(qualityChoices(files, "", kind = "") {}.isEmpty())
    }

    @Test fun `download lists every file with its type`() {
        val d = downloadChoices(files) {}
        assertEquals(listOf("Original · WebM", "1080p · WebM", "360p · MP4", "240p · WebM"), d.map { it.label })
        assertTrue(downloadChoices(listOf(p240)) {}.isEmpty())
    }

    @Test fun `a file without a height is named by its file name`() {
        assertEquals("My clip.mp4", fileLabel(WebFile("https://example.com/v/My%20clip.mp4?x=1", 0, "", false)))
        assertEquals("mp4", fileFamily(WebFile("https://example.com/a.m4v", 0, "", false)))
        assertEquals("mp4", fileFamily(WebFile("https://example.com/a", 0, "video/quicktime", false)))
    }
}
