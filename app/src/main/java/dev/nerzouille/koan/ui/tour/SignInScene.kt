package dev.nerzouille.koan.ui.tour

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import dev.nerzouille.koan.DeviceProfile
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SignInSceneTokens as T
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * "Sign in from your computer", drawn instead of recorded (no real account or code): a laptop
 * shows a made-up code, a phone slides in, a brand-gradient line scans the code, the code turns
 * into a check with a few sparkles and the K shows on the phone; a hold, then it loops. One clock
 * read in draw only: nothing recomposes per frame. Animations off: the finished frame, still.
 * Without live blur (Android 11) there are fewer sparkles; nothing here blurs anyway.
 */
@Composable
fun SignInScene(modifier: Modifier) {
    val palette = KoanTheme.colors
    val context = LocalContext.current
    val still = remember { animationsOff(context) }
    var clock by remember { mutableFloatStateOf(if (still) T.STILL_MS else 0f) }
    if (!still) {
        LaunchedEffect(Unit) {
            var last = -1L
            while (true) {
                withFrameNanos { now ->
                    if (last >= 0) clock = (clock + (now - last) / 1_000_000f) % T.LOOP_MS
                    last = now
                }
            }
        }
    }
    val k = painterResource(R.drawable.koan_k)
    val dark = palette.isDark
    val simple = !DeviceProfile.liveBlur
    Spacer(
        modifier.drawWithCache {
            val scene = Scene(size, dark, simple)
            onDrawBehind { scene.draw(this, clock) { s, a -> with(k) { draw(Size(s, s), alpha = a) } } }
        },
    )
}

/** Everything that only depends on the size and theme, built once; [draw] places it at a time. */
private class Scene(size: Size, dark: Boolean, private val simple: Boolean) {
    private val w = size.width
    private val h = size.height

    private val lid = if (dark) T.LidDark else T.LidLight
    private val display = if (dark) T.DisplayDark else T.DisplayLight
    private val base = if (dark) T.BaseDark else T.BaseLight
    private val code = if (dark) T.CodeDark else T.CodeLight
    private val mistAlpha = if (dark) T.MIST_ALPHA_DARK else T.MIST_ALPHA_LIGHT

    // The laptop, the phone in front of its right side, tilted: as wide as the frame allows with
    // even margins (the base's left end to the tilted phone's right edge), centred both ways.
    private val span = LID_W * BASE_W / 2f + LID_W * PHONE_X + tiltedHalfWidth(PHONE_W, PHONE_W * PHONE_H)
    private val unit = min(w * (1f - 2f * SIDE_MARGIN) / span, h * 0.78f)
    private val cx = (w - span * unit) / 2f + LID_W * BASE_W / 2f * unit
    private val lidW = unit * LID_W
    private val lidH = lidW * 0.64f
    private val phoneW = unit * PHONE_W
    private val phoneH = phoneW * PHONE_H
    private val phoneBelowLid = lidH * 0.6f + phoneH * 0.22f
    private val lidTopLeft = Offset(cx - lidW / 2f, (h - (phoneBelowLid + phoneH / 2f)) / 2f)
    private val bezel = unit * 0.022f
    private val baseW = lidW * BASE_W
    private val baseH = unit * 0.032f
    private val codeSize = lidH * 0.6f
    private val codeTopLeft = Offset(cx - codeSize / 2f, lidTopLeft.y + (lidH - codeSize) / 2f)
    private val phoneCenter = Offset(cx + lidW * PHONE_X, lidTopLeft.y + phoneBelowLid)

