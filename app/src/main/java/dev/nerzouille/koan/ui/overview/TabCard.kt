package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanColors

val CardHeaderHeight = 52.dp

/** What a card needs to draw itself. Immutable so unchanged cards skip recomposition. */
@Immutable
data class CardModel(val id: String, val title: String, val url: String, val active: Boolean)

/**
 * One card of the depth stack. Depth-dependent effects (blur, dim, ✕ visibility) are read from
 * [depth] inside draw/layer lambdas only, so scrolling never recomposes the card.
 */
@Composable
fun TabCard(
    card: CardModel,
    depth: () -> Float,
    blurFor: (Float) -> RenderEffect?,
    modifier: Modifier = Modifier,
) {
    val look = faviconLook(card.url)
    Box(modifier.background(KoanColors.CardPlaceholder).background(look.color.copy(alpha = 0.08f))) {
        // Thumbnail: only its top slice is blurred. Behind the focus line nothing below that slice is
        // ever visible, and blurring the whole card costs ~5× the GPU time (measured on the POCO).
        PlaceholderThumbnail(
            card.url,
            Modifier
                .fillMaxWidth()
                .height(BlurSliceHeight)
                .graphicsLayer { renderEffect = blurFor(depth()) },
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val dim = DepthCurve.dim(depth())
                    if (dim > 0.01f) drawRect(Color.Black, alpha = dim)
                },
        )
        CardHeader(card, depth)
        // 1dp light-catch hairline on the top edge.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.10f)),
        )
    }
}

/** Taller than the biggest peek of a card behind the focus (d = 1). */
private val BlurSliceHeight = 300.dp

@Composable
private fun CardHeader(card: CardModel, depth: () -> Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(CardHeaderHeight)
            .background(Color.Black.copy(alpha = 0.35f))
            .drawWithContent {
                drawContent()
                val dim = DepthCurve.headerDim(depth())
                if (dim > 0.01f) drawRect(Color.Black, alpha = dim)
            }
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(card.url, 22.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            card.title,
            color = KoanColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (card.active) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(KoanColors.DefaultAccent))
        }
        Spacer(Modifier.weight(0.001f))
        // ✕ only on the focused card; the stack's gesture handler does the hit-testing.
        Box(
            Modifier
                .size(CloseButtonSize)
                .graphicsLayer { alpha = (1f - kotlin.math.abs(depth()) * 2f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Close, null, tint = KoanColors.TextPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

val CloseButtonSize = 44.dp

/**
 * Never-opened tab: dark neutral surface, big favicon, domain, 8% favicon tint. Nothing else
 * (docs/UI_SPEC.md "Visual restraint"). Real thumbnails replace this in M3.
 */
@Composable
private fun PlaceholderThumbnail(url: String, modifier: Modifier) {
    Column(
        modifier.padding(top = CardHeaderHeight + 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Favicon(url, 64.dp)
        Spacer(Modifier.height(14.dp))
        Text(displayHost(url), color = KoanColors.TextSecondary, fontSize = 14.sp, maxLines = 1)
    }
}
