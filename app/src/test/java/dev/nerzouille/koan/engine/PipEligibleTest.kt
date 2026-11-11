package dev.nerzouille.koan.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipEligibleTest {
    private fun video(paused: Boolean = false, w: Int = 1280, h: Int = 720, fullscreen: Boolean = true) = WebVideo(
        tabId = "t", fullscreen = fullscreen, paused = paused, ended = false, waiting = false, time = 0.0, at = 0L,
        duration = 60.0, buffered = 0.0, rate = 1.0, muted = false, width = w, height = h, title = "", src = "",
    )

    @Test fun `playing fullscreen from Gecko's media session`() {
        assertTrue(webPipEligible(fullScreen = true, mediaSized = true, mediaPlaying = true, page = null))
    }

    @Test fun `the player's own view counts when the media session has no size or state`() {
        assertTrue(webPipEligible(fullScreen = true, mediaSized = false, mediaPlaying = false, page = video()))
    }

    @Test fun `never a paused video`() {
        assertFalse(webPipEligible(fullScreen = true, mediaSized = true, mediaPlaying = false, page = video(paused = true)))
    }

    @Test fun `never outside fullscreen or with no size at all`() {
        assertFalse(webPipEligible(fullScreen = false, mediaSized = true, mediaPlaying = true, page = video()))
        assertFalse(webPipEligible(fullScreen = true, mediaSized = false, mediaPlaying = true, page = video(w = 0, h = 0)))
    }
}
