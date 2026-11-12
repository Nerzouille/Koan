package dev.nerzouille.koan.ui.bar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.kit.KoanHaptics
import dev.nerzouille.koan.ui.kit.frameClock
import dev.nerzouille.koan.ui.kit.rememberAnimationsOff
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.privacy.PrivateFlip
import dev.nerzouille.koan.ui.privacy.flipTextDip
import dev.nerzouille.koan.ui.search.SearchField
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.BrandHorizontal
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PrivateTokens
import dev.nerzouille.koan.ui.theme.SearchFieldTokens
import dev.nerzouille.koan.ui.theme.brandOutline
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the address row shows about the live page. */
@Immutable
data class PageInfo(
    val url: String,
    val loading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val desktopSite: Boolean = false,
)

private const val SEARCH_HINT = "Search or type URL"
private const val PRIVATE_HINT = "Search privately"

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
 * The overview ([overview] → 1) keeps the layout and swaps the buttons: back becomes History,
 * the field "new search", ⋯ Settings.
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
    /** Overview: History in back's place. */
    onHistory: () -> Unit,
    /** Long press on History: the recently closed tabs. */
    onRecentlyClosed: () -> Unit,
    /** Overview: Settings in ⋯'s place. */
    onSettings: () -> Unit,
    /** Page view: a long press on the tab counter (its small menu). */
    onTabsMenu: () -> Unit,
    /** Page view: a long press on back (the pages before this one). */
    onBackMenu: () -> Unit,
    /** A press lands on the address pill: only there does a long press scrub the spaces. */
    pillPressed: () -> Unit,
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
    /** Something is downloading: its progress (0..1, NaN = size unknown), shown as a ring in the pill. */
    download: Float? = null,
    /** A download just finished and nothing else runs: the ring fills, shows a check, then goes. */
    downloadDone: Boolean = false,
    onDownloads: () -> Unit = {},
    /** The page is in a language that isn't on the phone: the translate icon (tap = the card). */
    translate: Boolean = false,
    onTranslate: () -> Unit = {},
    /** The text being edited, shared with the search sheet. */
    search: SearchField = remember { SearchField() },
    /** Inline completion: the full address for what was typed, if a page starts with it. */
    complete: (String) -> String? = { null },
    /** The completed page's title, shown after it ("— Title"). */
    completedTitle: () -> String? = { null },
    /** Private's texts (the mask, "Search privately"), and the flip they mist with. */
    private: Boolean = false,
    flip: PrivateFlip? = null,
) {
    val pageOnly = Modifier.graphicsLayer { alpha = 1f - overview() }
    val overviewOnly = Modifier.graphicsLayer { alpha = overview() }
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
                field = search,
                complete = complete,
                completedTitle = completedTitle,
                onSubmit = onSubmit,
                onCancel = { onEditingChange(false) },
                modifier = Modifier.weight(1f),
            )
        } else {
            Box {
                BarIcon(KoanIcons.Back, enabled = page.canGoBack, interactive = !overviewOpen, onClick = onBack, modifier = pageOnly, onHold = onBackMenu)
                BarIcon(KoanIcons.History, interactive = overviewOpen, onClick = onHistory, modifier = overviewOnly, onHold = onRecentlyClosed)
            }
            AddressField(
                page = page,
                overview = overview,
                swipe = swipe,
                previous = previous,
                next = next,
                shownUrl = shownUrl,
                space = space,
                download = if (overviewOpen) null else download,
                downloadDone = !overviewOpen && downloadDone,
                onDownloads = onDownloads,
                translate = !overviewOpen && translate,
                onTranslate = onTranslate,
                onClick = { if (overviewOpen) onSearchFromOverview() else onEditingChange(true) },
                onPress = pillPressed,
                modifier = Modifier.weight(1f),
                private = private,
                flip = flip,
            )
            Box(Modifier.flipTextDip(flip)) { TabsButton(count = tabCount, onClick = onTabs, onHold = if (overviewOpen) null else onTabsMenu) }
            Box {
                BarIcon(KoanIcons.More, interactive = !overviewOpen, onClick = onMenu, modifier = pageOnly, onHold = onSettings)
                BarIcon(KoanIcons.Settings, interactive = overviewOpen, onClick = onSettings, modifier = overviewOnly)
            }
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
    download: Float?,
    downloadDone: Boolean,
    onDownloads: () -> Unit,
    translate: Boolean,
    onTranslate: () -> Unit,
    onClick: () -> Unit,
    /** A finger lands on the pill (before the bar's drags see it). */
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    private: Boolean = false,
    flip: PrivateFlip? = null,
) {
    val host = displayHost(shownUrl ?: page.url)
    val press by rememberUpdatedState(onPress)
    // The last space shown stays composed while it fades out.
    var lastSpace by remember { mutableStateOf(space) }
    if (space != null) lastSpace = space
    val spaceMode = animateFloatAsState(if (space != null) 1f else 0f, tween(Motion.SPACE_MODE_FADE_MS), label = "spaceMode")
    // Two hit areas side by side, never overlapping: the text (tap = edit) and, at the end, the
    // download ring while something downloads (tap = Downloads), else the translate icon.
    val ring = download != null || downloadDone
    val translateIcon = translate && !ring
    Box(
        modifier
            .height(BarTokens.FieldHeight)
            .clip(CircleShape)
            .background(KoanTheme.colors.BarField)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    press()
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = if (ring || translateIcon) BarTokens.IconTouch - BarTokens.FieldPadding else 0.dp)
                .cleanTap(onTap = onClick)
                .padding(horizontal = BarTokens.FieldPadding),
            contentAlignment = Alignment.Center,
        ) { FieldLabels(host, overview, swipe, previous, next, lastSpace, { spaceMode.value }, private, flip) }
        if (ring) DownloadRing(download, downloadDone, onDownloads, Modifier.align(Alignment.CenterEnd))
        TranslateIcon(translateIcon, onTranslate, Modifier.align(Alignment.CenterEnd))
    }
}

