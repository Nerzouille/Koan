package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.nerzouille.koan.search.SearchEngine

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val label: String) {
    System("Follow system"),
    Light("Light"),
    Dark("Dark"),
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

    private val engine = stringPreferencesKey("search_engine")

    val searchEngine: Flow<SearchEngine> = store.data.map { prefs ->
        SearchEngine.entries.firstOrNull { it.name == prefs[engine] } ?: SearchEngine.DuckDuckGo
    }

    suspend fun setSearchEngine(value: SearchEngine) {
        store.edit { it[engine] = value.name }
    }
}
