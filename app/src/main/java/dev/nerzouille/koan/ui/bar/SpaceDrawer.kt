package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.overview.DrawerRow
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.overview.SpaceModel
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.overview.CurrentTabDot
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.theme.BrandSpinner
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs

/** The drawer's footer: account and sync, quietly. */
@Immutable
sealed interface DrawerStatus {
    data object None : DrawerStatus
    data object Connect : DrawerStatus
    data object Syncing : DrawerStatus
    data class Synced(val at: Long, val failed: Boolean = false, val offline: Boolean = false) : DrawerStatus
    data object SignInAgain : DrawerStatus
}

/**
 * Essentials grid on top, pinned rows (with Zen folders) below, the sync row at the bottom. One
 * page per space sliding with the space pager. All spaces are laid out so the sheet keeps the
 * height of the tallest one while switching.
 */
@Composable
fun DrawerContent(
    spaces: List<SpaceModel>,
    pager: SpacePager,
    drawer: SheetState,
    status: DrawerStatus,
    onOpenTab: (spaceIndex: Int, tabId: String) -> Unit,
    onToggleFolder: (folderId: String, collapsed: Boolean) -> Unit,
    onStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pages are measured at their natural height; the box is as tall as the tallest one (capped at
    // the full detent) and each page hangs from the sheet's top edge, clipped at the bar.
    var heights by remember { mutableStateOf(IntArray(0)) }
    LaunchedEffect(drawer, pager) {
        snapshotFlow { heights to pager.settled }.collect { (h, settled) -> drawer.refit(h, settled) }
    }
    Layout(
        content = {
            spaces.forEachIndexed { i, space ->
                DrawerPage(
                    space = space,
                    status = status,
                    interactive = drawer.open && i == pager.settled,
                    onOpenTab = { onOpenTab(i, it) },
                    onToggleFolder = onToggleFolder,
                    onStatus = onStatus,
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .graphicsLayer {
                val p = drawer.progress
                alpha = p
                // No offscreen buffer for the fade (the GPU would keep it cached; see DepthStack).
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val measured = IntArray(placeables.size) { placeables[it].height }
        if (!measured.contentEquals(heights)) heights = measured
        val tallest = placeables.maxOfOrNull { it.height } ?: 0
        val h = minOf(tallest, drawer.fullCapPx.toInt()).coerceAtLeast(0)
        if (drawer.layoutPx != h.toFloat()) drawer.layoutPx = h.toFloat()
        layout(constraints.maxWidth, h) {
            placeables.forEachIndexed { i, p ->
                p.placeWithLayer(0, 0) {
                    val f = i - pager.pos
                    translationX = f * (size.width + PagerTokens.PageGap.toPx())
                    alpha = if (abs(f) >= 1f) 0f else 1f
                    val slide = (1f - drawer.progress) * DrawerTokens.Gap.toPx() * 3f
                    translationY = drawer.sheetTop() + slide - if (i == pager.settled) drawer.scroll else 0f
                }
            }
        }
    }
}

@Composable
private fun DrawerPage(
    space: SpaceModel,
    status: DrawerStatus,
    interactive: Boolean,
    onOpenTab: (String) -> Unit,
    onToggleFolder: (String, Boolean) -> Unit,
    onStatus: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(DrawerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(DrawerTokens.Gap),
    ) {
        if (space.essentials.isEmpty() && space.pinned.isEmpty()) {
            Text(
                "No essentials or pinned tabs in ${space.name}",
                color = KoanTheme.colors.TextSecondary,
                fontSize = DrawerTokens.PinnedTitleSize,
                modifier = Modifier.padding(vertical = DrawerTokens.Padding),
            )
        }
        space.essentials.take(8).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DrawerTokens.EssentialGap)) {
                row.forEach { tab ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(DrawerTokens.EssentialHeight)
                            .clip(RoundedCornerShape(DrawerTokens.EssentialRadius))
                            .background(KoanTheme.colors.Glass)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Favicon(tab.url, DrawerTokens.EssentialFavicon, icon = tab.icon, staticIcon = tab.staticIcon) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (space.pinned.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DrawerTokens.PinnedRadius))
                    .background(KoanTheme.colors.Glass),
            ) {
                space.pinned.forEach { row ->
                    key(row.key) {
                        when (row) {
                            is DrawerRow.Tab -> PinnedRow(row, interactive, onOpenTab)
                            is DrawerRow.Folder -> FolderRow(row, interactive) { onToggleFolder(row.id, !row.collapsed) }
                        }
                    }
                }
            }
        }
        StatusRow(status, interactive, onStatus)
    }
}

@Composable
private fun PinnedRow(row: DrawerRow.Tab, interactive: Boolean, onOpenTab: (String) -> Unit) {
    val tab = row.card
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.PinnedRowHeight)
            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier)
            .padding(start = DrawerTokens.PinnedPadding + DrawerTokens.FolderIndent * row.depth, end = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(tab.url, DrawerTokens.PinnedFavicon, icon = tab.icon, staticIcon = tab.staticIcon)
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        Text(
            tab.title,
            color = KoanTheme.colors.TextPrimary,
            fontSize = DrawerTokens.PinnedTitleSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (tab.active) {
            CurrentTabDot()
            Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        }
        if (row.depth == 0) Icon(KoanIcons.Pin, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedGlyph))
    }
}