/**
 * The translate icon at the end of the pill: it pops in (a small spring scale and a fade, layer
 * only) when the page is in a language that isn't on the phone, and out again; a tap opens the
 * translation card.
 */
@Composable
private fun TranslateIcon(shown: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val view = LocalView.current
    val instant = rememberAnimationsOff()
    val t = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown) {
        val goal = if (shown) 1f else 0f
        if (instant) t.snapTo(goal) else t.animateTo(goal, if (shown) BarTokens.TranslateIn else tween(BarTokens.TRANSLATE_OUT_MS))
    }
    val visible by remember { derivedStateOf { t.value > 0.01f } }
    if (!visible && !shown) return
    Box(
        modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .then(if (shown) Modifier.cleanTap { KoanHaptics.tick(view); onClick() } else Modifier)
            .semantics { contentDescription = "Translate page" },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            KoanIcons.Translate,
            contentDescription = null,
            tint = KoanTheme.colors.TextSecondary,
            modifier = Modifier
                .size(BarTokens.TranslateIcon)
                .graphicsLayer {
                    val v = t.value
                    alpha = v.coerceIn(0f, 1f)
                    scaleX = 0.6f + 0.4f * v
                    scaleY = 0.6f + 0.4f * v
                },
        )
    }
}

/** The pill's text: the host, the space in space mode, the search hint, the swipe neighbours. */
@Composable
private fun FieldLabels(
    host: String,
    overview: () -> Float,
    swipe: () -> Float,
    previous: SwipeTarget?,
    next: SwipeTarget?,
    lastSpace: SwipeTarget?,
    /** 0…1, read in layers only. */
    spaceMode: () -> Float,
    private: Boolean = false,
    flip: PrivateFlip? = null,
) {
    Box(contentAlignment = Alignment.Center) {
        MaskedText(
            host.ifEmpty { if (private) PRIVATE_HINT else SEARCH_HINT },
            primary = host.isNotEmpty(),
            mask = private,
            modifier = Modifier.graphicsLayer {
                val f = swipe()
                translationX = -f * size.width * SWIPE_TRAVEL
                alpha = (1f - overview()) * (1f - spaceMode()) * swipeOut(f)
            },
        )
        lastSpace?.let { current ->
            SwipeLabel(current, 0f, swipe, overview, Modifier.graphicsLayer { alpha = spaceMode() })
        }
        MaskedText(if (private) PRIVATE_HINT else SEARCH_HINT, primary = false, mask = private, modifier = Modifier.graphicsLayer { alpha = overview() }.flipTextDip(flip))
        previous?.let { SwipeLabel(it, -1f, swipe, overview) }
        next?.let { SwipeLabel(it, 1f, swipe, overview) }
    }
}

