package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nerzouille.koan.data.ThemeMode
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetHeader
import dev.nerzouille.koan.ui.sheet.SheetPageScope
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.sheet.SheetSectionTitle
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** The Mozilla account as Settings shows it: null email = signed out. */
class AccountRow(val email: String?, val onSyncNow: () -> Unit, val onSignIn: () -> Unit, val onSignOut: () -> Unit)

/**
 * Settings, pushed inside the menu sheet. A skeleton for now: theme and account; the rest
 * (search engine, passwords, add-ons, About) arrives with the features behind it.
 * [devActions]: debug builds only.
 */
@Composable
fun SettingsPage(
    scope: SheetPageScope,
    theme: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    account: AccountRow?,
    devActions: List<Pair<String, () -> Unit>>,
) {
    val on = scope.interactive
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetTokens.Padding)
            .padding(bottom = SheetTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap),
    ) {
        SheetHeader("Settings", scope)
        SheetSectionTitle("Theme")
        SheetGroup {
            ThemeMode.entries.forEachIndexed { i, mode ->
                if (i > 0) SheetDivider()
                SheetRow(null, mode.label, on, onClick = { onTheme(mode) }) {
                    if (mode == theme) {
                        Icon(KoanIcons.Check, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
                    }
                }
            }
        }
        if (account != null) {
            SheetSectionTitle("Mozilla account")
            SheetGroup {
                if (account.email == null) {
                    SheetRow(null, "Connect your Zen Spaces", on, onClick = account.onSignIn)
                } else {
                    SheetRow(null, account.email, on, subtitle = "Signed in")
                    SheetDivider()
                    SheetRow(null, "Sync now", on, onClick = account.onSyncNow)
                    SheetDivider()
                    SheetRow(null, "Sign out", on, titleColor = KoanTheme.colors.Destructive, onClick = account.onSignOut)
                }
            }
        }
        if (devActions.isNotEmpty()) {
            SheetSectionTitle("Developer")
            SheetGroup {
                devActions.forEachIndexed { i, (label, action) ->
                    if (i > 0) SheetDivider()
                    SheetRow(null, label, on, onClick = action)
                }
            }
        }
    }
}
