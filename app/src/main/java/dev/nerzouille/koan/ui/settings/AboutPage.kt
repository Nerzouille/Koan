package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.BrandHorizontal
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.theme.brandOutline

private const val REPO = "https://github.com/Nerzouille/Koan"

/** About Koan (design 7o). The update check arrives with the rest of Settings (C6). */
@Composable
internal fun AboutPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit) {
    val palette = KoanTheme.colors
    SettingsPageFrame(null, on, onBack) {
        Column(Modifier.fillMaxWidth().padding(top = SettingsTokens.SectionTop * 2), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(SettingsTokens.AppIcon)
                    .clip(RoundedCornerShape(SettingsTokens.AppIconRadius))
                    .background(BrandHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(SettingsTokens.SectionTop))
            Text("Koan", color = palette.TextPrimary, fontSize = SettingsTokens.AppNameSize, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(SettingsTokens.Divider * 6))
            Text("Version ${ui.version}", color = palette.TextSecondary, fontSize = SettingsTokens.VersionSize)
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
        SettingsRow(null, "What's new", on, chevron = true, onClick = { ui.onOpenUrl("$REPO/blob/main/CHANGELOG.md") })
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
