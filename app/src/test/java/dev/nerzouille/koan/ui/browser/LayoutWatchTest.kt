package dev.nerzouille.koan.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutWatchTest {
    private val portrait = Px(1080, 2460)

    @Test fun `every layer at the display's shape is fine`() {
        assertEquals(emptyList<String>(), staleLayers(portrait, mapOf("window" to Px(1080, 2460), "compose" to Px(1080, 2460), "gecko" to Px(1080, 2250))))
    }

    /** The owner's case: back in portrait, Koan still laid out at landscape size. */
    @Test fun `a layer left at landscape size on a portrait display is stale`() {
        val layers = mapOf("window" to Px(1080, 2460), "decor" to Px(2460, 1080), "compose" to Px(2460, 1080), "gecko" to Px(1080, 2250))
        assertEquals(listOf("decor", "compose"), staleLayers(portrait, layers))
    }

    @Test fun `the player kept at portrait size on a landscape display is stale`() {
        assertEquals(listOf("gecko"), staleLayers(Px(2460, 1080), mapOf("compose" to Px(2460, 1080), "gecko" to Px(1080, 2250))))
    }

    @Test fun `sizes not known yet never count`() {
        assertEquals(emptyList<String>(), staleLayers(portrait, mapOf("gecko" to Px(0, 0))))
        assertEquals(emptyList<String>(), staleLayers(Px(0, 0), mapOf("compose" to Px(2460, 1080))))
    }

    @Test fun `repairs go relayout, then reapply, then a new activity only after leaving`() {
        assertEquals(LayoutFix.None, layoutFix(stale = false, attempt = 3, leaving = true))
        assertEquals(LayoutFix.Relayout, layoutFix(stale = true, attempt = 0, leaving = true))
        assertEquals(LayoutFix.Reapply, layoutFix(stale = true, attempt = 1, leaving = true))
        assertEquals(LayoutFix.Recreate, layoutFix(stale = true, attempt = 2, leaving = true))
        // Entering: a new activity would drop the video.
        assertEquals(LayoutFix.Reapply, layoutFix(stale = true, attempt = 2, leaving = false))
    }
}
