package dev.nerzouille.koan.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.lerp
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.ButtonTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.SheetTokens
import dev.nerzouille.koan.ui.theme.TourTokens
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * The tour (or a "What's new"): a tall sheet over Koan's dimmed UI, swipeable cards with Skip at
 * the top and the dots and Next at the bottom. The welcome card paints the whole sheet with the
 * brand gradient, which fades into the sheet's own colour as the next card slides in. Skip, Get
 * started or a swipe down on the sheet leave it, then [onClosed]; Back only steps to the previous
 * card.
 */
@Composable
fun TourOverlay(open: Boolean, sheet: TourSheet?, onOpenUrl: (String) -> Unit, onClosed: () -> Unit) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(open) { shown.animateTo(if (open) 1f else 0f, Motion.Sheet) }
    val visible by remember { derivedStateOf { open || shown.value > 0f } }
    val cards = sheet?.cards
    if (!visible || cards == null) return

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { cards.size }
    val context = LocalContext.current
    val still = remember { animationsOff(context) }
    val hasWelcome = cards.first().welcome
    val motion = remember { WelcomeMotion() }
    // How far the sheet is dragged down (px).
    val drag = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (!open) return@LaunchedEffect
        drag.snapTo(0f)
        pager.scrollToPage(0)
        if (hasWelcome) motion.play(still, pop = sheet is TourSheet.WhatsNew)
    }
    // The welcome card's idle motion runs only while it is (partly) on screen.
    val welcomeOnScreen by remember { derivedStateOf { hasWelcome && pager.currentPage + pager.currentPageOffsetFraction < 1f } }
    LaunchedEffect(open, welcomeOnScreen) { if (open && welcomeOnScreen && !still) motion.run() }
    // 1 on the welcome card … 0 once the next one is in (draw only).
    val welcome = { if (hasWelcome) 1f - (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f) else 0f }
    BackHandler(open && pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = Motion.PagerSettle) }
    }
    BackHandler(open && pager.currentPage == 0) {}
    val palette = KoanTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(palette.SheetScrim, alpha = shown.value * (1f - drag.value / size.height).coerceIn(0f, 1f)) }
            // Nothing behind it takes a touch.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(TourTokens.SHEET_FRACTION)
                .swipeDownToClose(drag) { if (open) onClosed() }
                .graphicsLayer {
                    // Dragged down, then on down to the edge as it closes.
                    translationY = drag.value + (1f - shown.value) * (size.height - drag.value)
                    clip = true
                    shape = RoundedCornerShape(topStart = SheetTokens.TopRadius, topEnd = SheetTokens.TopRadius)
                }
                .background(palette.Dock)
                .titleGlow(palette.isDark)
                .onGloballyPositioned { motion.sheet = it }
                .welcomeBackdrop(palette.isDark, motion, welcome)
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = TourTokens.SkipTop).height(TourTokens.SkipHeight).padding(horizontal = TourTokens.Padding / 2)) {
                TourButton("Skip", filled = false, welcome, Modifier.align(Alignment.CenterEnd)) { if (open) onClosed() }
            }
            if (sheet is TourSheet.WhatsNew) {
                Text(
                    "What's new in ${sheet.version}",
                    color = palette.TextSecondary,
                    fontSize = TourTokens.LineSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    // The intro card says it big: the header comes in as it leaves.
                    modifier = Modifier.fillMaxWidth().padding(bottom = TourTokens.TitleGap).graphicsLayer { alpha = 1f - welcome() },
                )
            }
            HorizontalPager(pager, modifier = Modifier.fillMaxWidth().weight(1f), beyondViewportPageCount = 0) { i ->
                val card = cards[i]
                if (card.welcome) {
                    WelcomePage(card, motion) { (pager.currentPage + pager.currentPageOffsetFraction - i).coerceIn(0f, 1f) }
                } else if (sheet is TourSheet.WhatsNew && i == cards.lastIndex) {
                    TourPage(card) { WhatsNewEnd { if (open) onOpenUrl(LATEST_RELEASE_URL) } }
                } else {
                    TourPage(card)
                }
            }
            Spacer(Modifier.height(TourTokens.Gap))
            PageDots(pager, welcome, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(TourTokens.Gap))
            val last = pager.currentPage == cards.lastIndex
            TourButton(if (!last) "Next" else if (sheet is TourSheet.WhatsNew) "Got it" else "Get started", filled = true, welcome, Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding)) {
                if (!open) return@TourButton
                if (last) onClosed() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = Motion.PagerSettle) }
            }
            Spacer(Modifier.height(TourTokens.Padding / 2))
        }
    }
}

