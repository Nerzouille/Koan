package dev.nerzouille.koan.ui.bar

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.overview.SpaceModel
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import kotlin.math.abs
import kotlin.math.floor

/**
 * Zen-style space row (design 4a): one icon per space, the active one in a pill tinted with its
 * colour, with its name. The pill follows the pager continuously, so a drag on the bar (handled by
 * KoanBar) slides it along. Taps jump to a space (one tick). Every item is laid out at its full
 * pill width once; positions, pill widths and fades are read from the pager in layer/draw lambdas.
 */
@Composable
fun WorkspaceSwitcher(spaces: List<SpaceModel>, pager: SpacePager, enabled: Boolean, modifier: Modifier = Modifier) {
    if (spaces.isEmpty()) return
    val view = LocalView.current
    val density = LocalDensity.current
    val palette = KoanTheme.colors
    val measurer = rememberTextMeasurer()
    val nameStyle = TextStyle(fontSize = BarTokens.SwitcherNameSize, fontWeight = FontWeight.SemiBold)
    val names = spaces.map { it.name }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val rowWidth = constraints.maxWidth.toFloat()
        val geo = remember(names, density, rowWidth) {
            with(density) {
                val maxName = BarTokens.SwitcherNameMax.roundToPx()
                val pillChrome = (BarTokens.SwitcherIconInset + BarTokens.SwitcherIcon + BarTokens.SwitcherNameGap + BarTokens.SwitcherPillEnd).toPx()
                val item = BarTokens.SwitcherItem.toPx()
                val extra = FloatArray(names.size) { i ->
                    val name = measurer.measure(names[i], nameStyle, maxLines = 1, constraints = Constraints(maxWidth = maxName)).size.width
                    (pillChrome + name - item).coerceAtLeast(0f)
                }
                SwitcherGeometry(extra, item, rowWidth)
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(pager, geo) {
                            detectTapGestures { p ->
                                val target = geo.hit(p.x, pager.settled.toFloat())
                                if (target >= 0 && target != pager.settled) {
                                    pager.animateTo(target)
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        )
        spaces.forEachIndexed { i, space ->
            val pillWidth = with(density) { (geo.item + geo.extra[i]).toDp() }
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = BarTokens.SwitcherBottom)
                    .width(pillWidth)
                    .height(BarTokens.SwitcherItem)
                    .graphicsLayer { translationX = geo.left(i, pager.pos) }
                    .drawWithContent {
                        val p = pager.pos
                        val e = geo.open(i, p)
                        val w = geo.width(i, p)
                        val h = BarTokens.SwitcherPillHeight.toPx()
                        if (e > 0.01f) {
                            drawRoundRect(
                                space.colors.pill,
                                topLeft = Offset(0f, (size.height - h) / 2f),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(h / 2f),
                                alpha = e,
                            )
                        }
                        // The name stays inside the pill while it opens.
                        clipRect(right = w) { this@drawWithContent.drawContent() }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(BarTokens.SwitcherIconInset))
                Box(
                    Modifier
                        .size(BarTokens.SwitcherIcon)
                        .graphicsLayer {
                            val e = geo.open(i, pager.pos)
                            alpha = BarTokens.SWITCHER_INACTIVE_ALPHA + (1f - BarTokens.SWITCHER_INACTIVE_ALPHA) * e
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (space.icon.isNotBlank()) {
                        ZenIcon(space.icon, BarTokens.SwitcherIcon, tint = palette.TextPrimary)
                    } else {
                        // A space without an icon shows its initial.
                        Text(space.name.take(1).uppercase(), color = palette.TextPrimary, style = nameStyle)
                    }
                }
                Spacer(Modifier.width(BarTokens.SwitcherNameGap))
                Text(
                    space.name,
                    color = space.colors.ink,
                    style = nameStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = BarTokens.SwitcherNameMax)
                        .graphicsLayer {
                            val e = geo.open(i, pager.pos)
                            alpha = ((e - BarTokens.SWITCHER_NAME_FROM) / (1f - BarTokens.SWITCHER_NAME_FROM)).coerceIn(0f, 1f)
                        },
                )
            }
        }
    }
}

/**
 * Item positions as a function of the pager position (all px). Item i is [item] wide, plus
 * [extra]`[i]` times how open its pill is: 1 on the settled space, sharing between two while dragging.
 */
private class SwitcherGeometry(val extra: FloatArray, val item: Float, val rowWidth: Float) {
    fun open(i: Int, pos: Float) = (1f - abs(i - pos)).coerceIn(0f, 1f)

    fun width(i: Int, pos: Float) = item + extra[i] * open(i, pos)

    fun left(i: Int, pos: Float): Float {
        var x = start(pos)
        for (j in 0 until i) x += width(j, pos)
        return x
    }

    /** Centred when the row fits; otherwise the active pill is kept centred, ends clamped. */
    private fun start(pos: Float): Float {
        var total = 0f
        for (j in extra.indices) total += width(j, pos)
        if (total <= rowWidth) return (rowWidth - total) / 2f
        val p = pos.coerceIn(0f, (extra.size - 1).toFloat())
        val a = floor(p).toInt()
        val b = (a + 1).coerceAtMost(extra.size - 1)
        val centre = centreOf(a, pos) + (centreOf(b, pos) - centreOf(a, pos)) * (p - a)
        return (rowWidth / 2f - centre).coerceIn(rowWidth - total, 0f)
    }

    private fun centreOf(i: Int, pos: Float): Float {
        var x = 0f
        for (j in 0 until i) x += width(j, pos)
        return x + width(i, pos) / 2f
    }

    /** The item under [x], or -1. */
    fun hit(x: Float, pos: Float): Int {
        for (i in extra.indices) {
            val l = left(i, pos)
            if (x >= l && x < l + width(i, pos)) return i
        }
        return -1
    }
}
