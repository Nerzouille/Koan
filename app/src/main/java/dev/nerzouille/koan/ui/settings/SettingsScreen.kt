package dev.nerzouille.koan.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.data.StartPageAfter
import dev.nerzouille.koan.data.ThemeMode
import dev.nerzouille.koan.search.EngineDraft
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.bar.label
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.menu.UBlockUi
import dev.nerzouille.koan.ui.sheet.SheetNav
import dev.nerzouille.koan.ui.theme.BrandSpinner
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.theme.SheetTokens
import dev.nerzouille.koan.ui.theme.brandOutline
import dev.nerzouille.koan.update.UpdateStatus
import kotlin.math.abs

/** Settings' pages. */
sealed interface SettingsPage {
    data object Root : SettingsPage
    data object SearchEngines : SettingsPage

    /** Add ([id] null, maybe prefilled from a page's [draft]) or edit a custom search engine. */
    data class EditEngine(val id: String?, val draft: EngineDraft? = null) : SettingsPage
    data object Account : SettingsPage
    data object About : SettingsPage
    data object DownloadFolder : SettingsPage
    data object Passwords : SettingsPage
    data class PasswordSite(val site: String) : SettingsPage
}

/** Everything Settings shows and does. [email] null = signed out. */
@Immutable
class SettingsUi(
    val theme: ThemeMode,
    val onTheme: (ThemeMode) -> Unit,
    val engine: SearchEngine,
    val onEngine: (SearchEngine) -> Unit,
    /** Koan's engines, then the owner's own. */
    val engines: List<SearchEngine>,
    val onSaveEngine: (SearchEngine) -> Unit,
    val onDeleteEngine: (SearchEngine) -> Unit,
    /** How long unused before Koan comes back on the start page. */
    val startPageAfter: StartPageAfter,
    val onStartPageAfter: (StartPageAfter) -> Unit,
    /** Suggestions from the search engine while typing. */
    val suggestions: Boolean,
    val onSuggestions: (Boolean) -> Unit,
    /** In the page, the status bar hides and shows with the dock as the page scrolls. */
    val hideStatusBar: Boolean,
    val onHideStatusBar: (Boolean) -> Unit,
    val email: String?,
    val status: DrawerStatus,
    val onSyncNow: () -> Unit,
    val onSignIn: () -> Unit,
    val onSignOut: () -> Unit,
    /** Koan is the phone's default browser. */
    val isDefault: Boolean,
    val onMakeDefault: () -> Unit,
    val version: String,
    /** One look at GitHub's releases (About calls it when it opens). */
    val checkUpdate: suspend () -> UpdateStatus,
    /** Replays the first-run tour (About). */
    val onTour: () -> Unit,
    /** Opens a page (changelog, licences) in a new tab. */
    val onOpenUrl: (String) -> Unit,
    /** Debug builds only. */
    val devActions: List<Pair<String, () -> Unit>>,
    /** Where downloads go: null = the phone's Download folder, else the picked folder's name. */
    val downloadFolder: String?,
    val onPickDownloadFolder: () -> Unit,
    val onDefaultDownloadFolder: () -> Unit,
    val onShowDownloads: () -> Unit,
    val passwords: PasswordsUi,
    /** uBlock Origin: null = not installed. */
    val ublock: UBlockUi?,
    /** Requests it blocked over the last 7 days. */
    val ublockWeek: Int,
    val onUblock: (Boolean) -> Unit,
    val onInstallUblock: () -> Unit,
)

/**
 * Settings, full screen (design 7n), its pages pushed from the right like a sheet's. Back pops a
 * page, then [onClose]. Page slides are layer-only.
 */
