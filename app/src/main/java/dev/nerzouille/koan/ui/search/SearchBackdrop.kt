package dev.nerzouille.koan.ui.search

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.nerzouille.koan.engine.TabManager
import dev.nerzouille.koan.ui.bar.bakeBlur
import dev.nerzouille.koan.ui.theme.SearchTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Behind the search sheet: the page, lightly blurred under the results only (its text still
 * faintly there), easing back to the untouched page over the zone that ends [fade] px above
 * [solidTop] (px from the top of the screen), plus a soft shadow the sheet casts upward from the field ([fieldTop]): black,
 * [SearchTokens.SHADOW] at the field, none at the top of the fade. No wash, no tint otherwise. No
 * live blur: the page is captured once ([snapshot]), halved, and baked into two blur levels (the
 * drawer's technique: three levels here), each drawn through its own eased vertical mask. [shown] fades it in and out.
 */
@Composable
fun SearchBackdrop(
    snapshot: suspend () -> TabManager.Snapshot?,
    solidTop: () -> Float,
    fieldTop: () -> Float,
    fade: () -> Float,
    shown: () -> Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val blurLayer = rememberGraphicsLayer()
    val outLayer = rememberGraphicsLayer()
    var levels by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var where by remember { mutableStateOf<TabManager.Snapshot?>(null) }
    LaunchedEffect(Unit) {
        val shot = snapshot() ?: return@LaunchedEffect
        where = shot
        val page = shot.bitmap
        val full = page.asImageBitmap()
        val half = withContext(Dispatchers.Default) {
            Bitmap.createScaledBitmap(page, page.width / 2, page.height / 2, true)
        }.asImageBitmap()
        levels = SearchTokens.BlurLevels.map { (radius, scale) ->
            // A half-size copy drawn back at twice the size: its radius counts twice.
            bakeBlur(if (scale < 1f) half else full, with(density) { radius.toPx() } * scale, blurLayer, outLayer, density)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val a = shown()
        if (a <= 0f) return@Canvas
        val field = fieldTop().coerceIn(0f, size.height)
        val solid = solidTop().coerceIn(0f, field)
        // The zone: sharp at its top, full blur from [full] down (the field, the lowest results).
        val top = solid - fade()
        val full = field - (field - top) * SearchTokens.BLUR_FULL_SHARE
        val baked = levels
        if (baked.isNotEmpty()) {
            // Lightest to strongest: each eases in over its own stretch of the zone, overlapping
            // the previous one's, so the blur grows continuously with no edge.
            val span = full - top
            val n = baked.size
            val window = span * WINDOW
            val step = (span - window) / (n - 1).coerceAtLeast(1)
            val at = where ?: return@Canvas
            baked.forEachIndexed { i, image -> masked(image, at, top + step * i, top + step * i + window, a) }
        }
        // The sheet's shadow, cast upward from the field, eased the same way.
        drawRect(eased(Color.Black.copy(alpha = SearchTokens.SHADOW), top, field), topLeft = Offset(0f, top), size = size.copy(height = field - top), alpha = a)
        drawRect(Color.Black.copy(alpha = SearchTokens.SHADOW), topLeft = Offset(0f, field), size = size.copy(height = size.height - field), alpha = a)
    }
}

/** Each blur level eases in over this share of the zone. */
private const val WINDOW = 0.4f

/** Transparent at [from], [color] at [to] (and below), along an ease-in-out curve: no visible edge. */
private fun eased(color: Color, from: Float, to: Float): Brush {
    val steps = 12
    val stops = Array(steps + 1) { i ->
        val f = i / steps.toFloat()
        val s = f * f * (3f - 2f * f) // smoothstep
        f to color.copy(alpha = color.alpha * s)
    }
    return Brush.verticalGradient(*stops, startY = from, endY = to)
}

/** [image] drawn over the page view ([at]), eased in from [from] (none) to [to] (all of it) and below. */
private fun DrawScope.masked(image: ImageBitmap, at: TabManager.Snapshot, from: Float, to: Float, alpha: Float) {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
        drawImage(
            image,
            srcOffset = IntOffset.Zero, srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(at.left, at.top), dstSize = IntSize(at.width, at.height),
        )
        drawRect(eased(Color.Black, from, to), blendMode = BlendMode.DstIn)
        canvas.restore()
    }
}