    private val modules = codeModules()
    private val module = codeSize / CODE_N
    private val brand = Brush.linearGradient(BrandTokens.Colors, codeTopLeft, codeTopLeft + Offset(codeSize, codeSize))
    private val scanBrush = Brush.horizontalGradient(
        listOf(Color.Transparent) + BrandTokens.Colors + Color.Transparent,
        startX = codeTopLeft.x - module * 2,
        endX = codeTopLeft.x + codeSize + module * 2,
    )
    private val mist = listOf(
        Triple(BrandTokens.Pink, Offset(w * 0.12f, h * 0.18f), unit * 0.75f),
        Triple(BrandTokens.Violet, Offset(w * 0.5f, h * 0.45f), unit * 0.8f),
        Triple(BrandTokens.Blue, Offset(w * 0.9f, h * 0.85f), unit * 0.75f),
    ).map { (c, at, r) -> Triple(Brush.radialGradient(listOf(c.copy(alpha = mistAlpha), Color.Transparent), at, r), at, r) }
    private val shadow = Brush.radialGradient(
        listOf(Color.Black.copy(alpha = if (dark) 0.35f else 0.12f), Color.Transparent),
        Offset(cx, lidTopLeft.y + lidH + baseH),
        baseW * 0.55f,
    )
    private val check = Path().apply {
        moveTo(-0.42f, 0.02f)
        lineTo(-0.12f, 0.32f)
        lineTo(0.45f, -0.3f)
    }
    private val checkLength = PathMeasure().apply { setPath(check, false) }.length
    private val star = Path().apply {
        moveTo(0f, -1f)
        quadraticTo(0.12f, -0.12f, 1f, 0f)
        quadraticTo(0.12f, 0.12f, 0f, 1f)
        quadraticTo(-0.12f, 0.12f, -1f, 0f)
        quadraticTo(-0.12f, -0.12f, 0f, -1f)
        close()
    }

    fun draw(scope: DrawScope, t: Float, drawK: DrawScope.(size: Float, alpha: Float) -> Unit) = with(scope) {
        val reset = phase(t, T.RESET_AT, T.RESET_MS, FastOutSlowInEasing)
        val phoneIn = phase(t, T.PHONE_IN_AT, T.PHONE_IN_MS, LinearOutSlowInEasing) * (1f - reset)
        val scan = phase(t, T.SCAN_AT, T.SCAN_MS, FastOutSlowInEasing)
        val done = phase(t, T.CHECK_AT, T.CHECK_MS, FastOutSlowInEasing) * (1f - reset)
        val kIn = phase(t, T.K_AT, T.K_MS, FastOutSlowInEasing) * (1f - reset)

        for ((brush, at, r) in mist) drawCircle(brush, r, at)
        drawOval(shadow, Offset(cx - baseW * 0.55f, lidTopLeft.y + lidH - baseH), Size(baseW * 1.1f, baseH * 5f))
        drawLaptop()
        drawCode(1f - done)
        if (scan > 0f && done < 1f) drawScan(scan)
        if (done > 0f) drawCheck(done, t)
        if (phoneIn > 0f) drawPhone(phoneIn, kIn, drawK)
    }

    private fun DrawScope.drawLaptop() {
        val lidSize = Size(lidW, lidH)
        drawRoundRect(lid, lidTopLeft, lidSize, CornerRadius(unit * 0.035f))
        drawRoundRect(
            display,
            lidTopLeft + Offset(bezel, bezel),
            Size(lidW - 2 * bezel, lidH - 2 * bezel),
            CornerRadius(unit * 0.02f),
        )
        val baseTop = lidTopLeft.y + lidH
        drawRoundRect(base, Offset(cx - baseW / 2f, baseTop), Size(baseW, baseH), CornerRadius(baseH / 2f))
        drawRoundRect(lid, Offset(cx - baseW * 0.09f, baseTop), Size(baseW * 0.18f, baseH * 0.38f), CornerRadius(baseH * 0.2f))
    }

    /** The made-up code, fading and shrinking towards its middle as it turns into the check. */
    private fun DrawScope.drawCode(shown: Float) {
        if (shown <= 0.001f) return
        val center = codeTopLeft + Offset(codeSize / 2f, codeSize / 2f)
        scale(0.85f + 0.15f * shown, center) {
            val gap = module * 0.12f
            for (i in modules.indices) {
                if (!modules[i]) continue
                val x = codeTopLeft.x + (i % CODE_N) * module
                val y = codeTopLeft.y + (i / CODE_N) * module
                drawRoundRect(code, Offset(x + gap, y + gap), Size(module - 2 * gap, module - 2 * gap), CornerRadius(module * 0.25f), alpha = shown)
            }
        }
    }