/**
 * A download is running (design 7g): a small neutral ring at the end of the pill, filling with
 * its progress. Its own 48dp hit target, outside the text's: tap = Downloads, with a light tick.
 * [done]: the download just finished (fixes F8): the ring fills up, a check draws in, then the
 * whole ring fades and shrinks away; it stays tappable until it leaves. Read in draw / layer only.
 */
@Composable
private fun DownloadRing(progress: Float?, done: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val palette = KoanTheme.colors
    val view = LocalView.current
    val instant = rememberAnimationsOff()
    // The progress the fill starts from once the download is done.
    val last = remember { floatArrayOf(0f) }
    if (progress != null) last[0] = if (progress.isNaN()) 0.25f else progress.coerceIn(0f, 1f)
    val t = remember { Animatable(if (done) 1f else 0f) }
    LaunchedEffect(done) {
        if (!done) t.snapTo(0f)
        else if (instant) t.snapTo(1f)
        else t.animateTo(1f, tween(DownloadTokens.DONE_MS, easing = LinearEasing))
    }
    Box(
        modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .cleanTap {
                KoanHaptics.tick(view)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(DownloadTokens.Ring)
                .graphicsLayer {
                    // The last part: fade and shrink out (none while running; gone at once without animations).
                    val out = if (!done) 0f else if (instant) 1f else phase(t.value, 0.78f, 1f)
                    alpha = 1f - out
                    val s = 1f + 0.12f * phase(t.value, 0.3f, 0.5f) * (1f - phase(t.value, 0.5f, 0.7f)) - 0.3f * out
                    scaleX = s
                    scaleY = s
                }
                .drawBehind {
                    val stroke = DownloadTokens.RingStroke.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(palette.Glass, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    val fill = phase(t.value, 0f, 0.3f)
                    // Size unknown: a quarter, so it still reads as "working".
                    val running = if (progress == null) last[0] else if (progress.isNaN()) 0.25f else progress.coerceIn(0f, 1f)
                    val sweep = 360f * (running + (1f - running) * fill)
                    drawArc(palette.TextSecondary, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    val check = phase(t.value, 0.25f, 0.55f)
                    if (check > 0f) drawCheck(check, palette.TextPrimary, stroke)
                },
        )
    }
}

/** 0 before [from], 1 after [to], linear (eased) in between. */
private fun phase(t: Float, from: Float, to: Float): Float {
    val x = ((t - from) / (to - from)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** The ✓ of KoanIcons.Check, drawn in up to [part] of its length, inside the ring. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheck(part: Float, color: Color, stroke: Float) {
    val k = size.width / 24f
    val a = Offset(7f * k, 12.5f * k)
    val b = Offset(10.5f * k, 16f * k)
    val c = Offset(17f * k, 8.5f * k)
    val first = (b - a).getDistance()
    val second = (c - b).getDistance()
    val len = (first + second) * part
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(a.x, a.y)
        if (len <= first) {
            val p = a + (b - a) * (len / first)
            lineTo(p.x, p.y)
        } else {
            lineTo(b.x, b.y)
            val p = b + (c - b) * ((len - first) / second)
            lineTo(p.x, p.y)
        }
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

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

/** Private (OVERVIEW_PRIVATE.md §4.4–§4.5): the mask in private.ink before the pill's text. */
@Composable
private fun MaskedText(text: String, primary: Boolean, mask: Boolean, modifier: Modifier) {
    if (!mask) return FieldText(text, primary, modifier)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(KoanIcons.Private, null, tint = KoanTheme.colors.PrivateInk, modifier = Modifier.size(PrivateTokens.AddressGlyph))
        Spacer(Modifier.width(PrivateTokens.AddressGap))
        FieldText(text, primary, Modifier.weight(1f, fill = false))
    }
}

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
    field: SearchField,
    complete: (String) -> String?,
    completedTitle: () -> String?,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Editing starts on the page's address, all selected (set before the first frame).
    remember { field.start(initial); true }
    val value = field.value
    val completing = !value.selection.collapsed && value.selection.end == value.text.length && value.selection.start > 0
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Back closes the search field, not just the keyboard.
    CloseWithKeyboard(focused = { focused }, onClose = onCancel)
    // "@" = pick an engine or a scope: the brand gradient flows in around the ring.
    val at = value.text.trimStart().startsWith("@")
    val flow = remember { Animatable(0f) }
    LaunchedEffect(at) { flow.animateTo(if (at) 1f else 0f, SearchFieldTokens.Flow) }
    // While in "@" mode the gradient turns slowly; only the shader moves (drawn, never recomposed).
    // Off when the system removes animations.
    val turn = remember { mutableFloatStateOf(0f) }
    val still = rememberAnimationsOff()
    LaunchedEffect(at) {
        if (!at || still) return@LaunchedEffect
        val from = turn.floatValue
        var elapsedMs = 0f
        frameClock {
            elapsedMs += it
            turn.floatValue = from + ringTurn(elapsedMs / 1000f)
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
            // The text scrolls inside the field (its frame stays put) and keeps the cursor a
            // little inside the edges. A cursor handle held at an edge maps to the text now under
            // it at each move, so the text keeps scrolling (the field's own scrolling stops at
            // the last visible character).
            var scrollX by remember { mutableFloatStateOf(0f) }
            var contentWidth by remember { mutableIntStateOf(0) }
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            var viewport by remember { mutableIntStateOf(0) }
            val edgePx = with(LocalDensity.current) { SearchFieldTokens.CursorEdge.toPx() }
            fun maxScroll() = (contentWidth - viewport).coerceAtLeast(0).toFloat()
            // Editing starts all selected: the start of the address shows.
            val previous = remember { arrayOf<TextRange?>(null) }
            val following = remember { intArrayOf(0) }
            LaunchedEffect(value.selection, layout, viewport) {
                val l = layout ?: return@LaunchedEffect
                if (viewport == 0) return@LaunchedEffect
                val sel = value.selection
                // The end that moved: the cursor, or the selection handle being dragged.
                val follow = when {
                    sel == previous[0] -> following[0]
                    sel.start != previous[0]?.start -> sel.start
                    else -> sel.end
                }
                previous[0] = sel
                following[0] = follow
                val x = l.getHorizontalPosition(follow.coerceIn(0, l.layoutInput.text.length), usePrimaryDirection = true)
                val margin = minOf(edgePx, viewport / 4f)
                val target = when {
                    x < scrollX + margin -> x - margin
                    x > scrollX + viewport - margin -> x - viewport + margin
                    else -> return@LaunchedEffect
                }
                scrollX = target.coerceIn(0f, maxScroll())
            }
            BasicTextField(
                value = value,
                onValueChange = { field.onChange(it, complete) },
                singleLine = true,
                textStyle = TextStyle(color = KoanTheme.colors.TextPrimary, fontSize = BarTokens.FieldTextSize),
                cursorBrush = if (at) BrandHorizontal else SolidColor(palette.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit(value.text) }),
                // The completed page's title follows the address, quietly ("github.com — GitHub").
                visualTransformation = run {
                    val title = completedTitle()
                    if (completing && !title.isNullOrBlank()) TitleAfter(title, palette.FieldHint) else VisualTransformation.None
                },
                onTextLayout = { layout = it },
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .onSizeChanged { viewport = it.width }
                            // Dragging the text scrolls it.
                            .draggable(
                                rememberDraggableState { dx -> scrollX = (scrollX - dx).coerceIn(0f, maxScroll()) },
                                Orientation.Horizontal,
                            ),
                    ) {
                        Box(
                            Modifier.layout { measurable, constraints ->
                                val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                                contentWidth = p.width
                                layout(constraints.maxWidth, p.height) { p.place(-scrollX.roundToInt(), 0) }
                            },
                        ) { inner() }
                    }
                },
                modifier = textStart
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .backBeforeKeyboard(onCancel)
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

/** Shows " — [title]" after the text (not part of it: the cursor and selection never go there). */
private class TitleAfter(private val title: String, private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = color)) { append(" — $title") }
        }
        val length = text.length
        return TransformedText(
            shown,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(length)
            },
        )
    }

    override fun equals(other: Any?) = other is TitleAfter && other.title == title && other.color == color
    override fun hashCode() = title.hashCode() * 31 + color.hashCode()
}

