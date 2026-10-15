package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.theme.ThumbTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.StackTokens
import dev.nerzouille.koan.thumbs.BakedBlur
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import dev.nerzouille.koan.favicon.IconStore
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * What a card needs to draw itself. Immutable so unchanged cards skip recomposition. [pinned] =
 * a live pinned/essential tab: shown with a pin glyph, closing it only unloads it.
 */
@Immutable
data class CardModel(
    val id: String,
    val title: String,
    val url: String,
    val active: Boolean,
    val pinned: Boolean = false,
    /** Favicon reference (see TabEntity.iconUrl). */
    val icon: String? = null,
    val staticIcon: Boolean = false,
    /** > 1 for a Zen split view shown as one card. */
    val splitCount: Int = 1,
    /** Placeholder while the first sync runs: soft bars instead of title and favicon. */
    val skeleton: Boolean = false,
    /** Key of the page's last screenshot (ThumbnailStore); null = never captured, placeholder. */
    val thumb: String? = null,
)

/**
 * One card of the depth stack. Depth-dependent effects (blur, dim, ✕ visibility) are read inside
 * draw/layer lambdas only, so scrolling never recomposes the card. Back cards fade towards
 * [dimColor]. The contact shadow it casts is a separate layer: [ContactShadow].
 */
@Composable
fun TabCard(
    card: CardModel,
    depth: () -> Float,
    blurFor: (Float) -> RenderEffect?,
    dimColor: Color,
    modifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    val dimOf = { d: Float -> (DepthCurve.dim(d) * palette.DimGain).coerceAtMost(palette.DimMax) }
    if (card.skeleton) return SkeletonCard(depth, dimColor, dimOf, modifier)
    val look = faviconLook(card.url)
    Box(modifier.background(palette.Card).background(look.color.copy(alpha = CardTokens.PLACEHOLDER_TINT))) {
        // Thumbnail: only its top slice is blurred. Behind the focus line nothing below that slice is
        // ever visible, and blurring the whole card costs ~5× the GPU time (measured on the POCO).
        val shot = card.thumb?.let { rememberScreenshot(it, depth, blurFor) }
        // Deep in the pile the placeholder is captured once (with the card colour under it) and
        // crossfaded through baked blurred copies of that capture, like a screenshot. It blurs live
        // until they are uploaded.
        val tint = look.color.copy(alpha = CardTokens.PLACEHOLDER_TINT)
        // A layer to capture into exists only while a capture is being taken (not one per card).
        var capturing by remember { mutableStateOf(false) }
        val capture = if (capturing) rememberGraphicsLayer() else null
        val captureLayer = rememberUpdatedState(capture)
        // Keyed on everything the placeholder draws (host, favicon, split count, theme colours), so
        // any change re-bakes instead of showing a stale blur. The title is in the header, not here.
        val bakeKey = "placeholder:${card.id}:${card.url}:${card.icon}:${card.staticIcon}:${card.splitCount}:" +
            "${palette.Card.value}:${palette.TextPrimary.value}:${palette.TextSecondary.value}:${palette.IconBacking.value}"
        var baked by remember(bakeKey) { mutableStateOf(BakedBlur.cached(bakeKey)) }
        if (baked == null && shot == null) {
            val density = LocalDensity.current.density
            LaunchedEffect(bakeKey) {
                // Once it shows blurred (so it is drawn, into the capture layer once there is one).
                snapshotFlow { blurFor(depth()) != null }.first { it }
                // Don't bake the letter tile while the favicon is still loading: wait for it and its fade.
                val iconKey = if (card.staticIcon) null else IconStore.keyFor(card.url, card.icon)
                if (iconKey != null && IconStore.cached(iconKey) == null) {
                    if (IconStore.load(card.url, card.icon) != null) delay(ICON_SETTLE_MS)
                }
                baked = try {
                    BakedBlur.capture(bakeKey, BakedBlur.radii(density)) {
                        capturing = true
                        // Recomposed with the layer, then drawn into it.
                        val layer = snapshotFlow { captureLayer.value }.filterNotNull().first()
                        withFrameNanos { }
                        withFrameNanos { }
                        layer.toImageBitmap()
                    }
                } finally {
                    capturing = false
                }
            }
        }
        PlaceholderThumbnail(
            card,
            Modifier
                .fillMaxWidth()
                .height(blurSlice())
                .graphicsLayer {
                    // Once the screenshot covers it, the placeholder is skipped.
                    val covered = shot != null && shot.reveal.value >= 1f
                    alpha = if (covered) 0f else 1f
                    renderEffect = if (covered || baked != null) null else blurFor(depth())
                }
                .drawWithContent {
                    capture?.record {
                        drawRect(palette.Card)
                        drawRect(tint)
                        this@drawWithContent.drawContent()
                    }
                    val levels = baked
                    val x = if (levels == null) 0f else bakedLevel(depth(), blurFor) * levels.size
                    if (levels == null || x < 1f) {
                        if (capture != null) drawLayer(capture) else drawContent()
                    }
                    levels?.forEachIndexed { i, image ->
                        val a = (x - i).coerceIn(0f, 1f)
                        if (a > 0f) drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = a, filterQuality = FilterQuality.Low)
                    }
                },
        )
        if (shot != null) Screenshot(shot, depth, blurFor)
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val dim = dimOf(depth())
                    if (dim > 0.01f) drawRect(dimColor, alpha = dim)
                },
        )
        CardHeader(card, depth, dimColor)
        Box(Modifier.fillMaxSize().border(CardTokens.Hairline, palette.CardBorder, CardShape))
    }
}

