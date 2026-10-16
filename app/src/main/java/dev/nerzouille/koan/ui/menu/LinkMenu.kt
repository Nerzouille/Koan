package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens
import mozilla.components.concept.engine.HitResult

/** What a long press landed on: a link, an image, or an image that is also a link. */
@Immutable
data class LinkTarget(val link: String?, val image: String?, val label: String) {
    companion object {
        /** Null for what Koan has no menu for (text, phone numbers, media…). */
        fun of(hit: HitResult): LinkTarget? = when (hit) {
            is HitResult.UNKNOWN -> hit.src.takeIf(::isWeb)?.let { LinkTarget(it, null, hit.linkText ?: "") }
            is HitResult.IMAGE_SRC -> LinkTarget(hit.uri.takeIf(::isWeb), hit.src.takeIf(::isWeb), "")
            is HitResult.IMAGE -> hit.src.takeIf(::isWeb)?.let { LinkTarget(null, it, hit.title ?: "") }
            else -> null
        }

        private fun isWeb(url: String) = url.startsWith("http://") || url.startsWith("https://")
    }
}

/** What the long-press menu can do with a [LinkTarget]. */
@Immutable
class LinkActions(
    val openNew: (String) -> Unit,
    val openBackground: (String) -> Unit,
    val copy: (String) -> Unit,
    val share: (String) -> Unit,
)

/** The long-press menu, on a floating card: the link (or image) on top, then what to do with it. */
@Composable
fun LinkMenu(target: LinkTarget, interactive: Boolean, actions: LinkActions) {
    val palette = KoanTheme.colors
    val shown = target.link ?: target.image.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Column(Modifier.padding(horizontal = SheetTokens.RowPadding / 2)) {
            Text(
                target.label.trim().ifEmpty { displayHost(shown) },
                color = palette.TextPrimary,
                fontSize = SheetTokens.RowTitleSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(shown, color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        target.link?.let { link ->
            SheetGroup {
                SheetRow(KoanIcons.Plus, "Open in new tab", interactive, onClick = { actions.openNew(link) })
                SheetDivider()
                SheetRow(KoanIcons.Essentials, "Open in background", interactive, onClick = { actions.openBackground(link) })
                SheetDivider()
                SheetRow(KoanIcons.Link, "Copy link", interactive, onClick = { actions.copy(link) })
                SheetDivider()
                SheetRow(KoanIcons.Share, "Share link", interactive, onClick = { actions.share(link) })
            }
        }
        target.image?.let { image ->
            SheetGroup {
                SheetRow(KoanIcons.Plus, "Open image in new tab", interactive, onClick = { actions.openNew(image) })
                SheetDivider()
                SheetRow(KoanIcons.Link, "Copy image link", interactive, onClick = { actions.copy(image) })
            }
        }
    }
}
