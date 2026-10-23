package dev.nerzouille.koan

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceProfileTest {
    private val gib = 1024L * 1024 * 1024

    @Test fun `8 and 6 GB phones keep today's budgets`() {
        assertEquals(MemoryTier.High, DeviceProfile.tierFor(7 * gib + gib / 2, lowRam = false))
        assertEquals(MemoryTier.High, DeviceProfile.tierFor(5 * gib + gib / 2, lowRam = false))
        assertEquals(4, MemoryTier.High.liveSessions)
        assertEquals(720, MemoryTier.High.thumbWidthPx)
        assertEquals(3, MemoryTier.High.bakedLevels)
    }

    @Test fun `4 GB is mid, 3 GB is low`() {
        assertEquals(MemoryTier.Mid, DeviceProfile.tierFor(3 * gib + gib * 6 / 10, lowRam = false))
        assertEquals(MemoryTier.Low, DeviceProfile.tierFor(2 * gib + gib * 8 / 10, lowRam = false))
    }

    @Test fun `a low-RAM device is low whatever it reports`() {
        assertEquals(MemoryTier.Low, DeviceProfile.tierFor(8 * gib, lowRam = true))
    }
}
