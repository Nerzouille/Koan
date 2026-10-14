package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.lastPlaceStore: DataStore<Preferences> by preferencesDataStore(name = "last_place")

/**
 * The space and tab the user was on, on disk: swiping Koan away from recents drops the saved
 * instance state, and the next launch must still open where they left. (The live page itself,
 * scroll and history included, is restored by the engine from M3 on.)
 */
class LastPlace(context: Context) {
    data class Place(val spaceUuid: String?, val tabId: String?)

    private val store = context.applicationContext.lastPlaceStore
    private val space = stringPreferencesKey("space")
    private val tab = stringPreferencesKey("tab")

    suspend fun read(): Place = store.data.first().let { Place(it[space], it[tab]) }

    suspend fun saveSpace(uuid: String) {
        store.edit { it[space] = uuid }
    }

    suspend fun saveTab(id: String?) {
        store.edit { if (id == null) it.remove(tab) else it[tab] = id }
    }
}
