package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme

/** A file's type at a glance, in a column: a document glyph over its extension (up to 4 letters). */
@Composable
fun ColumnScope.FileGlyph(fileName: String, glyph: Dp, extSize: TextUnit) {
    val ext = fileName.substringAfterLast('.', "").take(4).uppercase()
    Icon(KoanIcons.File, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(glyph))
    if (ext.isNotEmpty()) Text(ext, color = KoanTheme.colors.TextSecondary, fontSize = extSize, fontWeight = FontWeight.SemiBold)
}
