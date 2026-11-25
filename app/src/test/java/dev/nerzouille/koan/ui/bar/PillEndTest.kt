package dev.nerzouille.koan.ui.bar

import org.junit.Assert.assertEquals
import org.junit.Test

class PillEndTest {
    @Test fun `the download ring takes the translate icon's slot, then gives it back`() {
        assertEquals(PillEnd.Ring, pillEndSlot(download = 0.4f, downloadDone = false, translate = true))
        assertEquals(PillEnd.Ring, pillEndSlot(download = Float.NaN, downloadDone = false, translate = true))
        assertEquals(PillEnd.Ring, pillEndSlot(download = null, downloadDone = true, translate = true))
        assertEquals(PillEnd.Translate, pillEndSlot(download = null, downloadDone = false, translate = true))
        assertEquals(PillEnd.None, pillEndSlot(download = null, downloadDone = false, translate = false))
    }
}
