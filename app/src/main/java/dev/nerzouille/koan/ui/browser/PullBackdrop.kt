package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * What the page uncovers as it slides down (the prototypes, [PullStyle.slides]): a band as tall as
 * the pull, drawn under the page. The K comes out from under the page's top edge as it moves down,
 * growing and fading in; let go past the trigger, it spins like a flicked wheel and slows down,
 * breathes gently while the page still loads, and settles upright when it's done (all still with
 * "remove animations"). Read at draw time only: a pull redraws this band, it never recomposes.
 */
@Composable
fun PullBackdrop(state: PullRefresh, style: PullStyle, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    val still = rememberAnimationsOff()
    val angle = remember { Animatable(0f) }
    val breath = remember { Animatable(1f) }
    LaunchedEffect(state.refreshing, still) {
        when {
            still -> {
                angle.snapTo(0f)
                breath.snapTo(1f)
            }
            state.refreshing -> {
                angle.snapTo(angle.value % FULL_TURN)
                angle.animateDecay(PullTokens.K_FLICK_DEG_PER_S, exponentialDecay(PullTokens.K_FRICTION))
                // Still loading once it has slowed down: a gentle, eased breath.
                while (true) {
                    breath.animateTo(PullTokens.K_BREATH, tween(PullTokens.K_BREATH_MS, easing = FastOutSlowInEasing))
                    breath.animateTo(1f, tween(PullTokens.K_BREATH_MS, easing = FastOutSlowInEasing))
                }
            }
            else -> {
                // Loaded: a small settle, upright again.
                launch { breath.animateTo(1f, spring()) }
                val upright = (angle.value / FULL_TURN).roundToInt() * FULL_TURN
                angle.animateTo(upright, spring(dampingRatio = PullTokens.K_SETTLE_DAMPING, stiffness = PullTokens.K_SETTLE_STIFFNESS))
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val k = koanK()
                val inkColor = if (style == PullStyle.Arc) Color.White else palette.TextPrimary
                val ink: Brush = Brush.linearGradient(listOf(inkColor, inkColor))
                onDrawBehind {
                    val p = state.pull.value
                    if (p <= 0.5f) return@onDrawBehind
                    val f = (p / state.triggerPx).coerceIn(0f, 1f)
                    clipRect(bottom = p) {
                        if (style == PullStyle.Arc) mesh(p, f, palette) else glow(p, f, palette)
                        // Out from under the page's edge: all of it once reloading.
                        val e = if (state.refreshing) 1f else easeOut(f)
                        val grow = PullTokens.K_MIN_SCALE + (1f - PullTokens.K_MIN_SCALE) * e
                        val side = PullTokens.KSize.toPx() * grow * breath.value
                        val y = p + side / 2f - (side + PullTokens.KGap.toPx()) * e
                        drawK(k, ink, Offset(size.width / 2f, y), side, angle.value, alpha = e)
                    }
                }
            },
    )
}

private const val FULL_TURN = 360f

private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

/** Where a blob sits: x a share of the width, y a share of the band, [r] a share of the radius. */
private class Spot(val color: Color, val x: Float, val y: Float, val r: Float = 1f)

private val MeshSpots = listOf(
    Spot(BrandTokens.Pink, 0.12f, 0.2f),
    Spot(BrandTokens.Blue, 0.92f, 0.55f),
    Spot(BrandTokens.Violet, 0.5f, 1.05f, 0.8f),
)
private val GlowSpots = listOf(
    Spot(BrandTokens.Pink, 0.28f, 0.35f),
    Spot(BrandTokens.Blue, 0.74f, 0.3f),
    Spot(BrandTokens.Violet, 0.5f, 0.55f, 0.7f),
)

/**
 * Arc: soft pink, violet and blue blobs over a violet ground, like a mesh gradient; they drift a
 * little with the pull ([f]): pink and blue towards the middle, violet up. Opaque.
 */
private fun DrawScope.mesh(p: Float, f: Float, palette: KoanPalette) {
    val dark = palette.isDark
    drawRect(if (dark) PullTokens.ArcGroundDark else PullTokens.ArcGroundLight)
    val w = size.width
    val r = w * PullTokens.ARC_BLOB_RADIUS
    val drift = PullTokens.ARC_DRIFT * f
    val a = if (dark) PullTokens.ARC_BLOB_ALPHA_DARK else 1f
    MeshSpots.forEachIndexed { i, s ->
        val x = s.x + if (s.x < HALF) drift else if (s.x > HALF) -drift else 0f
        val y = if (i == MeshSpots.lastIndex) s.y - drift * 2f else s.y
        blob(s.color, Offset(w * x, p * y), r * s.r, a)
    }
}

/**
 * Haze: Koan's neutral base; soft pink and blue glows rise from the page's edge (the band's
 * bottom), brighter as you pull ([f]).
 */
private fun DrawScope.glow(p: Float, f: Float, palette: KoanPalette) {
    drawRect(palette.Base)
    val w = size.width
    val r = w * PullTokens.HAZE_RADIUS
    val rise = p * PullTokens.HAZE_RISE * f
    val a = PullTokens.HAZE_MIN + (PullTokens.HAZE_MAX - PullTokens.HAZE_MIN) * f
    GlowSpots.forEach { s -> blob(s.color, Offset(w * s.x, p + r * s.y - rise), r * s.r, a * s.r) }
}

private const val HALF = 0.5f

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius), radius, center)
}

/** The K ([path], in its 108-unit icon viewport) centred on [center], [side] px across, turned [angle]°. */
private fun DrawScope.drawK(path: Path, ink: Brush, center: Offset, side: Float, angle: Float, alpha: Float) {
    val s = side / K_SPAN
    translate(center.x - K_CENTER.x * s, center.y - K_CENTER.y * s) {
        scale(s, s, pivot = Offset.Zero) {
            rotate(angle, pivot = K_CENTER) { drawPath(path, ink, alpha = alpha.coerceIn(0f, 1f)) }
        }
    }
}

/** The K of Koan's icon (res/drawable/ic_launcher_monochrome.xml, branding/koan-k-monochrome.svg). */
internal fun koanK(): Path = PathParser().parsePathString(K_PATH).toPath()

/** The K's own box in the 108-unit viewport: about 24 → 83 across, 29 → 83 down. */
private val K_CENTER = Offset(54f, 55f)
private const val K_SPAN = 60f

private const val K_PATH =
    "M24.63,48.24 A4.70,4.70 0 0 1 33.20,44.39 Q40.19,58.52 47.18,72.66 A5.88,5.88 0 1 1 36.47,77.47 " +
        "Q30.55,62.85 24.63,48.24Z M42.77,58.20 A3.09,3.09 0 0 1 38.13,54.16 Q47.61,41.60 57.09,29.03 " +
        "A5.29,5.29 0 1 1 65.05,35.96 Q53.91,47.08 42.77,58.20Z M43.46,62.37 A3.97,3.97 0 0 1 44.21,54.56 " +
        "Q59.51,53.75 74.82,52.95 A8.52,8.52 0 1 1 73.22,69.73 Q58.34,66.05 43.46,62.37Z " +
        "M36.70,58.52 A2.94,2.94 0 1 1 42.58,58.52 A2.94,2.94 0 1 1 36.70,58.52Z"
