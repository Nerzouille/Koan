package dev.nerzouille.koan.ui.downloads

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.nerzouille.koan.ui.kit.EmptyLine
import dev.nerzouille.koan.ui.kit.FileGlyph
import dev.nerzouille.koan.ui.kit.LibraryRowFrame
import dev.nerzouille.koan.ui.kit.LibraryRowSubtitle
import dev.nerzouille.koan.ui.kit.LibraryRowTitle
import dev.nerzouille.koan.ui.kit.ListPage
import dev.nerzouille.koan.ui.kit.SelectionAction
import dev.nerzouille.koan.ui.kit.SelectionBar
import dev.nerzouille.koan.ui.kit.SelectionHeader
import dev.nerzouille.koan.ui.kit.SwipeRow
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.byDay
import dev.nerzouille.koan.ui.kit.dayRowShape
import dev.nerzouille.koan.ui.kit.rememberSelection
import dev.nerzouille.koan.ui.kit.undoToast
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
 * the finished ones by day, with the library rules (ui/kit/LibraryRow): a tap opens the file; a
 * swipe left to right hides it from the list (the file stays), right to left deletes the file
 * (both with Undo: the file is only deleted once the toast is gone); a long press enters
 * selection mode (Open, Copy link, Share, Delete). Search and Clear sit at the bottom, in thumb reach.
 * [folder] names where files go ("Download", or the picked folder).
 */
