package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.CardChoices
import dev.nerzouille.koan.ui.kit.CardHeader
import dev.nerzouille.koan.ui.kit.CardIconTile
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
        CardHeader(
            name, detail, leading = { CardIconTile(KoanIcons.Download) }, alignment = Alignment.Top,
            titleMaxLines = 1, titleOverflow = TextOverflow.MiddleEllipsis, textMaxLines = 1,
        )
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        CardChoices("Download", interactive, onAction = { onDownload() }, onCancel = onCancel)
    }
}
