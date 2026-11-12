package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.ui.theme.KoanPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The depth → dim mapping of the stack's back cards, pinned to v0.4.0's values (owner, 2026-11-12). */
class DepthDimTest {
    private fun dims(p: KoanPalette) = (0..6).map { DepthCurve.cardDim(it.toFloat(), p.DimGain, p.DimMax) }

    @Test fun `light haze, as in 0_4_0`() {
        val want = listOf(0f, 0.20f, 0.35f, 0.475f, 0.60f, 0.725f, 0.825f)
        dims(KoanPalette.Light).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
    }

    @Test fun `dark dim, as in 0_4_0`() {
        val want = listOf(0f, 0.16f, 0.28f, 0.38f, 0.48f, 0.58f, 0.66f)
        dims(KoanPalette.Dark).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
    }

    @Test fun `Private dims like the normal stack of its theme`() {
        assertEquals(dims(KoanPalette.Light), dims(KoanPalette.Dusk))
        assertEquals(dims(KoanPalette.Dark), dims(KoanPalette.Night))
    }

    @Test fun `every card further back is dimmer, in every palette`() {
        for (p in listOf(KoanPalette.Light, KoanPalette.Dark, KoanPalette.Dusk, KoanPalette.Night)) {
            val d = (0..60).map { DepthCurve.cardDim(it / 10f, p.DimGain, p.DimMax) }
            d.zipWithNext().forEach { (a, b) -> assertTrue("not darker further back: $a → $b", b > a) }
        }
    }

    @Test fun `headers dim half as much, capped so titles stay legible`() {
        assertEquals(0.10f, DepthCurve.cardHeaderDim(1f, 1.25f, 0.85f), 0.001f)
        assertEquals(0.25f, DepthCurve.cardHeaderDim(6f, 1.25f, 0.85f), 0.001f)
        assertEquals(0.20f, DepthCurve.cardHeaderDim(6f, 1f, 1f), 0.001f)
    }
}
