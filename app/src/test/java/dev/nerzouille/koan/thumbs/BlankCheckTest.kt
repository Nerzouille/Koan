package dev.nerzouille.koan.thumbs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BlankCheckTest {
    // A thumbnail-sized page (720 wide), as ARGB pixels.
    private val w = 720
    private val h = 1600

    private fun page(fill: Int, paint: (IntArray) -> Unit = {}): IntArray = IntArray(w * h) { fill }.also(paint)

    private fun blank(px: IntArray) = BlankCheck.looksBlank(w, h) { y, row -> System.arraycopy(px, y * w, row, 0, w) }

    /** Dark strokes, like lines of text: [lines] lines from [top], 2 px strokes every 9 px. */
    private fun text(px: IntArray, top: Int, lines: Int) {
        for (l in 0 until lines) for (y in top + l * 48 until top + l * 48 + 24) for (x in 40 until 680) {
            if (x % 9 < 2) px[y * w + x] = 0xFF202122.toInt()
        }
    }

    @Test fun `flat white and flat dark are blank`() {
        assertTrue(blank(page(0xFFFFFFFF.toInt())))
        assertTrue(blank(page(0xFF1C1B1F.toInt())))
    }

    @Test fun `compression noise stays blank`() {
        val rnd = Random(1)
        assertTrue(blank(page(0xFFF8F9FA.toInt()) { px ->
            for (i in px.indices) { val d = rnd.nextInt(-8, 9); val c = 0xF8 + d; px[i] = (0xFF shl 24) or (c.coerceIn(0, 255) * 0x010101) }
        }))
    }

    @Test fun `a few lines of text are content`() {
        assertFalse(blank(page(0xFFFFFFFF.toInt()) { text(it, top = 300, lines = 3) }))
    }

    @Test fun `a header bar on a white page is content`() {
        assertFalse(blank(page(0xFFFFFFFF.toInt()) { px -> for (i in 0 until w * 120) px[i] = 0xFFFF6600.toInt() }))
    }

    @Test fun `a lone speck is still blank`() {
        assertTrue(blank(page(0xFFFFFFFF.toInt()) { px -> for (y in 800 until 804) for (x in 300 until 304) px[y * w + x] = 0xFF000000.toInt() }))
    }
}
