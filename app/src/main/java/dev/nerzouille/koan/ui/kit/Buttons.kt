package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.theme.ButtonTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/** The one primary button: black in light, white in dark. Never the brand gradient. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(KoanTheme.colors.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = ButtonTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ButtonTokens.IconGap, Alignment.CenterHorizontally),
    ) {
        leading()
        Text(text, color = KoanTheme.colors.OnPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.SemiBold)
    }
}

/** A secondary action (Cancel): plain text, no surface. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = ButtonTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = KoanTheme.colors.TextPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}

/** A clear secondary action: a neutral filled pill, [surface] behind it. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, surface: Color = KoanTheme.colors.GroupedRow) {
    Row(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(surface)
            .clickable(onClick = onClick)
            .padding(horizontal = ButtonTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = KoanTheme.colors.TextPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.SemiBold)
    }
}

/** A destructive action: red text only, on a neutral row. */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(KoanTheme.colors.GroupedRow)
            .clickable(onClick = onClick)
            .padding(horizontal = ButtonTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = KoanTheme.colors.Destructive, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}
