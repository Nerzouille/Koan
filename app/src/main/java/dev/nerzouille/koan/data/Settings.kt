package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dev.nerzouille.koan.search.CustomEngines
import dev.nerzouille.koan.search.SearchEngine

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

/** The owner's settings. */
class Settings(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val theme = stringPreferencesKey("theme")

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[theme] } ?: ThemeMode.System
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[theme] = mode.name }
    }

    private val hideStatus = booleanPreferencesKey("hide_status_bar")

    /** The status bar stays hidden (a swipe from the top edge shows it for a moment); off by default. */
    val hideStatusBar: Flow<Boolean> = store.data.map { it[hideStatus] ?: false }

    suspend fun setHideStatusBar(on: Boolean) {
        store.edit { it[hideStatus] = on }
    }

    private val downloads = stringPreferencesKey("download_location")

    /** Where downloads go: a folder the owner picked (a content:// tree), or null = the phone's Download folder. */
    val downloadLocation: Flow<String?> = store.data.map { it[downloads] }

    suspend fun setDownloadLocation(value: String?) {
        store.edit { if (value == null) it.remove(downloads) else it[downloads] = value }
    }

    private val suggest = booleanPreferencesKey("search_suggestions")

    /** Suggestions from the search engine while typing (on by default). */
    val searchSuggestions: Flow<Boolean> = store.data.map { it[suggest] ?: true }

    suspend fun setSearchSuggestions(on: Boolean) {
        store.edit { it[suggest] = on }
    }

    private val ublockOffered = booleanPreferencesKey("ublock_offered")

    /** uBlock Origin was installed once already (removed or turned off since = the owner's choice). */
    suspend fun ublockOffered(): Boolean = store.data.first()[ublockOffered] ?: false

    suspend fun markUblockOffered() {
        store.edit { it[ublockOffered] = true }
    }

    private val blocked = stringPreferencesKey("ublock_blocked_days")

    /** Requests uBlock blocked, per day ("yyyymmdd" → count), the last 7 days. */
    val blockedByDay: Flow<Map<String, Int>> = store.data.map { prefs -> parseDays(prefs[blocked]) }

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
    suspend fun tourSeen(): String? = store.data.first()[tourSeen]

    suspend fun setTourSeen(version: String) {
        store.edit { it[tourSeen] = version }
    }

    private val engine = stringPreferencesKey("search_engine")
    private val customEngines = stringPreferencesKey("custom_search_engines")

    /** Koan's engines, then the owner's own (in the order they were added). */
    val searchEngines: Flow<List<SearchEngine>> = store.data.map { prefs ->
        SearchEngine.builtIns + CustomEngines.fromJson(prefs[customEngines])
    }

    /** The default engine; DuckDuckGo when none was picked or the picked one was deleted. */
    val searchEngine: Flow<SearchEngine> = store.data.map { prefs ->
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

    val startPageAfter: Flow<StartPageAfter> = store.data.map { prefs ->
        StartPageAfter.entries.firstOrNull { it.name == prefs[startAfter] } ?: StartPageAfter.Hour
    }

    suspend fun setStartPageAfter(value: StartPageAfter) {
        store.edit { it[startAfter] = value.name }
    }
}
