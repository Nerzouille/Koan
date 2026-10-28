package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/**
 * A page started a download on its own (no tap just before): the same floating card as a
 * permission's, with the file's name, its size (if known) and site, then Cancel / Download.
 */
@Composable
fun DownloadPrompt(name: String, detail: String, interactive: Boolean, onCancel: () -> Unit, onDownload: () -> Unit) {
    val palette = KoanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(SheetTokens.RowIconBox + SheetTokens.Gap)
                    .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
                    .background(palette.GroupedRow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Download, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph + SheetTokens.Divider * 2))
            }
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text(name, color = palette.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                if (detail.isNotEmpty()) Text(detail, color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            CardButton("Cancel", interactive, onCancel, Modifier.weight(1f))
            PrimaryButton("Download", onClick = { if (interactive) onDownload() }, modifier = Modifier.weight(1f))
        }
    }
}
