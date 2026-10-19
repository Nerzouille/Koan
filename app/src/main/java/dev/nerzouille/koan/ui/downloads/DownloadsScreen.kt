package dev.nerzouille.koan.ui.downloads

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.engine.downloads.Downloads
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.DayCard
import dev.nerzouille.koan.ui.kit.ListPage
import dev.nerzouille.koan.ui.kit.byDay
import dev.nerzouille.koan.ui.settings.SettingsDivider
import dev.nerzouille.koan.ui.settings.SettingsSection
import dev.nerzouille.koan.ui.theme.BrandHorizontal
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status

/**
 * Downloads (design 7i): what is downloading (gradient progress, pause / resume, cancel), then
 * the finished ones by day; a finished row opens to Open / Share / Delete. Search and Clear sit at
 * the bottom, in thumb reach. [folder] names where files go ("Download", or the picked folder).
 */
@Composable
fun DownloadsScreen(
    downloads: List<DownloadState>,
    folder: String,
    actions: Downloads,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val shown = downloads
        .filterNot { it.private || it.status == Status.CANCELLED }
        .filter { query.isBlank() || it.fileName.orEmpty().contains(query.trim(), ignoreCase = true) }
        .sortedByDescending { it.createdTime }
    val (running, done) = shown.partition { it.status in RUNNING }

    ListPage(
        title = "Downloads",
        query = query,
        searchHint = "Search downloads",
        onQuery = { query = it },
        onClear = if (downloads.isNotEmpty()) onClear else null,
        onClose = onClose,
        modifier = modifier,
        headerTrailing = {
            Row(
                Modifier
                    .height(DownloadTokens.ChipHeight)
                    .clip(CircleShape)
                    .background(palette.Card)
                    .clickable(onClick = actions::showFolder)
                    .padding(horizontal = SettingsTokens.RowInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KoanIcons.Folder, null, tint = palette.TextSecondary, modifier = Modifier.size(SettingsTokens.RowGlyph * 0.8f))
                Spacer(Modifier.width(SettingsTokens.Divider * 6))
                Text(folder, color = palette.TextSecondary, fontSize = DownloadTokens.ChipTextSize, maxLines = 1)
            }
        },
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SettingsTokens.Padding),
        ) {
            if (shown.isEmpty()) {
                Text(
                    if (query.isBlank()) "Files you download show up here." else "No download matches “${query.trim()}”.",
                    color = palette.TextSecondary,
                    fontSize = SettingsTokens.RowValueSize,
                    modifier = Modifier.padding(top = SettingsTokens.SectionTop),
                )
            }
            if (running.isNotEmpty()) {
                SettingsSection("In progress")
                DayCard { running.forEachIndexed { i, d -> if (i > 0) SettingsDivider(); RunningRow(d, actions) } }
            }
            // One card per day, newest first.
            byDay(done, { it.createdTime }).forEach { (day, items) ->
                SettingsSection(day)
                DayCard {
                    items.forEachIndexed { i, d ->
                        if (i > 0) SettingsDivider()
                        DoneRow(d, open = expanded == d.id, actions = actions) { expanded = if (expanded == d.id) null else d.id }
                    }
                }
            }
            Spacer(Modifier.height(SettingsTokens.SectionTop))
        }
    }
}

private val RUNNING = setOf(Status.INITIATED, Status.DOWNLOADING, Status.PAUSED, Status.FAILED)

