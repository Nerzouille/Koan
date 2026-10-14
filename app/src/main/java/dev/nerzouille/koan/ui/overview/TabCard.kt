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
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    val dimOf = { d: Float -> (DepthCurve.dim(d) * palette.DimGain).coerceAtMost(palette.DimMax) }
    if (card.skeleton) return SkeletonCard(depth, dimColor, dimOf, modifier)
    val look = faviconLook(card.url)
    Box(modifier.background(palette.Card).background(look.color.copy(alpha = CardTokens.PLACEHOLDER_TINT))) {
        // Thumbnail: only its top slice is blurred. Behind the focus line nothing below that slice is
        // ever visible, and blurring the whole card costs ~5× the GPU time (measured on the POCO).
        val shot = card.thumb?.let { rememberScreenshot(it, depth) }
        PlaceholderThumbnail(
            card,
            Modifier
                .fillMaxWidth()
                .height(blurSlice())
                .graphicsLayer {
                    // Once the screenshot covers it, the placeholder is skipped (no blur buffer).
                    val covered = shot != null && shot.reveal.value >= 1f
                    alpha = if (covered) 0f else 1f
                    renderEffect = if (covered) null else blurFor(depth())
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
        CardHeader(card, depth, dimColor, accent)
        Box(Modifier.fillMaxSize().border(CardTokens.Hairline, palette.CardBorder, CardShape))
    }
}

private val CardShape = RoundedCornerShape(CardTokens.Radius)

@Composable
private fun CardHeader(card: CardModel, depth: () -> Float, dimColor: Color, accent: Color) {
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
                Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(accent))
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
/** A card's screenshot: decoded once near the visible range, then faded in over the placeholder. */
@Stable
private class ScreenshotState(initial: ImageBitmap?) {
    var image by mutableStateOf(initial)
    val reveal = Animatable(if (initial != null) 1f else 0f)
}

@Composable
private fun rememberScreenshot(key: String, depth: () -> Float): ScreenshotState {
    val near by remember { derivedStateOf { depth() < OFFSCREEN_DEPTH_FROM } }
    val state = remember(key) { ScreenshotState(ThumbnailStore.cached(key)) }
    if (state.image == null && near) {
        LaunchedEffect(key) {
            state.image = ThumbnailStore.load(key) ?: return@LaunchedEffect
            state.reveal.animateTo(1f, tween(ThumbTokens.FADE_MS))
        }
    }
    return state
}

@Composable
private fun Screenshot(state: ScreenshotState, depth: () -> Float, blurFor: (Float) -> RenderEffect?) {
    val reveal = state.reveal
    val bitmap = state.image ?: return
    val shot = Modifier.fillMaxWidth().padding(top = CardTokens.HeaderHeight)
    Image(
        bitmap, null,
        contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter, filterQuality = FilterQuality.Medium,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (blurFor(depth()) == null) reveal.value else 0f
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .then(shot),
    )
    Image(
        bitmap, null,
        contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter, filterQuality = FilterQuality.Low,
        modifier = Modifier
            .fillMaxWidth()
            .height(blurSlice())
            .graphicsLayer {
                val blur = blurFor(depth())
                alpha = if (blur != null) reveal.value else 0f
                renderEffect = blur
            }
            .then(shot),
    )
}

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
