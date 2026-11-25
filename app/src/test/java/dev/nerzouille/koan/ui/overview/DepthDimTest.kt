package dev.nerzouille.koan.ui.overview

import androidx.compose.ui.graphics.Color
import dev.nerzouille.koan.ui.theme.KoanPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Depth cue A (owner, 2026-11-26): back cards recede into a colour, header and thumbnail alike. */
class DepthDimTest {
    private fun dims(p: KoanPalette) = (0..6).map { DepthCurve.cardDim(it.toFloat(), p.DimGain, p.DimMax) }

    @Test fun `light recedes into a grey darker than Base`() {
        val want = listOf(0f, 0.176f, 0.308f, 0.418f, 0.528f, 0.62f, 0.62f)
        dims(KoanPalette.Light).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
        assertEquals(Color(0xFFB9B9C2), KoanPalette.Light.DimColor)
    }

    @Test fun `dark recedes into its Base, stronger than light`() {
        val want = listOf(0f, 0.224f, 0.392f, 0.532f, 0.672f, 0.812f, 0.85f)
        dims(KoanPalette.Dark).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
        assertEquals(Color(0xFF121214), KoanPalette.Dark.DimColor)
        assertEquals(KoanPalette.Night.Base, KoanPalette.Night.DimColor)
    }

    @Test fun `Private recedes like the normal stack of its theme`() {
        assertEquals(dims(KoanPalette.Light), dims(KoanPalette.Dusk))
        assertEquals(dims(KoanPalette.Dark), dims(KoanPalette.Night))
    }

    @Test fun `every card further back recedes more, up to the cap`() {
        for (p in listOf(KoanPalette.Light, KoanPalette.Dark, KoanPalette.Dusk, KoanPalette.Night)) {
            val d = (0..60).map { DepthCurve.cardDim(it / 10f, p.DimGain, p.DimMax) }
            d.zipWithNext().forEach { (a, b) -> assertTrue("receded less further back: $a → $b", b >= a) }
            assertTrue(d[10] < d[30])
        }
    }
}