@Composable
private fun RunningRow(d: DownloadState, actions: Downloads) {
    val palette = KoanTheme.colors
    val total = d.contentLength
    val fraction = if (total != null && total > 0) (d.currentBytesCopied.toFloat() / total).coerceIn(0f, 1f) else null
    val state = when (d.status) {
        Status.PAUSED -> "Paused"
        Status.FAILED -> "Couldn't finish"
        Status.INITIATED -> "Starting…"
        else -> fraction?.let { "${(it * 100).toInt()}%" } ?: ""
    }
    Row(Modifier.fillMaxWidth().padding(vertical = DownloadTokens.RowPaddingV), verticalAlignment = Alignment.CenterVertically) {
        FileTile(d)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.fileName.orEmpty(), color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(state, color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize)
            }
            // Koan is working: the brand gradient fills the bar.
            val track = palette.Glass
            Box(
                Modifier
                    .padding(top = DownloadTokens.ProgressTop, bottom = DownloadTokens.ProgressTop / 2)
                    .fillMaxWidth()
                    .height(DownloadTokens.ProgressHeight)
                    .drawBehind {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(track, cornerRadius = r)
                        val f = fraction ?: 0f
                        if (f > 0f) drawRoundRect(BrandHorizontal, size = Size(size.width * f, size.height), cornerRadius = r)
                    },
            )
            Text(
                listOfNotNull(sizeText(d.currentBytesCopied) + (total?.let { " of ${sizeText(it)}" } ?: ""), host(d)).joinToString(" · "),
                color = palette.TextSecondary,
                fontSize = SettingsTokens.RowSubtitleSize,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(DownloadTokens.ActionGap))
        when (d.status) {
            Status.PAUSED -> RoundButton(KoanIcons.Play) { actions.resume(d) }
            Status.FAILED -> RoundButton(KoanIcons.Retry) { actions.retry(d) }
            else -> RoundButton(KoanIcons.Pause) { actions.pause(d) }
        }
        Spacer(Modifier.width(DownloadTokens.ActionGap))
        RoundButton(KoanIcons.Close) { actions.cancel(d) }
    }
}

@Composable
private fun DoneRow(d: DownloadState, open: Boolean, actions: Downloads, onToggle: () -> Unit) {
    val palette = KoanTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = DownloadTokens.RowPaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTile(d)
            Spacer(Modifier.width(DownloadTokens.RowGap))
            Column(Modifier.weight(1f)) {
                Text(d.fileName.orEmpty(), color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull((d.contentLength ?: d.currentBytesCopied).let(::sizeText), host(d)).joinToString(" · "),
                    color = palette.TextSecondary,
                    fontSize = SettingsTokens.RowSubtitleSize,
                    maxLines = 1,
                )
            }
        }
        if (open) {
            Row(
                Modifier.padding(bottom = DownloadTokens.RowPaddingV),
                horizontalArrangement = Arrangement.spacedBy(DownloadTokens.ActionGap),
            ) {
                Action(KoanIcons.Open, "Open", primary = true, modifier = Modifier.weight(1f)) { actions.open(d) }
                Action(KoanIcons.Share, "Share", modifier = Modifier.weight(1f)) { actions.share(d) }
                Action(KoanIcons.Trash, "Delete", destructive = true, modifier = Modifier.weight(1f)) { actions.delete(d) }
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, modifier: Modifier, primary: Boolean = false, destructive: Boolean = false, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    val color = when {
        primary -> palette.OnPrimary
        destructive -> palette.Destructive
        else -> palette.TextPrimary
    }
    Row(
        modifier
            .height(DownloadTokens.ActionHeight)
            .clip(CircleShape)
            .background(if (primary) palette.Primary else palette.GroupedRow)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(SettingsTokens.RowGlyph * 0.85f))
        Spacer(Modifier.width(SettingsTokens.Divider * 6))
        Text(label, color = color, fontSize = SettingsTokens.RowValueSize, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(DownloadTokens.RoundButton)
            .clip(CircleShape)
            .background(KoanTheme.colors.GroupedRow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph * 0.8f))
    }
}

/** A file's type at a glance: a document glyph over its extension. */
@Composable
fun FileTile(d: DownloadState, background: Color = KoanTheme.colors.GroupedRow) {
    val ext = d.fileName.orEmpty().substringAfterLast('.', "").take(4).uppercase()
    Column(
        Modifier
            .size(DownloadTokens.FileTile)
            .clip(RoundedCornerShape(DownloadTokens.FileTileRadius))
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(KoanIcons.File, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(DownloadTokens.FileGlyph))
        if (ext.isNotEmpty()) Text(ext, color = KoanTheme.colors.TextSecondary, fontSize = DownloadTokens.FileExtSize, fontWeight = FontWeight.SemiBold)
    }
}

fun sizeText(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "${bytes shr 10} KB"
    else -> "$bytes B"
}

/** Where it came from: the page that linked it, else the file's own host. */
fun host(d: DownloadState): String? =
    d.referrerUrl?.let(::displayHost)?.takeIf { it.isNotBlank() && "." in it } ?: displayHost(d.url).ifBlank { null }
