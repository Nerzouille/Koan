package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SelectionTokens
import dev.nerzouille.koan.ui.theme.SettingsTokens

/*
 * The library pages' selection mode (History, Downloads, Passwords): a long press checks a row,
 * taps then check or uncheck rows, the header says "N selected" (Select all, ✕) and a bar at the
 * bottom, in thumb reach, holds the actions for what is checked. Back, ✕ or unchecking the last
 * row leaves it.
 */

/** The checked rows of a library page, by key. Empty = not in selection mode. */
@Stable
class Selection<K> {
    var keys by mutableStateOf(emptySet<K>())
        private set

    val active: Boolean get() = keys.isNotEmpty()

    operator fun contains(key: K) = key in keys

    fun start(key: K) {
        keys = setOf(key)
    }

    fun toggle(key: K) {
        keys = if (key in keys) keys - key else keys + key
    }

    fun set(all: Set<K>) {
        keys = all
    }

    fun clear() {
        keys = emptySet()
    }

    /** Rows that left the list (deleted, filtered out) are no longer checked. */
    fun keepOnly(existing: Set<K>) {
        if (!existing.containsAll(keys)) keys = keys.intersect(existing)
    }
}

@Composable
fun <K> rememberSelection(): Selection<K> = remember { Selection() }

/** One action of the selection bar. */
@Immutable
class SelectionAction(val icon: ImageVector, val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** The header in selection mode, in place of the page bar: ✕, "N selected", Select all. */
@Composable
fun SelectionHeader(count: Int, interactive: Boolean, onCancel: () -> Unit, onSelectAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(SettingsTokens.TopBar)
            .padding(horizontal = SettingsTokens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SettingsTokens.TopBar)
                .clip(CircleShape)
                .then(if (interactive) Modifier.clickable(onClick = onCancel) else Modifier),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(KoanIcons.Close, "Leave selection", tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.BackGlyph))
        }
        Text(
            "$count selected",
            color = palette.TextPrimary,
            style = KoanType.title(SettingsTokens.BarTitleSize),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onSelectAll != null) {
            Text(
                "Select all",
                color = palette.TextPrimary,
                fontSize = SettingsTokens.RowValueSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (interactive) Modifier.clickable(onClick = onSelectAll) else Modifier)
                    .padding(horizontal = SelectionTokens.SelectAllPadH, vertical = SelectionTokens.SelectAllPadV),
            )
        }
    }
}

/** The actions for the checked rows, side by side on one rounded bar (icon over label). */
@Composable
fun SelectionBar(actions: List<SelectionAction>, interactive: Boolean, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(SelectionTokens.BarHeight)
            .clip(RoundedCornerShape(SelectionTokens.BarRadius))
            .background(palette.Card)
            .padding(SelectionTokens.BarInset),
        horizontalArrangement = Arrangement.spacedBy(SelectionTokens.BarInset),
    ) {
        actions.forEach { action ->
            val tint = if (action.destructive) palette.Destructive else palette.TextPrimary
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(SelectionTokens.BarRadius - SelectionTokens.BarInset))
                    .then(if (interactive) Modifier.clickable(onClick = action.onClick) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(action.icon, null, tint = tint, modifier = Modifier.size(SelectionTokens.Glyph))
                Text(
                    action.label,
                    color = tint,
                    fontSize = SelectionTokens.LabelSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = SelectionTokens.LabelGap),
                )
            }
        }
    }
}

/** A row's check mark in selection mode: an empty ring, or a filled disc with ✓. */
@Composable
fun SelectionCheck(checked: Boolean) {
    val palette = KoanTheme.colors
    val fill = animateColorAsState(if (checked) palette.Primary else Color.Transparent, label = "check")
    Box(
        Modifier
            .size(SelectionTokens.Check)
            .clip(CircleShape)
            .drawBehind { drawRect(fill.value) }
            .then(if (checked) Modifier else Modifier.border(SelectionTokens.CheckRing, palette.TextSecondary, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(KoanIcons.Check, null, tint = palette.OnPrimary, modifier = Modifier.size(SelectionTokens.CheckGlyph))
    }
}

/** A checked row's background: a light wash over the row's own surface. Read it in draw only. */
@Composable
fun selectionWash(checked: Boolean): State<Color> {
    val palette = KoanTheme.colors
    return animateColorAsState(if (checked) palette.Glass else Color.Transparent, label = "wash")
}
