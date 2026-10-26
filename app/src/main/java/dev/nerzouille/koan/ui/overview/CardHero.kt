package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import dev.nerzouille.koan.ui.browser.CoverTarget
import dev.nerzouille.koan.ui.browser.PagePreview
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import kotlin.math.max

/**
 * Page ↔ overview without a double image: the overview is opaque, and the tab's card flies
 * between the page and its slot in the stack over it. At the page end it is the page's screenshot
 * full size (its header just above the screen); at the stack end it is exactly the card. The real
 * card hides meanwhile. Without a card to land on (a new tab, a stack not on screen) the page
 * shrinks towards the middle instead.
 */
@Stable
class CardHero {
    /** The card the transition flies to or from; null = none. */
    var cardId by mutableStateOf<String?>(null)

    /** The page view's bounds in the window (px). */
    var pageRect by mutableStateOf(Rect.Zero)

    /** Card size at the focus (px), from the stack. */
    var cardSize by mutableStateOf(Size.Zero)

    /** The overview's content, inside its zoom: card bounds are measured from it. */
    var overview: LayoutCoordinates? = null

    /** Page ↔ overview, 0 = page … 1 = overview (read in layers and draw only). */
    var progress: () -> Float = { 0f }

    /** The overview is drawn (open, or still moving); read in layers and draw only. */
    var overviewShown: () -> Boolean = { false }

    /**
     * The card flies: the overview is drawn but not settled open. That includes progress 0 (the
     * overview's first frame), where the card covers it as the page. The real card stays hidden.
     */
    val flying: Boolean
        get() = overviewShown() && progress() < 1f

    private val stacks = mutableStateMapOf<Any, (String) -> Rect?>()

    /** A stack on screen tells where its cards are (in the overview's unzoomed coordinates). */
    fun register(key: Any, rectOf: (String) -> Rect?) {
        stacks[key] = rectOf
    }

    fun unregister(key: Any) {
        stacks.remove(key)
    }

    fun hides(id: String) = flying && id == cardId

    /** Where [cardId] sits in the stack now, unzoomed; null if no stack on screen shows it. */
    fun cardRect(): Rect? {
        val id = cardId ?: return null
        return stacks.values.firstNotNullOfOrNull { it(id) }
    }
}

val LocalCardHero = staticCompositionLocalOf { CardHero() }

/**
 * The flying card, drawn over the overview while [progress] (0 = page … 1 = overview) is between
 * the two. [zoom]: the overview's scale around the screen centre. [card]: the card [hero] flies
 * to, if any; [fallback]: what the page shows otherwise. Every frame only touches the layer.
 */
@Composable
fun CardHeroLayer(
    hero: CardHero,
    card: CardModel?,
    fallback: CoverTarget?,
    progress: () -> Float,
    zoom: () -> Float,
) {
    val page = hero.pageRect
    if (page.isEmpty) return
    val density = LocalDensity.current
    val palette = KoanTheme.colors
    // The overview zooms around the window's centre.
    val window = LocalWindowInfo.current.containerSize
    val centre = Offset(window.width / 2f, window.height / 2f)
    val headerPx = with(density) { CardTokens.HeaderHeight.toPx() }
    val radiusPx = with(density) { CardTokens.Radius.toPx() }
    val hairlinePx = with(density) { CardTokens.Hairline.toPx() }
    val elevationPx = with(density) { palette.CardElevation.toPx() }
    // Laid out at the card's own width (the header then lands exactly), tall enough for the page.
    val withCard = card != null && hero.cardSize.width > 0f
    val w = if (withCard) hero.cardSize.width else page.width
    val pageH = page.height * w / page.width
    val top0 = if (withCard) headerPx else 0f
    val h = if (withCard) max(top0 + pageH, hero.cardSize.height) else pageH
    // The visible part and the scale this frame, set by the layer, read by the border.
    var visible = HeroClip(top0, top0 + pageH, 0f)
    var layerScale = 1f
    Box(
        Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .requiredSize(with(density) { w.toDp() }, with(density) { h.toDp() })
            .graphicsLayer {
                // At rest (page or overview) it waits unseen.
                alpha = if (hero.flying) 1f else 0f
                val p = progress()
                val slot = if (withCard && hero.flying) hero.cardRect() else null
                val z = zoom()
                fun zoomed(r: Rect) = Rect(
                    centre + (r.topLeft - centre) * z,
                    centre + (r.bottomRight - centre) * z,
                )
                // Where it lands: the card, else the page shrunk towards the middle.
                val end = if (slot != null) zoomed(slot) else zoomed(scaled(page, Motion.HERO_FALLBACK_SCALE))
                val startScale = page.width / w
                val endScale = end.width / w
                val s = lerp(startScale, endScale, p)
                val endTop = if (slot != null) 0f else top0
                val clipTop = lerp(top0, endTop, p)
                visible = HeroClip(clipTop, lerp(top0 + page.height / startScale, endTop + end.height / endScale, p), lerp(0f, radiusPx, p) / s)
                layerScale = s
                val at = Offset(lerp(page.left, end.left, p), lerp(page.top, end.top, p))
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = s
                scaleY = s
                translationX = at.x
                translationY = at.y - s * clipTop
                // A new shape value each frame: the layer rebuilds its outline only when it changes.
                shape = visible
                clip = true
                shadowElevation = elevationPx * p
                ambientShadowColor = palette.CardShadowColor
                spotShadowColor = palette.CardShadowColor
            }
            .background(palette.Card)
            .drawWithContent {
                drawContent()
                // The card's hairline, as it lands.
                val p = progress()
                if (withCard && p > 0f) {
                    val v = visible
                    drawRoundRect(
                        palette.CardBorder,
                        topLeft = Offset(0f, v.top),
                        size = Size(size.width, v.bottom - v.top),
                        cornerRadius = CornerRadius(v.corner),
                        style = Stroke(hairlinePx / layerScale),
                        alpha = p,
                    )
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            if (withCard && card != null) CardHeader(card, depth = { 0f }, dimColor = Color.Transparent)
            val target = if (card != null) {
                CoverTarget(card.id, card.url, card.icon, card.staticIcon, card.thumb, blank = false)
            } else {
                fallback
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(palette.Base)) {
                if (target != null && !target.blank) PagePreview(target)
            }
        }
    }
}

private fun scaled(r: Rect, f: Float): Rect {
    val c = r.center
    return Rect(c + (r.topLeft - c) * f, c + (r.bottomRight - c) * f)
}

/** The flying card's visible band (layout px): full width, [top] to [bottom], rounded by [corner]. */
private data class HeroClip(val top: Float, val bottom: Float, val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(0f, top, size.width, bottom, CornerRadius(corner)))
}
