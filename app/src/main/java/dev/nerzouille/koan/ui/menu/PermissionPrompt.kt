package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.engine.SiteAsk
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.PrimaryButton
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
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(SheetTokens.RowIconBox + SheetTokens.Gap)
                    .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
                    .background(palette.GroupedRow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ask.icon(), null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph + SheetTokens.Divider * 2))
            }
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text("$host wants to ${ask.wants}", color = palette.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold)
                Text(ask.why(), color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
            }
        }
        Spacer(Modifier.height(SheetTokens.Gap / 2))
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            Secondary("Block", interactive, onBlock, Modifier.weight(1f))
            Secondary("Always allow", interactive, onAlways, Modifier.weight(1f))
        }
        PrimaryButton("Allow once", onClick = { if (interactive) onOnce() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Secondary(text: String, interactive: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(KoanTheme.colors.GroupedRow)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = KoanTheme.colors.TextPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}
