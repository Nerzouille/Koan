package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import kotlin.math.roundToInt

/**
 * A space's page before it is composed for real (that costs ~40 ms): its header and the card frames
 * of its stack, drawn in one canvas, in the same places as the real stack at rest. The overview
 * keeps one beside the current space from its first frame, so a swipe never pops a page in; the
 * real page fades in over it once it is composed.
 */
@Composable
internal fun GhostSpace(
    space: SpaceModel,
    badge: SyncBadge,
    /** The stack's focused card (the space's current tab). */
    focus: Int,
    pageOffset: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SpaceHeader(space, badge, onBack = {}, onNewTab = {}, onBadge = {})
        Box(Modifier.weight(1f).fillMaxWidth().stackParallax(pageOffset)) {
            GhostStack(space.stack, focus)
        }
    }
}

@Composable
private fun GhostStack(cards: List<CardModel>, focus: Int) {
    if (cards.isEmpty()) return
    val palette = KoanTheme.colors
    // The focused card's screenshot, only if it is already in memory (never decoded for a ghost).
    val thumb = remember(cards.getOrNull(focus)?.thumb) { cards.getOrNull(focus)?.thumb?.let(ThumbnailStore::cached) }
    Canvas(Modifier.fillMaxSize()) {
        val frame = StackFrame(size.width, size.height, density)
        val radius = CardTokens.Radius.toPx()
        val header = CardTokens.HeaderHeight.toPx()
        val behind = focus.coerceAtMost(DepthCurve.MAX_VISIBLE_D.toInt())
        val inFront = focus < cards.lastIndex
        // Back to front: the pile, the focused card, the peek.
        for (d in behind downTo (if (inFront) -1 else 0)) {
            val depth = d.toFloat()
            val s = DepthCurve.scale(depth)
            val w = frame.cardWidth * s
            val h = frame.cardHeight * s
            val left = (size.width - w) / 2f
            val top = frame.topOf(depth)
            val alpha = DepthCurve.alpha(depth)
            val r = CornerRadius(radius * s)
            val card = Path().apply { addRoundRect(RoundRect(left, top, left + w, top + h, r)) }
            clipPath(card) {
                drawRect(palette.Card, Offset(left, top), Size(w, h), alpha = alpha)
                if (d == 0 && thumb != null) {
                    val body = h - header * s
                    val drawH = (thumb.height * w / thumb.width).coerceAtMost(body)
                    drawImage(
                        thumb,
                        srcSize = IntSize(thumb.width, (drawH * thumb.width / w).roundToInt().coerceAtMost(thumb.height)),
                        dstOffset = IntOffset(left.roundToInt(), (top + header * s).roundToInt()),
                        dstSize = IntSize(w.roundToInt(), drawH.roundToInt()),
                        alpha = alpha,
                    )
                }
                drawRect(palette.CardStrip, Offset(left, top), Size(w, header * s), alpha = alpha)
                val dim = (DepthCurve.dim(depth) * palette.DimGain).coerceAtMost(palette.DimMax)
                if (dim > 0f) drawRect(if (palette.DimToBase) palette.Base else Color.Black, Offset(left, top), Size(w, h), alpha = dim * alpha)
            }
            drawRoundRect(palette.CardBorder, Offset(left, top), Size(w, h), r, alpha = alpha, style = Stroke(CardTokens.Hairline.toPx()))
        }
    }
}
