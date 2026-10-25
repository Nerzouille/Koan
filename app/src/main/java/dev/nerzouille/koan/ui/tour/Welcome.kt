package dev.nerzouille.koan.ui.tour

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.DeviceProfile
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.TourTokens
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The tour's welcome card: the 3D K on the brand gradient. It springs up from below turning into
 * its tilt, a light sweeps across it, then the title; while shown it floats among twinkling
 * sparkles over a slowly turning vortex. Swiping on flies it off to the left, following the pager
 * both ways. Everything moves in layers and draw only: nothing recomposes per frame.
 */

/** The system's "remove animations" (animator duration scale 0): no motion, everything just shows. */
fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** The welcome card's clocks, read in layers and draw only. */
@Stable
class WelcomeMotion {
    /** The K's rise, 0 … 1 (a spring: a little past 1 at its peak). */
    val rise = Animatable(0f)

    /** Ms since the tour opened, up to the end of the intro. */
    private val intro = Animatable(0f)

    /** The sheet's coordinates, and the K's centre in them (where the sheet draws the vortex). */
    var sheet: LayoutCoordinates? = null
    var kCenter by mutableStateOf(Offset.Unspecified)

    /** Ms of idle motion (float, sparkles, vortex); runs only while the card is on screen. */
    var clock by mutableFloatStateOf(0f)
        private set

    fun sweep(): Float = phase(TourTokens.SWEEP_AT_MS, TourTokens.SWEEP_MS, FastOutSlowInEasing)
    fun sparkles(): Float = phase(TourTokens.SWEEP_AT_MS, TourTokens.SWEEP_MS, LinearEasing)
    fun vortex(): Float = phase(0, TourTokens.SWEEP_AT_MS, LinearOutSlowInEasing)
    fun title(): Float = phase(TourTokens.TEXT_AT_MS, TourTokens.TEXT_MS, LinearOutSlowInEasing)
    fun line(): Float = phase(TourTokens.TEXT_AT_MS + TourTokens.TEXT_STAGGER_MS, TourTokens.TEXT_MS, LinearOutSlowInEasing)

    private fun phase(start: Int, duration: Int, easing: Easing): Float =
        easing.transform(((intro.value - start) / duration).coerceIn(0f, 1f))

    /** The intro from the start, or straight to its end when animations are off. */
    suspend fun play(still: Boolean) = coroutineScope {
        if (still) {
            rise.snapTo(1f)
            intro.snapTo(TourTokens.INTRO_MS.toFloat())
            clock = STILL_CLOCK
            return@coroutineScope
        }
        rise.snapTo(0f)
        intro.snapTo(0f)
        clock = 0f
        launch {
            delay(TourTokens.K_RISE_DELAY_MS.toLong())
            rise.animateTo(1f, spring(TourTokens.K_RISE_DAMPING, TourTokens.K_RISE_STIFFNESS))
        }
        intro.animateTo(TourTokens.INTRO_MS.toFloat(), tween(TourTokens.INTRO_MS, easing = LinearEasing))
    }

    /** Advances the idle clock every frame until cancelled (the card left the screen). */
    suspend fun run() {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) clock += (now - last) / 1_000_000f
                last = now
            }
        }
    }

    private companion object {
        /** Animations off: the sparkles frozen mid-twinkle. */
        const val STILL_CLOCK = 1_300f
    }
}

/**
 * The welcome card's page: the K with its sparkles, the title and line in white (the sheet draws
 * the gradient and the vortex behind it all, [welcomeBackdrop]).
 * [leave]: 0 on the card … 1 once the next one has slid in.
 */
