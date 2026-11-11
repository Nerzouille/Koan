package dev.nerzouille.koan.ui.kit

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BrandTokens

/**
 * The soft pink → violet → blue glow at the top of a page (the brand gradient, kept faint): three
 * radial spots across [height], each at [alpha].
 */
fun Modifier.brandWash(height: Dp, alpha: Float): Modifier = drawBehind {
    val h = height.toPx()
    val spots = listOf(
        BrandTokens.Pink to Offset(size.width * 0.1f, h * 0.15f),
        BrandTokens.Violet to Offset(size.width * 0.55f, h * 0.05f),
        BrandTokens.Blue to Offset(size.width * 0.95f, h * 0.3f),
    )
    for ((color, center) in spots) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, h * 0.75f), h * 0.75f, center)
    }
}