private val CardShape = RoundedCornerShape(CardTokens.Radius)

@Composable
private fun CardHeader(card: CardModel, depth: () -> Float, dimColor: Color) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(CardTokens.HeaderHeight)
            .background(palette.CardStrip)
            .drawWithContent {
                drawContent()
                val dim = (DepthCurve.headerDim(depth()) * palette.DimGain).coerceAtMost(palette.DimMax)
                if (dim > 0.01f) drawRect(dimColor, alpha = dim)
            }
            .padding(start = CardTokens.HeaderPaddingStart, end = CardTokens.HeaderPaddingEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(card.url, CardTokens.Favicon, icon = card.icon, staticIcon = card.staticIcon)
        Spacer(Modifier.width(CardTokens.HeaderGap))
        // Title + markers take the free width, so the ✕ always sits at the right edge.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                card.title,
                color = KoanTheme.colors.TextPrimary,
                fontSize = CardTokens.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (card.pinned) {
                Spacer(Modifier.width(CardTokens.HeaderGap))
                Icon(KoanIcons.Pin, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(CardTokens.PinGlyph))
            }
            if (card.active) {
                Spacer(Modifier.width(CardTokens.HeaderGap))
                CurrentTabDot()
            }
        }
        // ✕ only on the focused card; the stack's gesture handler does the hit-testing.
        Box(
            Modifier
                .size(CardTokens.CloseTouch)
                .graphicsLayer {
                    alpha = (1f - kotlin.math.abs(depth()) * 2f).coerceIn(0f, 1f)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(CardTokens.CloseButton).clip(CircleShape).background(palette.CloseButton),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Close, null, tint = palette.CloseIcon, modifier = Modifier.size(CardTokens.CloseIcon))
            }
        }
    }
}

/**
 * Never-opened tab: dark neutral surface, big favicon, domain, 8% favicon tint. Nothing else
 * (docs/UI_SPEC.md "Visual restraint"). The real screenshot covers it once there is one.
 */