@Composable
fun WelcomePage(card: TourCard, motion: WelcomeMotion, leave: () -> Float) {
    val simple = !DeviceProfile.liveBlur
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageWidth = constraints.maxWidth.toFloat()
        val pageHeight = constraints.maxHeight.toFloat()
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(TourTokens.K)
                    // Where the sheet draws the vortex: the K's centre, measured with the card at rest.
                    .onGloballyPositioned { k ->
                        val sheet = motion.sheet ?: return@onGloballyPositioned
                        if (leave() == 0f && sheet.isAttached) {
                            motion.kCenter = sheet.localPositionOf(k, Offset(k.size.width / 2f, k.size.height / 2f))
                        }
                    },
            ) {
                Spacer(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = kLeaveX(leave(), pageWidth)
                            translationY = kRiseY(motion.rise.value, pageHeight) + kFloatY(motion.clock)
                            alpha = motion.sparkles() * (1f - 2f * leave()).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                        .sparkles(simple) { motion.clock },
                )
                Image(
                    painterResource(R.drawable.koan_k),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val r = motion.rise.value
                            val p = leave()
                            translationX = kLeaveX(p, pageWidth)
                            translationY = kRiseY(r, pageHeight) + kFloatY(motion.clock)
                            rotationZ = (1f - r) * TourTokens.K_START_TURN + p * TourTokens.K_LEAVE_TURN +
                                sin(motion.clock / TourTokens.FLOAT_TURN_MS * 2f * PI.toFloat()) * TourTokens.FLOAT_TURN
                            val s = (TourTokens.K_START_SCALE + (1f - TourTokens.K_START_SCALE) * r) * (1f - 0.1f * p)
                            scaleX = s
                            scaleY = s
                            alpha = (r * 2.5f).coerceIn(0f, 1f)
                            // The sweep paints over the K's own pixels only: its own layer while it runs.
                            val sweeping = motion.sweep() in 0.001f..0.999f
                            compositingStrategy = if (sweeping) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                        }
                        .lightSweep { motion.sweep() },
                )
            }
            Spacer(Modifier.height(TourTokens.WelcomeTextGap))
            val rise = TourTokens.TextRise
            Text(
                card.title,
                color = Color.White,
                fontSize = TourTokens.WelcomeTitleSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TourTokens.Padding)
                    .graphicsLayer {
                        val t = motion.title()
                        alpha = t
                        translationY = (1f - t) * rise.toPx()
                    },
            )
            Spacer(Modifier.height(TourTokens.TitleGap))
            Text(
                card.line,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = TourTokens.WelcomeLineSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TourTokens.Padding)
                    .graphicsLayer {
                        val t = motion.line()
                        alpha = t
                        translationY = (1f - t) * rise.toPx()
                    },
            )
            Spacer(Modifier.weight(0.75f))
        }
    }
}

/** From below the page into place (past it a little at the spring's peak). */
private fun kRiseY(rise: Float, pageHeight: Float): Float = (1f - rise) * pageHeight * 0.6f

private fun kLeaveX(leave: Float, pageWidth: Float): Float = -leave * pageWidth * TourTokens.K_LEAVE_EXTRA

private fun Density.kFloatY(clock: Float): Float =
    sin(clock / TourTokens.FLOAT_MS * 2f * PI.toFloat()) * TourTokens.FloatAmplitude.toPx()

/**
 * The welcome card's background, drawn by the whole sheet (no seam at the page's edges): the vivid
 * brand gradient and the vortex around the K, at [amount] (1 on the card, fading to the sheet's
 * own colour as the next card slides in, the vortex trailing behind the page).
 */
fun Modifier.welcomeBackdrop(dark: Boolean, motion: WelcomeMotion, amount: () -> Float): Modifier = drawWithCache {
    val gradient = Brush.linearGradient(BrandTokens.Colors, start = Offset.Zero, end = Offset(size.width, size.height))
    val vortex = Vortex(this, simple = !DeviceProfile.liveBlur)
    onDrawBehind {
        val a = amount()
        if (a <= 0.001f) return@onDrawBehind
        drawRect(gradient, alpha = a)
        if (dark) drawRect(Color.Black, alpha = a * TourTokens.WELCOME_DIM_DARK)
        val k = motion.kCenter
        if (k.isSpecified) {
            val trail = -(1f - a) * size.width * (1f - TourTokens.VORTEX_LEAVE_TRAIL)
            translate(k.x + trail, k.y) { vortex.draw(this, motion.clock, motion.vortex() * a) }
        }
    }
}

/**
 * Soft rings around the origin, each a white sweep (two bright arcs) turning with the clock; inner
 * rings turn faster. Each ring is drawn three times, narrower and brighter, for soft edges.
 */
