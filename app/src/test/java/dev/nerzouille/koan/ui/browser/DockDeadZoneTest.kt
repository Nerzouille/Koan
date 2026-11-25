package dev.nerzouille.koan.ui.browser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DockDeadZoneTest {
    private fun hiddenDock(deadZone: Float = 30f) = DockState(CoroutineScope(Dispatchers.Unconfined)).apply {
        heightPx = 100f
        deadZonePx = deadZone
        onTouchDown()
        scrollBy(150f) // scrolled down: hidden
        onTouchDown()
    }

    @Test fun `a small scroll up leaves a hidden dock hidden`() {
        val dock = hiddenDock()
        dock.scrollBy(-20f)
        assertEquals(100f, dock.offset)
    }

    @Test fun `past the dead zone the dock follows the rest`() {
        val dock = hiddenDock()
        dock.scrollBy(-20f)
        dock.scrollBy(-25f) // 10 swallowed, 15 move it
        assertEquals(85f, dock.offset)
    }

    @Test fun `scrolling back down refills the dead zone, a new touch resets it`() {
        val dock = hiddenDock()
        dock.scrollBy(-25f)
        dock.scrollBy(10f) // 15 held now
        dock.scrollBy(-20f) // 15 swallowed, 5 move it
        assertEquals(95f, dock.offset)
        dock.scrollBy(10f)
        dock.onTouchDown()
        dock.scrollBy(-29f)
        assertEquals(100f, dock.offset)
    }

    @Test fun `zero is the old behaviour, a shown dock is never held`() {
        val old = hiddenDock(deadZone = 0f)
        old.scrollBy(-10f)
        assertEquals(90f, old.offset)
        val half = hiddenDock()
        half.scrollBy(-40f) // 30 swallowed, 10 move it: 90, partly shown
        half.onTouchDown()
        half.scrollBy(-10f) // partly shown: follows at once
        assertEquals(80f, half.offset)
    }
}
