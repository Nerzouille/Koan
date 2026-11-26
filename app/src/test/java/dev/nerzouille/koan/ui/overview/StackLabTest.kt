package dev.nerzouille.koan.ui.overview

import androidx.compose.ui.graphics.Color
import dev.nerzouille.koan.ui.theme.KoanPalette
import org.junit.Assert.assertEquals
import org.junit.Test

/** The dev stack looks keep beta.1's cards (unit tests run the debug build, where the lab is on). */
class StackLabTest {
    @Test fun `the looks dim the cards as beta_1 did`() {
                // Light: Dim × 1.25, capped at 0.85; the header half of it, capped at 0.2, then × 1.25.
        assertEquals(0.16f * 1.25f, LabCards.cardDim(1f, KoanPalette.Light)!!, 1e-5f)
        assertEquals(0.66f * 1.25f, LabCards.cardDim(6f, KoanPalette.Light)!!, 1e-5f)
        assertEquals(0.08f * 1.25f, LabCards.headerDim(1f, KoanPalette.Light)!!, 1e-5f)
        assertEquals(0.25f, LabCards.headerDim(6f, KoanPalette.Light)!!, 1e-5f)
        // Dark: towards black, uncapped.
        assertEquals(0.66f, LabCards.cardDim(6f, KoanPalette.Dark)!!, 1e-5f)
        assertEquals(Color.Black, LabCards.dimColor(KoanPalette.Dark))
        assertEquals(Color(0xFF050412), LabCards.dimColor(KoanPalette.Night))
        assertEquals(KoanPalette.Light.DimColor, LabCards.dimColor(KoanPalette.Light))
    }
}
