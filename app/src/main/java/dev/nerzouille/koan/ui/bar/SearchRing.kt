package dev.nerzouille.koan.ui.bar

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.SearchFieldTokens
import kotlin.math.PI
import kotlin.math.sin

/**
 * The address input's ring (design 7b–7d): [neutral] while typing; as [flow] goes 0 → 1 (typing
 * "@") the brand gradient flows in from the left around both halves of the pill, a bit thicker,
 * crisp, with only a faint halo tight to the stroke. In "@" mode the gradient turns slowly around
 * the ring ([turn], degrees). Both are read while drawing only: the shader moves, nothing else.
 */
fun Modifier.searchRing(neutral: Color, ring: Dp, flow: () -> Float, turn: () -> Float): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val r = h / 2f
    val base = ring.toPx()
    val at = SearchFieldTokens.AtRing.toPx()
    val halo = SearchFieldTokens.Halo.toPx()
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
    val full = Path().apply { addRoundRect(RoundRect(base / 2f, base / 2f, w - base / 2f, h - base / 2f, CornerRadius(r))) }
    // A sweep around the field's centre, closed on itself (pink → violet → blue → pink), turned
    // through its local matrix each frame: no new shader, no new brush.
    val colors = (BrandTokens.Colors + BrandTokens.Colors.first()).map { it.toArgb() }.toIntArray()
    val shader = SweepGradient(w / 2f, h / 2f, colors, null)
    val matrix = Matrix()
    val brush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        val f = flow().coerceIn(0f, 1f)
        // The neutral ring gives way as the gradient arrives.
        if (f < 1f) drawPath(full, neutral, alpha = 1f - f * f, style = Stroke(base))
        if (f <= 0.001f) return@onDrawWithContent
        // The sweep starts at 3 o'clock; 180° puts pink on the left, where the flow comes in.
        matrix.setRotate(180f + turn(), w / 2f, h / 2f)
        shader.setLocalMatrix(matrix)
        val width = base + (at - base) * f
        for (i in 0..1) {
            val m = halves[i]
            val seg = segments[i]
            seg.reset()
            m.getSegment(0f, m.length * f, seg, true)
            drawPath(seg, brush, alpha = f * SearchFieldTokens.HALO_ALPHA, style = Stroke(width + 2f * halo, cap = StrokeCap.Round))
            drawPath(seg, brush, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

/** The ring's turn at [seconds] into "@" mode: a slow turn whose speed drifts, never mechanical. */
fun ringTurn(seconds: Float): Float {
    val t = SearchFieldTokens
    return seconds * t.TURN_DEG_PER_S +
        t.WOBBLE_1_DEG * sin(2f * PI.toFloat() * seconds / t.WOBBLE_1_S) +
        t.WOBBLE_2_DEG * sin(2f * PI.toFloat() * seconds / t.WOBBLE_2_S + 1f)
}
