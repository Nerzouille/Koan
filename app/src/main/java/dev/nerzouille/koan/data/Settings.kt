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

    private val engine = stringPreferencesKey("search_engine")

    val searchEngine: Flow<SearchEngine> = store.data.map { prefs ->
        SearchEngine.entries.firstOrNull { it.name == prefs[engine] } ?: SearchEngine.DuckDuckGo
    }

    suspend fun setSearchEngine(value: SearchEngine) {
        store.edit { it[engine] = value.name }
    }

    private val startAfter = stringPreferencesKey("start_page_after")

    val startPageAfter: Flow<StartPageAfter> = store.data.map { prefs ->
        StartPageAfter.entries.firstOrNull { it.name == prefs[startAfter] } ?: StartPageAfter.Hour
    }

    suspend fun setStartPageAfter(value: StartPageAfter) {
        store.edit { it[startAfter] = value.name }
    }
}
