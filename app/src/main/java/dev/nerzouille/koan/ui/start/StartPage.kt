package dev.nerzouille.koan.ui.start

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.bar.EssentialsGrid
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.overview.SpaceModel
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.StartPageTokens

/**
 * Where Koan comes back after a while unused: the current space's essentials above the dock, whose
 * search field is focused. Not a tab: nothing exists until a search or an essential opens one.
 * Swipe down (or Back, or the field's ✕) goes back to the last tab. It shows at once and fades out.
 * [bottom]: the dock's height above the keyboard.
 */
@Composable
fun StartPage(open: Boolean, space: SpaceModel?, bottom: Dp, onOpenTab: (String) -> Unit, onDismiss: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    // The last space shown stays while it fades out.
    var shown by remember { mutableStateOf(space) }
    if (open && space != null) shown = space
    LaunchedEffect(open) {
        if (open) alpha.snapTo(1f) else alpha.animateTo(0f, tween(StartPageTokens.FADE_OUT_MS))
    }
    if (!open && alpha.value == 0f) return
    val current = shown ?: return
    val dismiss by rememberUpdatedState(onDismiss)
    val palette = KoanTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(palette.Base)
            .then(
                if (!open) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                            total += dy
                            if (total > StartPageTokens.SwipeDownToDismiss.toPx()) {
                                total = Float.NEGATIVE_INFINITY
                                dismiss()
                            }
                            change.consume()
                        }
                    }
                },
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = StartPageTokens.SidePadding)
                .padding(bottom = bottom + StartPageTokens.BottomGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(OverviewTokens.SpaceIconBox).clip(RoundedCornerShape(OverviewTokens.SpaceIconRadius)).background(palette.Glass),
                    contentAlignment = Alignment.Center,
                ) { ZenIcon(current.icon, OverviewTokens.SpaceIconGlyph, tint = palette.TextPrimary) }
                Spacer(Modifier.width(OverviewTokens.TitleGap))
                Text(
                    current.name,
                    color = palette.TextPrimary,
                    fontSize = OverviewTokens.TitleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(StartPageTokens.TitleGap))
            if (current.essentials.isEmpty()) {
                Text(
                    "No essentials in ${current.name}",
                    color = palette.TextSecondary,
                    fontSize = DrawerTokens.PinnedTitleSize,
                )
            } else {
                EssentialsGrid(current.essentials, interactive = open, onOpenTab = onOpenTab)
            }
        }
    }
}