/**
 * One Back closes a bar field (search, find) together with its keyboard. A back gesture goes to
 * the keyboard first and only hides it: the keyboard going away while the field still has focus
 * is that Back, so [onClose] follows.
 */
@Composable
private fun CloseWithKeyboard(focused: () -> Boolean, onClose: () -> Unit) {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val close by rememberUpdatedState(onClose)
    val isFocused by rememberUpdatedState(focused)
    LaunchedEffect(ime, density) {
        var shown = false
        snapshotFlow { ime.getBottom(density) > 0 }.collect { visible ->
            if (shown && !visible && isFocused()) close()
            shown = visible
        }
    }
}

/** A Back key (not a gesture) reaches the field before the keyboard: one press closes both. */
private fun Modifier.backBeforeKeyboard(onClose: () -> Unit): Modifier =
    onPreInterceptKeyBeforeSoftKeyboard { e ->
        if (e.key != Key.Back) return@onPreInterceptKeyBeforeSoftKeyboard false
        if (e.type == KeyEventType.KeyUp) onClose()
        true
    }

/** Find in page: the query, how many matches, previous / next, close. Back closes it too. */
@Composable
private fun FindBar(find: FindUi, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var focused by remember { mutableStateOf(false) }
    // A tap on the page moves focus away first, so hiding the keyboard that way (then ↑ / ↓)
    // keeps find open; Back closes it.
    CloseWithKeyboard(focused = { focused }, onClose = find.onClose)
    val palette = KoanTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(BarTokens.FieldHeight)
                .clip(CircleShape)
                .background(palette.BarField)
                // Find is an active moment: the full brand gradient rings its field (docs/UI_SPEC.md "Brand gradient").
                .brandOutline(CircleShape)
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
                        .backBeforeKeyboard(find.onClose),
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

/** The tab counter: tap = overview (and back); [onHold]: a long press opens its menu, with a tick. */
@Composable
private fun TabsButton(count: Int, onClick: () -> Unit, onHold: (() -> Unit)?) {
    val view = LocalView.current
    Box(
        Modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .cleanTap(onTap = onClick)
            .holdPress(
                enabled = onHold != null,
                onHold = {
                    KoanHaptics.hold(view)
                    onHold?.invoke()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
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
    /** A long press, if the button has one. */
    onHold: (() -> Unit)? = null,
) {
    val view = LocalView.current
    Box(
        modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .cleanTap(enabled = enabled && interactive, onTap = onClick)
            .holdPress(
                enabled = onHold != null && enabled && interactive,
                onHold = {
                    KoanHaptics.hold(view)
                    onHold?.invoke()
                },
            ),
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