@Composable
fun DownloadsScreen(
    downloads: List<DownloadState>,
    folder: String,
    actions: Downloads,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onToast: (Toast) -> Unit,
    onCopyLink: (String) -> Unit,
    /** Asks first (deleting several files): title, text, the confirm button, what it does. */
    onConfirm: (title: String, text: String, confirm: String, action: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    /** A finished video: Koan's player (false = not a video, or it can't). */
    onPlayVideo: (DownloadState) -> Boolean = { false },
) {
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    val hidden by actions.hidden.collectAsState()
    val shown = downloads
        .filterNot { it.status == Status.CANCELLED || it.id in hidden }
        .filter { query.isBlank() || it.fileName.orEmpty().contains(query.trim(), ignoreCase = true) }
        .sortedByDescending { it.createdTime }
    val (running, done) = shown.partition { it.status in RUNNING }
    val selection = rememberSelection<String>()
    SideEffect { selection.keepOnly(done.mapTo(HashSet()) { it.id }) }
    val checked = done.filter { it.id in selection }
    fun open(d: DownloadState) {
        if (onPlayVideo(d)) return
        if (!actions.open(d)) onToast(Toast("Couldn't open this file", detail = "It may have been moved or deleted"))
    }
    fun share(list: List<DownloadState>) {
        val ok = if (list.size == 1) actions.share(list[0]) else actions.shareAll(list)
        if (!ok) onToast(Toast(if (list.size == 1) "Couldn't share this file" else "Couldn't share these files"))
    }
    fun hide(list: List<DownloadState>) {
        list.forEach { actions.removeLater(it, deleteFile = false) }
        val what = if (list.size == 1) "Hidden from the list" else "${list.size} hidden from the list"
        val detail = if (list.size == 1) "The file stays in $folder" else "The files stay in $folder"
        onToast(undoToast(what, detail, onGone = { actions.commit(list) }) { list.forEach(actions::restore) })
    }
    fun delete(list: List<DownloadState>) {
        list.forEach { actions.removeLater(it, deleteFile = true) }
        onToast(undoToast(if (list.size == 1) "File deleted" else "${list.size} files deleted", onGone = { actions.commit(list) }) { list.forEach(actions::restore) })
    }
    fun act(run: () -> Unit) = { selection.clear(); run() }
    // Hide is a swipe only (left to right); the bar keeps what a tap can't do.
    val bar: List<SelectionAction> = if (checked.size == 1) {
        val d = checked[0]
        val (icon, label) = openLook(d)
        listOf(
            SelectionAction(icon, label, onClick = act { open(d) }),
            SelectionAction(KoanIcons.Link, "Copy link", onClick = act { onCopyLink(d.url) }),
            SelectionAction(KoanIcons.Share, "Share", onClick = act { share(listOf(d)) }),
            SelectionAction(KoanIcons.Trash, "Delete", destructive = true, onClick = act { delete(listOf(d)) }),
        )
    } else {
        listOf(
            SelectionAction(KoanIcons.Share, "Share", onClick = act { share(checked) }),
            SelectionAction(KoanIcons.Trash, "Delete", destructive = true) {
                val list = checked
                onConfirm("Delete ${list.size} files?", "They're deleted from $folder.", "Delete") {
                    selection.clear()
                    delete(list)
                }
            },
        )
    }

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
        selectionHeader = if (!selection.active) null else {
            { SelectionHeader(checked.size, interactive = true, onCancel = selection::clear, onSelectAll = { selection.set(done.mapTo(HashSet()) { it.id }) }) }
        },
        selectionBar = if (!selection.active) null else {
            { SelectionBar(bar, interactive = true) }
        },
    ) {
        BackHandler(enabled = selection.active, onBack = selection::clear)
        val selecting = selection.active
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SettingsTokens.Padding),
        ) {
            if (shown.isEmpty()) {
                EmptyLine(
                    if (query.isBlank()) "Files you download show up here." else "No download matches “${query.trim()}”.",
                    Modifier.padding(top = SettingsTokens.SectionTop),
                )
            }
            if (running.isNotEmpty()) {
                SettingsSection("In progress")
                DayCard { running.forEachIndexed { i, d -> if (i > 0) SettingsDivider(); RunningRow(d, actions) } }
            }
            // One card per day, newest first.
            byDay(done, { it.createdTime }).forEach { (day, items) ->
                SettingsSection(day)
                items.forEachIndexed { i, d ->
                    key(d.id) {
                        SwipeRow(
                            onDelete = { delete(listOf(d)) },
                            onHide = { hide(listOf(d)) },
                            enabled = !selecting,
                            shape = dayRowShape(first = i == 0, last = i == items.lastIndex),
                        ) {
                            DoneRow(
                                d, first = i == 0,
                                checked = if (selecting) d.id in selection else null,
                                onTap = { if (selecting) selection.toggle(d.id) else open(d) },
                                onLong = { if (selecting) selection.toggle(d.id) else selection.start(d.id) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(SettingsTokens.SectionTop))
        }
    }
}

/** What opening [d] does, by its type: play a video or a song, view a picture, install an app, else read it. */
private fun openLook(d: DownloadState): Pair<ImageVector, String> = fileOpenLook(d.fileName.orEmpty(), d.contentType)

/** Same, from a file's name and type (the duplicate card's Open row too). */
fun fileOpenLook(fileName: String, contentType: String?): Pair<ImageVector, String> {
    val type = contentType.orEmpty().lowercase()
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        type.startsWith("video/") || type.startsWith("audio/") || ext in MEDIA -> KoanIcons.Play to "Open"
        type.startsWith("image/") || ext in IMAGES -> KoanIcons.Image to "Open"
        type == "application/vnd.android.package-archive" || ext == "apk" -> KoanIcons.Install to "Install"
        else -> KoanIcons.File to "Open"
    }
}

private val MEDIA = setOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "mp3", "m4a", "aac", "ogg", "opus", "flac", "wav")
private val IMAGES = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "avif", "bmp", "svg")

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
                if (d.private) PrivateMark()
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

/**
 * A download from a private tab: a small mask before its name. It stays in the list until Koan
 * restarts (the file stays in Download).
 */
@Composable
private fun PrivateMark() {
    androidx.compose.material3.Icon(
        KoanIcons.Private, "Private",
        tint = KoanTheme.colors.TextSecondary,
        modifier = Modifier.padding(end = DownloadTokens.PrivateGap).size(DownloadTokens.PrivateGlyph),
    )
}

@Composable
private fun DoneRow(d: DownloadState, first: Boolean, checked: Boolean?, onTap: () -> Unit, onLong: () -> Unit) {
    LibraryRowFrame(first, onClick = onTap, onLongClick = onLong, checked = checked) {
        FileTile(d)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.private) PrivateMark()
                LibraryRowTitle(d.fileName.orEmpty())
            }
            LibraryRowSubtitle(listOfNotNull((d.contentLength ?: d.currentBytesCopied).let(::sizeText), host(d)).joinToString(" · "))
        }
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
    Column(
        Modifier
            .size(DownloadTokens.FileTile)
            .clip(RoundedCornerShape(DownloadTokens.FileTileRadius))
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FileGlyph(d.fileName.orEmpty(), DownloadTokens.FileGlyph, DownloadTokens.FileExtSize)
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
