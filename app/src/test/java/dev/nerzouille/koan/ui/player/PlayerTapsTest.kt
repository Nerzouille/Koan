package dev.nerzouille.koan.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTapsTest {
    private val long = 10_000L

    @Test fun `hidden controls take no touches, visible ones do (fading out too)`() {
        assertFalse(controlsTakeTouches(visible = false, hidden = false))
        assertFalse(controlsTakeTouches(visible = true, hidden = true))
        assertTrue(controlsTakeTouches(visible = true, hidden = false))
    }

    @Test fun `a single tap in the middle toggles at once`() {
        assertEquals(TapAction.ToggleNow, tapAction(false, zone = 0, seekSide = 0, sinceSeekMs = long, lastTapZone = 0, sinceTapMs = long))
        // Twice quickly in the middle: two toggles, never a seek.
        assertEquals(TapAction.ToggleNow, tapAction(false, zone = 0, seekSide = 0, sinceSeekMs = long, lastTapZone = 0, sinceTapMs = 100))
    }

    @Test fun `a single tap on a side toggles a moment later`() {
        assertEquals(TapAction.ToggleLater, tapAction(false, zone = -1, seekSide = 0, sinceSeekMs = long, lastTapZone = 0, sinceTapMs = long))
        assertEquals(TapAction.ToggleLater, tapAction(false, zone = 1, seekSide = 0, sinceSeekMs = long, lastTapZone = -1, sinceTapMs = 100))
    }

    @Test fun `a double tap on a side seeks, then each more tap adds`() {
        assertEquals(TapAction.SeekPair, tapAction(false, zone = 1, seekSide = 0, sinceSeekMs = long, lastTapZone = 1, sinceTapMs = 150))
        assertEquals(TapAction.SeekMore, tapAction(false, zone = 1, seekSide = 1, sinceSeekMs = 300, lastTapZone = 1, sinceTapMs = long))
        // Too late for a pair: a toggle again.
        assertEquals(TapAction.ToggleLater, tapAction(false, zone = 1, seekSide = 0, sinceSeekMs = long, lastTapZone = 1, sinceTapMs = PlayerTokens.TAP_PAIR_MS))
    }

    @Test fun `locked, any tap shows Unlock`() {
        assertEquals(TapAction.ShowUnlock, tapAction(true, zone = 1, seekSide = 1, sinceSeekMs = 10, lastTapZone = 1, sinceTapMs = 10))
    }

    @Test fun `quick play pause taps each turn it the other way`() {
        // No recent tap: the opposite of what the page says.
        assertEquals(false, nextPlay(reportedPlaying = true, intent = null, sinceIntentMs = 0))
        assertEquals(true, nextPlay(reportedPlaying = false, intent = false, sinceIntentMs = 5_000))
        // A tap just paused it, the page still says playing: the next tap plays.
        assertEquals(true, nextPlay(reportedPlaying = true, intent = false, sinceIntentMs = 150))
        // Then pause again.
        assertEquals(false, nextPlay(reportedPlaying = false, intent = true, sinceIntentMs = 150))
    }
}
