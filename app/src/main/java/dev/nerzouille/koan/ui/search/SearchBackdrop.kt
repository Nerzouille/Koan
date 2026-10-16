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
import dev.nerzouille.koan.ui.bar.bakeBlur
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SearchTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Behind the search sheet: the page, blurred progressively (like iOS's variable blur). Fully
 * blurred and dimmed from the top of the results ([solidTop], px from the top of the screen) down,
 * through the field to the keyboard; fading back to the sharp page over [SearchTokens.BlurFade]
 * above. No live blur: the page is captured once ([snapshot]), halved, and baked into two blur
 * levels (the drawer's technique), each drawn through a vertical gradient mask. [shown] fades it
 * all in and out with the sheet. Until the blur is baked, a stronger plain dim keeps page text away.
 */
@Composable
fun SearchBackdrop(snapshot: suspend () -> Bitmap?, solidTop: () -> Float, shown: () -> Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val blurLayer = rememberGraphicsLayer()
    val outLayer = rememberGraphicsLayer()
    var levels by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(Unit) {
        val page = snapshot() ?: return@LaunchedEffect
        val half = withContext(Dispatchers.Default) {
            Bitmap.createScaledBitmap(page, page.width / 2, page.height / 2, true).also { if (it != page) page.recycle() }
        }.asImageBitmap()
        // Radii for the half-size copy: drawn back at twice the size, they read twice as strong.
        levels = SearchTokens.BlurRadii.map { r -> bakeBlur(half, with(density) { r.toPx() } / 2f, blurLayer, outLayer, density) }
    }
    val palette = KoanTheme.colors
    val dim = palette.Base
    Canvas(modifier.fillMaxSize()) {
        val a = shown()
        if (a <= 0f) return@Canvas
        val solid = solidTop().coerceIn(0f, size.height)
        val fade = SearchTokens.BlurFade.toPx()
        val top = solid - fade
        val mid = solid - fade / 2f
        val baked = levels
        if (baked.size == 2) {
            // The weaker blur fades in over the upper half of the fade, the stronger one over the lower half.
            masked(baked[0], top, mid, a)
            masked(baked[1], mid, solid, a)
        }
        // The frosted tint, same gradient: page text never competes with a result.
        val strength = if (baked.size == 2) SearchTokens.DIM else SearchTokens.DIM_UNBLURRED
        drawRect(
            Brush.verticalGradient(0f to dim.copy(alpha = 0f), 1f to dim.copy(alpha = strength), startY = top, endY = solid),
            topLeft = Offset(0f, top), size = size.copy(height = fade), alpha = a,
        )
        drawRect(dim.copy(alpha = strength), topLeft = Offset(0f, solid), size = size.copy(height = size.height - solid), alpha = a)
    }
}

/** [image] stretched over the screen, visible from [from] (transparent) to [to] (opaque) and below. */
private fun DrawScope.masked(image: ImageBitmap, from: Float, to: Float, alpha: Float) {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(image.width, image.height), dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = from, endY = to),
            blendMode = BlendMode.DstIn,
        )
        canvas.restore()
    }
}
