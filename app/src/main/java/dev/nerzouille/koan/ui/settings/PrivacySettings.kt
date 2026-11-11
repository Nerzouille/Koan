package dev.nerzouille.koan.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.data.Settings
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanToggle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Settings › "Private browsing" and the privacy options that apply to every tab (owner's decisions
 * 2026-11-11, docs/FEATURES.md "Private browsing"). Each row reads and writes its own setting.
 */
@Composable
fun PrivacySettings(settings: Settings, on: Boolean) {
    val scope = rememberCoroutineScope()

    @Composable
    fun Toggle(icon: androidx.compose.ui.graphics.vector.ImageVector?, title: String, subtitle: String, flow: Flow<Boolean>, default: Boolean, set: suspend (Boolean) -> Unit) {
        val value by flow.collectAsState(default)
        SettingsRow(icon, title, on, subtitle = subtitle, onClick = { scope.launch { set(!value) } }) {
            KoanToggle(checked = value, onCheckedChange = if (on) ({ v -> scope.launch { set(v) } }) else null)
        }
    }

    SettingsSection("Private browsing")
    Toggle(KoanIcons.Private, "Lock private tabs", "Fingerprint or PIN after 1 min away or the screen off", settings.privateLock, true, settings::setPrivateLock)
    SettingsDivider()
    Toggle(KoanIcons.Private, "Allow screenshots", "Screenshots and screen recordings can show private tabs", settings.privateScreenshots, false, settings::setPrivateScreenshots)
    SettingsDivider()
    Toggle(KoanIcons.Search, "Search suggestions", "From the search engine while you type in Private", settings.privateSuggestions, false, settings::setPrivateSuggestions)
    SettingsDivider()
    Toggle(KoanIcons.Shield, "Refuse cookie banners", "Where Firefox's rules know the site", settings.privateCookieBanners, false, settings::setPrivateCookieBanners)

    SettingsSection("All tabs")
    Toggle(KoanIcons.Shield, "Bounce-tracking protection", "Clears data of sites that only redirect you", settings.bounceTracking, true, settings::setBounceTracking)
    SettingsDivider()
    Toggle(KoanIcons.Link, "Skip Google AMP pages", "Open the original article (always in Private)", settings.ampNormalTabs, false, settings::setAmpNormalTabs)
    SettingsDivider()
    Toggle(KoanIcons.Globe, "DNS over HTTPS", "Look up sites through Mozilla's resolver (Cloudflare), else the phone's DNS", settings.dnsOverHttps, false, settings::setDnsOverHttps)

    // Dev builds only: lets private screens be captured for design checks. Never in a release build.
    if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "dev") {
        SettingsDivider()
        Toggle(null, "Dev: screenshots in Private", "FLAG_SECURE off (dev builds only)", settings.devFlagSecureOff, false, settings::setDevFlagSecureOff)
    }
}
