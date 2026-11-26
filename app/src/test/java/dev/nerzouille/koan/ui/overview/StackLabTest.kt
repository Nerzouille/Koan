package dev.nerzouille.koan.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Test

/** Cast's values as the owner set them (2026-11-26): strength 0.45, thickness 1.00, surface light 0.67. */
class StackLabTest {
    @Test fun `Cast is the look by default`() {
        assertEquals(StackStyle.Cast, StackLab.style)
    }

    @Test fun `Cast's shadows at 0_45`() {
        assertEquals(0.30f * 0.45f, LabTokens.CastAroundLight.alpha, 0.005f)
        assertEquals(0.80f * 0.45f, LabTokens.CastAroundDark.alpha, 0.005f)
        assertEquals(0.30f * 0.45f, LabTokens.CastContactLight.alpha, 0.005f)
        assertEquals(0.85f * 0.45f, LabTokens.CastContactDark.alpha, 0.005f)
    }

    @Test fun `thickness at 1_00 and surface light at 0_67`() {
        assertEquals(0.18f, LabTokens.ThicknessLight.alpha, 0.005f)
        assertEquals(0.70f, LabTokens.ThicknessDark.alpha, 0.005f)
        // Tuned on a scale where 2 drew top white 24% / bottom ink 12% (light), 12% / black 32% (dark).
        assertEquals(0.24f * 0.67f / 2f, LabTokens.SurfaceTopLight.alpha, 0.005f)
        assertEquals(0.12f * 0.67f / 2f, LabTokens.SurfaceBottomLight.alpha, 0.005f)
        assertEquals(0.12f * 0.67f / 2f, LabTokens.SurfaceTopDark.alpha, 0.005f)
        assertEquals(0.32f * 0.67f / 2f, LabTokens.SurfaceBottomDark.alpha, 0.005f)
    }
}
