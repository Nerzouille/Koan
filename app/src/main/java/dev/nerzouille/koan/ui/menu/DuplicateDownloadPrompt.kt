package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanField
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/**
 * A download of a file that is already there: the same card as the download confirmation. The
 * file's thumbnail (or its type), its name (the pencil edits it), the folder, then Open (the file
 * already there), Replace, Keep both ("name (1).ext"), Cancel. [size]: the new download's size.
 */
@Composable
fun DuplicateDownloadPrompt(
    name: String,
    keepBothName: (String) -> String,
    folder: String,
    thumbnail: ImageBitmap?,
    size: String?,
    interactive: Boolean,
    onOpen: () -> Unit,
    onReplace: (String) -> Unit,
    onKeepBoth: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val palette = KoanTheme.colors
    var edited by remember(name) { mutableStateOf(name) }
    var editing by remember(name) { mutableStateOf(false) }
    val finalName = edited.trim().ifEmpty { name }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(SheetTokens.RowIconBox + SheetTokens.Gap * 2)
                    .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
                    .background(palette.GroupedRow),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(SheetTokens.RowIconBox + SheetTokens.Gap * 2))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(KoanIcons.File, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
                        val ext = name.substringAfterLast('.', "").take(4).uppercase()
                        if (ext.isNotEmpty()) Text(ext, color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize * 0.7f, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column(Modifier.weight(1f)) {
                Text("Already downloaded", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
                if (!editing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(finalName, color = palette.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize), maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(SheetTokens.Gap / 2))
                        Box(
                            Modifier.size(SheetTokens.RowIconBox).clip(CircleShape).clickable(enabled = interactive) { editing = true },
                            contentAlignment = Alignment.Center,
                        ) { Icon(KoanIcons.Edit, "Rename", tint = palette.TextSecondary, modifier = Modifier.size(SheetTokens.RowGlyph * 0.8f)) }
                    }
                }
                Text("In $folder", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (editing) {
            val focus = remember { FocusRequester() }
            KoanField(edited, { edited = it }, "File name", Modifier.fillMaxWidth(), inputModifier = Modifier.focusRequester(focus))
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        SheetGroup {
            SheetRow(KoanIcons.Open, "Open", interactive, subtitle = "The file already there", onClick = onOpen)
            SheetDivider()
            SheetRow(KoanIcons.Download, "Replace", interactive, subtitle = listOfNotNull("Downloads it again over the old one", size).joinToString(" · "), onClick = { onReplace(finalName) })
            SheetDivider()
            SheetRow(KoanIcons.Plus, "Keep both", interactive, subtitle = listOfNotNull("Saves as ${keepBothName(finalName)}", size).joinToString(" · "), onClick = { onKeepBoth(finalName) })
        }
        QuietButton("Cancel", onClick = { if (interactive) onCancel() }, modifier = Modifier.fillMaxWidth())
    }
}
