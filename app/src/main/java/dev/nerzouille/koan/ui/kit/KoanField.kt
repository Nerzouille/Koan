package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.FieldTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/*
 * The one field style (docs/DESIGN_SYSTEM.md "Field"): 50dp, pill, Glass fill, 16sp text and hint,
 * an optional leading glyph (search) and a ✕ that empties it. Used by page searches (History,
 * Downloads, Passwords), forms (search engine) and page prompts. The dock's address field and
 * find-in-page keep their own (17sp, on the dock).
 */

/** The text style inside a field. */
@Composable
fun fieldTextStyle() = TextStyle(color = KoanTheme.colors.TextPrimary, fontSize = FieldTokens.TextSize)

/**
 * The field's frame: [leading], the [hint] while [empty], the input from [input] (given the text
 * style), then [onClear]'s ✕ when not [empty]. Callers with a TextFieldValue use this directly.
 */
@Composable
fun KoanFieldFrame(
    hint: String,
    empty: Boolean,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    onClear: (() -> Unit)? = null,
    input: @Composable (TextStyle) -> Unit,
) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .height(FieldTokens.Height)
            .clip(CircleShape)
            .background(palette.Glass)
            .padding(start = FieldTokens.PaddingH, end = if (onClear != null && !empty) FieldTokens.PaddingH / 2 else FieldTokens.PaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = palette.FieldHint, modifier = Modifier.size(FieldTokens.Glyph))
            Spacer(Modifier.width(FieldTokens.GlyphGap))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (empty && hint.isNotEmpty()) {
                Text(hint, color = palette.FieldHint, fontSize = FieldTokens.TextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            input(fieldTextStyle())
        }
        if (onClear != null && !empty) {
            Box(Modifier.size(FieldTokens.ClearTouch).clip(CircleShape).clickable(onClick = onClear), contentAlignment = Alignment.Center) {
                Icon(KoanIcons.Close, "Clear", tint = palette.FieldHint, modifier = Modifier.size(FieldTokens.ClearGlyph))
            }
        }
    }
}

/** A one-line field on a String. [search]: a search glyph in front and a ✕ to empty it. */
@Composable
fun KoanField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    search: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    inputModifier: Modifier = Modifier,
) {
    KoanFieldFrame(
        hint = hint,
        empty = value.isEmpty(),
        modifier = modifier,
        leading = if (search) KoanIcons.Search else null,
        onClear = if (search) ({ onValueChange("") }) else null,
    ) { style ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(KoanTheme.colors.TextPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = inputModifier.fillMaxWidth(),
        )
    }
}
