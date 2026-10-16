package dev.nerzouille.koan.ui.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.collectLatest

/**
 * The overview behind the drawer, blurred without live blur: when the drawer starts opening, the
 * overview is captured once and baked into blurred copies ([radiiPx], weakest first), which
 * crossfade in with the drag. Nothing is blurred per frame, the blur never steps, and the GPU only
 * ever caches buffers for these few radii. The copies are dropped when the drawer is closed again.
 */
@Composable
fun Modifier.drawerBackdrop(progress: () -> Float, radiiPx: List<Float>): Modifier {
    val density = LocalDensity.current
    val content = rememberGraphicsLayer()
    val blur = rememberGraphicsLayer()
    val out = rememberGraphicsLayer()
    var baked by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(radiiPx) {
        snapshotFlow { progress() > 0f }.collectLatest { opening ->
            baked = if (opening) {
                val sharp = content.toImageBitmap()
                radiiPx.map { r -> bakeBlur(sharp, r, blur, out, density) }
            } else {
                emptyList()
            }
        }
    }
    return drawWithContent {
        content.record { this@drawWithContent.drawContent() }
        val levels = baked
        // 0..levels.size: each copy fades in fully over the one below before the next starts.
        val x = progress() * levels.size
        if (levels.isEmpty() || x < levels.size) drawLayer(content)
        levels.forEachIndexed { i, image ->
            val a = (x - i).coerceIn(0f, 1f)
            if (a > 0f) drawImage(image, alpha = a)
        }
    }
}

/** [src] blurred by [radiusPx], baked into a new bitmap through two graphics layers. Shared with the search sheet. */
internal suspend fun bakeBlur(src: ImageBitmap, radiusPx: Float, blur: GraphicsLayer, out: GraphicsLayer, density: Density): ImageBitmap {
    val size = IntSize(src.width, src.height)
    // Clamp, not Decal: Decal fades the edges to transparent and the page underneath (often a white
    // site) showed through as light strips on the left and right.
    blur.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
    blur.record(density, LayoutDirection.Ltr, size) { drawImage(src) }
    // A layer's own effect applies when it is drawn, so draw it into a second one to keep the blur.
    out.record(density, LayoutDirection.Ltr, size) { drawLayer(blur) }
    return out.toImageBitmap()
}
