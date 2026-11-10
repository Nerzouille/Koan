package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.Motion
import kotlin.math.min

/**
 * Back on a tab's first page closes it: the page (its last pixels, [shot]) shrinks into a card and
 * slides away to the left, like a card swiped off the overview, while the next tab comes up under
 * it. [progress] 0 → 1 drives it, read in the layer only (no recomposition per frame).
 */
@Composable
fun ClosingPage(shot: ImageBitmap, progress: Animatable<Float, AnimationVector1D>, toTop: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .then(if (toTop) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars))
            .graphicsLayer {
                val p = progress.value
                // First it lets go of the screen and becomes a card...
                val shrink = min(p / SHRINK_UNTIL, 1f)
                val scale = 1f - (1f - Motion.HERO_FALLBACK_SCALE) * shrink
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.45f)
                shape = RoundedCornerShape(CardTokens.Radius * shrink)
                clip = true
                // ...then flies off to the left, speeding up.
                val fly = ((p - FLY_FROM) / (1f - FLY_FROM)).coerceIn(0f, 1f)
                translationX = -size.width * FLY_DISTANCE * fly * fly
                shadowElevation = 24f * shrink
                alpha = 1f - 0.25f * fly
            },
    ) {
        Image(
            shot,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val SHRINK_UNTIL = 0.45f
private const val FLY_FROM = 0.3f
private const val FLY_DISTANCE = 1.15f