/**
 * A vertical drag on the sheet moves it down 1:1 ([drag], px); past a quarter of its height or on
 * a fling down it closes ([onClose]), otherwise it springs back. A sideways drag is the pager's: it
 * takes it first, and this one lets go.
 */
private fun Modifier.swipeDownToClose(drag: Animatable<Float, *>, onClose: () -> Unit): Modifier = composed {
    val scope = rememberCoroutineScope()
    pointerInput(Unit) {
        val fling = SheetTokens.FlingVelocity.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                scope.launch { drag.snapTo((drag.value + over).coerceAtLeast(0f)) }
            } ?: return@awaitEachGesture
            tracker.addPosition(start.uptimeMillis, start.position)
            val ended = verticalDrag(start.id) { change ->
                tracker.addPosition(change.uptimeMillis, change.position)
                val dy = change.positionChange().y
                change.consume()
                scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) }
            }
            val v = if (ended) tracker.calculateVelocity().y else 0f
            if (drag.value > size.height * SheetTokens.COMMIT_FRACTION || v > fling) {
                onClose()
            } else {
                scope.launch { drag.animateTo(0f, Motion.Sheet) }
            }
        }
    }
}

/** One card, from the top: title, line, then the clip in its phone frame in all the room left. */
@Composable
private fun TourPage(card: TourCard, footer: (@Composable () -> Unit)? = null) {
    val palette = KoanTheme.colors
    val res = if (palette.isDark) card.dark else card.light
    val still = if (palette.isDark) card.stillDark else card.stillLight
    val media = res != null || still != null || card.scene != null
    // Without a clip the text sits in the middle, under its icon, instead of over an empty frame.
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (!media) Arrangement.Center else Arrangement.Top,
    ) {
        if (!media && card.icon != null) {
            Box(
                Modifier.size(TourTokens.IconTile).clip(RoundedCornerShape(TourTokens.IconTileRadius)).background(palette.GroupedRow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(card.icon, null, tint = palette.TextPrimary, modifier = Modifier.size(TourTokens.IconGlyph))
            }
            Spacer(Modifier.height(TourTokens.Gap * 1.5f))
        }
        Text(
            card.title,
            color = palette.TextPrimary,
            fontSize = TourTokens.TitleSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding),
        )
        Spacer(Modifier.height(TourTokens.TitleGap))
        // Room for two lines on every card, so the frames line up while swiping.
        Text(
            card.line,
            color = palette.TextSecondary,
            fontSize = TourTokens.LineSize,
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TourTokens.Padding),
        )
        if (media) {
            Spacer(Modifier.height(TourTokens.Gap))
            BoxWithConstraints(Modifier.fillMaxWidth(TourTokens.FRAME_WIDTH).weight(1f), contentAlignment = Alignment.Center) {
                // Shaped like its clip, as big as the room allows; a focus crop takes all the room.
                val bezels = TourTokens.FrameBezel * 2
                val aspect = card.framing.aspect
                val width = if (aspect == null) maxWidth else min(maxWidth, (maxHeight - bezels) * aspect + bezels)
                val height = if (aspect == null) maxHeight else min(maxHeight, (width - bezels) / aspect + bezels)
                PhoneFrame(Modifier.size(width, height)) { inner ->
                    if (card.scene == TourScene.SignIn) {
                        SignInScene(inner)
                    } else if (res != null) {
                        TourVideo(res, inner)
                    } else if (still != null) {
                        Image(painterResource(still), null, inner, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                    }
                }
            }
            Spacer(Modifier.height(TourTokens.FrameShadow / 2))
        }
        footer?.invoke()
    }
}

