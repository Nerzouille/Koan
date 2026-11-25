package dev.nerzouille.koan.ui.player

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenOrientationTest {
    @Test fun `out of fullscreen, portrait`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, fullscreenOrientation(false, videoPortrait = false, rotationLocked = false, turned = true))
    }

    @Test fun `opens on the video's shape`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, fullscreenOrientation(true, videoPortrait = false, rotationLocked = false, turned = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, fullscreenOrientation(true, videoPortrait = true, rotationLocked = false, turned = false))
    }

    @Test fun `auto-rotate on, once turned, follows the phone`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, fullscreenOrientation(true, videoPortrait = false, rotationLocked = false, turned = true))
    }

    @Test fun `auto-rotate off, the video shape with both sides by the sensor`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, fullscreenOrientation(true, videoPortrait = false, rotationLocked = true, turned = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, fullscreenOrientation(true, videoPortrait = false, rotationLocked = true, turned = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, fullscreenOrientation(true, videoPortrait = true, rotationLocked = true, turned = false))
    }

    @Test fun `the rotate pill shows when the phone turns to the other shape`() {
        // Landscape screen: the phone turns towards portrait.
        assertEquals(true, rotateOffer(previous = false, held = true, screenPortrait = false))
        // Still held so: no change (the 3 s run out on their own).
        assertEquals(null, rotateOffer(previous = true, held = true, screenPortrait = false))
        // Back to the screen's shape: away.
        assertEquals(false, rotateOffer(previous = true, held = false, screenPortrait = false))
        // In between: no change.
        assertEquals(null, rotateOffer(previous = false, held = null, screenPortrait = false))
        // Portrait screen: towards landscape.
        assertEquals(true, rotateOffer(previous = true, held = false, screenPortrait = true))
    }

    @Test fun `how the phone is held`() {
        assertEquals(true, heldPortrait(0))
        assertEquals(true, heldPortrait(350))
        assertEquals(true, heldPortrait(180))
        assertEquals(false, heldPortrait(90))
        assertEquals(false, heldPortrait(270))
        assertEquals(null, heldPortrait(45))
        assertEquals(null, heldPortrait(-1))
    }

    @Test fun `standalone, auto-rotate on, follows the phone until Rotate turns it`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, standaloneOrientation(rotationLocked = false, videoPortrait = null, rotated = false, turned = false))
        // A landscape video turned: portrait, until the phone is held in portrait; then the sensor.
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, standaloneOrientation(rotationLocked = false, videoPortrait = false, rotated = true, turned = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, standaloneOrientation(rotationLocked = false, videoPortrait = false, rotated = true, turned = true))
        assertEquals(null, standaloneOrientation(rotationLocked = false, videoPortrait = null, rotated = true, turned = false))
    }

    @Test fun `standalone, auto-rotate off, the video's shape or the other one`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, standaloneOrientation(rotationLocked = true, videoPortrait = false, rotated = false, turned = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, standaloneOrientation(rotationLocked = true, videoPortrait = false, rotated = true, turned = true))
        assertEquals(null, standaloneOrientation(rotationLocked = true, videoPortrait = null, rotated = false, turned = false))
    }

    @Test fun `the Rotate row shows only with auto-rotate off, like the pill`() {
        var n = 0
        assertEquals(null, rotateRow(rotationLocked = false) { n++ })
        val row = rotateRow(rotationLocked = true) { n++ }!!
        assertEquals("Rotate", row.label)
        row.onClick()
        assertEquals(1, n)
    }
}
