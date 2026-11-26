package dev.nerzouille.koan.ui.start

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanDivider
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.search.CopiedLink
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.StartPageTokens

/** A page visited lately, for the start page's quick rows. */
@Immutable
data class RecentPage(val url: String, val title: String)

/** The last download, if it finished within the hour: its file name, where it came from. */
@Immutable
data class RecentDownload(val name: String, val url: String)

/**
 * The start page's quick rows: the latest pages visited ([pages]), the last download if recent
 * ([download]), and the link just copied ([copied], shown right on the field).
 */
@Immutable
data class StartQuick(
    val pages: List<RecentPage> = emptyList(),
    val download: RecentDownload? = null,
    val copied: CopiedLink? = null,
    val onOpenUrl: (String) -> Unit = {},
    val onOpenDownload: () -> Unit = {},
)

/** "Recent": the pages, then the download, in one card above the space's essentials. */
@Composable
internal fun QuickRows(quick: StartQuick, interactive: Boolean) {
    if (quick.pages.isEmpty() && quick.download == null) return
    val palette = KoanTheme.colors
    Text("Recent", color = palette.TextSecondary, fontSize = StartPageTokens.LabelSize, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(StartPageTokens.ContinueLabelGap))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(StartPageTokens.ContinueRadius)).background(palette.Glass)) {
        quick.pages.forEachIndexed { i, page ->
            if (i > 0) KoanDivider(inset = StartPageTokens.ContinuePadding * 2 + StartPageTokens.QuickRowFavicon)
            val host = displayHost(page.url)
            QuickRow(page.title.ifBlank { host }, host, interactive, onClick = { quick.onOpenUrl(page.url) }) {
                Favicon(page.url, StartPageTokens.QuickRowFavicon)
            }
        }
        quick.download?.let { d ->
            if (quick.pages.isNotEmpty()) KoanDivider(inset = StartPageTokens.ContinuePadding * 2 + StartPageTokens.QuickRowFavicon)
            QuickRow(d.name, "Downloaded · ${displayHost(d.url)}", interactive, onClick = quick.onOpenDownload) {
                Glyph(KoanIcons.Download)
            }
        }
    }
    Spacer(Modifier.height(StartPageTokens.SectionTop))
}

@Composable
private fun QuickRow(title: String, subtitle: String, interactive: Boolean, onClick: () -> Unit, leading: @Composable () -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(StartPageTokens.QuickRowHeight)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = StartPageTokens.ContinuePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(StartPageTokens.QuickRowFavicon), contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(StartPageTokens.ContinuePadding))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = palette.TextPrimary,
                fontSize = StartPageTokens.LabelSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = palette.TextSecondary,
                    fontSize = StartPageTokens.LabelSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Glyph(icon: ImageVector) {
    Icon(icon, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(StartPageTokens.QuickRowFavicon))
}

/** The latest pages to offer: newest first, without [skip] (the tab under the start page), at most [max]. */
fun recentPagesToShow(visits: List<RecentPage>, skip: String?, max: Int): List<RecentPage> =
    visits.filter { it.url.isNotBlank() && it.url != skip && !it.url.startsWith("about:") }.distinctBy { it.url }.take(max)
