package dev.nerzouille.koan.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.kit.CardChoices
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/** What Clear browsing data clears. */
data class ClearChoice(val history: Boolean, val cookies: Boolean, val cache: Boolean)

/**
 * The floating card behind History's "Clear browsing data": history and cache by default; cookies
 * only when asked (it signs you out of sites). The synced history is only hidden on the phone.
 */
@Composable
fun ClearDataCard(interactive: Boolean, onClear: (ClearChoice) -> Unit, onCancel: () -> Unit) {
    var history by remember { mutableStateOf(true) }
    var cookies by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Text("Clear browsing data?", color = KoanTheme.colors.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize))
        SheetGroup {
            SheetRow(null, "History", interactive, subtitle = "Desktop history is only hidden here", onClick = { history = !history }) {
                KoanToggle(history, if (interactive) ({ history = it }) else null)
            }
            SheetDivider()
            SheetRow(null, "Cookies and site data", interactive, subtitle = "Signs you out of most sites", onClick = { cookies = !cookies }) {
                KoanToggle(cookies, if (interactive) ({ cookies = it }) else null)
            }
            SheetDivider()
            SheetRow(null, "Cached images and files", interactive, onClick = { cache = !cache }) {
                KoanToggle(cache, if (interactive) ({ cache = it }) else null)
            }
        }
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        CardChoices(
            "Clear", interactive,
            onAction = { if (history || cookies || cache) onClear(ClearChoice(history, cookies, cache)) },
            onCancel = onCancel,
            destructive = true,
        )
    }
}
