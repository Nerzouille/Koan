package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.theme.brandOutline
import dev.nerzouille.koan.update.UpdateStatus

private const val REPO = "https://github.com/Nerzouille/Koan"

/**
 * About Koan (design 7o). Opening it looks at GitHub's releases once (no background polling): the
 * chip under the version says "Up to date" or offers the newer release.
 */
@Composable
internal fun AboutPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit) {
    val palette = KoanTheme.colors
    var attempt by remember { mutableIntStateOf(0) }
    var update by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Checking) }
    LaunchedEffect(attempt) {
        update = UpdateStatus.Checking
        update = ui.checkUpdate()
    }
    SettingsPageFrame(null, on, onBack) {
        Column(Modifier.fillMaxWidth().padding(top = SettingsTokens.SectionTop * 2), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.koan_icon),
                null,
                Modifier.size(SettingsTokens.AppIcon).clip(RoundedCornerShape(SettingsTokens.AppIconRadius)),
            )
            Spacer(Modifier.height(SettingsTokens.SectionTop))
            Text("Koan", color = palette.TextPrimary, fontSize = SettingsTokens.AppNameSize, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(SettingsTokens.Divider * 6))
            Text("Version ${ui.version}", color = palette.TextSecondary, fontSize = SettingsTokens.VersionSize)
            Spacer(Modifier.height(SettingsTokens.SectionTop * 0.6f))
            UpdateChip(update, on) { status ->
                when (status) {
                    is UpdateStatus.Available -> ui.onOpenUrl(status.url)
                    UpdateStatus.Failed -> attempt++
                    else -> {}
                }
            }
        }
        Spacer(Modifier.height(SettingsTokens.SectionTop * 3))
        val shape = RoundedCornerShape(SettingsTokens.CardRadius)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.Card)
                .brandOutline(shape)
                .padding(SettingsTokens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(KoanIcons.Essentials, null, tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
            Spacer(Modifier.width(SettingsTokens.RowGlyphGap))
            Column {
                Text("Built to pair with Zen", color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.SemiBold)
                Text("Spaces, Essentials and tabs stay in sync", color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize)
            }
        }
        Spacer(Modifier.height(SettingsTokens.Divider * 8))
        SettingsRow(null, "What's new in ${ui.version.split('.').take(2).joinToString(".")}", on, chevron = true, onClick = { ui.onOpenUrl("$REPO/blob/main/CHANGELOG.md") })
        SettingsDivider()
        SettingsRow(null, "Privacy notice", on, chevron = true, onClick = { ui.onOpenUrl("$REPO/blob/main/docs/PRIVACY.md") })
        SettingsDivider()
        SettingsRow(null, "Open-source licences", on, chevron = true, onClick = { ui.onOpenUrl("$REPO/blob/main/THIRD_PARTY_NOTICES.md") })
        SettingsDivider()
        Spacer(Modifier.height(SettingsTokens.SectionTop * 2))
        Text(
            "Built on GeckoView · Made for one hand",
            color = palette.TextSecondary,
            fontSize = SettingsTokens.FooterSize,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** The small chip under the version: checking, up to date, a newer release (tap opens it), or a failed check (tap retries). */
@Composable
private fun UpdateChip(status: UpdateStatus, on: Boolean, onClick: (UpdateStatus) -> Unit) {
    val palette = KoanTheme.colors
    val (icon, label) = when (status) {
        UpdateStatus.Checking -> null to "Checking for updates…"
        UpdateStatus.UpToDate -> KoanIcons.Check to "Up to date"
        is UpdateStatus.Available -> KoanIcons.Download to "Update available: ${status.version}"
        UpdateStatus.Failed -> KoanIcons.Retry to "Couldn't check for updates"
    }
    val tappable = on && (status is UpdateStatus.Available || status == UpdateStatus.Failed)
    val shape = RoundedCornerShape(SettingsTokens.ChipHeight / 2)
    Row(
        Modifier
            .height(SettingsTokens.ChipHeight)
            .shadow(SettingsTokens.ChipShadow, shape)
            .clip(shape)
            .background(palette.Card)
            .then(if (tappable) Modifier.clickable { onClick(status) } else Modifier)
            .padding(horizontal = SettingsTokens.ChipPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.ChipGlyph))
            Spacer(Modifier.width(SettingsTokens.ChipGap))
        }
        Text(
            label,
            color = if (status == UpdateStatus.Checking) palette.TextSecondary else palette.TextPrimary,
            fontSize = SettingsTokens.ChipTextSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
