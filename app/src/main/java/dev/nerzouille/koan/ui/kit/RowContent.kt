package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * A list row's text, taking the free width: its title (Medium, one line; greyed when not [enabled],
 * else [titleColor] if set) over an optional one-line [subtitle]. Shared by sheet and settings rows.
 */
@Composable
fun RowScope.RowTexts(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    titleColor: Color,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
) {
    val palette = KoanTheme.colors
    Column(Modifier.weight(1f)) {
        Text(
            title,
            color = when {
                !enabled -> palette.TextDisabled
                titleColor != Color.Unspecified -> titleColor
                else -> palette.TextPrimary
            },
            fontSize = titleSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(subtitle, color = palette.TextSecondary, fontSize = subtitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
