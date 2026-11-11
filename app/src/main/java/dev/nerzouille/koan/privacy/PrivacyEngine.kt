package dev.nerzouille.koan.privacy

import android.util.Log
import dev.nerzouille.koan.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession.BounceTrackingProtectionMode
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicyForSessionTypes
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference

/**
 * What the engine does for privacy (docs/FEATURES.md "Private browsing", owner's decisions
 * 2026-11-11). Private tabs only, unless said otherwise:
 * - HTTPS-only, fingerprinting protection (Firefox's, not the Tor-style one), tracking content and
 *   email trackers blocked, Total Cookie Protection, Global Privacy Control;
 * - tracking parameters stripped from links: Mozilla's list (click IDs) plus [STRIP_LIST];
 * - cookie banners refused where Firefox's rules know the site (optional, off);
 * all tabs: WebRTC never shows local network addresses; bounce-tracking protection (GeckoView
 * can't limit it to Private; on, a setting); DNS over HTTPS (optional, off).
 */
object PrivacyEngine {
    private const val TAG = "PrivacyEngine"

    /** Mozilla's DoH resolver (the one Firefox offers by default). */
    const val DOH_URL = "https://mozilla.cloudflare-dns.com/dns-query"

    /**
     * Added to Mozilla's own strip list (which GeckoView keeps current): campaign parameters and
     * click IDs. Only stripped in private tabs.
     */
    val STRIP_LIST = listOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id", "utm_name",
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "twclid", "ttclid", "igshid",
        "li_fat_id", "mc_eid", "mkt_tok", "_hsenc", "_hsmi", "oly_anon_id", "oly_enc_id", "vero_id", "rb_clickid", "s_cid",
    )

    /** The tracking protection policy: Firefox's recommended one; [bounce] = bounce-tracking protection on. */
    fun policy(bounce: Boolean): TrackingProtectionPolicyForSessionTypes = TrackingProtectionPolicy.select(
        trackingCategories = arrayOf(TrackingProtectionPolicy.TrackingCategory.RECOMMENDED),
        bounceTrackingProtectionMode = if (bounce) BounceTrackingProtectionMode.ENABLED else BounceTrackingProtectionMode.ENABLED_STANDBY,
    )

    /** Gecko preferences for what GeckoView has no API for. */
    fun prefs(cookieBanners: Boolean): Map<String, Any> = mapOf(
        // WebRTC: only the default route's address is gathered, and local addresses are hidden
        // behind random .local names. The public address is the one every site sees anyway.
        "media.peerconnection.ice.default_address_only" to true,
        "media.peerconnection.ice.obfuscate_host_addresses" to true,
        // Private: tracking content blocked, Global Privacy Control sent.
        "privacy.trackingprotection.pbmode.enabled" to true,
        "privacy.globalprivacycontrol.pbmode.enabled" to true,
        // Private: cookie banners refused (1), where Firefox's rules know the site; 0 = off.
        "cookiebanners.service.mode.privateBrowsing" to if (cookieBanners) 1 else 0,
    )

    /** Applies the settings now and whenever they change. Call once, with the engine running. */
    fun start(engine: Engine, settings: Settings, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            combine(settings.bounceTracking, settings.dnsOverHttps, settings.privateCookieBanners) { b, d, c -> Triple(b, d, c) }
                .distinctUntilChanged()
                .collect { (bounce, doh, banners) ->
                    engine.settings.trackingProtectionPolicy = policy(bounce)
                    engine.settings.dohSettingsMode = if (doh) Engine.DohSettingsMode.INCREASED else Engine.DohSettingsMode.DEFAULT
                    if (doh) engine.settings.dohProviderUrl = DOH_URL
                    setPrefs(prefs(banners))
                }
        }
    }

    private fun setPrefs(values: Map<String, Any>) {
        val prefs = values.map { (name, value) ->
            when (value) {
                is Boolean -> SetGeckoPreference.setBoolPref(name, value, GeckoPreferenceController.PREF_BRANCH_USER)
                is Int -> SetGeckoPreference.setIntPref(name, value, GeckoPreferenceController.PREF_BRANCH_USER)
                else -> SetGeckoPreference.setStringPref(name, value.toString(), GeckoPreferenceController.PREF_BRANCH_USER)
            }
        }
        GeckoPreferenceController.setGeckoPrefs(prefs).accept(
            { result -> result?.filterValues { !it }?.keys?.takeIf { it.isNotEmpty() }?.let { Log.w(TAG, "Not set: $it") } },
            { e -> Log.w(TAG, "Privacy prefs not set", e) },
        )
    }
}
