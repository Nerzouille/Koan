package dev.nerzouille.koan.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

/** Koan's own downloads stop as soon as they pass their cap (audit 6.6). */
class KoanHttpTest {
    @Test fun underTheCapIsReadWhole() {
        val bytes = ByteArray(40_000) { it.toByte() }
        assertArrayEquals(bytes, KoanHttp.readCapped(ByteArrayInputStream(bytes), 40_000))
    }

    @Test fun overTheCapGivesNothing() {
        assertNull(KoanHttp.readCapped(ByteArrayInputStream(ByteArray(40_001)), 40_000))
    }
}
