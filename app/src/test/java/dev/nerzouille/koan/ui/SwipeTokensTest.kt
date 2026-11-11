package dev.nerzouille.koan.ui

import dev.nerzouille.koan.ui.theme.SwipeTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/** The row fling moved from px/s to dp/s: on the POCO (density 2.75) it is today's 1 800 px/s. */
class SwipeTokensTest {
    @Test fun flingMatchesTheOldPixelThresholdOnThePoco() {
        assertEquals(1800f, SwipeTokens.FLING_DP_PER_S * 2.75f, 2f)
    }
}
