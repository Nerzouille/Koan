package dev.nerzouille.koan.ui.overview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One ambient light (direction 3, owner 2026-11-26): the fade, the hand-over of the front shadow. */
class DepthDimTest {
    private fun fades(p: KoanPalette) = (0..4).map { DepthCurve.cardDim(it.toFloat(), p.DimGain, p.DimMax, p.isDark) }

    @Test fun `light fades towards its background, 0 to 0_32`() {
        val want = listOf(0f, 0.10f, 0.18f, 0.26f, 0.32f)
        fades(KoanPalette.Light).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
        assertEquals(KoanPalette.Light.Base, KoanPalette.Light.DimColor)
        assertEquals(Color(0xFFF5F5F7), KoanPalette.Light.DimColor)
    }

    @Test fun `dark fades towards its background, 0 to 0_58`() {
        val want = listOf(0f, 0.20f, 0.35f, 0.48f, 0.58f)
        fades(KoanPalette.Dark).zip(want).forEach { (got, w) -> assertEquals(w, got, 0.001f) }
        assertEquals(Color(0xFF121214), KoanPalette.Dark.DimColor)
    }

    @Test fun `Private fades like its theme, towards its own background`() {
        assertEquals(fades(KoanPalette.Light), fades(KoanPalette.Dusk))
        assertEquals(fades(KoanPalette.Dark), fades(KoanPalette.Night))
        assertEquals(KoanPalette.Dusk.Base, KoanPalette.Dusk.DimColor)
        assertEquals(KoanPalette.Night.Base, KoanPalette.Night.DimColor)
    }

    @Test fun `every card further back fades more, held past the last step`() {
        for (p in listOf(KoanPalette.Light, KoanPalette.Dark, KoanPalette.Dusk, KoanPalette.Night)) {
            val d = (0..60).map { DepthCurve.cardDim(it / 10f, p.DimGain, p.DimMax, p.isDark) }
            d.zipWithNext().forEach { (a, b) -> assertTrue("faded less further back: $a → $b", b >= a) }
            assertEquals(d[40], d[60], 1e-6f)
        }
    }

    @Test fun `only the front card casts the front shadow, handed over as the front moves`() {
        assertEquals(1f, DepthCurve.frontness(slot = 5f, frontSlot = 5f))
        assertEquals(0f, DepthCurve.frontness(slot = 4f, frontSlot = 5f))
        assertEquals(0f, DepthCurve.frontness(slot = 6f, frontSlot = 5f))
        // Half way through the hand-over: half each, never more than one shadow in total.
        assertEquals(0.5f, DepthCurve.frontness(slot = 5f, frontSlot = 5.5f))
        assertEquals(0.5f, DepthCurve.frontness(slot = 6f, frontSlot = 5.5f))
    }

    @Test fun `the shadows are the design's values`() {
        with(CardTokens.FrontKeyLight) { assertEquals(listOf(16.dp, 36.dp, (-8).dp), listOf(y, blur, spread)); assertEquals(0.18f, alpha) }
        with(CardTokens.FrontAmbientLight) { assertEquals(listOf(2.dp, 6.dp), listOf(y, blur)); assertEquals(0.06f, alpha) }
        with(CardTokens.BackLight) { assertEquals(listOf(1.dp, 2.dp), listOf(y, blur)); assertEquals(0.05f, alpha) }
        assertEquals(0.70f, CardTokens.FrontKeyDark.alpha)
        assertEquals(0.40f, CardTokens.BackDark.alpha)
    }
}