@Composable
private fun PlaceholderThumbnail(card: CardModel, modifier: Modifier) {
    Column(
        modifier.padding(top = CardTokens.HeaderHeight + CardTokens.PlaceholderTop),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Favicon(card.url, CardTokens.PlaceholderFavicon, icon = card.icon, staticIcon = card.staticIcon)
        Spacer(Modifier.height(CardTokens.PlaceholderGap))
        val host = displayHost(card.url)
        Text(
            if (card.splitCount > 1) "$host  ·  split view of ${card.splitCount}" else host,
            color = KoanTheme.colors.TextSecondary,
            fontSize = CardTokens.PlaceholderDomainSize,
            maxLines = 1,
        )
    }
}

/**
 * The page's real screenshot over the placeholder, top-aligned under the header. Two layers of the
 * same image: full and sharp while the card is near the focus, only its top slice and blurred
 * deeper in the pile (blurring the whole card costs ~5×). Decoded off the main thread, only while
 * the card is near the visible range ([depth] is constant beyond it), and faded in.
 */
/**
 * A card's screenshot: decoded once near the visible range, then faded in over the placeholder.
 * A fresher screenshot (new key) fades in over the one shown: once a card has a screenshot it never
 * goes back to its placeholder.
 */
@Stable
private class ScreenshotState(key: String, initial: ImageBitmap?, baked: List<ImageBitmap>?) {
    /** The key of [image] (it lags the card's key while a fresher screenshot loads). */
    var shownKey by mutableStateOf(key)
    var image by mutableStateOf(initial)
    val reveal = Animatable(if (initial != null) 1f else 0f)

    /** The screenshot being replaced, drawn under [image] while it fades in. */
    var previous by mutableStateOf<ImageBitmap?>(null)

    /** A screenshot is fading in (keeps the effect that runs the fade alive until it ends). */
    var fading by mutableStateOf(false)

    /** Pre-blurred copies of the top slice, weakest first (thumbs/BakedBlur); null until baked. */
    var baked by mutableStateOf(baked)
}

@Composable
private fun rememberScreenshot(key: String, depth: () -> Float, blurFor: (Float) -> RenderEffect?): ScreenshotState {
    val near by remember { derivedStateOf { depth() < OFFSCREEN_DEPTH_FROM } }
    val state = remember { ScreenshotState(key, ThumbnailStore.cached(key), BakedBlur.cached(key)) }
    if (state.shownKey != key || state.image == null || state.fading) {
        LaunchedEffect(key) {
            snapshotFlow { near }.first { it }
            // Until it is decoded, the screenshot shown (if any) stays.
            val next = ThumbnailStore.load(key) ?: return@LaunchedEffect
            state.fading = true
            state.previous = state.image.takeIf { state.shownKey != key }
            state.image = next
            state.baked = BakedBlur.cached(key)
            state.shownKey = key
            state.reveal.snapTo(0f)
            state.reveal.animateTo(1f, tween(ThumbTokens.FADE_MS))
            state.previous = null
            state.fading = false
        }
    }
    if (state.baked == null) {
        LaunchedEffect(key) {
            // Uploaded (baked at capture) once the card shows blurred; it blurs live until then.
            snapshotFlow { blurFor(depth()) != null }.first { it }
            state.baked = BakedBlur.screenshot(key)
        }
    }
    return state
}