/** A Zen folder: tap to fold/unfold its rows (remembered on the phone, never synced). */
@Composable
private fun FolderRow(row: DrawerRow.Folder, interactive: Boolean, onToggle: () -> Unit) {
    val turn = animateFloatAsState(if (row.collapsed) -90f else 0f, Motion.FolderChevron, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.PinnedRowHeight)
            .then(if (interactive) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(start = DrawerTokens.PinnedPadding + DrawerTokens.FolderIndent * row.depth, end = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(DrawerTokens.PinnedFavicon), contentAlignment = Alignment.Center) {
            if (row.icon.isNullOrBlank()) {
                Icon(KoanIcons.Folder, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedFavicon))
            } else {
                ZenIcon(row.icon, DrawerTokens.PinnedFavicon, tint = KoanTheme.colors.TextSecondary)
            }
        }
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        // Name and count take the free width, so the chevron always sits at the right edge.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name.ifBlank { "Folder" },
                color = KoanTheme.colors.TextPrimary,
                fontSize = DrawerTokens.PinnedTitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(DrawerTokens.Gap))
            Text("${row.count}", color = KoanTheme.colors.TextSecondary, fontSize = DrawerTokens.FolderCountSize)
        }
        Icon(
            KoanIcons.ChevronDown, null, tint = KoanTheme.colors.TextSecondary,
            modifier = Modifier.size(DrawerTokens.PinnedGlyph).graphicsLayer { rotationZ = turn.value },
        )
    }
}

/** "Synced 4 min ago", "Syncing…", or a quiet amber "Sign in again". Tap = act on it. */
@Composable
private fun StatusRow(status: DrawerStatus, interactive: Boolean, onClick: () -> Unit) {
    val (text, attention) = status.label() ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.StatusRowHeight)
            .clip(CircleShape)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (attention) {
            Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanTheme.colors.Attention))
            Spacer(Modifier.width(DrawerTokens.Gap))
        } else if (status == DrawerStatus.Syncing) {
            BrandSpinner(FirstRunTokens.BadgeSpinner)
            Spacer(Modifier.width(DrawerTokens.Gap))
        }
        Text(text, color = KoanTheme.colors.TextSecondary, fontSize = DrawerTokens.StatusTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** What the status says, and whether it needs attention (amber). Null = nothing to show. */
fun DrawerStatus.label(): Pair<String, Boolean>? = when (this) {
    DrawerStatus.None -> null
    DrawerStatus.Connect -> "Connect your Zen Spaces" to false
    DrawerStatus.Syncing -> "Syncing with Zen…" to false
    DrawerStatus.SignInAgain -> "Sign in again to keep your Zen Spaces in sync" to true
    is DrawerStatus.Synced -> when {
        offline -> "Offline · showing your spaces from ${ago(at)}" to true
        failed -> "Couldn't sync · tap to retry" to true
        else -> "Synced ${ago(at)} · Sync now" to false
    }
}

private fun ago(at: Long): String {
    if (at <= 0L) return "never"
    val minutes = (System.currentTimeMillis() - at) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}
