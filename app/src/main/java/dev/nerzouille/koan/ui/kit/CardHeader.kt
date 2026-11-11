package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/**
 * A floating card's header: a [leading] visual, then its title (18sp SemiBold) over one line of
 * [text] (14sp, secondary). [alignment]: Top when the title may wrap (permission, download).
 */
@Composable
fun CardHeader(
    title: String,
    text: String?,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    titleMaxLines: Int = Int.MAX_VALUE,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    textMaxLines: Int = Int.MAX_VALUE,
) {
    val palette = KoanTheme.colors
    Row(modifier, verticalAlignment = alignment) {
        leading()
        Spacer(Modifier.width(SheetTokens.RowPadding))
        Column {
            Text(title, color = palette.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize), maxLines = titleMaxLines, overflow = titleOverflow)
            if (!text.isNullOrEmpty()) {
                Text(
                    text, color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize, maxLines = textMaxLines,
                    overflow = if (textMaxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A card's icon tile: the glyph on a 40dp rounded `GroupedRow` square. */
@Composable
fun CardIconTile(icon: ImageVector) {
    Box(
        Modifier
            .size(SheetTokens.RowIconBox + SheetTokens.Gap)
            .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
            .background(KoanTheme.colors.GroupedRow),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph + SheetTokens.Divider * 2))
    }
}
