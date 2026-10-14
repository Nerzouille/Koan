package dev.nerzouille.koan.ui.bar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import kotlin.math.abs

/** What the address row shows about the live page. */
@Immutable
data class PageInfo(
    val url: String,
    val loading: Boolean,
    val progress: Float,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

private const val SEARCH_HINT = "Search or type URL"

/** A neighbour space, named in the address pill while a swipe heads to it. */
@Immutable
data class SwipeSpace(val name: String, val icon: String)

/**
 * back · address field · tabs · menu. In the page view, tapping the field turns it into an input.
 * In the overview ([overview] → 1) back and menu fade out and the field becomes "new search".
 */
@Composable
fun AddressRow(
    page: PageInfo,
    tabCount: Int,
    /** The current space's accent: cursor and focus ring. */
    accent: Color,
    editing: Boolean,
    overviewOpen: Boolean,
    overview: () -> Float,
    onEditingChange: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    onSearchFromOverview: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabs: () -> Unit,
    menuExtras: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    /** Page view space swipe: -1..1 towards [previous] / [next]; read in layers only. */
    swipe: () -> Float = { 0f },
    previous: SwipeSpace? = null,
    next: SwipeSpace? = null,
) {
    val pageOnly = Modifier.graphicsLayer { alpha = 1f - overview() }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = BarTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BarTokens.RowGap),
    ) {
        if (editing) {
            AddressInput(
                initial = if (page.url == "about:blank") "" else page.url,
                accent = accent,
                onSubmit = onSubmit,
                onCancel = { onEditingChange(false) },
                modifier = Modifier.weight(1f),
            )
        } else {
            BarIcon(KoanIcons.Back, enabled = page.canGoBack, interactive = !overviewOpen, onClick = onBack, modifier = pageOnly)
            AddressField(
                page = page,
                overview = overview,
                swipe = swipe,
                previous = previous,
                next = next,
                onClick = { if (overviewOpen) onSearchFromOverview() else onEditingChange(true) },
                modifier = Modifier.weight(1f),
            )
            TabsButton(count = tabCount, onClick = onTabs)
            BarMenu(
                page = page,
                interactive = !overviewOpen,
                onForward = onForward,
                onReload = onReload,
                onStop = onStop,
                extras = menuExtras,
                modifier = pageOnly,
            )
        }
    }
}

/**
 * Host of the page, crossfading to the search hint in the overview. While a swipe switches space,
 * the host slides away and the space it heads to slides in.
 */
@Composable
private fun AddressField(
    page: PageInfo,
    overview: () -> Float,
    swipe: () -> Float,
    previous: SwipeSpace?,
    next: SwipeSpace?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = displayHost(page.url)
    Box(
        modifier
            .height(BarTokens.FieldHeight)
            .clip(CircleShape)
            .background(KoanTheme.colors.BarField)
            .clickable(onClick = onClick)
            .padding(horizontal = BarTokens.FieldPadding),
        contentAlignment = Alignment.Center,
    ) {
        FieldText(
            host.ifEmpty { SEARCH_HINT },
            primary = host.isNotEmpty(),
            Modifier.graphicsLayer {
                val f = swipe()
                translationX = -f * size.width * SWIPE_TRAVEL
                alpha = (1f - overview()) * (1f - abs(f) * SWIPE_FADE).coerceIn(0f, 1f)
            },
        )
        FieldText(SEARCH_HINT, primary = false, Modifier.graphicsLayer { alpha = overview() })
        previous?.let { SwipeLabel(it, -1f, swipe, overview) }
        next?.let { SwipeLabel(it, 1f, swipe, overview) }
    }
}

/** The space a page-view swipe heads to: icon and name, sliding in from its side. */
@Composable
private fun SwipeLabel(space: SwipeSpace, side: Float, swipe: () -> Float, overview: () -> Float) {
    Row(
        Modifier.graphicsLayer {
            val f = swipe()
            translationX = (side - f) * size.width * SWIPE_TRAVEL
            alpha = (1f - overview()) * (f * side * SWIPE_FADE).coerceIn(0f, 1f)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZenIcon(space.icon, BarTokens.SwitcherIcon, tint = KoanTheme.colors.TextPrimary)
        Spacer(Modifier.width(BarTokens.SwitcherNameGap))
        FieldText(space.name, primary = true, Modifier)
    }
}

/** How far (share of the text's width) the host and the space name travel during a swipe. */
private const val SWIPE_TRAVEL = 0.6f
private const val SWIPE_FADE = 2.5f

@Composable
private fun FieldText(text: String, primary: Boolean, modifier: Modifier) {
    Text(
        text = text,
        color = if (primary) KoanTheme.colors.TextPrimary else KoanTheme.colors.FieldHint,
        fontSize = BarTokens.FieldTextSize,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun AddressInput(
    initial: String,
    accent: Color,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(BarTokens.FieldHeight)
                .clip(CircleShape)
                .background(KoanTheme.colors.BarField)
                .then(if (focused) Modifier.border(BarTokens.FocusRing, accent, CircleShape) else Modifier)
                .padding(horizontal = BarTokens.FieldPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.text.isEmpty()) {
                Text(SEARCH_HINT, color = KoanTheme.colors.FieldHint, fontSize = BarTokens.FieldTextSize)
            }
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = KoanTheme.colors.TextPrimary, fontSize = BarTokens.FieldTextSize),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit(value.text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        // Losing focus (keyboard dismissed, tap elsewhere) cancels the edit.
                        if (it.isFocused) hadFocus = true else if (hadFocus) onCancel()
                    },
            )
        }
        BarIcon(KoanIcons.Close, onClick = onCancel)
    }
    BackHandler(onBack = onCancel)
}

@Composable
private fun TabsButton(count: Int, onClick: () -> Unit) {
    Box(Modifier.size(BarTokens.IconTouch).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(BarTokens.TabsBadge)
                .border(BarTokens.TabsBadgeBorder, KoanTheme.colors.Icon, RoundedCornerShape(BarTokens.TabsBadgeRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "∞" else count.toString(),
                color = KoanTheme.colors.Icon,
                fontSize = BarTokens.TabsCountSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BarMenu(
    page: PageInfo,
    interactive: Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    extras: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        BarIcon(KoanIcons.More, interactive = interactive, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Forward") },
                leadingIcon = { Icon(KoanIcons.Forward, null, Modifier.size(BarTokens.MenuGlyph)) },
                enabled = page.canGoForward,
                onClick = { open = false; onForward() },
            )
            DropdownMenuItem(
                text = { Text(if (page.loading) "Stop" else "Reload") },
                leadingIcon = { Icon(if (page.loading) KoanIcons.Close else KoanIcons.Reload, null, Modifier.size(BarTokens.MenuGlyph)) },
                onClick = { open = false; if (page.loading) onStop() else onReload() },
            )
            // Dev-only items (50-card check, bar comparison) until M1 is signed off.
            extras.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(KoanIcons.Plus, null, Modifier.size(BarTokens.MenuGlyph)) },
                    onClick = { open = false; action() },
                )
            }
        }
    }
}

@Composable
private fun BarIcon(
    icon: ImageVector,
    enabled: Boolean = true,
    interactive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .clickable(enabled = enabled && interactive, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) KoanTheme.colors.Icon else KoanTheme.colors.TextDisabled,
            modifier = Modifier.size(BarTokens.Icon),
        )
    }
}
