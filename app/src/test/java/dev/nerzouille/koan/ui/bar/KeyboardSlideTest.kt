package dev.nerzouille.koan.ui.bar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The search morph and the dock follow the keyboard's slide; the shape only ever grows or shrinks. */
class KeyboardSlideTest {
    private val full = 900
    private val nav = 48

    @Test fun `the fraction is how far up the keyboard is`() {
        assertEquals(0f, keyboardFraction(0, full))
        assertEquals(0.5f, keyboardFraction(450, full))
        assertEquals(1f, keyboardFraction(full, full))
        assertEquals(0f, keyboardFraction(300, 0))
    }

    @Test fun `the dock leaves on the first frame and lands with the keyboard`() {
        assertTrue(keyboardRise(9, full, nav) > 0f)
        assertEquals((full - nav).toFloat(), keyboardRise(full, full, nav))
        assertEquals(0f, keyboardRise(0, full, nav))
    }

    @Test fun `the dock never goes below the keyboard's top`() {
        for (now in 0..full step 10) {
            // Resting above the navigation bar, it sits at nav + rise; the keyboard's top is at now.
            assertTrue(nav + keyboardRise(now, full, nav) >= now)
        }
    }

    @Test fun `the shape grows steadily from the pill to the field`() {
        val pill = Rect(120f, 0f, 700f, 100f)
        val size = Size(900f, 100f)
        assertEquals(pill, morphRect(pill, size, 0f))
        assertEquals(Rect(0f, 0f, 900f, 100f), morphRect(pill, size, 1f))
        val widths = (0..20).map { morphRect(pill, size, it / 20f).width }
        widths.zipWithNext().forEach { (a, b) -> assertTrue("shrank on the way: $a → $b", b >= a) }
        val lefts = (0..20).map { morphRect(pill, size, it / 20f).left }
        lefts.zipWithNext().forEach { (a, b) -> assertTrue("went back: $a → $b", b <= a) }
    }

    @Test fun `without the pill, the field's own bounds`() {
        assertEquals(Rect(0f, 0f, 900f, 100f), morphRect(null, Size(900f, 100f), 0.3f))
    }
}
