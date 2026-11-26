package dev.nerzouille.koan.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import dev.nerzouille.koan.ui.kit.frameClock
import dev.nerzouille.koan.ui.kit.rememberAnimationsOff
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arc: what the page uncovers as it slides down, a band as tall as the pull drawn under the page.
 * Soft pink, violet and blue blobs (a mesh gradient), and the K coming out from under the page's
 * edge, growing and fading in. Let go past the trigger, the K spins and the blobs drift with it,
 * one signal ([PullSpin]): a flick that slows down, small pushes while the page still loads, then
 * the page slides back over it while it still turns. "Remove animations": all still. The clock
 * writes floats read at draw time only: nothing recomposes per frame.
 */
@Composable
fun PullBackdrop(state: PullRefresh, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    val still = rememberAnimationsOff()
    val spin = remember { PullSpin() }
    LaunchedEffect(state, still) {
        if (still) return@LaunchedEffect
        snapshotFlow { state.phase }.collectLatest { phase ->
            when (phase) {
                // Let go: the flick, then pushes while it loads.
                PullPhase.Refreshing -> {
                    spin.flick()
                    frameClock { dt -> spin.step(dt, loading = true) }
                }
                // Loaded: it keeps slowing down while the page slides back over it.
                PullPhase.Settling -> if (spin.speed > 0f) frameClock { dt -> spin.step(dt, loading = false) }
                // Covered: still, for the next pull.
                PullPhase.Idle -> spin.stop()
                PullPhase.Pulling -> Unit
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val k = koanK()
                val ink = Brush.linearGradient(listOf(Color.White, Color.White))
                onDrawBehind {
                    val p = state.pull.value
                    if (p <= 0.5f) return@onDrawBehind
                    val f = (p / state.triggerPx).coerceIn(0f, 1f)
                    clipRect(bottom = p) {
                        mesh(p, f, spin.drift, palette)
                        // Out from under the page's edge: all of it once reloading (or on its way back).
                        val e = if (state.phase == PullPhase.Pulling) easeOut(f) else 1f
                        val grow = PullTokens.K_MIN_SCALE + (1f - PullTokens.K_MIN_SCALE) * e
                        val side = PullTokens.KSize.toPx() * grow
                        // Loaded: it stays where it rested and the page slides back over it, still turning.
                        val covering = state.phase == PullPhase.Settling && spin.speed > 0f
                        val edge = if (covering) state.restPx else p
                        val y = edge + side / 2f - (side + PullTokens.KGap.toPx()) * e
                        drawK(k, ink, Offset(size.width / 2f, y), side, spin.angle, alpha = e)
                    }
                }
            },
    )
}

private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

/** Where a blob sits: x a share of the width, y a share of the band, [r] a share of the radius. */
private class Spot(val color: Color, val x: Float, val y: Float, val r: Float = 1f)

private val MeshSpots = listOf(
    Spot(BrandTokens.Pink, 0.12f, 0.2f),
    Spot(BrandTokens.Blue, 0.92f, 0.55f),
    Spot(BrandTokens.Violet, 0.5f, 1.05f, 0.8f),
)

/**
 * Soft pink, violet and blue blobs over a violet ground, like a mesh gradient: they lean in a little
 * with the pull ([f]) and circle slowly with the K's turn ([drift], each a step apart). Opaque.
 */
private fun DrawScope.mesh(p: Float, f: Float, drift: Float, palette: KoanPalette) {
    val dark = palette.isDark
    drawRect(if (dark) PullTokens.ArcGroundDark else PullTokens.ArcGroundLight)
    val w = size.width
    val r = w * PullTokens.ARC_BLOB_RADIUS
    val lean = PullTokens.ARC_DRIFT * f
    val a = if (dark) PullTokens.ARC_BLOB_ALPHA_DARK else 1f
    MeshSpots.forEachIndexed { i, s ->
        val phase = drift + i * SPOT_PHASE_STEP
        val x = s.x + (if (s.x < HALF) lean else if (s.x > HALF) -lean else 0f) + PullTokens.ARC_CIRCLE * cos(phase)
        val y = (if (i == MeshSpots.lastIndex) s.y - lean * 2f else s.y) + PullTokens.ARC_CIRCLE * sin(phase)
        blob(s.color, Offset(w * x, p * y), r * s.r, a)
    }
}

private const val HALF = 0.5f
private const val SPOT_PHASE_STEP = 2.1f

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