private class Vortex(density: Density, simple: Boolean) {
    private val rings = if (simple) TourTokens.VORTEX_RINGS_SIMPLE else TourTokens.VORTEX_RINGS
    private val inner = with(density) { TourTokens.VortexInner.toPx() }
    private val step = with(density) { TourTokens.VortexStep.toPx() }
    private val brushes = List(rings) { i ->
        val a = TourTokens.VORTEX_ALPHA * (1f - 0.6f * i / rings)
        val w = Color.White
        Brush.sweepGradient(listOf(w.copy(alpha = 0f), w.copy(alpha = a), w.copy(alpha = 0f), w.copy(alpha = a * 0.55f), w.copy(alpha = 0f)), Offset.Zero)
    }
    private val glowRadius = inner * 1.4f
    private val glow = Brush.radialGradient(
        listOf(Color.White.copy(alpha = TourTokens.VORTEX_GLOW_ALPHA), Color.White.copy(alpha = 0f)),
        Offset.Zero,
        glowRadius,
    )
    private val strokes = with(density) { TourTokens.VortexStroke.toPx() }.let { listOf(Stroke(it), Stroke(it * 0.6f), Stroke(it * 0.25f)) }

    fun draw(scope: DrawScope, clock: Float, alpha: Float) = with(scope) {
        if (alpha <= 0.001f) return
        drawCircle(glow, glowRadius, Offset.Zero, alpha = alpha)
        val turn = clock / TourTokens.VORTEX_TURN_MS * 360f
        for (i in 0 until rings) {
            rotate(turn * (1.6f - 0.12f * i) + i * 47f, Offset.Zero) {
                for (s in strokes) drawCircle(brushes[i], inner + i * step, Offset.Zero, alpha = alpha, style = s)
            }
        }
    }
}

/** Where the sparkles sit around the K (fractions of its box), their size and twinkle. */
private class Sparkle(val x: Float, val y: Float, val size: Float, val periodMs: Float, val phase: Float)

private val Sparkles = listOf(
    Sparkle(-0.12f, 0.12f, 1.0f, 2_300f, 0.0f),
    Sparkle(1.06f, 0.02f, 0.75f, 2_900f, 1.7f),
    Sparkle(1.14f, 0.62f, 0.55f, 2_100f, 3.1f),
    Sparkle(0.02f, 1.02f, 0.6f, 2_600f, 4.4f),
    Sparkle(0.62f, -0.14f, 0.5f, 1_900f, 2.4f),
    Sparkle(0.86f, 1.1f, 0.85f, 3_200f, 5.2f),
)

/** Four-point stars (a soft dot behind each) that grow and fade in turn. */
private fun Modifier.sparkles(simple: Boolean, clock: () -> Float): Modifier = drawWithCache {
    val unit = 9.dp.toPx()
    // A four-point star of radius 1, its sides curved in.
    val star = Path().apply {
        moveTo(0f, -1f)
        quadraticTo(0.12f, -0.12f, 1f, 0f)
        quadraticTo(0.12f, 0.12f, 0f, 1f)
        quadraticTo(-0.12f, 0.12f, -1f, 0f)
        quadraticTo(-0.12f, -0.12f, 0f, -1f)
        close()
    }
    val halo = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f)), Offset.Zero, 1f)
    val shown = if (simple) Sparkles.take(TourTokens.SPARKLES_SIMPLE) else Sparkles
    onDrawBehind {
        val t = clock()
        for (sp in shown) {
            val wave = 0.5f + 0.5f * sin(t / sp.periodMs * 2f * PI.toFloat() + sp.phase)
            val level = wave * wave
            if (level < 0.02f) continue
            val r = unit * sp.size * (0.45f + 0.55f * level)
            translate(size.width * sp.x, size.height * sp.y) {
                scale(r * 1.8f, r * 1.8f, Offset.Zero) { drawCircle(halo, 1f, Offset.Zero, alpha = level) }
                scale(r, r, Offset.Zero) { drawPath(star, Color.White, alpha = level) }
            }
        }
    }
}

/** A diagonal band of light crossing the K once ([progress] 0 … 1), on its own pixels only. */
private fun Modifier.lightSweep(progress: () -> Float): Modifier = drawWithContent {
    drawContent()
    val p = progress()
    if (p <= 0.001f || p >= 0.999f) return@drawWithContent
    val band = size.width * 0.35f
    val x = -band + (size.width + 2f * band) * p
    val w = Color.White
    drawRect(
        Brush.linearGradient(
            listOf(w.copy(alpha = 0f), w.copy(alpha = 0.7f), w.copy(alpha = 0f)),
            start = Offset(x - band, size.height * 0.2f),
            end = Offset(x + band, size.height * 0.8f - band * 0.6f),
        ),
        blendMode = BlendMode.SrcAtop,
    )
}
