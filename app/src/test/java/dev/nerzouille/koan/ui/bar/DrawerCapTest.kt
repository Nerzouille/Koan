package dev.nerzouille.koan.ui.bar

import org.junit.Assert.assertEquals
import org.junit.Test

/** #11 (owner, 2026-11-26): the open drawer keeps the whole dock under 75% of the screen. */
class DrawerCapTest {
    @Test fun `the dock stays under three quarters of the screen`() {
        // 2400 px window, 300 px of rows and navigation bar: the drawer gets 1500 px.
        assertEquals(1500f, drawerFullCap(windowPx = 2400f, topPx = 120f, dockPx = 300f), 0.01f)
    }

    @Test fun `and never above the status bar`() {
        // A short window where 75% would reach past the status bar.
        assertEquals(500f, drawerFullCap(windowPx = 1000f, topPx = 300f, dockPx = 200f), 0.01f)
    }
}
