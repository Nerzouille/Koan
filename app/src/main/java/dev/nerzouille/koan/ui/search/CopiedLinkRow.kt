package dev.nerzouille.koan.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SearchTokens

/** "Open link you copied", the link's host under it: one row, in a card or on the start page. */
@Composable
fun CopiedLinkRow(link: CopiedLink, interactive: Boolean, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(SearchTokens.RowHeight)
            .then(if (interactive) Modifier.clickable { onOpen(link.url) } else Modifier)
            .padding(horizontal = SearchTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(SearchTokens.Favicon).clip(CircleShape).background(palette.GroupedRow),
            contentAlignment = Alignment.Center,
        ) { Icon(KoanIcons.Link, null, tint = palette.TextSecondary, modifier = Modifier.size(SearchTokens.ChipIcon)) }
        Spacer(Modifier.width(SearchTokens.RowPadding))
        Column(Modifier.weight(1f)) {
            Text(
                "Open link you copied",
                color = palette.TextPrimary,
                fontSize = SearchTokens.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                link.host,
                color = palette.TextSecondary,
                fontSize = SearchTokens.LabelSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
