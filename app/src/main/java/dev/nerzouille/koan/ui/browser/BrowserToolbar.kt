package dev.nerzouille.koan.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanColors

private val ToolbarHeight = 56.dp
private val PillHeight = 44.dp

/** Bottom toolbar: back · address pill · tabs · menu. Tapping the pill turns it into an input. */
@Composable
fun BrowserToolbar(
    url: String,
    loading: Boolean,
    progress: Float,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(KoanColors.Base)) {
        ProgressLine(loading = loading, progress = progress)
        Row(
            Modifier
                .fillMaxWidth()
                .height(ToolbarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (editing) {
                AddressInput(
                    initial = if (url == "about:blank") "" else url,
                    onSubmit = onSubmit,
                    onCancel = { onEditingChange(false) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                ToolbarIcon(KoanIcons.Back, enabled = canGoBack, onClick = onBack)
                AddressPill(url = url, onClick = { onEditingChange(true) }, modifier = Modifier.weight(1f))
                TabsButton(count = tabCount, onClick = onTabs)
                ToolbarMenu(
                    loading = loading,
                    canGoForward = canGoForward,
                    onForward = onForward,
                    onReload = onReload,
                    onStop = onStop,
                )
            }
        }
    }
}

/** 2dp line; progress is read inside graphicsLayer so updates only redraw, never recompose. */
@Composable
private fun ProgressLine(loading: Boolean, progress: Float) {
    val animated by animateFloatAsState(if (loading) progress.coerceAtLeast(0.05f) else 1f, spring(), label = "progress")
    val alpha by animateFloatAsState(if (loading) 1f else 0f, label = "progressAlpha")
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .graphicsLayer {
                scaleX = animated
                this.alpha = alpha
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .background(KoanColors.DefaultAccent),
    )
}

@Composable
private fun AddressPill(url: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val host = displayHost(url)
    Box(
        modifier
            .height(PillHeight)
            .clip(CircleShape)
            .background(KoanColors.Glass)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = host.ifEmpty { "Search or enter address" },
            color = if (host.isEmpty()) KoanColors.TextSecondary else KoanColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
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
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(PillHeight)
                .clip(CircleShape)
                .background(KoanColors.Glass)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.text.isEmpty()) {
                Text("Search or enter address", color = KoanColors.TextSecondary, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = KoanColors.TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(KoanColors.DefaultAccent),
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
                        // Losing focus (keyboard dismissed, tap elsewhere) cancels the edit.
                        if (it.isFocused) hadFocus = true else if (hadFocus) onCancel()
                    },
            )
        }
        ToolbarIcon(KoanIcons.Close, onClick = onCancel)
    }
    BackHandler(onBack = onCancel)
}

@Composable
private fun TabsButton(count: Int, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(24.dp)
                .border(2.dp, KoanColors.TextPrimary, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "∞" else count.toString(),
                color = KoanColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ToolbarMenu(
    loading: Boolean,
    canGoForward: Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolbarIcon(KoanIcons.More, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Forward") },
                leadingIcon = { Icon(KoanIcons.Forward, null, Modifier.size(20.dp)) },
                enabled = canGoForward,
                onClick = { open = false; onForward() },
            )
            DropdownMenuItem(
                text = { Text(if (loading) "Stop" else "Reload") },
                leadingIcon = { Icon(if (loading) KoanIcons.Close else KoanIcons.Reload, null, Modifier.size(20.dp)) },
                onClick = { open = false; if (loading) onStop() else onReload() },
            )
        }
    }
}

@Composable
private fun ToolbarIcon(icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) KoanColors.TextPrimary else KoanColors.TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp),
        )
    }
}
