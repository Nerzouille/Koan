package dev.nerzouille.koan.ui.menu

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.CardChoices
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/**
 * A page wants to open an app (an "Open app" button, an app's own link): the floating card asks
 * first. "Open in YouTube?", the site, "Always for this site", then Cancel / Open.
 */
@Composable
fun AppLinkPrompt(app: String, icon: Bitmap?, site: String, interactive: Boolean, onCancel: () -> Unit, onOpen: (always: Boolean) -> Unit) {
    val palette = KoanTheme.colors
    var always by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, SheetTokens.RowIconBox + SheetTokens.Gap)
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text("Open in $app?", color = palette.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (site.isNotEmpty()) {
                    Text("$site wants to open the app", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (site.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SheetTokens.GroupRadius))
                    .background(palette.GroupedRow)
                    .clickable(enabled = interactive) { always = !always }
                    .padding(horizontal = SheetTokens.RowPadding)
                    .height(SheetTokens.RowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Always for this site", color = palette.TextPrimary, fontSize = SheetTokens.RowTitleSize, modifier = Modifier.weight(1f))
                KoanToggle(always, if (interactive) ({ always = it }) else null)
            }
        }
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        CardChoices("Open", interactive, onAction = { onOpen(always) }, onCancel = onCancel)
    }
}

/** An app's own icon (or a generic glyph if Android gave none), on a menu row's tile. */
@Composable
fun AppIcon(icon: Bitmap?, size: androidx.compose.ui.unit.Dp) {
    val image = remember(icon) { icon?.asImageBitmap() }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(image, null, Modifier.size(size))
        } else {
            Box(
                Modifier.size(size).clip(RoundedCornerShape(SheetTokens.RowIconRadius)).background(KoanTheme.colors.GroupedRow),
                contentAlignment = Alignment.Center,
            ) { Icon(KoanIcons.Open, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph)) }
        }
    }
}
