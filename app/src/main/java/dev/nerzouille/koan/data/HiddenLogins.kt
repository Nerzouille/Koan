package dev.nerzouille.koan.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Mozilla account's logins deleted on the phone: hidden here (the list, autofill) but left on
 * the account. Only their ids, which say nothing about the login.
 */
class HiddenLogins(context: Context) {
    private val prefs = context.getSharedPreferences("hidden_logins", Context.MODE_PRIVATE)
    private val _guids = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    val guids: StateFlow<Set<String>> get() = _guids

    fun add(ids: List<String>) = save(_guids.value + ids)

    fun remove(ids: List<String>) = save(_guids.value - ids.toSet())

    private fun save(ids: Set<String>) {
        if (ids == _guids.value) return
        _guids.value = ids
        prefs.edit().putStringSet(KEY, ids).apply()
    }

    private companion object {
        const val KEY = "guids"
    }
}
