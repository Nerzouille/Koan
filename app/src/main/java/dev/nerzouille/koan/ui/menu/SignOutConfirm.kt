package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.kit.DestructiveButton
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** Floating card before signing out: the Zen Spaces leave the phone, phone-only tabs stay. */
@Composable
fun SignOutConfirm(interactive: Boolean, onSignOut: () -> Unit, onCancel: () -> Unit) {
    Confirm(
        title = "Sign out?",
        text = "Your Zen Spaces leave this phone until you sign in again. Tabs opened only on this phone stay.",
        confirm = "Sign out",
        interactive = interactive,
        onConfirm = onSignOut,
        onCancel = onCancel,
    )
}

/** A small confirmation on a floating card: what happens, then the (red) action and Cancel. */
@Composable
fun Confirm(title: String, text: String, confirm: String, interactive: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Text(title, color = KoanTheme.colors.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold)
        Text(text, color = KoanTheme.colors.TextSecondary, fontSize = SheetTokens.CardTextSize)
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        DestructiveButton(confirm, onClick = { if (interactive) onConfirm() }, modifier = Modifier.fillMaxWidth())
        QuietButton("Cancel", onClick = { if (interactive) onCancel() }, modifier = Modifier.fillMaxWidth())
    }
}
