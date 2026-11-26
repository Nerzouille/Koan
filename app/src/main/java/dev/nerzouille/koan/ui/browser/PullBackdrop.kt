package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import dev.nerzouille.koan.ui.kit.rememberAnimationsOff
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PullTokens

/**
 * What the page uncovers as it slides down (the prototypes, [PullStyle.slides]): a band as tall as
 * the pull, drawn under the page, with the K in its middle. The K grows with the pull up to the
 * trigger and turns while the page reloads (still, with "remove animations"). Everything is read
 * at draw time: a pull redraws this band only, it never recomposes.
 */
@Composable
fun PullBackdrop(state: PullRefresh, style: PullStyle, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    val still = rememberAnimationsOff()
    val turn = if (still) null else rememberInfiniteTransition(label = "pullK").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(PullTokens.K_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(PullTokens.Max)
            .drawWithCache {
                val k = koanK()
                val gradient = Brush.horizontalGradient(BrandTokens.Colors)
                val ink: Brush = when (style) {
                    PullStyle.Arc -> Brush.linearGradient(listOf(Color.White, Color.White))
                    PullStyle.Logo -> Brush.linearGradient(BrandTokens.Colors, Offset(0f, 0f), Offset(K_VIEWPORT, K_VIEWPORT))
                    else -> Brush.linearGradient(listOf(palette.TextPrimary, palette.TextPrimary))
                }
                onDrawBehind {
                    val p = state.pull.value
                    if (p <= 0.5f) return@onDrawBehind
                    val f = (p / state.triggerPx).coerceIn(0f, 1f)
                    clipRect(bottom = p) {
                        when (style) {
                            PullStyle.Arc -> drawRect(gradient)
                            PullStyle.Haze -> {
                                drawRect(palette.Base)
                                haze(p, PullTokens.HAZE_MIN + (PullTokens.HAZE_MAX - PullTokens.HAZE_MIN) * f)
                            }
                            else -> drawRect(palette.Base)
                        }
                        val side = PullTokens.KSize.toPx() * (PullTokens.K_MIN_SCALE + (1f - PullTokens.K_MIN_SCALE) * f)
                        val angle = if (state.refreshing) turn?.value ?: 0f else 0f
                        drawK(k, ink, Offset(size.width / 2f, p / 2f), side, angle)
                    }
                }
            },
    )
}

/** Koan's haze (Settings, About): three soft brand spots across the band, at [alpha]. */
private fun DrawScope.haze(height: Float, alpha: Float) {
    val r = maxOf(height, size.width * 0.35f)
    HAZE_SPOTS.forEach { (color, at) ->
        val center = Offset(size.width * at.x, height * at.y)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, r), r, center)
    }
}

/** The K ([path], in its 108-unit icon viewport) centred on [center], [side] px across, turned [angle]°. */
private fun DrawScope.drawK(path: Path, ink: Brush, center: Offset, side: Float, angle: Float) {
    val s = side / K_SPAN
    translate(center.x - K_CENTER.x * s, center.y - K_CENTER.y * s) {
        scale(s, s, pivot = Offset.Zero) {
            rotate(angle, pivot = K_CENTER) { drawPath(path, ink) }
        }
    }
}

/** The K of Koan's icon (res/drawable/ic_launcher_monochrome.xml, branding/koan-k-monochrome.svg). */
internal fun koanK(): Path = PathParser().parsePathString(K_PATH).toPath()

private const val K_VIEWPORT = 108f

/** The haze's three spots, as in Settings' wash: colour, then where (fractions of the band). */
private val HAZE_SPOTS = listOf(
    BrandTokens.Pink to Offset(0.1f, 0.3f),
    BrandTokens.Violet to Offset(0.55f, 0.1f),
    BrandTokens.Blue to Offset(0.95f, 0.5f),
)

/** The K's own box in the viewport: about 24 → 83 across, 29 → 83 down. */
private val K_CENTER = Offset(54f, 55f)
private const val K_SPAN = 60f

private const val K_PATH =
    "M24.63,48.24 A4.70,4.70 0 0 1 33.20,44.39 Q40.19,58.52 47.18,72.66 A5.88,5.88 0 1 1 36.47,77.47 " +
        "Q30.55,62.85 24.63,48.24Z M42.77,58.20 A3.09,3.09 0 0 1 38.13,54.16 Q47.61,41.60 57.09,29.03 " +
        "A5.29,5.29 0 1 1 65.05,35.96 Q53.91,47.08 42.77,58.20Z M43.46,62.37 A3.97,3.97 0 0 1 44.21,54.56 " +
        "Q59.51,53.75 74.82,52.95 A8.52,8.52 0 1 1 73.22,69.73 Q58.34,66.05 43.46,62.37Z " +
        "M36.70,58.52 A2.94,2.94 0 1 1 42.58,58.52 A2.94,2.94 0 1 1 36.70,58.52Z"
