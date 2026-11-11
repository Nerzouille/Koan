package dev.nerzouille.koan.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nerzouille.koan.ui.kit.CardChoices
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetToggleRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/** What Clear browsing data clears. */
data class ClearChoice(val history: Boolean, val cookies: Boolean, val cache: Boolean, val privateBookmarks: Boolean = false)

/**
 * The floating card behind History's "Clear browsing data": history and cache by default; cookies
 * only when asked (it signs you out of sites). The synced history is only hidden on the phone.
 * [privateBookmarks]: how many bookmarks Private has, when it is shown (0 = no row): off by default.
 */
@Composable
fun ClearDataCard(interactive: Boolean, onClear: (ClearChoice) -> Unit, onCancel: () -> Unit, privateBookmarks: Int = 0) {
    var history by remember { mutableStateOf(true) }
    var cookies by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf(true) }
    var marks by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Text("Clear browsing data?", color = KoanTheme.colors.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize))
        SheetGroup {
            SheetToggleRow(null, "History", interactive, checked = history, onChange = { history = it }, subtitle = "Desktop history is only hidden here")
            SheetDivider()
            SheetToggleRow(null, "Cookies and site data", interactive, checked = cookies, onChange = { cookies = it }, subtitle = "Signs you out of most sites")
            SheetDivider()
            SheetToggleRow(null, "Cached images and files", interactive, checked = cache, onChange = { cache = it })
            if (privateBookmarks > 0) {
                SheetDivider()
                SheetToggleRow(
                    null, "Private bookmarks", interactive, checked = marks, onChange = { marks = it },
                    subtitle = if (privateBookmarks == 1) "1 bookmark made in Private" else "$privateBookmarks bookmarks made in Private",
                )
            }
        }
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        CardChoices(
            "Clear", interactive,
            onAction = { if (history || cookies || cache || marks) onClear(ClearChoice(history, cookies, cache, privateBookmarks > 0 && marks)) },
            onCancel = onCancel,
            destructive = true,
        )
    }
}
