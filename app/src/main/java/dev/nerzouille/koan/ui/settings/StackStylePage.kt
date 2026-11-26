package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.browser.PullLab
import dev.nerzouille.koan.ui.browser.PullStyle
import dev.nerzouille.koan.ui.overview.StackLab
import dev.nerzouille.koan.ui.overview.StackStyle
import dev.nerzouille.koan.ui.theme.SettingsTokens

/** Settings › Debug › Stack style (dev and debug builds only): Cast, or beta.1's stack to compare. */
@Composable
fun StackStylePage(interactive: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { StackLab.load(context) }
    SettingsPageFrame("Stack style", interactive, onBack) {
        SettingsSection("Look")
        Box(Modifier.padding(horizontal = SettingsTokens.RowInset)) {
            Column {
                Spacer(Modifier.height(8.dp))
                Segmented(StackStyle.entries.map { it to it.short }, StackLab.style, interactive, StackLab::select)
            }
        }
    }
}

/** Settings › Debug › Pull to refresh (dev and debug builds only): the current circle or a prototype. */
@Composable
fun PullStylePage(interactive: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { PullLab.load(context) }
    SettingsPageFrame("Pull to refresh", interactive, onBack) {
        SettingsSection("Look")
        Box(Modifier.padding(horizontal = SettingsTokens.RowInset)) {
            Column {
                Spacer(Modifier.height(8.dp))
                Segmented(PullStyle.entries.map { it to it.short }, PullLab.style, interactive, PullLab::select)
            }
        }
    }
}
