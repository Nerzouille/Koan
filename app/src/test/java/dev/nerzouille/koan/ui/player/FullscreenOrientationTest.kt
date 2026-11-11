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

    @Test fun `auto-rotate off, the video shape as a user orientation (the system's rotate button stays)`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, fullscreenOrientation(true, videoPortrait = false, rotationLocked = true, turned = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, fullscreenOrientation(true, videoPortrait = false, rotationLocked = true, turned = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, fullscreenOrientation(true, videoPortrait = true, rotationLocked = true, turned = false))
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
}