@Composable
private fun Screenshot(state: ScreenshotState, depth: () -> Float, blurFor: (Float) -> RenderEffect?) {
    val reveal = state.reveal
    val bitmap = state.image ?: return
    val shot = Modifier.fillMaxWidth().padding(top = CardTokens.HeaderHeight)
    // The sharp screenshot, full width from the top; a screenshot being replaced stays under it
    // while it fades in.
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Hidden once the blurred slice covers it (nothing below the slice shows): the first
                // baked copy fully faded in, or any live blur.
                val hidden = if (state.baked != null) bakedLevel(depth(), blurFor) >= 1f else blurFor(depth()) != null
                alpha = if (hidden) 0f else 1f
                clip = true
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .then(shot)
            .drawBehind {
                state.previous?.let { drawTopWidth(it, 1f, FilterQuality.Medium) }
                drawTopWidth(bitmap, reveal.value, FilterQuality.Medium)
            },
    )
    // The blurred slice: baked copies crossfaded by depth (no GPU buffer per card), or a live blur
    // of the screenshot until they are uploaded. One plain box, so composing a card stays cheap.
    Box(
        Modifier
            .fillMaxWidth()
            .height(blurSlice())
            .then(shot)
            .graphicsLayer {
                renderEffect = if (state.baked == null) blurFor(depth()) else null
                clip = true
            }
            .drawBehind {
                val levels = state.baked
                if (levels == null) {
                    if (blurFor(depth()) != null) {
                        val h = (bitmap.height * size.width / bitmap.width).roundToInt()
                        drawImage(bitmap, dstSize = IntSize(size.width.roundToInt(), h), alpha = reveal.value, filterQuality = FilterQuality.Low)
                    }
                    return@drawBehind
                }
                val x = bakedLevel(depth(), blurFor) * levels.size
                levels.forEachIndexed { i, image ->
                    val a = (x - i).coerceIn(0f, 1f) * reveal.value
                    if (a > 0f) {
                        val h = (image.height * size.width / image.width).roundToInt()
                        drawImage(image, dstSize = IntSize(size.width.roundToInt(), h), alpha = a, filterQuality = FilterQuality.Low)
                    }
                }
            },
    )
}

/** Draws [image] scaled to the full width, from the top (clipped by the layer below). */
private fun DrawScope.drawTopWidth(image: ImageBitmap, alpha: Float, quality: FilterQuality) {
    if (alpha <= 0f) return
    val h = (image.height * size.width / image.width).roundToInt()
    drawImage(image, dstSize = IntSize(size.width.roundToInt(), h), alpha = alpha, filterQuality = quality)
}

/** 0 sharp … 1 blurred as deep as the pile goes; 0 wherever [blurFor] says no blur (off screen). */
private fun bakedLevel(d: Float, blurFor: (Float) -> RenderEffect?): Float =
    if (blurFor(d) == null) 0f else DepthCurve.blurDp(d) / MAX_BLUR_DP

private val MAX_BLUR_DP = StackTokens.BlurDp.max()


/** Favicon fade (Favicon.kt, 140 ms) plus a few frames, before a placeholder is captured. */
private const val ICON_SETTLE_MS = 250L

@Composable
private fun blurSlice() = with(LocalDensity.current) { CardTokens.BLUR_SLICE_PX.toDp() }

/** Beyond this depth a card is off screen (DepthStack hands it a constant far depth). */
private const val OFFSCREEN_DEPTH_FROM = 50f

/** A card-shaped placeholder: header bars where favicon and title will be, nothing else. */
@Composable
private fun SkeletonCard(depth: () -> Float, dimColor: Color, dimOf: (Float) -> Float, modifier: Modifier) {
    Box(
        modifier
            .background(KoanTheme.colors.Card)
            .drawBehind {
                val dim = dimOf(depth())
                if (dim > 0.01f) drawRect(dimColor, alpha = dim)
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(CardTokens.HeaderHeight)
                .background(KoanTheme.colors.CardStrip)
                .padding(start = CardTokens.HeaderPaddingStart, end = CardTokens.HeaderPaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(CardTokens.Favicon).clip(RoundedCornerShape(CardTokens.Favicon * 0.28f)).background(KoanTheme.colors.Skeleton))
            Spacer(Modifier.width(CardTokens.HeaderGap))
            Box(Modifier.width(CardTokens.SkeletonTitle).height(CardTokens.SkeletonBar).clip(CircleShape).background(KoanTheme.colors.Skeleton))
        }
        Box(Modifier.fillMaxSize().border(CardTokens.Hairline, KoanTheme.colors.CardBorder, CardShape))
    }
}

/** The current tab is a state, not a moment: a small neutral dot, never the brand gradient. */
@Composable
internal fun CurrentTabDot() {
    Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanTheme.colors.TextSecondary))
}