@Composable
fun SettingsScreen(ui: SettingsUi, nav: SheetNav<SettingsPage>, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    BackHandler { if (!nav.pop()) onClose() }
    Box(
        modifier
            .fillMaxSize()
            .background(palette.Base)
            .settingsWash(palette.isDark),
    ) {
        nav.pages.forEachIndexed { i, page ->
            key(i, page) {
                val interactive = i == nav.depth
                val back = if (i == 0) onClose else { { nav.pop(); Unit } }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val f = i - nav.pos
                            translationX = size.width * if (f >= 0f) f else f * SheetTokens.PUSH_PARALLAX
                            alpha = if (abs(f) >= 1f) 0f else 1f
                        }
                        .then(if (i > 0) Modifier.background(palette.Base).settingsWash(palette.isDark) else Modifier),
                ) {
                    when (page) {
                        SettingsPage.Root -> RootPage(ui, interactive, back) { nav.push(it) }
                        SettingsPage.SearchEngines -> EnginesPage(ui, interactive, back) { nav.push(it) }
                        is SettingsPage.EditEngine -> EngineEditPage(ui, page, interactive, back)
                        SettingsPage.Account -> AccountPage(ui, interactive, back)
                        SettingsPage.About -> AboutPage(ui, interactive, back)
                        SettingsPage.DownloadFolder -> FolderPage(ui, interactive, back)
                        SettingsPage.Passwords -> PasswordsPage(
                            ui.passwords, interactive, back,
                            onOpenSite = { nav.push(SettingsPage.PasswordSite(it)) },
                        )
                        is SettingsPage.PasswordSite -> PasswordSitePage(ui.passwords, page.site, interactive, back)
                    }
                }
            }
        }
    }
}

@Composable
private fun RootPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit, push: (SettingsPage) -> Unit) {
    SettingsPageFrame("Settings", on, onBack) {
        AccountCard(ui, on) { if (ui.email == null) ui.onSignIn() else push(SettingsPage.Account) }

        SettingsSection("General")
        SettingsRow(KoanIcons.Search, "Search engine", on, value = ui.engine.label, chevron = true, onClick = { push(SettingsPage.SearchEngines) })
        SettingsDivider()
        SettingsRow(
            KoanIcons.Reader, "Search suggestions", on,
            subtitle = "From ${ui.engine.label} while you type",
            onClick = { ui.onSuggestions(!ui.suggestions) },
        ) { KoanToggle(checked = ui.suggestions, onCheckedChange = if (on) ui.onSuggestions else null) }
        SettingsDivider()
        SettingsRow(KoanIcons.Essentials, "Start page", on, subtitle = "When Koan comes back after this long unused")
        Segmented(StartPageAfter.entries.map { it to it.label }, ui.startPageAfter, on, ui.onStartPageAfter)
        Spacer(Modifier.height(SettingsTokens.Divider * 10))
        SettingsDivider()
        SettingsRow(
            KoanIcons.Globe, "Default browser", on,
            subtitle = if (ui.isDefault) "Links from other apps open in Koan" else "Open links from other apps in Koan",
            value = if (ui.isDefault) "Koan" else "Set",
            chevron = !ui.isDefault,
            onClick = if (ui.isDefault) null else ui.onMakeDefault,
        )

        SettingsSection("Display")
        SettingsRow(KoanIcons.Contrast, "Theme", on)
        Segmented(
            listOf(ThemeMode.Light to "Light", ThemeMode.Dark to "Dark", ThemeMode.System to "Follow system"),
            ui.theme, on, ui.onTheme,
        )
        Spacer(Modifier.height(SettingsTokens.Divider * 10))
        SettingsDivider()
        SettingsRow(
            KoanIcons.StatusBar, "Hide status bar while scrolling", on,
            subtitle = "It hides with the dock and comes back when you scroll up",
            onClick = { ui.onHideStatusBar(!ui.hideStatusBar) },
        ) { KoanToggle(checked = ui.hideStatusBar, onCheckedChange = if (on) ui.onHideStatusBar else null) }

        SettingsSection("Privacy")
        SettingsRow(
            KoanIcons.Key, "Passwords", on,
            value = ui.passwords.summaries.size.takeIf { it > 0 }?.let { "$it saved" },
            chevron = true,
            onClick = { push(SettingsPage.Passwords) },
        )
        SettingsDivider()
        val ublock = ui.ublock
        if (ublock == null) {
            SettingsRow(KoanIcons.Shield, "uBlock Origin", on, subtitle = "Blocks ads and trackers", value = "Install", onClick = ui.onInstallUblock)
        } else {
            SettingsRow(
                KoanIcons.Shield, "uBlock Origin", on,
                subtitle = if (!ublock.enabled) "Off" else "%,d requests blocked this week".format(ui.ublockWeek),
                onClick = { ui.onUblock(!ublock.enabled) },
            ) { KoanToggle(checked = ublock.enabled, onCheckedChange = if (on) ui.onUblock else null) }
        }

        SettingsSection("Koan")
        SettingsRow(KoanIcons.Download, "Downloads", on, chevron = true, onClick = ui.onShowDownloads)
        SettingsDivider()
        SettingsRow(KoanIcons.Folder, "Save downloads to", on, value = ui.downloadFolder ?: "Download", chevron = true, onClick = { push(SettingsPage.DownloadFolder) })
        SettingsDivider()
        SettingsRow(KoanIcons.Info, "About Koan", on, value = ui.version, chevron = true, onClick = { push(SettingsPage.About) })

        if (ui.devActions.isNotEmpty()) {
            SettingsSection("Developer")
            ui.devActions.forEachIndexed { i, (label, action) ->
                if (i > 0) SettingsDivider()
                SettingsRow(null, label, on, onClick = action)
            }
        }
    }
}

