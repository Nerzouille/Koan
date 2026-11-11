package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.layout.fillMaxWidth

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

/**
 * The one destructive button: red SemiBold text on a neutral pill ([surface]), never a red fill.
 * Destructive rows ("Sign out") are rows with a red title instead.
 */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, surface: Color = KoanTheme.colors.GroupedRow) {
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
        Text(text, color = KoanTheme.colors.Destructive, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.SemiBold)
    }
}

/*
 * A card's buttons, one rule everywhere (no text-only Cancel): a card with two choices puts them
 * side by side, same size, [CardChoices]; a card with a list of choices ends with [CardCancel].
 */

/** Two choices side by side: [cancel] neutral on the left, the action on the right (red text when [destructive]). */
@Composable
fun CardChoices(
    action: String,
    interactive: Boolean,
    onAction: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    cancel: String = "Cancel",
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonTokens.ChoiceGap)) {
        SecondaryButton(cancel, onClick = { if (interactive) onCancel() }, modifier = Modifier.weight(1f))
        if (destructive) {
            DestructiveButton(action, onClick = { if (interactive) onAction() }, modifier = Modifier.weight(1f))
        } else {
            PrimaryButton(action, onClick = { if (interactive) onAction() }, modifier = Modifier.weight(1f))
        }
    }
}

/** A list card's way out: a full-width neutral button under the list. */
@Composable
fun CardCancel(interactive: Boolean, onCancel: () -> Unit, modifier: Modifier = Modifier, text: String = "Cancel") {
    SecondaryButton(text, onClick = { if (interactive) onCancel() }, modifier = modifier.fillMaxWidth())
}
