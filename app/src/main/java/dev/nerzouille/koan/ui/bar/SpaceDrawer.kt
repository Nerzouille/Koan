package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
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
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The essentials/pinned drawer that grows out of the overview's bar. */
@Stable
class DrawerState(private val scope: CoroutineScope) {
    /** 0 closed … 1 open; follows the finger 1:1 while dragging. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** Where it is going: flips on release or tap. Decides whether the sheet takes input. */
    var open by mutableStateOf(false)
        private set

    /** Height of the drawer content (px): the distance a full open/close travels. */
    var heightPx by mutableFloatStateOf(1f)
        internal set

    private var job: Job? = null

    fun grab() {
        job?.cancel()
    }

    /** Finger moved by [dyPx] (negative = up = open). */
    fun dragBy(dyPx: Float) {
        progress = (progress - dyPx / heightPx).coerceIn(0f, 1f)
    }

    /** Finger up with vertical velocity [vyPx] px/s: settle open or closed. */
    fun release(vyPx: Float, flingPx: Float) {
        val target = when {
            vyPx < -flingPx -> true
            vyPx > flingPx -> false
            else -> progress > 0.5f
        }
        animateTo(target, -vyPx / heightPx)
    }

    fun animateTo(open: Boolean, velocity: Float = 0f) {
        this.open = open
        job?.cancel()
        job = scope.launch {
            animate(progress, if (open) 1f else 0f, velocity, Motion.Drawer) { v, _ -> progress = v.coerceIn(0f, 1f) }
        }
    }

    fun snapClosed() {
        job?.cancel()
        progress = 0f
        open = false
    }
}

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
    drawer: DrawerState,
    status: DrawerStatus,
    onOpenTab: (spaceIndex: Int, tabId: String) -> Unit,
    onToggleFolder: (folderId: String, collapsed: Boolean) -> Unit,
    onStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { drawer.heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                val p = drawer.progress
                alpha = p
                translationY = (1f - p) * DrawerTokens.Gap.toPx() * 3f
            },
    ) {
        spaces.forEachIndexed { i, space ->
            DrawerPage(
                space = space,
                status = status,
                interactive = drawer.open && i == pager.settled,
                onOpenTab = { onOpenTab(i, it) },
                onToggleFolder = onToggleFolder,
                onStatus = onStatus,
                modifier = Modifier.graphicsLayer {
                    val f = i - pager.pos
                    translationX = f * (size.width + PagerTokens.PageGap.toPx())
                    alpha = if (abs(f) >= 1f) 0f else 1f
                },
            )
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(DrawerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(DrawerTokens.Gap),
    ) {
        if (space.essentials.isEmpty() && space.pinned.isEmpty()) {
            Text(
                "No essentials or pinned tabs in ${space.name}",
                color = KoanColors.TextSecondary,
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
                            .background(KoanColors.Glass)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Favicon(tab.url, DrawerTokens.EssentialFavicon, icon = tab.icon, staticIcon = tab.staticIcon) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (space.pinned.isNotEmpty()) {
            // Long lists scroll inside the sheet (no stretch overscroll, so a drag at the top of the
            // list still reaches the sheet and closes it).
            val maxHeight = DrawerTokens.PinnedRowHeight * DrawerTokens.VISIBLE_PINNED_ROWS
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .clip(RoundedCornerShape(DrawerTokens.PinnedRadius))
                        .background(KoanColors.Glass)
                        .verticalScroll(rememberScrollState(), enabled = interactive),
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
            color = KoanColors.TextPrimary,
            fontSize = DrawerTokens.PinnedTitleSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (tab.active) {
            Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanColors.DefaultAccent))
            Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        }
        if (row.depth == 0) Icon(KoanIcons.Pin, null, tint = KoanColors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedGlyph))
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
                Icon(KoanIcons.Folder, null, tint = KoanColors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedFavicon))
            } else {
                ZenIcon(row.icon, DrawerTokens.PinnedFavicon, tint = KoanColors.TextSecondary)
            }
        }
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        // Name and count take the free width, so the chevron always sits at the right edge.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name.ifBlank { "Folder" },
                color = KoanColors.TextPrimary,
                fontSize = DrawerTokens.PinnedTitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(DrawerTokens.Gap))
            Text("${row.count}", color = KoanColors.TextSecondary, fontSize = DrawerTokens.FolderCountSize)
        }
        Icon(
            KoanIcons.ChevronDown, null, tint = KoanColors.TextSecondary,
            modifier = Modifier.size(DrawerTokens.PinnedGlyph).graphicsLayer { rotationZ = turn.value },
        )
    }
}

/** "Synced 4 min ago", "Syncing…", or a quiet amber "Sign in again". Tap = act on it. */
@Composable
private fun StatusRow(status: DrawerStatus, interactive: Boolean, onClick: () -> Unit) {
    if (status == DrawerStatus.None) return
    val (text, attention) = when (status) {
        DrawerStatus.Connect -> "Connect your Zen Spaces" to false
        DrawerStatus.Syncing -> "Syncing with Zen…" to false
        DrawerStatus.SignInAgain -> "Sign in again to keep your Zen Spaces in sync" to true
        is DrawerStatus.Synced -> when {
            status.offline -> "Offline · showing your spaces from ${ago(status.at)}" to true
            status.failed -> "Couldn't sync · tap to retry" to true
            else -> "Synced ${ago(status.at)} · Sync now" to false
        }
        DrawerStatus.None -> return
    }
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
            Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanColors.Attention))
            Spacer(Modifier.width(DrawerTokens.Gap))
        }
        Text(text, color = KoanColors.TextSecondary, fontSize = DrawerTokens.StatusTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
