package dev.nerzouille.koan.data

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/**
 * Koan's DataStores never stop it from starting: a corrupted file (a crash mid-write, a full disk)
 * is replaced by an empty one, so every setting goes back to its default, and a read error gives
 * the defaults for that read instead of throwing.
 */
object SafeStores {
    private const val TAG = "KoanStores"

    /** Replaces a corrupted preferences file by an empty one (defaults); logs only the store's name. */
    fun resetOnCorruption(name: String) = ReplaceFileCorruptionHandler<Preferences> { e: CorruptionException ->
        Log.w(TAG, "$name: corrupted file replaced by defaults (${e.javaClass.simpleName})")
        emptyPreferences()
    }

    internal fun readError(e: Throwable) {
        Log.w(TAG, "read error, defaults used (${e.javaClass.simpleName})")
    }
}

/** A preferences DataStore with [SafeStores.resetOnCorruption]. */
fun safePreferencesDataStore(name: String) =
    preferencesDataStore(name = name, corruptionHandler = SafeStores.resetOnCorruption(name))

/** [DataStore.data] that gives empty preferences (defaults) instead of throwing on a read error. */
val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { e ->
        if (e !is IOException) throw e
        SafeStores.readError(e)
        emit(emptyPreferences())
    }
