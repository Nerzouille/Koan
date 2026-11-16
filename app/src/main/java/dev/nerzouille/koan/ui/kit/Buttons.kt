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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.theme.ButtonTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/** What a [KoanButton] looks like. */
enum class ButtonKind { Primary, Secondary, Destructive, Quiet }

/**
 * The one text button: a 50dp pill. [ButtonKind.Primary] is black in light and white in dark (never
 * the brand gradient); Secondary and Destructive sit on a neutral [surface] (Destructive: red text,
 * never a red fill); Quiet is plain text. [enabled] false: no click and no ripple (a closing card).
 * [paddingH]: side padding; buttons sharing a row by weight use none, so their label keeps its room.
 */
@Composable
fun KoanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    surface: Color = KoanTheme.colors.GroupedRow,
    paddingH: Dp = ButtonTokens.PaddingH,
    leading: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    val fill = when (kind) {
        ButtonKind.Primary -> palette.Primary
        ButtonKind.Secondary, ButtonKind.Destructive -> surface
        ButtonKind.Quiet -> Color.Transparent
    }
    val textColor = when (kind) {
        ButtonKind.Primary -> palette.OnPrimary
        ButtonKind.Secondary, ButtonKind.Quiet -> palette.TextPrimary
        ButtonKind.Destructive -> palette.Destructive
    }
    Row(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ButtonTokens.IconGap, Alignment.CenterHorizontally),
    ) {
        leading()
        Text(
            text,
            color = textColor,
            fontSize = ButtonTokens.TextSize,
            fontWeight = if (kind == ButtonKind.Quiet) FontWeight.Medium else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** The one primary button: black in light, white in dark. Never the brand gradient. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: @Composable RowScope.() -> Unit = {}) =
    KoanButton(text, onClick, modifier, ButtonKind.Primary, leading = leading)

/** A secondary action (Cancel): plain text, no surface. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    KoanButton(text, onClick, modifier, ButtonKind.Quiet)

/** A clear secondary action: a neutral filled pill, [surface] behind it. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surface: Color = KoanTheme.colors.GroupedRow,
    enabled: Boolean = true,
    paddingH: Dp = ButtonTokens.PaddingH,
) = KoanButton(text, onClick, modifier, ButtonKind.Secondary, enabled, surface, paddingH)

/**
 * The one destructive button: red SemiBold text on a neutral pill ([surface]), never a red fill.
 * Destructive rows ("Sign out") are rows with a red title instead.
 */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, surface: Color = KoanTheme.colors.GroupedRow, paddingH: Dp = ButtonTokens.PaddingH) =
    KoanButton(text, onClick, modifier, ButtonKind.Destructive, surface = surface, paddingH = paddingH)

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
        SecondaryButton(cancel, onClick = onCancel, modifier = Modifier.weight(1f), enabled = interactive, paddingH = 0.dp)
        KoanButton(
            action, onClick = { if (interactive) onAction() }, modifier = Modifier.weight(1f),
            kind = if (destructive) ButtonKind.Destructive else ButtonKind.Primary, paddingH = 0.dp,
        )
    }
}

/** A list card's way out: a full-width neutral button under the list. */
@Composable
fun CardCancel(interactive: Boolean, onCancel: () -> Unit, modifier: Modifier = Modifier, text: String = "Cancel") {
    SecondaryButton(text, onClick = { if (interactive) onCancel() }, modifier = modifier.fillMaxWidth())
}
