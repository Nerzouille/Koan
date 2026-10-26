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
 * between the page and its slot. It flies inside its stack, in the card's own place (the cards
 * behind stay behind it, the ones in front come in over it), and lands with the stack's own
 * geometry. At the page end it is the page's screenshot full size (its header just above the
 * screen). The real card hides meanwhile. Without a stack showing the card (a new tab…), the page
 * shrinks towards the middle over the whole overview instead.
 */
@Stable
class CardHero {
    /** The card the transition flies to or from; null = none. */
    var cardId by mutableStateOf<String?>(null)

    /** The page view's bounds in the window (px). */
    var pageRect by mutableStateOf(Rect.Zero)

    /** The overview's content, inside its zoom: stacks place themselves from it. */
    var overview: LayoutCoordinates? = null

    /** Page ↔ overview, 0 = page … 1 = overview (read in layers and draw only). */
    var progress: () -> Float = { 0f }

    /** The overview's zoom around the window centre (read in layers and draw only). */
    var zoom: () -> Float = { 1f }

    /** The overview is drawn (open, or still moving); read in layers and draw only. */
    var overviewShown: () -> Boolean = { false }

    /**
     * The card flies: the overview is drawn but not settled open. That includes progress 0 (the
     * overview's first frame), where the card covers it as the page. The real card stays hidden.
     */
    val flying: Boolean
        get() = overviewShown() && progress() < 1f

    private val stacks = mutableStateMapOf<Any, (String) -> Boolean>()

    /** A stack on screen says whether it shows a card (it then flies the card itself). */
    fun register(key: Any, shows: (String) -> Boolean) {
        stacks[key] = shows
    }

    fun unregister(key: Any) {
        stacks.remove(key)
    }

    /** A stack on screen flies [cardId]; else the page shrinks over the overview (draw only). */
    fun inStack(): Boolean {
        val id = cardId ?: return false
        return stacks.values.any { it(id) }
    }

    fun hides(id: String) = flying && id == cardId

    /**
     * How far the other cards have come in (1 = in place): they slide and fade in with the flying
     * card, and back out with it.
     */
    fun entrance(): Float = if (flying) progress() else 1f

    /** The bottom edge of the screen in the coordinates of [inside] (a stack inside the overview's zoom). */
    fun screenBottomIn(inside: LayoutCoordinates?, windowCentre: Offset): Float? {
        val root = overview ?: return null
        if (inside == null || !inside.isAttached || !root.isAttached) return null
        val origin = root.localPositionOf(inside, Offset.Zero)
        return windowCentre.y + (2f * windowCentre.y - windowCentre.y) / zoom() - origin.y
    }

    /** [pageRect] in the coordinates of [inside] (a stack inside the overview's zoom), or null. */
    fun pageIn(inside: LayoutCoordinates?, windowCentre: Offset): Rect? {
        val root = overview ?: return null
        if (inside == null || !inside.isAttached || !root.isAttached) return null
        val origin = root.localPositionOf(inside, Offset.Zero)
        val z = zoom()
        fun local(p: Offset) = windowCentre + (p - windowCentre) / z - origin
        return Rect(local(pageRect.topLeft), local(pageRect.bottomRight))
    }
}

val LocalCardHero = staticCompositionLocalOf { CardHero() }

/**
 * The card in flight inside its stack, between [start] (the page, in the stack's coordinates) and
 * [end] (its slot), at [progress]. [cardSize]: the card at the focus. Drawn only while [visible].
 */
@Composable
internal fun FlyingHero(
    card: CardModel,
    cardSize: Size,
    progress: () -> Float,
    visible: () -> Boolean,
    start: () -> Rect?,
    end: () -> Rect,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val headerPx = with(density) { CardTokens.HeaderHeight.toPx() }
    // Tall enough for the page at the card's width (a phone screen is taller than a card).
    val window = LocalWindowInfo.current.containerSize
    val h = max(headerPx + window.height * cardSize.width / max(window.width, 1).toFloat(), cardSize.height)
    HeroBody(
        target = CoverTarget(card.id, card.url, card.icon, card.staticIcon, card.thumb, blank = false),
        header = card,
        layoutSize = Size(cardSize.width, h),
        headerPx = headerPx,
        progress = progress,
        geometry = geometry@{ p ->
            if (!visible()) return@geometry null
            val from = start() ?: return@geometry null
            HeroGeometry.between(from, end(), cardSize.width, headerPx, landsOnCard = true, p)
        },
        // Taller than the stack: pinned to its top-left, never centred on the overflow.
        modifier = modifier.wrapContentSize(Alignment.TopStart, unbounded = true),
    )
}

