package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.engine.SiteAsk
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.CardHeader
import dev.nerzouille.koan.ui.kit.CardIconTile
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.SecondaryButton
import dev.nerzouille.koan.ui.theme.ButtonTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** The glyph of a permission (prompt card, Site settings). */
fun SiteAsk.icon(): ImageVector = when (this) {
    SiteAsk.Location -> KoanIcons.Location
    SiteAsk.Camera, SiteAsk.CameraAndMicrophone -> KoanIcons.Camera
    SiteAsk.Microphone -> KoanIcons.Mic
    SiteAsk.Storage -> KoanIcons.Database
    SiteAsk.ProtectedMedia -> KoanIcons.Lock
    SiteAsk.CrossSiteCookies -> KoanIcons.Link
    SiteAsk.LocalNetwork -> KoanIcons.Globe
    SiteAsk.VirtualReality -> KoanIcons.Desktop
}

private fun SiteAsk.why(): String = when (this) {
    SiteAsk.Location -> "It can see where this phone is."
    SiteAsk.Camera -> "It can see what your camera sees."
    SiteAsk.Microphone -> "It can hear your microphone."
    SiteAsk.CameraAndMicrophone -> "It can see and hear you."
    SiteAsk.Storage -> "Its data won't be cleared when the phone needs space."
    SiteAsk.ProtectedMedia -> "Needed for some streaming services."
    SiteAsk.CrossSiteCookies -> "It wants to know you from another site."
    SiteAsk.LocalNetwork -> "It wants to talk to things on your Wi-Fi."
    SiteAsk.VirtualReality -> "It wants to use a headset or motion sensors."
}

/**
 * A site asks for something (design 7j), on a floating card sized to its content: what and why,
 * then Block / Always allow, and Allow once as the main button. Never the brand gradient.
 */
@Composable
fun PermissionPrompt(host: String, ask: SiteAsk, interactive: Boolean, onBlock: () -> Unit, onAlways: () -> Unit, onOnce: () -> Unit) {
    val palette = KoanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        CardHeader("$host wants to ${ask.wants}", ask.why(), leading = { CardIconTile(ask.icon()) }, alignment = Alignment.Top)
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonTokens.ChoiceGap)) {
            SecondaryButton("Block", onBlock, Modifier.weight(1f), enabled = interactive, paddingH = 0.dp)
            SecondaryButton("Always allow", onAlways, Modifier.weight(1f), enabled = interactive, paddingH = 0.dp)
        }
        PrimaryButton("Allow once", onClick = { if (interactive) onOnce() }, modifier = Modifier.fillMaxWidth())
    }
}