    /** The gradient line going down the code, a soft glow trailing above it. */
    private fun DrawScope.drawScan(p: Float) {
        val edge = (min(p, 1f - p) * 8f).coerceIn(0f, 1f)
        val y = codeTopLeft.y - module + (codeSize + 2 * module) * p
        val left = codeTopLeft.x - module * 2
        val width = codeSize + module * 4
        val trail = codeSize * 0.28f
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, BrandTokens.Violet.copy(alpha = 0.22f)), startY = y - trail, endY = y),
            Offset(left, y - trail),
            Size(width, trail),
            alpha = edge,
        )
        drawRoundRect(scanBrush, Offset(left, y - module * 0.2f), Size(width, module * 0.4f), CornerRadius(module * 0.2f), alpha = edge)
    }

    /** A brand-gradient disc where the code was, its check drawn on, sparkles twinkling around. */
    private fun DrawScope.drawCheck(p: Float, t: Float) {
        val center = codeTopLeft + Offset(codeSize / 2f, codeSize / 2f)
        val r = codeSize * 0.36f
        // A little past full size, then back (ease out back).
        val s = 1f + 2.2f * (p - 1f) * (p - 1f) * (p - 1f) + 1.2f * (p - 1f) * (p - 1f)
        drawCircle(Brush.radialGradient(listOf(BrandTokens.Violet.copy(alpha = 0.3f * p), Color.Transparent), center, r * 2.2f), r * 2.2f, center)
        scale(s, center) {
            drawCircle(brand, r, center, alpha = p)
            val drawn = Path()
            PathMeasure().apply { setPath(check, false) }.getSegment(0f, checkLength * p, drawn, true)
            translate(center.x, center.y) {
                scale(r, r, Offset.Zero) {
                    drawPath(drawn, Color.White, style = Stroke(0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        val shown = if (simple) SPARKLES.take(2) else SPARKLES
        for (sp in shown) {
            val wave = 0.5f + 0.5f * sin(t / sp.periodMs * 2f * PI.toFloat() + sp.phase)
            val level = wave * wave * p
            if (level < 0.02f) continue
            val size = r * 0.16f * sp.size * (0.5f + 0.5f * level)
            translate(center.x + sp.x * r, center.y + sp.y * r) {
                scale(size, size, Offset.Zero) { drawPath(star, BrandTokens.Violet.copy(alpha = 0.9f), alpha = level) }
            }
        }
    }

    /** The phone rising into place, tilted a little; its camera view, then the K. */
    private fun DrawScope.drawPhone(p: Float, kIn: Float, drawK: DrawScope.(size: Float, alpha: Float) -> Unit) {
        val c = phoneCenter + Offset(0f, (1f - p) * h * 0.55f)
        val topLeft = c - Offset(phoneW / 2f, phoneH / 2f)
        rotate(-PHONE_TILT - PHONE_TILT * (1f - p), c) {
            // A soft oval shadow, a little down and right, fading out before its edge.
            val shadowAt = c + Offset(phoneW * 0.1f, phoneH * 0.06f)
            val shadowR = phoneH * 0.6f
            scale(phoneW / phoneH * 1.5f, 1f, shadowAt) {
                drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.2f * p), Color.Transparent), shadowAt, shadowR), shadowR, shadowAt)
            }
            drawRoundRect(T.PhoneBody, topLeft, Size(phoneW, phoneH), CornerRadius(phoneW * 0.17f), alpha = p)
            val inset = phoneW * 0.045f
            val screenTopLeft = topLeft + Offset(inset, inset)
            val screen = Size(phoneW - 2 * inset, phoneH - 2 * inset)
            drawRoundRect(
                Brush.verticalGradient(listOf(T.PhoneScreenTop, T.PhoneScreenBottom), startY = screenTopLeft.y, endY = screenTopLeft.y + screen.height),
                screenTopLeft,
                screen,
                CornerRadius(phoneW * 0.13f),
                alpha = p,
            )
            // The camera's frame: four corners, fading out as the K comes.
            val box = screen.width * 0.62f
            val boxTopLeft = Offset(c.x - box / 2f, c.y - box / 2f)
            val arm = box * 0.22f
            val corners = Path().apply {
                for ((x, y, dx, dy) in listOf(
                    listOf(0f, 0f, 1f, 1f), listOf(box, 0f, -1f, 1f), listOf(0f, box, 1f, -1f), listOf(box, box, -1f, -1f),
                )) {
                    moveTo(boxTopLeft.x + x, boxTopLeft.y + y + dy * arm)
                    lineTo(boxTopLeft.x + x, boxTopLeft.y + y)
                    lineTo(boxTopLeft.x + x + dx * arm, boxTopLeft.y + y)
                }
            }
            drawPath(corners, Color.White, alpha = 0.7f * p * (1f - kIn), style = Stroke(phoneW * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (kIn > 0f) {
                val glowR = screen.width * 0.6f
                drawCircle(Brush.radialGradient(listOf(BrandTokens.Violet.copy(alpha = 0.45f * kIn), Color.Transparent), c, glowR), glowR, c)
                // Placed by its optical centre (where its weight is), not its box: the K's thick
                // stroke is on the left.
                val s = screen.width * 0.62f * (0.8f + 0.2f * kIn)
                translate(c.x - s * K_CENTER_X, c.y - s * K_CENTER_Y) { drawK(s, kIn) }
            }
        }
    }

    private fun phase(t: Float, at: Float, ms: Float, easing: androidx.compose.animation.core.Easing): Float =
        easing.transform(((t - at) / ms).coerceIn(0f, 1f))

    /** A made-up 21 × 21 code: the three corner squares of a QR code, the rest a fixed scatter. */
    private fun codeModules(): BooleanArray = BooleanArray(CODE_N * CODE_N) { i ->
        val x = i % CODE_N
        val y = i / CODE_N
        val finder = listOf(0 to 0, CODE_N - 7 to 0, 0 to CODE_N - 7).firstOrNull { (fx, fy) -> x - fx in 0..6 && y - fy in 0..6 }
        if (finder != null) {
            val dx = x - finder.first
            val dy = y - finder.second
            val ring = minOf(dx, dy, 6 - dx, 6 - dy)
            ring != 1
        } else if (listOf(0 to 0, CODE_N - 7 to 0, 0 to CODE_N - 7).any { (fx, fy) -> x - fx in -1..7 && y - fy in -1..7 }) {
            false
        } else {
            ((x * 73 + y * 151 + x * y * 7) % 11) < 5
        }
    }

    /** Half the width of a w × h box turned by [PHONE_TILT]. */
    private fun tiltedHalfWidth(w: Float, h: Float): Float {
        val a = PHONE_TILT / 180f * PI.toFloat()
        return w / 2f * cos(a) + h / 2f * sin(a)
    }

    private class Spark(val x: Float, val y: Float, val size: Float, val periodMs: Float, val phase: Float)

    private companion object {
        const val CODE_N = 21

        /** Sizes as shares of the scene's unit; the phone's height and place from its width and the lid. */
        const val LID_W = 0.8f
        const val BASE_W = 1.16f
        const val PHONE_W = 0.3f
        const val PHONE_H = 2.02f
        const val PHONE_X = 0.45f
        const val PHONE_TILT = 6f
        const val SIDE_MARGIN = 0.07f

        /** The K image's alpha centroid, as shares of its size (R.drawable.koan_k). */
        const val K_CENTER_X = 0.46f
        const val K_CENTER_Y = 0.511f
        val SPARKLES = listOf(
            Spark(-1.35f, -0.85f, 1.0f, 1_500f, 0.4f),
            Spark(1.3f, -1.05f, 0.75f, 1_800f, 2.0f),
            Spark(1.45f, 0.55f, 0.6f, 1_300f, 3.4f),
            Spark(-1.25f, 0.95f, 0.7f, 1_700f, 5.0f),
        )
    }
}
