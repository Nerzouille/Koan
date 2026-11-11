package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dev.nerzouille.koan.search.CustomEngines
import dev.nerzouille.koan.search.SearchEngine

private val Context.settingsStore: DataStore<Preferences> by safePreferencesDataStore(name = "settings")

enum class ThemeMode(val label: String) {
    System("Follow system"),
    Light("Light"),
    Dark("Dark"),
}

/** How long Koan stays unused before it comes back on the start page instead of the last tab. */
enum class StartPageAfter(val label: String, val millis: Long?) {
    HalfHour("30 min", 30 * 60_000L),
    Hour("1 h", 60 * 60_000L),
    FourHours("4 h", 4 * 60 * 60_000L),
    Never("Never", null),
}

/** Which collections two-way sync sends to the Mozilla account (docs/SYNC_WRITE.md). */
data class SyncBackChoice(val passwords: Boolean = false, val history: Boolean = false) {
    val any: Boolean get() = passwords || history
}

/** The owner's settings. */
class Settings internal constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsStore)

    private val theme = stringPreferencesKey("theme")

    val themeMode: Flow<ThemeMode> = store.safeData.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[theme] } ?: ThemeMode.System
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[theme] = mode.name }
    }

    private val hideStatus = booleanPreferencesKey("hide_status_bar")

    /** The status bar stays hidden (a swipe from the top edge shows it for a moment); off by default. */
    val hideStatusBar: Flow<Boolean> = store.safeData.map { it[hideStatus] ?: false }

    suspend fun setHideStatusBar(on: Boolean) {
        store.edit { it[hideStatus] = on }
    }

    private val sendPasswords = booleanPreferencesKey("send_passwords")
    private val sendHistory = booleanPreferencesKey("send_history")

    /** Two-way sync (docs/SYNC_WRITE.md), per collection, all off by default. */
    private val passwordEdits = booleanPreferencesKey("sync_back_password_edits")

    /** Two-way sync may also change and delete the account's passwords (off; the owner turns it on after checking). */
    val syncBackPasswordEdits: Flow<Boolean> = store.safeData.map { it[passwordEdits] ?: false }

    suspend fun setSyncBackPasswordEdits(on: Boolean) {
        store.edit { it[passwordEdits] = on }
    }

    val syncBack: Flow<SyncBackChoice> = store.safeData.map {
        SyncBackChoice(it[sendPasswords] ?: false, it[sendHistory] ?: false)
    }

    suspend fun setSyncBack(choice: SyncBackChoice) {
        store.edit {
            it[sendPasswords] = choice.passwords
            it[sendHistory] = choice.history
        }
    }

    private val writeBack = booleanPreferencesKey("write_back_enabled")

    /**
     * CLAUDE.md rule 4: Koan may write the Zen `spaces` collection only while this is on. Off by
     * default and not shown in the UI (milestone M6 adds it). Enforced by SpacesWriteGate on every
     * sync request.
     */
    val writeBackEnabled: Flow<Boolean> = store.safeData.map { it[writeBack] ?: false }

    suspend fun setWriteBackEnabled(on: Boolean) {
        store.edit { it[writeBack] = on }
    }

    private val pip = booleanPreferencesKey("picture_in_picture")

    /** Leaving Koan while a fullscreen video plays keeps it playing in a small window; on by default. */
    val pictureInPicture: Flow<Boolean> = store.safeData.map { it[pip] ?: true }

    suspend fun setPictureInPicture(on: Boolean) {
        store.edit { it[pip] = on }
    }

    private val downloads = stringPreferencesKey("download_location")

    /** Where downloads go: a folder the owner picked (a content:// tree), or null = the phone's Download folder. */
    val downloadLocation: Flow<String?> = store.safeData.map { it[downloads] }

    suspend fun setDownloadLocation(value: String?) {
        store.edit { if (value == null) it.remove(downloads) else it[downloads] = value }
    }

    private val suggest = booleanPreferencesKey("search_suggestions")

    /** Suggestions from the search engine while typing (on by default). */
    val searchSuggestions: Flow<Boolean> = store.safeData.map { it[suggest] ?: true }

    suspend fun setSearchSuggestions(on: Boolean) {
        store.edit { it[suggest] = on }
    }

    private val ublockOffered = booleanPreferencesKey("ublock_offered")

    /** uBlock Origin was installed once already (removed or turned off since = the owner's choice). */
    suspend fun ublockOffered(): Boolean = store.safeData.first()[ublockOffered] ?: false

    suspend fun markUblockOffered() {
        store.edit { it[ublockOffered] = true }
    }

    private val blocked = stringPreferencesKey("ublock_blocked_days")

    /** Requests uBlock blocked, per day ("yyyymmdd" → count), the last 7 days. */
    val blockedByDay: Flow<Map<String, Int>> = store.safeData.map { prefs -> parseDays(prefs[blocked]) }

    suspend fun addBlocked(day: String, count: Int) {
        if (count <= 0) return
        store.edit { prefs ->
            val days = parseDays(prefs[blocked]).toMutableMap()
            days[day] = (days[day] ?: 0) + count
            prefs[blocked] = days.entries.sortedByDescending { it.key }.take(7).joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    private fun parseDays(raw: String?): Map<String, Int> =
        raw.orEmpty().split(',').mapNotNull { e -> e.split(':').takeIf { it.size == 2 }?.let { (d, c) -> c.toIntOrNull()?.let { d to it } } }.toMap()

    private val tourSeen = stringPreferencesKey("tour_seen")

    /** Feature version ("0.3") whose tour or "What's new" was last seen; null = none yet. */
    suspend fun tourSeen(): String? = store.safeData.first()[tourSeen]

    suspend fun setTourSeen(version: String) {
        store.edit { it[tourSeen] = version }
    }

    private val engine = stringPreferencesKey("search_engine")
    private val customEngines = stringPreferencesKey("custom_search_engines")

    /** Koan's engines, then the owner's own (in the order they were added). */
    val searchEngines: Flow<List<SearchEngine>> = store.safeData.map { prefs ->
        SearchEngine.builtIns + CustomEngines.fromJson(prefs[customEngines])
    }

    /** The default engine; DuckDuckGo when none was picked or the picked one was deleted. */
    val searchEngine: Flow<SearchEngine> = store.safeData.map { prefs ->
        val all = SearchEngine.builtIns + CustomEngines.fromJson(prefs[customEngines])
        all.firstOrNull { it.id == prefs[engine] } ?: SearchEngine.DuckDuckGo
    }

    suspend fun setSearchEngine(value: SearchEngine) {
        store.edit { it[engine] = value.id }
    }

    /** Adds [value], or replaces the custom engine with its id. */
    suspend fun saveCustomEngine(value: SearchEngine) {
        store.edit { prefs ->
            val list = CustomEngines.fromJson(prefs[customEngines])
            val updated = if (list.any { it.id == value.id }) list.map { if (it.id == value.id) value else it } else list + value
            prefs[customEngines] = CustomEngines.toJson(updated)
        }
    }

    /** Removes a custom engine; if it was the default, DuckDuckGo is again. */
    suspend fun deleteCustomEngine(id: String) {
        store.edit { prefs ->
            prefs[customEngines] = CustomEngines.toJson(CustomEngines.fromJson(prefs[customEngines]).filterNot { it.id == id })
            if (prefs[engine] == id) prefs.remove(engine)
        }
    }

    private val startAfter = stringPreferencesKey("start_page_after")

    val startPageAfter: Flow<StartPageAfter> = store.safeData.map { prefs ->
        StartPageAfter.entries.firstOrNull { it.name == prefs[startAfter] } ?: StartPageAfter.Hour
    }

    suspend fun setStartPageAfter(value: StartPageAfter) {
        store.edit { it[startAfter] = value.name }
    }

    // ---- private mode (OVERVIEW_PRIVATE.md, owner's decisions 2026-11-11) ----

    private fun flag(name: String, default: Boolean): Pair<Flow<Boolean>, suspend (Boolean) -> Unit> {
        val key = booleanPreferencesKey(name)
        return store.safeData.map { it[key] ?: default } to { on -> store.edit { it[key] = on } }
    }

    private val lockFlag = flag("private_lock", true)
    private val screenshotsFlag = flag("private_screenshots", false)
    private val privateSuggestionsFlag = flag("private_suggestions", false)
    private val cookieBannersFlag = flag("private_cookie_banners", false)
    private val ampNormalFlag = flag("amp_normal_tabs", false)
    private val dohFlag = flag("dns_over_https", false)
    private val bounceFlag = flag("bounce_tracking", true)
    private val devSecureOffFlag = flag("dev_flag_secure_off", false)

    /** Opening Private asks for the fingerprint or PIN (after 1 min away or the screen off). On by default. */
    val privateLock: Flow<Boolean> = lockFlag.first
    suspend fun setPrivateLock(on: Boolean) = lockFlag.second(on)

    /** Screenshots and screen recordings may show private tabs. Off by default (FLAG_SECURE). */
    val privateScreenshots: Flow<Boolean> = screenshotsFlag.first
    suspend fun setPrivateScreenshots(on: Boolean) = screenshotsFlag.second(on)

    /** Search suggestions from the engine while typing in Private. Off by default. */
    val privateSuggestions: Flow<Boolean> = privateSuggestionsFlag.first
    suspend fun setPrivateSuggestions(on: Boolean) = privateSuggestionsFlag.second(on)

    /** Private tabs refuse cookie banners where Firefox's rules know the site. Off by default. */
    val privateCookieBanners: Flow<Boolean> = cookieBannersFlag.first
    suspend fun setPrivateCookieBanners(on: Boolean) = cookieBannersFlag.second(on)

    /** Google AMP links open the original article in normal tabs too (always in Private). Off by default. */
    val ampNormalTabs: Flow<Boolean> = ampNormalFlag.first
    suspend fun setAmpNormalTabs(on: Boolean) = ampNormalFlag.second(on)

    /** DNS over HTTPS (Mozilla's default provider, falls back to the phone's DNS), all tabs. Off by default. */
    val dnsOverHttps: Flow<Boolean> = dohFlag.first
    suspend fun setDnsOverHttps(on: Boolean) = dohFlag.second(on)

    /** Bounce-tracking protection, all tabs (GeckoView can't limit it to Private). On by default. */
    val bounceTracking: Flow<Boolean> = bounceFlag.first
    suspend fun setBounceTracking(on: Boolean) = bounceFlag.second(on)

    /** Dev builds only (Settings › About, hidden): FLAG_SECURE off so private screens can be captured for checks. */
    val devFlagSecureOff: Flow<Boolean> = devSecureOffFlag.first
    suspend fun setDevFlagSecureOff(on: Boolean) = devSecureOffFlag.second(on)
}
