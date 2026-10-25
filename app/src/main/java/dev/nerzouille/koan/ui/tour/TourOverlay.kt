package dev.nerzouille.koan.ui.tour

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.lerp
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.TourTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * The tour (or a "What's new"), full screen over everything: swipeable cards, bottom-anchored, on
 * a neutral page with the brand glow at the top. Skip (top right) or Get started leave it, then
 * [onClosed]; Back only steps to the previous card. It fades and rises in, fades out.
 */
@Composable
fun TourOverlay(open: Boolean, sheet: TourSheet?, onClosed: () -> Unit) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(open) { shown.animateTo(if (open) 1f else 0f, Motion.Sheet) }
    val visible by remember { derivedStateOf { open || shown.value > 0f } }
    val cards = sheet?.cards
    if (!visible || cards == null) return

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { cards.size }
    LaunchedEffect(open) { if (open) pager.scrollToPage(0) }
    BackHandler(open && pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = Motion.PagerSettle) }
    }
    BackHandler(open && pager.currentPage == 0) {}
    val palette = KoanTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = shown.value
                translationY = (1f - shown.value) * TourTokens.Gap.toPx() * 2
            }
            .background(palette.Base)
            .topGlow(palette.isDark)
            // Nothing behind it takes a touch.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(TourTokens.SkipHeight).padding(horizontal = TourTokens.Padding / 2)) {
            QuietButton("Skip", onClick = { if (open) onClosed() }, modifier = Modifier.align(Alignment.CenterEnd))
        }
        if (sheet is TourSheet.WhatsNew) {
            Text(
                "What's new in ${sheet.version}",
                color = palette.TextSecondary,
                fontSize = TourTokens.LineSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalPager(pager, modifier = Modifier.fillMaxWidth().weight(1f), beyondViewportPageCount = 0) { i ->
            TourPage(cards[i])
        }
        Spacer(Modifier.height(TourTokens.Gap))
        PageDots(pager, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(TourTokens.Gap))
        val last = pager.currentPage == cards.lastIndex
        PrimaryButton(
            if (last) "Get started" else "Next",
            onClick = {
                if (!open) return@PrimaryButton
                if (last) onClosed() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = Motion.PagerSettle) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding),
        )
        Spacer(Modifier.height(TourTokens.Padding / 2))
    }
}

/** One card, anchored to the bottom: [app icon], title, line, then the clip in its phone frame. */
@Composable
private fun TourPage(card: TourCard) {
    val palette = KoanTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = TourTokens.Padding)) {
        val iconBlock = if (card.appIcon) TourTokens.Icon + TourTokens.Gap * 1.5f else 0.dp
        val textBlock = TourTokens.TitleSize.value.dp * 1.4f + TourTokens.TitleGap + TourTokens.LineHeight + TourTokens.Gap
        // The frame fits what is left; too little room (a small screen with the icon) drops the clip.
        val frameHeight = min(maxHeight - iconBlock - textBlock, maxWidth * TourTokens.FRAME_MAX_WIDTH / TourTokens.ANIMATION_ASPECT)
        val res = if (palette.isDark) card.dark else card.light
        val clip = res != null && frameHeight >= MinFrameHeight
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
            if (card.appIcon) {
                AppIcon()
                Spacer(Modifier.height(TourTokens.Gap * 1.5f))
            }
            Text(
                card.title,
                color = palette.TextPrimary,
                fontSize = TourTokens.TitleSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(TourTokens.TitleGap))
            Text(
                card.line,
                color = palette.TextSecondary,
                fontSize = TourTokens.LineSize,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().height(TourTokens.LineHeight),
            )
            if (clip) {
                Spacer(Modifier.height(TourTokens.Gap))
                PhoneFrame(res!!, frameHeight)
            }
        }
    }
}

/** Below this the clip would be too small to read: the card shows text only. */
private val MinFrameHeight = 220.dp

@Composable
private fun AppIcon() {
    val glow = TourTokens.IconGlow
    Box(
        Modifier
            .size(TourTokens.Icon)
            .drawBehind {
                val r = glow.toPx()
                val c = center
                drawCircle(
                    Brush.radialGradient(
                        listOf(BrandTokens.Violet.copy(alpha = TourTokens.ICON_GLOW_ALPHA), BrandTokens.Pink.copy(alpha = TourTokens.ICON_GLOW_ALPHA / 3), Color.Transparent),
                        c,
                        r,
                    ),
                    r,
                    c,
                )
            },
    ) {
        Image(
            painterResource(R.drawable.koan_icon),
            null,
            Modifier.size(TourTokens.Icon).clip(RoundedCornerShape(TourTokens.IconRadius)),
        )
    }
}