/** Under the last highlight: the full changelog on GitHub ([onChangelog]). */
@Composable
private fun WhatsNewEnd(onChangelog: () -> Unit) {
    val palette = KoanTheme.colors
    Spacer(Modifier.height(TourTokens.Gap))
    Row(
        Modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(palette.GroupedRow)
            .clickable(onClick = onChangelog)
            .padding(horizontal = ButtonTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KoanIcons.GitHub, null, tint = palette.TextPrimary, modifier = Modifier.size(TourTokens.ChangelogIcon))
        Spacer(Modifier.width(TourTokens.TitleGap * 1.5f))
        Text("See the full changelog", color = palette.TextPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}

/**
 * The recording in a phone frame (rounded, thin border, soft shadow, a slim bezel), so its dock
 * reads as a demo and never as Koan's own.
 */
@Composable
private fun PhoneFrame(modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    val palette = KoanTheme.colors
    val outer = RoundedCornerShape(TourTokens.FrameRadius)
    Box(
        modifier
            .shadow(TourTokens.FrameShadow, outer, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(outer)
            .background(palette.Card)
            .border(TourTokens.FrameBorder, palette.CardBorder, outer)
            .padding(TourTokens.FrameBezel),
    ) {
        val inner = Modifier.fillMaxSize().clip(RoundedCornerShape(TourTokens.FrameRadius - TourTokens.FrameBezel))
        content(inner.background(palette.GroupedRow))
    }
}

/** One dot per card; the current one stretches into a pill, following the finger (draw only). */
@Composable
private fun PageDots(pager: PagerState, welcome: () -> Float, modifier: Modifier) {
    val palette = KoanTheme.colors
    val count = pager.pageCount
    val dot = TourTokens.Dot
    val width = TourTokens.DotActiveWidth + (dot + TourTokens.DotGap) * (count - 1)
    Canvas(modifier.size(width, dot)) {
        val d = dot.toPx()
        val wide = TourTokens.DotActiveWidth.toPx()
        val gap = TourTokens.DotGap.toPx()
        val pos = pager.currentPage + pager.currentPageOffsetFraction
        val w = welcome()
        val on = lerpColor(palette.TextPrimary, Color.White, w)
        val off = lerpColor(palette.TextDisabled, Color.White.copy(alpha = 0.45f), w)
        var x = 0f
        for (i in 0 until count) {
            val near = (1f - abs(pos - i)).coerceIn(0f, 1f)
            val dw = lerp(d, wide, near)
            drawRoundRect(
                color = lerpColor(off, on, near),
                topLeft = Offset(x, 0f),
                size = Size(dw, d),
                cornerRadius = CornerRadius(d / 2),
            )
            x += dw + gap
        }
    }
}

/**
 * Next / Get started ([filled], the primary button) or Skip, recoloured for the gradient by
 * [welcome] (white with black text on it), in draw only.
 */
@Composable
private fun TourButton(text: String, filled: Boolean, welcome: () -> Float, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    Box(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .then(if (filled) Modifier.drawBehind { drawRect(lerpColor(palette.Primary, Color.White, FastOutSlowInEasing.transform(welcome()))) } else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = ButtonTokens.PaddingH),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = TextStyle(fontSize = ButtonTokens.TextSize, fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium),
            color = {
                // The label flips at half way, where the fill is mid-grey: it never fades into it.
                if (filled) (if (welcome() > 0.5f) Color.Black else palette.OnPrimary) else lerpColor(palette.TextPrimary, Color.White, welcome())
            },
        )
    }
}

/** The soft pink → violet → blue glow behind the title (the brand gradient, kept faint). */
private fun Modifier.titleGlow(dark: Boolean): Modifier = drawBehind {
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
