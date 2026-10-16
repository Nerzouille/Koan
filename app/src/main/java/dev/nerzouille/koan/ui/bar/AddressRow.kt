package dev.nerzouille.koan.ui.bar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
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
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.BrandHorizontal
import dev.nerzouille.koan.ui.theme.SearchFieldTokens
import androidx.compose.animation.core.Animatable
import android.provider.Settings
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/** What the address row shows about the live page. */
@Immutable
data class PageInfo(
    val url: String,
    val loading: Boolean,
    val progress: Float,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val desktopSite: Boolean = false,
)

private const val SEARCH_HINT = "Search or type URL"

/** Find in page, while it is open: what it found and what it can do. */
@Immutable
class FindUi(
    /** "3/12", "No matches", or empty before the first search. */
    val count: String,
    val onQuery: (String) -> Unit,
    val onNext: (forward: Boolean) -> Unit,
    val onClose: () -> Unit,
)

/**
 * What the address pill shows for a neighbour while a swipe heads to it: a tab (favicon and domain)
 * or a space (its icon and name).
 */
@Immutable
data class SwipeTarget(
    val name: String,
    val spaceIcon: String? = null,
    val url: String = "",
    val favicon: String? = null,
    val staticIcon: Boolean = false,
)

/**
 * back · address field · tabs · menu. In the page view, tapping the field turns it into an input.
 * In the overview ([overview] → 1) back and menu fade out and the field becomes "new search".
 */
@Composable
fun AddressRow(
    page: PageInfo,
    tabCount: Int,
    editing: Boolean,
    overviewOpen: Boolean,
    overview: () -> Float,
    onEditingChange: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    onSearchFromOverview: () -> Unit,
    onBack: () -> Unit,
    onTabs: () -> Unit,
    /** ⋯: opens the menu sheet. */
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    /** Page view swipe: -1..1 towards [previous] / [next]; read in layers only. */
    swipe: () -> Float = { 0f },
    previous: SwipeTarget? = null,
    next: SwipeTarget? = null,
    /** Page view: a tab swipe rests on a tab that is not live yet; the pill shows its url. */
    shownUrl: String? = null,
    /** Page view space mode: the pill shows this space instead of the page's domain. */
    space: SwipeTarget? = null,
    /** Find in page is open: the find bar takes the row. */
    find: FindUi? = null,
) {
    val pageOnly = Modifier.graphicsLayer { alpha = 1f - overview() }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = BarTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BarTokens.RowGap),
    ) {
        if (find != null && !editing && !overviewOpen) {
            FindBar(find, Modifier.weight(1f))
        } else if (editing) {
            AddressInput(
                initial = if (page.url == "about:blank") "" else page.url,
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
                shownUrl = shownUrl,
                space = space,
                onClick = { if (overviewOpen) onSearchFromOverview() else onEditingChange(true) },
                modifier = Modifier.weight(1f),
            )
            TabsButton(count = tabCount, onClick = onTabs)
            BarIcon(KoanIcons.More, interactive = !overviewOpen, onClick = onMenu, modifier = pageOnly)
        }
    }
}

/**
 * Host of the page, crossfading to the search hint in the overview. While a swipe switches tab or
 * space, the host slides away and the one it heads to slides in. In space mode ([space]) the host
 * gives way to the current space's icon and name.
 */
@Composable
private fun AddressField(
    page: PageInfo,
    overview: () -> Float,
    swipe: () -> Float,
    previous: SwipeTarget?,
    next: SwipeTarget?,
    shownUrl: String?,
    space: SwipeTarget?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = displayHost(shownUrl ?: page.url)
    // The last space shown stays composed while it fades out.
    var lastSpace by remember { mutableStateOf(space) }
    if (space != null) lastSpace = space
    val spaceMode = animateFloatAsState(if (space != null) 1f else 0f, tween(Motion.SPACE_MODE_FADE_MS), label = "spaceMode")
    Box(
        modifier
            .height(BarTokens.FieldHeight)
            .clip(CircleShape)
            .background(KoanTheme.colors.BarField)
            .cleanTap(onTap = onClick)
            .padding(horizontal = BarTokens.FieldPadding),
        contentAlignment = Alignment.Center,
    ) {
        FieldText(
            host.ifEmpty { SEARCH_HINT },
            primary = host.isNotEmpty(),
            Modifier.graphicsLayer {
                val f = swipe()
                translationX = -f * size.width * SWIPE_TRAVEL
                alpha = (1f - overview()) * (1f - spaceMode.value) * swipeOut(f)
            },
        )
        lastSpace?.let { current ->
            SwipeLabel(current, 0f, swipe, overview, Modifier.graphicsLayer { alpha = spaceMode.value })
        }
        FieldText(SEARCH_HINT, primary = false, Modifier.graphicsLayer { alpha = overview() })
        previous?.let { SwipeLabel(it, -1f, swipe, overview) }
        next?.let { SwipeLabel(it, 1f, swipe, overview) }
    }
}

/**
 * A tab or space in the pill: icon and name. [side] -1 / 1: a neighbour a swipe heads to, sliding
 * in from that side; 0: the one in the middle, sliding out as the swipe goes.
 */
