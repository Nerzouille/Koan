package dev.nerzouille.koan.ui.kit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.settings.settingsWash
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.nerzouille.koan.ui.topSafePadding

/**
 * A full-screen list page opened from the ⋯ menu (Downloads, History): ‹ and the title on top,
 * the settings wash behind, the list in the middle ([content], which scrolls itself), and the
 * search field with a small Clear button at the bottom, in thumb reach and on the keyboard.
 * [onClear] null hides Clear.
 */
@Composable
fun ListPage(
    title: String,
    query: String,
    searchHint: String,
    onQuery: (String) -> Unit,
    onClear: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: @Composable RowScope.() -> Unit = {},
    /** Selection mode (ui/kit/Selection): its header in place of the page bar… */
    selectionHeader: (@Composable () -> Unit)? = null,
    /** …and its action bar in place of the search field. */
    selectionBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    val palette = KoanTheme.colors
    // The last header and bar stay for their fade out.
    // (Plain holders, not state: writing state here would recompose again on every pass.)
    val lastHeader = remember { Last<@Composable () -> Unit>() }
    val lastBar = remember { Last<@Composable () -> Unit>() }
    if (selectionHeader != null) lastHeader.value = selectionHeader
    if (selectionBar != null) lastBar.value = selectionBar
    Column(
        modifier
            .fillMaxSize()
            .background(palette.Base)
            .settingsWash(palette.isDark)
            .topSafePadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Crossfade(selectionHeader != null, animationSpec = tween(SELECTION_FADE_MS), label = "header") { selecting ->
            val header = lastHeader.value
            if (selecting && header != null) header() else PageBar(title, interactive = true, onBack = onClose, trailing = headerTrailing)
        }

        Column(Modifier.weight(1f).fillMaxWidth(), content = content)

        Crossfade(selectionBar != null, animationSpec = tween(SELECTION_FADE_MS), label = "bottom") { selecting ->
            val bar = lastBar.value.takeIf { selecting }
            Row(
                Modifier.padding(horizontal = SettingsTokens.Padding, vertical = DownloadTokens.BottomGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (bar != null) {
                    bar()
                } else {
                    KoanField(query, onQuery, searchHint, Modifier.weight(1f), search = true)
                    if (onClear != null) {
                        Spacer(Modifier.width(DownloadTokens.ActionGap))
                        DestructiveButton("Clear", onClick = onClear, surface = palette.Card)
                    }
                }
            }
        }
    }
}

/** Into and out of selection mode: the header and the bottom bar crossfade. */
const val SELECTION_FADE_MS = 180

/** One day's card in a list page: rounded, the rows inside separated by hairlines. */
@Composable
fun DayCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DownloadTokens.GroupRadius))
            .background(KoanTheme.colors.Card)
            .padding(horizontal = SettingsTokens.RowInset),
        content = content,
    )
}

/**
 * The shape of one row of a day's card drawn row by row (a lazy list): only the card's first and
 * last rows are rounded.
 */
fun dayRowShape(first: Boolean, last: Boolean): Shape {
    val r = DownloadTokens.GroupRadius
    val flat = 0.dp
    return RoundedCornerShape(
        topStart = if (first) r else flat, topEnd = if (first) r else flat,
        bottomStart = if (last) r else flat, bottomEnd = if (last) r else flat,
    )
}

/** Newest first, by day: Today, Yesterday, then the date ("Monday 12 October", a year back on). */
fun <T> byDay(items: List<T>, time: (T) -> Long, today: LocalDate = LocalDate.now()): List<Pair<String, List<T>>> {
    val zone = ZoneId.systemDefault()
    return items.sortedByDescending(time)
        .groupBy { Instant.ofEpochMilli(time(it)).atZone(zone).toLocalDate() }
        .toList()
        .sortedByDescending { it.first }
        .map { (date, list) -> dayLabel(date, today) to list }
}

fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> DAY.format(date)
    else -> DAY_YEAR.format(date)
}

private val DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val DAY_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

private class Last<T> {
    var value: T? = null
}
