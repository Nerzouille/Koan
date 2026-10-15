package dev.nerzouille.koan.ui.bar

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.SearchFieldTokens

/**
 * The address input's ring (design 7b–7d): [neutral] while typing; as [flow] goes 0 → 1 (typing
 * "@") the brand gradient flows in from the left around both halves of the pill, a bit thicker,
 * with a soft glow. [flow] is read while drawing only.
 */
fun Modifier.searchRing(neutral: Color, ring: Dp, flow: () -> Float): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val r = h / 2f
    val base = ring.toPx()
    val at = SearchFieldTokens.AtRing.toPx()
    val glow = SearchFieldTokens.Glow.toPx()
    val inset = at / 2f
    // Two halves of the pill outline, both starting at the left middle: over the top, and under.
    val top = Path().apply {
        moveTo(inset, r)
        arcTo(Rect(inset, inset, h - inset, h - inset), 180f, 90f, false)
        lineTo(w - r, inset)
        arcTo(Rect(w - h + inset, inset, w - inset, h - inset), 270f, 90f, false)
    }
    val bottom = Path().apply {
        moveTo(inset, r)
        arcTo(Rect(inset, inset, h - inset, h - inset), 180f, -90f, false)
        lineTo(w - r, h - inset)
        arcTo(Rect(w - h + inset, inset, w - inset, h - inset), 90f, -90f, false)
    }
    val halves = listOf(top, bottom).map { p -> PathMeasure().apply { setPath(p, false) } }
    val segments = listOf(Path(), Path())
    val brush = Brush.horizontalGradient(BrandTokens.Colors, startX = 0f, endX = w)
    val full = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(base / 2f, base / 2f, w - base / 2f, h - base / 2f, androidx.compose.ui.geometry.CornerRadius(r))) }
    onDrawWithContent {
        drawContent()
        val f = flow().coerceIn(0f, 1f)
        // The neutral ring gives way as the gradient arrives.
        if (f < 1f) drawPath(full, neutral, alpha = 1f - f * f, style = Stroke(base))
        if (f <= 0.001f) return@onDrawWithContent
        val width = base + (at - base) * f
        for (i in 0..1) {
            val m = halves[i]
            val seg = segments[i]
            seg.reset()
            m.getSegment(0f, m.length * f, seg, true)
            drawPath(seg, brush, alpha = f * SearchFieldTokens.GLOW_ALPHA * 0.5f, style = Stroke(glow, cap = StrokeCap.Round))
            drawPath(seg, brush, alpha = f * SearchFieldTokens.GLOW_ALPHA, style = Stroke(glow / 2f, cap = StrokeCap.Round))
            drawPath(seg, brush, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}