/**
 * The recording in a phone frame (rounded, thin border, soft shadow, a slim bezel), so its dock
 * reads as a demo and never as Koan's own.
 */
@Composable
private fun PhoneFrame(@RawRes res: Int, height: Dp) {
    val palette = KoanTheme.colors
    val outer = RoundedCornerShape(TourTokens.FrameRadius)
    Box(
        Modifier
            .size(height * TourTokens.ANIMATION_ASPECT, height)
            .shadow(TourTokens.FrameShadow, outer, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(outer)
            .background(palette.Card)
            .border(TourTokens.FrameBorder, palette.CardBorder, outer)
            .padding(TourTokens.FrameBezel),
    ) {
        TourAnimation(res, Modifier.fillMaxSize().clip(RoundedCornerShape(TourTokens.FrameRadius - TourTokens.FrameBezel)))
    }
}

/**
 * A looping animated WebP, decoded only while its card is composed (the one on screen, and its
 * neighbour during a swipe) and dropped after. Drawn straight into the layer: on a hardware canvas
 * the render thread runs the frames itself.
 */
@Composable
private fun TourAnimation(@RawRes res: Int, modifier: Modifier) {
    val context = LocalContext.current
    val drawable by produceState<AnimatedImageDrawable?>(null, res) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, res)) }.getOrNull() as? AnimatedImageDrawable
        }
    }
    // Software fallback: the drawable asks for redraws through its callback (held here: it keeps it weakly).
    val frame = remember { mutableIntStateOf(0) }
    val callback = remember {
        val handler = Handler(Looper.getMainLooper())
        object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) { frame.intValue++ }
            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) { handler.postAtTime(what, who, `when`) }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) { handler.removeCallbacks(what, who) }
        }
    }
    DisposableEffect(drawable) {
        val d = drawable
        d?.callback = callback
        d?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        d?.start()
        onDispose {
            d?.stop()
            d?.callback = null
        }
    }
    val palette = KoanTheme.colors
    Box(
        modifier
            .background(palette.GroupedRow)
            .drawBehind {
                val d = drawable ?: return@drawBehind
                frame.intValue
                d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawIntoCanvas { d.draw(it.nativeCanvas) }
            },
    )
}

/** One dot per card; the current one stretches into a pill, following the finger (draw only). */
@Composable
private fun PageDots(pager: PagerState, modifier: Modifier) {
    val palette = KoanTheme.colors
    val count = pager.pageCount
    val dot = TourTokens.Dot
    val width = TourTokens.DotActiveWidth + (dot + TourTokens.DotGap) * (count - 1)
    Canvas(modifier.size(width, dot)) {
        val d = dot.toPx()
        val wide = TourTokens.DotActiveWidth.toPx()
        val gap = TourTokens.DotGap.toPx()
        val pos = pager.currentPage + pager.currentPageOffsetFraction
        var x = 0f
        for (i in 0 until count) {
            val near = (1f - abs(pos - i)).coerceIn(0f, 1f)
            val w = lerp(d, wide, near)
            drawRoundRect(
                color = lerpColor(palette.TextDisabled, palette.TextPrimary, near),
                topLeft = Offset(x, 0f),
                size = Size(w, d),
                cornerRadius = CornerRadius(d / 2),
            )
            x += w + gap
        }
    }
}

/** The soft pink → violet → blue glow at the top of the page (the brand gradient, kept faint). */
private fun Modifier.topGlow(dark: Boolean): Modifier = drawBehind {
    val h = TourTokens.GlowHeight.toPx()
    val a = if (dark) TourTokens.GLOW_ALPHA_DARK else TourTokens.GLOW_ALPHA_LIGHT
    val spots = listOf(
        BrandTokens.Pink to Offset(size.width * 0.1f, h * 0.15f),
        BrandTokens.Violet to Offset(size.width * 0.55f, h * 0.05f),
        BrandTokens.Blue to Offset(size.width * 0.95f, h * 0.3f),
    )
    for ((color, center) in spots) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), center, h * 0.75f), h * 0.75f, center)
    }
}
