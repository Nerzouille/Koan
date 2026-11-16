package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.downloads.fileOpenLook
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.CardCancel
import dev.nerzouille.koan.ui.kit.FileGlyph
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/**
 * A download of a file that is already there: the same card as the download confirmation. The
 * file's thumbnail (or its type), its name (a tap on it or on the pencil edits it in place: the
 * extension kept, the base name selected; Done or a choice confirms), the folder, then Open (the
 * file already there; its glyph by type), Replace, Keep both ("name (1).ext"), and Cancel under the
 * list. [size]: the new download's size. [thumbModifier]: the thumbnail's shared bounds when the
 * card morphs from the image card.
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
    contentType: String? = null,
    thumbModifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    val base = remember(name) { name.substringBeforeLast('.', name).length }
    var field by remember(name) { mutableStateOf(TextFieldValue(name, TextRange(0, base))) }
    var editing by remember(name) { mutableStateOf(false) }
    val finalName = field.text.trim().ifEmpty { name }
    fun startEditing() {
        if (!interactive) return
        // Only the name before the extension is selected: typing replaces it, the extension stays.
        val b = field.text.substringBeforeLast('.', field.text).length
        field = field.copy(selection = TextRange(0, b))
        editing = true
    }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                thumbModifier
                    .size(SheetTokens.RowIconBox + SheetTokens.Gap * 2)
                    .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
                    .background(palette.GroupedRow),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(SheetTokens.RowIconBox + SheetTokens.Gap * 2))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FileGlyph(name, SheetTokens.RowGlyph, SheetTokens.RowSubtitleSize * 0.7f)
                    }
                }
            }
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column(Modifier.weight(1f)) {
                Text("Already downloaded", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val style = KoanType.headline(SheetTokens.CardTitleSize).copy(color = palette.TextPrimary)
                    if (editing) {
                        val focus = remember { FocusRequester() }
                        BasicTextField(
                            field, { field = it },
                            textStyle = style,
                            singleLine = true,
                            cursorBrush = SolidColor(palette.TextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { editing = false }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focus)
                                .drawBehind {
                                    val box = drawContext.size
                                    val y = box.height + SheetTokens.Divider.toPx() * 2
                                    drawLine(palette.TextSecondary, Offset(0f, y), Offset(box.width, y), SheetTokens.Divider.toPx())
                                },
                        )
                        LaunchedEffect(Unit) { focus.requestFocus() }
                    } else {
                        Text(
                            finalName, color = palette.TextPrimary, style = style, maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                            modifier = Modifier.weight(1f, fill = false).clickable(enabled = interactive, onClick = ::startEditing),
                        )
                    }
                    // The pencil: a real tap target next to the name.
                    Box(
                        Modifier.size(SheetTokens.EditTarget).clip(CircleShape).clickable(enabled = interactive) { if (editing) editing = false else startEditing() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (editing) KoanIcons.Check else KoanIcons.Edit, if (editing) "Done" else "Rename", tint = palette.TextSecondary, modifier = Modifier.size(SheetTokens.EditGlyph)) }
                }
                Text("In $folder", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val (openIcon, openLabel) = fileOpenLook(name, contentType)
        SheetGroup {
            SheetRow(openIcon, openLabel, interactive, subtitle = "The file already there", onClick = onOpen)
            SheetDivider()
            SheetRow(KoanIcons.Download, "Replace", interactive, subtitle = listOfNotNull("Downloads it again over the old one", size).joinToString(" · "), onClick = { editing = false; onReplace(finalName) })
            SheetDivider()
            SheetRow(KoanIcons.Plus, "Keep both", interactive, subtitle = listOfNotNull("Saves as ${keepBothName(finalName)}", size).joinToString(" · "), onClick = { editing = false; onKeepBoth(finalName) })
        }
        CardCancel(interactive, onCancel)
    }
}
