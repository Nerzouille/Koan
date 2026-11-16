package dev.nerzouille.koan.ui.kit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/** A one-line "nothing here" hint in a list page (secondary text, 15sp by default). */
@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier, size: TextUnit = SettingsTokens.RowValueSize) {
    Text(text, color = KoanTheme.colors.TextSecondary, fontSize = size, modifier = modifier)
}