@Composable
private fun SwipeLabel(target: SwipeTarget, side: Float, swipe: () -> Float, overview: () -> Float, modifier: Modifier = Modifier) {
    Row(
        modifier.graphicsLayer {
            val f = swipe()
            translationX = (side - f) * size.width * SWIPE_TRAVEL
            alpha = (1f - overview()) * if (side == 0f) swipeOut(f) else swipeIn(f * side)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (target.spaceIcon != null) {
            ZenIcon(target.spaceIcon, BarTokens.SwitcherIcon, tint = KoanTheme.colors.TextPrimary)
        } else {
            Favicon(target.url, BarTokens.SwitcherIcon, icon = target.favicon, staticIcon = target.staticIcon)
        }
        Spacer(Modifier.width(BarTokens.SwitcherNameGap))
        FieldText(target.name, primary = true, Modifier)
    }
}

/** How far (share of the text's width) the host and the space name travel during a swipe. */
private const val SWIPE_TRAVEL = 0.6f

/**
 * The label leaving fades out over the first quarter of the swipe; the one coming in fades in only
 * once it is nearly gone, so the two never read on top of each other.
 */
private fun swipeOut(f: Float) = (1f - abs(f) * 4f).coerceIn(0f, 1f)
private fun swipeIn(f: Float) = ((f - 0.18f) * 4f).coerceIn(0f, 1f)

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
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // "@" = pick an engine or a scope: the brand gradient flows in around the ring.
    val at = value.text.trimStart().startsWith("@")
    val flow = remember { Animatable(0f) }
    LaunchedEffect(at) { flow.animateTo(if (at) 1f else 0f, SearchFieldTokens.Flow) }
    // While in "@" mode the gradient turns slowly; only the shader moves (drawn, never recomposed).
    // Off when the system removes animations.
    val turn = remember { mutableFloatStateOf(0f) }
    val resolver = LocalContext.current.contentResolver
    LaunchedEffect(at) {
        if (!at) return@LaunchedEffect
        if (Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return@LaunchedEffect
        val start = withFrameNanos { it }
        val from = turn.floatValue
        while (true) {
            withFrameNanos { now -> turn.floatValue = from + ringTurn((now - start) / 1e9f) }
        }
    }
    val palette = KoanTheme.colors

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(BarTokens.FieldHeight)
                .then(if (focused) Modifier.searchRing(palette.FocusRing, SearchFieldTokens.Ring, flow = { flow.value }, turn = { turn.floatValue }) else Modifier)
                .clip(CircleShape)
                .background(palette.BarField)
                .padding(start = BarTokens.FieldPadding - SearchFieldTokens.GlyphGap / 2, end = BarTokens.FieldPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(KoanIcons.Search, null, tint = palette.FieldHint, modifier = Modifier.size(SearchFieldTokens.Glyph))
            val textStart = Modifier.padding(start = SearchFieldTokens.Glyph + SearchFieldTokens.GlyphGap)
            if (value.text.isEmpty()) {
                Text(SEARCH_HINT, color = palette.FieldHint, fontSize = BarTokens.FieldTextSize, modifier = textStart)
            }
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = KoanTheme.colors.TextPrimary, fontSize = BarTokens.FieldTextSize),
                cursorBrush = if (at) BrandHorizontal else SolidColor(palette.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit(value.text) }),
                modifier = textStart
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

/** Find in page: the query, how many matches, previous / next, close. Back closes it too. */
@Composable
private fun FindBar(find: FindUi, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var focused by remember { mutableStateOf(false) }
    // A Back gesture goes to the keyboard first and only hides it. The keyboard going away while
    // the field still has focus is that Back: find closes with it. A tap on the page moves focus
    // away first, so hiding the keyboard that way (then ↑ / ↓) keeps find open.
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val onClose by rememberUpdatedState(find.onClose)
    LaunchedEffect(ime, density) {
        var shown = false
        snapshotFlow { ime.getBottom(density) > 0 }.collect { visible ->
            if (shown && !visible && focused) onClose()
            shown = visible
        }
    }
    val palette = KoanTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(BarTokens.FieldHeight)
                .clip(CircleShape)
                .background(palette.BarField)
                .padding(start = BarTokens.FieldPadding - SearchFieldTokens.GlyphGap / 2, end = BarTokens.FieldPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(KoanIcons.Search, null, tint = palette.FieldHint, modifier = Modifier.size(SearchFieldTokens.Glyph))
            Spacer(Modifier.width(SearchFieldTokens.GlyphGap))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) Text("Find in page", color = palette.FieldHint, fontSize = BarTokens.FieldTextSize)
                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (it.text != value.text) find.onQuery(it.text)
                        value = it
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = palette.TextPrimary, fontSize = BarTokens.FieldTextSize),
                    cursorBrush = SolidColor(palette.TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSearch = { find.onNext(true) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        // A Back key (not a gesture) reaches the field before the keyboard: one
                        // press closes find and the keyboard together.
                        .onPreInterceptKeyBeforeSoftKeyboard { e ->
                            if (e.key != Key.Back) return@onPreInterceptKeyBeforeSoftKeyboard false
                            if (e.type == KeyEventType.KeyUp) find.onClose()
                            true
                        },
                )
            }
            if (find.count.isNotEmpty()) {
                Spacer(Modifier.width(SearchFieldTokens.GlyphGap))
                Text(find.count, color = palette.TextSecondary, fontSize = BarTokens.TabsCountSize * 1.15f, maxLines = 1)
            }
        }
        BarIcon(KoanIcons.ArrowUp, onClick = { find.onNext(false) })
        BarIcon(KoanIcons.ChevronDown, onClick = { find.onNext(true) })
        BarIcon(KoanIcons.Close, onClick = find.onClose)
    }
    BackHandler(onBack = find.onClose)
}

@Composable
private fun TabsButton(count: Int, onClick: () -> Unit) {
    Box(Modifier.size(BarTokens.IconTouch).clip(CircleShape).cleanTap(onTap = onClick), contentAlignment = Alignment.Center) {
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
            .cleanTap(enabled = enabled && interactive, onTap = onClick),
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
