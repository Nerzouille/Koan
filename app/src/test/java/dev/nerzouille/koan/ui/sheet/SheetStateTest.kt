package dev.nerzouille.koan.ui.sheet

import androidx.compose.animation.core.exponentialDecay
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a sheet lands on release. Settle animations are launched on a test scope that never runs
 * them, so [SheetState.height] only moves with the drags.
 */
class SheetStateTest {
    private val fling = 1000f

    private fun sheet(content: Int, compactCap: Float = 500f, fullCap: Float = 1500f) =
        SheetState(TestScope(), exponentialDecay()).apply {
            compactCapPx = compactCap
            fullCapPx = fullCap
            minStepPx = 48f
            refit(intArrayOf(content), 0)
        }

    private fun SheetState.drag(dy: Float, velocity: Float = 0f): Boolean {
        grab()
        dragBy(dy)
        return release(velocity, fling)
    }

    @Test
    fun `a drag past a quarter of the gap opens to compact`() {
        val s = sheet(800)
        assertTrue(s.drag(-200f))
        assertEquals(Detent.Compact, s.detent)
    }

    @Test
    fun `a short drag falls back closed`() {
        val s = sheet(800)
        assertFalse(s.drag(-100f))
        assertEquals(Detent.Closed, s.detent)
    }

    @Test
    fun `leaving a detent commits early, coming back commits late`() {
        val s = sheet(2000)
        s.drag(-500f)
        assertEquals(Detent.Compact, s.detent)
        // Down 100 of 500: 80% of the way, still above the 75% needed to stay.
        assertFalse(s.drag(100f))
        assertEquals(Detent.Compact, s.detent)
        // Down 250 from the compact height: 50%, closes.
        val t = sheet(2000)
        t.drag(-500f)
        assertTrue(t.drag(250f))
        assertEquals(Detent.Closed, t.detent)
    }

    @Test
    fun `a fling up goes to full only when full is a real step`() {
        val tall = sheet(2000)
        tall.drag(-500f)
        assertTrue(tall.drag(-10f, velocity = -2000f))
        assertEquals(Detent.Full, tall.detent)

        val short = sheet(520)
        short.drag(-500f)
        assertFalse(short.drag(-10f, velocity = -2000f))
        assertEquals(Detent.Compact, short.detent)
    }

    @Test
    fun `in full the list scrolls after the sheet and scrolls back before it shrinks`() {
        val s = sheet(2000)
        s.grab()
        s.dragBy(-1700f)
        assertEquals(1500f, s.height, 0.01f)
        assertEquals(200f, s.scroll, 0.01f)
        s.dragBy(300f)
        assertEquals(0f, s.scroll, 0.01f)
        assertEquals(1400f, s.height, 0.01f)
    }

    @Test
    fun `a fling down from compact closes`() {
        val s = sheet(800)
        s.drag(-500f)
        assertTrue(s.drag(5f, velocity = 2000f))
        assertEquals(Detent.Closed, s.detent)
    }
}
