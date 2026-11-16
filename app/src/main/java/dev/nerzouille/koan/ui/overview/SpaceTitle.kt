package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.OverviewTokens

/** A space's icon on its glass tile and its name, as the overview header and the start page show them. */
@Composable
fun RowScope.SpaceTitle(icon: String, name: String, nameModifier: Modifier = Modifier) {
    Box(
        Modifier.size(OverviewTokens.SpaceIconBox).clip(RoundedCornerShape(OverviewTokens.SpaceIconRadius)).background(KoanTheme.colors.Glass),
        contentAlignment = Alignment.Center,
    ) { ZenIcon(icon, OverviewTokens.SpaceIconGlyph, tint = KoanTheme.colors.TextPrimary) }
    Spacer(Modifier.width(OverviewTokens.TitleGap))
    Text(
        name,
        color = KoanTheme.colors.TextPrimary,
        style = KoanType.title(OverviewTokens.TitleSize),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = nameModifier,
    )
}
