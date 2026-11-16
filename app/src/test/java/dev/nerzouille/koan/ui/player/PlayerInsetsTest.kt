package dev.nerzouille.koan.ui.player

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerInsetsTest {
    private val density = Density(1f)

    @Test fun `portrait puts the top row under the status bar, plain sides`() {
        val ins = playerInsets(emptyList(), width = 400.dp, height = 880.dp, bottomPillH = 96.dp, density = density, statusTop = 36.dp)
        assertEquals(36.dp + PlayerTokens.CutoutGap, ins.top)
        assertEquals(PlayerTokens.Inset, ins.titleStart)
        assertEquals(PlayerTokens.Inset, ins.rightEnd)
    }

    @Test fun `portrait with no status bar height keeps the margin`() {
        val ins = playerInsets(emptyList(), width = 400.dp, height = 880.dp, bottomPillH = 96.dp, density = density, statusTop = 0.dp)
        assertEquals(PlayerTokens.Inset, ins.top)
    }

    @Test fun `landscape stays on the margins, status bar ignored`() {
        val ins = playerInsets(emptyList(), width = 880.dp, height = 400.dp, bottomPillH = 56.dp, density = density, statusTop = 36.dp)
        assertEquals(PlayerTokens.Inset, ins.top)
        assertEquals(PlayerTokens.Inset, ins.titleStart)
    }
}
