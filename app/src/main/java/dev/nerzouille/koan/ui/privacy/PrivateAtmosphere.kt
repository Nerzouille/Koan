package dev.nerzouille.koan.ui.privacy

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import dev.nerzouille.koan.ui.theme.PrivateTokens
import kotlin.math.pow
import kotlin.random.Random

/**
 * Private's sky behind the overview (OVERVIEW_PRIVATE.md §5.2–§5.3): a gradient from the top, three
 * soft glows, and a static star field placed never over the UI ([avoid], in px of this layer:
 * the header row, the card pile or the empty state, the dock zone). Every part follows [flip] in
 * the draw phase: it travels with the private page; entering Private, the stars appear from the
 * right with a small twinkle.
 */
@Composable
fun PrivateAtmosphere(flip: PrivateFlip, dark: Boolean, avoid: (IntSize) -> List<Rect>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val seed = rememberSkySeed(LocalContext.current)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val sky = remember(size, seed, avoid(size)) { if (size == IntSize.Zero) null else placeSky(seed, size, avoid(size), density.density) }
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .drawWithCache {
                val gradLen = (if (dark) PrivateTokens.GradientNightLength else PrivateTokens.GradientDuskLength).toPx()
                val gradient = Brush.verticalGradient(
                    listOf(if (dark) PrivateTokens.GradientNight else PrivateTokens.GradientDusk, Color.Transparent),
                    startY = 0f,
                    endY = gradLen,
                )
                val w = this.size.width
                val dp = density.density
                // (centre x, centre y, radius x, radius y, colour, alpha)
                val glows = if (dark) {
                    listOf(
                        Glow(0.16f * w, 20 * dp, 280 * dp, 220 * dp, Color(0xFF7864FF), 0.26f),
                        Glow(0.86f * w, 0f, 260 * dp, 200 * dp, Color(0xFF9A6CFF), 0.20f),
                        Glow(0.50f * w, 200 * dp, 320 * dp, 170 * dp, Color(0xFF7864FF), 0.10f),
                    )
                } else {
                    listOf(
                        Glow(0.16f * w, 20 * dp, 280 * dp, 220 * dp, Color(0xFF4B3BE0), 0.34f),
                        Glow(0.86f * w, 0f, 260 * dp, 200 * dp, Color(0xFF8050E8), 0.26f),
                        Glow(0.50f * w, 200 * dp, 320 * dp, 170 * dp, Color(0xFF4B3BE0), 0.14f),
                    )
                }
                val starA = if (dark) PrivateTokens.StarNightA else PrivateTokens.StarDuskA
                val starB = if (dark) PrivateTokens.StarNightB else PrivateTokens.StarDuskB
                val sparkleColour = if (dark) PrivateTokens.StarNightB else PrivateTokens.StarDuskB
                val starMax = if (dark) PrivateTokens.STAR_ALPHA_NIGHT else PrivateTokens.STAR_ALPHA_DUSK
                val sparkleMax = if (dark) PrivateTokens.SPARKLE_ALPHA_NIGHT else PrivateTokens.SPARKLE_ALPHA_DUSK
                val fade = PrivateTokens.StarFade.toPx()
                val band = PrivateTokens.GlowBand.toPx()
                onDrawBehind {
                    if (!flip.target && !flip.running) return@onDrawBehind
                    // It travels with the private page (the caller moves this layer).
                    val a = 1f
                    val left = 0f
                    val right = w
                    clipRectSafe(left, right, this.size.height) {
                        drawRect(gradient, alpha = a, size = Size(w, gradLen))
                        clipRectSafe(left, right, band) {
                            for (g in glows) {
                                // A radial gradient drawn in a circle, squashed into the ellipse.
                                scale(1f, g.ry / g.rx, pivot = Offset(g.x, g.y)) {
                                    drawCircle(g.brush(a), g.rx, Offset(g.x, g.y))
                                }
                            }
                        }
                        val s = sky ?: return@clipRectSafe
                        for ((i, star) in s.stars.withIndex()) {
                            val (f, sc) = flip.star(star.x / w)
                            if (f <= 0f || sc <= 0f) continue
                            val alpha = starMax * (1f - star.y / fade).coerceAtLeast(0f).pow(1.1f) * star.weight * f * a
                            drawCircle(if (i % 5 < 3) starA else starB, star.radius * sc, Offset(star.x, star.y), alpha = alpha.coerceIn(0f, 1f))
                        }
                        for (sp in s.sparkles) {
                            val (f, sc) = flip.star(sp.x / w)
                            if (f <= 0f || sc <= 0f) continue
                            val alpha = sparkleMax * (1f - sp.y / fade).coerceAtLeast(0f).pow(0.6f) * f * a
                            translate(sp.x, sp.y) {
                                rotate(45f * (1f - f), pivot = Offset.Zero) {
                                    scale(sp.size / 24f * sc, sp.size / 24f * sc, pivot = Offset.Zero) {
                                        translate(-12f, -12f) { drawPath(SPARKLE, sparkleColour, alpha = alpha.coerceIn(0f, 1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
    )
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRectSafe(left: Float, right: Float, bottom: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(left, 0f, right, bottom)
    block()
    drawContext.canvas.restore()
}

private class Glow(val x: Float, val y: Float, val rx: Float, val ry: Float, val colour: Color, val alpha: Float) {
    private val full = Brush.radialGradient(listOf(colour.copy(alpha = alpha), colour.copy(alpha = 0f)), Offset(x, y), rx)

    /** At rest the brush is made once; only a crossfade makes new ones. */
    fun brush(a: Float): Brush = if (a >= 0.999f) full else Brush.radialGradient(listOf(colour.copy(alpha = alpha * a), colour.copy(alpha = 0f)), Offset(x, y), rx)
}

private class Star(val x: Float, val y: Float, val radius: Float, val weight: Float)

private class Sparkle(val x: Float, val y: Float, val size: Float)

private class Sky(val stars: List<Star>, val sparkles: List<Sparkle>)

/** Sparkle path, 24 grid (§5.3). */
private val SPARKLE = Path().apply {
    moveTo(12f, 1f); lineTo(13.6f, 10.4f); lineTo(23f, 12f); lineTo(13.6f, 13.6f)
    lineTo(12f, 23f); lineTo(10.4f, 13.6f); lineTo(1f, 12f); lineTo(10.4f, 10.4f); close()
}

/**
 * Places the stars and sparkles in the top [PrivateTokens.StarArea] of [size], rejecting any point
 * (any sparkle box) inside [avoid]. Gives up on a point after a few tries, so a crowded layout just
 * has fewer stars.
 */
private fun placeSky(seed: Long, size: IntSize, avoid: List<Rect>, dp: Float): Sky {
    val random = Random(seed)
    val area = (PrivateTokens.StarArea.value * dp).coerceAtMost(size.height.toFloat())
    fun free(r: Rect) = avoid.none { it.overlaps(r) }
    val sparkles = ArrayList<Sparkle>()
    var tries = 0
    while (sparkles.size < PrivateTokens.SPARKLES && tries++ < 2000) {
        val s = PrivateTokens.SparkleSizes[random.nextInt(PrivateTokens.SparkleSizes.size)] * dp
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * area
        if (free(Rect(x - s / 2f, y - s / 2f, x + s / 2f, y + s / 2f))) sparkles += Sparkle(x, y, s)
    }
    val stars = ArrayList<Star>()
    tries = 0
    while (stars.size < PrivateTokens.STARS && tries++ < 4000) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * area
        if (!free(Rect(x - 1f, y - 1f, x + 1f, y + 1f))) continue
        val r = PrivateTokens.StarRadii[random.nextInt(PrivateTokens.StarRadii.size)] * dp
        stars += Star(x, y, r, 0.5f + 0.5f * random.nextFloat())
    }
    return Sky(stars, sparkles)
}

/** One sky per install: the seed is made once and kept. */
@Composable
private fun rememberSkySeed(context: Context): Long = remember {
    val prefs = context.getSharedPreferences("private_sky", Context.MODE_PRIVATE)
    prefs.getLong("seed", 0L).takeIf { it != 0L } ?: Random.nextLong().also { prefs.edit().putLong("seed", it).apply() }
}
