package dev.nerzouille.koan.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerModelTest {
    @Test
    fun videoFiles() {
        assertEquals("https://example.com/clip.mp4", downloadableVideo("https://example.com/clip.mp4"))
        assertEquals("https://example.com/a/clip.webm?x=1", downloadableVideo("https://example.com/a/clip.webm?x=1"))
        assertNull(downloadableVideo("blob:https://example.com/123"))
        assertNull(downloadableVideo("https://example.com/watch?v=1"))
        // A wiki's page about a file is a page.
        assertNull(downloadableVideo("https://wiki.example.com/wiki/File:Clip.webm"))
        assertNull(downloadableVideo(null))
    }

    @Test
    fun positionMovesOnlyWhilePlaying() {
        val playing = PlayerState(paused = false, time = 10.0, at = 1_000, rate = 2.0, duration = 60.0)
        assertEquals(12.0, playing.positionAt(2_000), 1e-9)
        assertEquals(60.0, playing.positionAt(100_000), 1e-9)
        val paused = playing.copy(paused = true)
        assertEquals(10.0, paused.positionAt(5_000), 1e-9)
        val ended = playing.copy(ended = true)
        assertEquals(60.0, ended.positionAt(5_000), 1e-9)
    }

    @Test
    fun clockAndNames() {
        assertEquals("5:58", clock(358.4))
        assertEquals("1:02:07", clock(3727.0))
        assertEquals("0:00", clock(-3.0))
        assertEquals("short.mp4", middleEllipsis("short.mp4", 36))
        val long = middleEllipsis("a-very-long-video-file-name-from-the-camera-2026.mp4", 24)
        assertEquals(24, long.length)
        assert(long.endsWith(".mp4"))
        assertEquals("Normal", speedLabel(1.0))
        assertEquals("1.5×", speedLabel(1.5))
        assertEquals("2", speedNumber(2.0))
    }
}