/**
 * The fallback, over the whole overview: no stack shows the card (a new tab, a link from another
 * app…), so the page shrinks towards the middle, opaque. [card]: shown if known, else [fallback].
 */
@Composable
fun PageHeroLayer(hero: CardHero, card: CardModel?, fallback: CoverTarget?) {
    val page = hero.pageRect
    if (page.isEmpty) return
    val window = LocalWindowInfo.current.containerSize
    val centre = Offset(window.width / 2f, window.height / 2f)
    val target = card?.let { CoverTarget(it.id, it.url, it.icon, it.staticIcon, it.thumb, blank = false) } ?: fallback
    HeroBody(
        target = target,
        header = null,
        layoutSize = page.size,
        headerPx = 0f,
        progress = hero.progress,
        geometry = geometry@{ p ->
            if (!hero.flying || hero.inStack()) return@geometry null
            val z = hero.zoom()
            val shrunk = scaled(page, Motion.HERO_FALLBACK_SCALE)
            val end = Rect(centre + (shrunk.topLeft - centre) * z, centre + (shrunk.bottomRight - centre) * z)
            HeroGeometry.between(page, end, page.width, 0f, landsOnCard = false, p)
        },
        modifier = Modifier.wrapContentSize(Alignment.TopStart, unbounded = true),
    )
}

/** Where the flying body is this frame: its layer transform and its visible band. */
private class HeroGeometry(val scale: Float, val translation: Offset, val clip: HeroClip) {
    companion object {
        /**
         * From [from] (the page: the body's content below its header, full size) to [to] (the card
         * slot: the whole body, header included, if [landsOnCard]), for a body [w] wide.
         */
        fun between(from: Rect, to: Rect, w: Float, headerPx: Float, landsOnCard: Boolean, p: Float): HeroGeometry {
            val startScale = from.width / w
            val endScale = to.width / w
            val s = lerp(startScale, endScale, p)
            val endTop = if (landsOnCard) 0f else headerPx
            val clipTop = lerp(headerPx, endTop, p)
            val clipBottom = lerp(headerPx + from.height / startScale, endTop + to.height / endScale, p)
            val at = Offset(lerp(from.left, to.left, p), lerp(from.top, to.top, p))
            return HeroGeometry(s, Offset(at.x, at.y - s * clipTop), HeroClip(clipTop, clipBottom, 0f))
        }
    }
}

@Composable
private fun HeroBody(
    target: CoverTarget?,
    header: CardModel?,
    layoutSize: Size,
    headerPx: Float,
    progress: () -> Float,
    geometry: (Float) -> HeroGeometry?,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val palette = KoanTheme.colors
    val radiusPx = with(density) { CardTokens.Radius.toPx() }
    val hairlinePx = with(density) { CardTokens.Hairline.toPx() }
    // As high as the cards (they all share one elevation): the stack then draws it in its card's
    // place, the cards in front over it.
    val elevationPx = with(density) { palette.CardElevation.toPx() }
    // This frame's band and scale, set by the layer, read by the border.
    var band = HeroClip(0f, 0f, 0f)
    var layerScale = 1f
    Box(
        modifier
            .requiredSize(with(density) { layoutSize.width.toDp() }, with(density) { layoutSize.height.toDp() })
            .graphicsLayer {
                val p = progress()
                val g = geometry(p)
                if (g == null) {
                    alpha = 0f
                    return@graphicsLayer
                }
                alpha = 1f
                layerScale = g.scale
                // Rounded on screen like the card as it lands.
                band = g.clip.copy(corner = lerp(0f, radiusPx, p) / g.scale)
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = g.scale
                scaleY = g.scale
                translationX = g.translation.x
                translationY = g.translation.y
                // A new shape value each frame: the layer rebuilds its outline only when it changes.
                shape = band
                clip = true
                shadowElevation = elevationPx
                ambientShadowColor = palette.CardShadowColor
                spotShadowColor = palette.CardShadowColor
            }
            .background(palette.Card)
            .drawWithContent {
                drawContent()
                // The card's hairline, as it lands.
                val p = progress()
                if (header != null && p > 0f) {
                    val v = band
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
            if (header != null) CardHeader(header, depth = { 0f }, dimColor = Color.Transparent)
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
