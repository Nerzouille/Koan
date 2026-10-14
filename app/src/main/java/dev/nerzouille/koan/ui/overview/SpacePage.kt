package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs

@Immutable
data class SpaceModel(
    val uuid: String,
    val name: String,
    val icon: String,
    val gradient: List<Color>,
    val essentials: List<CardModel>,
    /** The drawer's pinned list: tabs and folder headers, collapsed folders' tabs left out. */
    val pinned: List<DrawerRow>,
    val stack: List<CardModel>,
    /** Comes from Zen (false = the phone-only space used before sign-in). */
    val zen: Boolean = true,
)

/** One row of the drawer's pinned list. [depth] indents rows inside (nested) folders. */
@Immutable
sealed interface DrawerRow {
    val key: String

    data class Tab(val card: CardModel, val depth: Int, val folderId: String? = null) : DrawerRow {
        override val key get() = card.id
    }

    data class Folder(val id: String, val name: String, val icon: String?, val depth: Int, val collapsed: Boolean, val count: Int) : DrawerRow {
        override val key get() = "folder:$id"
    }
}

/**
 * One space in the overview: compact header and the depth stack. Essentials and pinned tabs live
 * in the bar's drawer. [pageOffset] is this page's distance from the settled one while switching
 * spaces (-1..1).
 */
@Composable
fun SpacePage(
    space: SpaceModel,
    stack: StackState,
    active: Boolean,
    sync: SyncUi,
    pageOffset: () -> Float,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onOpenTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onBadge: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SpaceHeader(space, sync.badge, onBack = onDismiss, onNewTab = onNewTab, onBadge = onBadge)
        val connect = sync.connect && !space.zen
        if (connect && space.stack.isNotEmpty()) ConnectCard(onConnect, compact = true)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    // The incoming stack trails the header slightly (0.85× speed) and grows 0.96 → 1.
                    val f = pageOffset()
                    translationX = -f * size.width * PagerTokens.PARALLAX
                    val s = 1f - PagerTokens.INCOMING_SCALE * abs(f).coerceAtMost(1f)
                    scaleX = s
                    scaleY = s
                },
        ) {
            when {
                space.stack.isEmpty() && sync.firstSync -> SkeletonStack()
                connect && space.stack.isEmpty() -> ConnectCard(onConnect, compact = false, Modifier.align(Alignment.Center))
                else -> DepthStack(
                    cards = space.stack,
                    state = stack,
                    enabled = active,
                    onOpen = onOpenTab,
                    onClose = onCloseTab,
                    onReorder = onReorder,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Placeholder cards in the real stack geometry, breathing slowly while the first sync runs. */
@Composable
internal fun SkeletonStack(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val state = remember { StackState((SkeletonCards.size - 2).toFloat(), scope) }
    val breath = rememberInfiniteTransition(label = "skeleton")
    val alpha = breath.animateFloat(
        initialValue = 1f,
        targetValue = FirstRunTokens.SKELETON_ALPHA_MIN,
        animationSpec = infiniteRepeatable(tween(FirstRunTokens.SKELETON_BREATH_MS), RepeatMode.Reverse),
        label = "alpha",
    )
    DepthStack(
        cards = SkeletonCards,
        state = state,
        enabled = false,
        onOpen = {},
        onClose = {},
        onReorder = {},
        modifier = modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value },
    )
}

/** Compact header: back, space icon + name, sync badge, new tab. Swipe down on it = back to the current tab. */
@Composable
private fun SpaceHeader(space: SpaceModel, badge: SyncBadge, onBack: () -> Unit, onNewTab: () -> Unit, onBadge: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(OverviewTokens.HeaderHeight)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                    total += dy
                    if (total > OverviewTokens.SwipeDownToDismiss.toPx()) {
                        total = Float.NEGATIVE_INFINITY
                        onBack()
                    }
                    change.consume()
                }
            }
            .padding(horizontal = OverviewTokens.HeaderPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIcon(KoanIcons.Back, onBack)
        Box(
            Modifier.size(OverviewTokens.SpaceIconBox).clip(RoundedCornerShape(OverviewTokens.SpaceIconRadius)).background(KoanColors.Glass),
            contentAlignment = Alignment.Center,
        ) { ZenIcon(space.icon, OverviewTokens.SpaceIconGlyph, tint = KoanColors.TextPrimary) }
        Spacer(Modifier.width(OverviewTokens.TitleGap))
        Text(
            space.name,
            color = KoanColors.TextPrimary,
            fontSize = OverviewTokens.TitleSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        SyncBadgeView(badge, onBadge)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .padding(end = OverviewTokens.HeaderPadding)
                .size(OverviewTokens.NewTabButton)
                .clip(CircleShape)
                .background(KoanColors.NewTabButton)
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center,
        ) {
            Icon(KoanIcons.Plus, "New tab", tint = KoanColors.NewTabIcon, modifier = Modifier.size(OverviewTokens.NewTabGlyph))
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(OverviewTokens.HeaderIcon).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = KoanColors.TextPrimary, modifier = Modifier.size(OverviewTokens.HeaderGlyph))
    }
}

/**
 * Sync state at a glance, next to the space name: nothing when all is well, a slowly pulsing dot
 * while syncing, an amber dot when the last sync failed or the account needs signing in again.
 * Tap = sync now (or sign in).
 */
@Composable
private fun SyncBadgeView(badge: SyncBadge, onClick: () -> Unit) {
    if (badge == SyncBadge.None) return
    val pulse = rememberInfiniteTransition(label = "badge")
    val alpha = pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(FirstRunTokens.SKELETON_BREATH_MS), RepeatMode.Reverse),
        label = "pulse",
    )
    val syncing = badge == SyncBadge.Syncing
    Box(
        Modifier
            .size(FirstRunTokens.BadgeTouch)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(FirstRunTokens.BadgeDot)
                .graphicsLayer { this.alpha = if (syncing) alpha.value else 1f }
                .clip(CircleShape)
                .background(if (syncing) KoanColors.TextSecondary else KoanColors.Attention),
        )
    }
}