/** The Mozilla account, on a card that holds your focus (gradient outline). */
@Composable
private fun AccountCard(ui: SettingsUi, on: Boolean, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    val shape = RoundedCornerShape(SettingsTokens.CardRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.Card)
            .brandOutline(shape)
            .then(if (on) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(SettingsTokens.CardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(SettingsTokens.Avatar)
                    .clip(CircleShape)
                    .background(palette.Glass),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ui.email?.firstOrNull()?.uppercase() ?: "Z",
                    color = palette.TextPrimary,
                    fontSize = SettingsTokens.AvatarTextSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(SettingsTokens.RowGlyphGap))
            Column(Modifier.weight(1f)) {
                Text(
                    if (ui.email == null) "Connect your Zen Spaces" else "Mozilla account",
                    color = palette.TextPrimary,
                    fontSize = SettingsTokens.RowTitleSize,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    ui.email ?: "Sign in with your Mozilla account",
                    color = palette.TextSecondary,
                    fontSize = SettingsTokens.RowSubtitleSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(KoanIcons.ChevronRight, null, tint = palette.TextSecondary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        val (text, attention) = ui.status.label()?.takeIf { ui.email != null } ?: return@Column
        Spacer(Modifier.height(SettingsTokens.Divider * 12))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ui.status == DrawerStatus.Syncing) {
                BrandSpinner(FirstRunTokens.BadgeSpinner)
                Spacer(Modifier.width(SettingsTokens.Divider * 8))
            } else if (attention) {
                Box(Modifier.size(SettingsTokens.Divider * 6).clip(CircleShape).background(palette.Attention))
                Spacer(Modifier.width(SettingsTokens.Divider * 8))
            }
            Text(text.substringBefore(" · Sync now"), color = palette.TextPrimary, fontSize = SettingsTokens.RowValueSize)
        }
    }
}

/** Where downloads go: the phone's Download folder (default) or a folder picked once. */
@Composable
private fun FolderPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit) {
    SettingsPageFrame("Save downloads to", on, onBack) {
        SettingsRow(null, "Download", on, subtitle = "The phone's Download folder", onClick = ui.onDefaultDownloadFolder) {
            if (ui.downloadFolder == null) Icon(KoanIcons.Check, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        SettingsDivider()
        SettingsRow(null, ui.downloadFolder ?: "Choose a folder…", on, subtitle = if (ui.downloadFolder != null) "Tap to choose another folder" else null, onClick = ui.onPickDownloadFolder) {
            if (ui.downloadFolder != null) Icon(KoanIcons.Check, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
    }
}

@Composable
private fun AccountPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit) {
    SettingsPageFrame("Mozilla account", on, onBack) {
        SettingsRow(null, ui.email.orEmpty(), on, subtitle = ui.status.label()?.first?.substringBefore(" · Sync now"))
        SettingsDivider()
        SettingsRow(null, "Sync now", on, onClick = ui.onSyncNow)
        SettingsDivider()
        SettingsRow(null, "Sign out", on, titleColor = KoanTheme.colors.Destructive, onClick = ui.onSignOut)
    }
}
