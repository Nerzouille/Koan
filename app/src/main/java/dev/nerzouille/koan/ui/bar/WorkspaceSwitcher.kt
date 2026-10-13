package dev.nerzouille.koan.ui.bar

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.data.SpaceEntity
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Current space centred, neighbours faded at the sides, page dots. Drags are handled by the whole
 * bar (KoanBar); this only handles taps on a neighbour's name (jump there, one tick).
 */
@Composable
fun WorkspaceSwitcher(spaces: List<SpaceEntity>, pager: SpacePager, enabled: Boolean, modifier: Modifier = Modifier) {
    val view = LocalView.current
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(pager) {
                            detectTapGestures { p ->
                                val steps = ((p.x - size.width / 2f) / BarTokens.LabelSpacing.toPx()).roundToInt()
                                val target = (pager.settled + steps).coerceIn(0, pager.count - 1)
                                if (target != pager.settled) {
                                    pager.animateTo(target)
                                    tick()
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            KoanIcons.Back, null, tint = KoanColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = BarTokens.ChevronInset).size(BarTokens.Chevron),
        )
        Icon(
            KoanIcons.Forward, null, tint = KoanColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = BarTokens.ChevronInset).size(BarTokens.Chevron),
        )
        spaces.forEachIndexed { i, space ->
            Row(
                Modifier
                    .padding(bottom = BarTokens.LabelLift)
                    .graphicsLayer {
                        val f = i - pager.pos
                        translationX = f * BarTokens.LabelSpacing.toPx()
                        val far = abs(f)
                        alpha = if (far > 1.6f) 0f else (1f - far * BarTokens.LABEL_FAR_FADE).coerceAtLeast(0f)
                        val s = 1f - BarTokens.LABEL_FAR_SCALE * far.coerceAtMost(1f)
                        scaleX = s
                        scaleY = s
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(space.icon, fontSize = BarTokens.LabelSize)
                Spacer(Modifier.width(BarTokens.LabelGap))
                Text(space.name, color = KoanColors.TextPrimary, fontSize = BarTokens.LabelSize, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        PageDots(count = spaces.size, pos = { pager.pos }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = BarTokens.DotsBottom))
    }
}

@Composable
private fun PageDots(count: Int, pos: () -> Float, modifier: Modifier = Modifier) {
    val dot = BarTokens.Dot
    val gap = BarTokens.DotGap
    Canvas(modifier.size(width = (dot + gap) * count + dot * 2, height = dot)) {
        val d = dot.toPx()
        val g = gap.toPx()
        val p = pos()
        var x = 0f
        for (i in 0 until count) {
            val near = (1f - abs(i - p)).coerceIn(0f, 1f)
            val w = d + near * d * 1.6f
            drawRoundRect(
                color = KoanColors.PageDot.copy(alpha = 0.3f + 0.6f * near),
                topLeft = Offset(x, 0f),
                size = Size(w, d),
                cornerRadius = CornerRadius(d / 2f),
            )
            x += w + g
        }
    }
}
