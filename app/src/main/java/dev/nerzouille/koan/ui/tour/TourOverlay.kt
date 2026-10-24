package dev.nerzouille.koan.ui.tour

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanSheet
import dev.nerzouille.koan.ui.sheet.SheetNav
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.TourTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * The tour (or a "What's new") in a sheet: swipeable cards, Skip always there, page dots, Next then
 * Get started. However it closes (Skip, Get started, a drag down, Back), [onClosed] runs once.
 */
@Composable
fun TourOverlay(state: SheetState, sheet: TourSheet?, onClosed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val nav = remember { SheetNav(scope, Unit) }
    LaunchedEffect(state) {
        var wasOpen = false
        snapshotFlow { state.open }.collect { open ->
            if (wasOpen && !open) onClosed()
            wasOpen = open
        }
    }
    val cards = sheet?.cards ?: return
    KoanSheet(state, nav, compactMaxFraction = TourTokens.MAX_FRACTION) { _, page ->
        val pager = rememberPagerState { cards.size }
        val close = { state.animateTo(Detent.Closed) }
        Box(Modifier.fillMaxWidth().height(TourTokens.SkipHeight).padding(horizontal = TourTokens.Padding / 2)) {
            QuietButton("Skip", onClick = { if (page.interactive) close() }, modifier = Modifier.align(Alignment.CenterEnd))
        }
        if (sheet is TourSheet.WhatsNew) {
            Text(
                "What's new in ${sheet.version}",
                color = KoanTheme.colors.TextSecondary,
                fontSize = TourTokens.LineSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = TourTokens.TitleGap),
            )
        }
        HorizontalPager(
            pager,
            // The glow stays put behind the titles while the cards slide over it.
            modifier = Modifier.fillMaxWidth().titleGlow(KoanTheme.colors.isDark),
            beyondViewportPageCount = 0,
        ) { i -> TourPage(cards[i]) }
        Spacer(Modifier.height(TourTokens.Gap))
        PageDots(pager, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(TourTokens.Gap))
        val last = pager.currentPage == cards.lastIndex
        PrimaryButton(
            if (last) "Get started" else "Next",
            onClick = {
                if (!page.interactive) return@PrimaryButton
                if (last) close() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = Motion.PagerSettle) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding),
        )
        Spacer(Modifier.height(TourTokens.Padding))
    }
}

@Composable
private fun TourPage(card: TourCard) {
    val palette = KoanTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding)) {
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
        val res = if (palette.isDark) card.dark else card.light
        if (res != null) {
            Spacer(Modifier.height(TourTokens.Gap))
            TourAnimation(res, Modifier.fillMaxWidth().aspectRatio(TourTokens.ANIMATION_ASPECT))
        }
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
            .clip(RoundedCornerShape(TourTokens.AnimationRadius))
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

/** The soft pink → violet → blue glow behind the title (the brand gradient, kept faint). */
private fun Modifier.titleGlow(dark: Boolean): Modifier = drawBehind {
    val r = TourTokens.GlowRadius.toPx()
    val a = if (dark) TourTokens.GLOW_ALPHA_DARK else TourTokens.GLOW_ALPHA_LIGHT
    val y = TourTokens.TitleSize.toPx() * 0.6f
    val spots = listOf(
        BrandTokens.Pink to Offset(size.width * 0.25f, y),
        BrandTokens.Violet to Offset(size.width * 0.5f, y - r * 0.2f),
        BrandTokens.Blue to Offset(size.width * 0.75f, y),
    )
    for ((color, center) in spots) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), center, r), r, center)
    }
}
