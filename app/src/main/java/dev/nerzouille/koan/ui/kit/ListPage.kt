package dev.nerzouille.koan.ui.kit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.settings.settingsWash
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    val palette = KoanTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(palette.Base)
            .settingsWash(palette.isDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(SettingsTokens.TopBar)
                .padding(horizontal = SettingsTokens.Padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(SettingsTokens.TopBar).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.CenterStart) {
                Icon(KoanIcons.Back, null, tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph + SettingsTokens.Divider * 4))
            }
            Text(title, color = palette.TextPrimary, fontSize = SettingsTokens.TitleSize * 0.7f, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            headerTrailing()
        }

        Column(Modifier.weight(1f).fillMaxWidth(), content = content)

        Row(
            Modifier.padding(horizontal = SettingsTokens.Padding, vertical = DownloadTokens.BottomGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(DownloadTokens.SearchHeight)
                    .clip(CircleShape)
                    .background(palette.BarField)
                    .padding(horizontal = SettingsTokens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KoanIcons.Search, null, tint = palette.FieldHint, modifier = Modifier.size(SettingsTokens.RowGlyph))
                Spacer(Modifier.width(SettingsTokens.RowInset))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(searchHint, color = palette.FieldHint, fontSize = SettingsTokens.RowTitleSize)
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize),
                        cursorBrush = SolidColor(palette.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (onClear != null) {
                Spacer(Modifier.width(DownloadTokens.ActionGap))
                Box(
                    Modifier
                        .height(DownloadTokens.SearchHeight)
                        .clip(CircleShape)
                        .background(palette.Card)
                        .clickable(onClick = onClear)
                        .padding(horizontal = SettingsTokens.Padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Clear", color = palette.Destructive, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

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
