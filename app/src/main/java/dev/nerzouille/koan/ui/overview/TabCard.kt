package dev.nerzouille.koan.ui.overview

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
        PlaceholderThumbnail(
            card,
            Modifier
                .fillMaxWidth()
                .height(CardTokens.BlurSlice)
                .graphicsLayer { renderEffect = blurFor(depth()) },
        )
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
                .graphicsLayer { alpha = (1f - kotlin.math.abs(depth()) * 2f).coerceIn(0f, 1f) },
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
 * (docs/UI_SPEC.md "Visual restraint"). Real thumbnails replace this in M3.
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
