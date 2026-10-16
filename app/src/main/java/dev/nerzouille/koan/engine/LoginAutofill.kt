package dev.nerzouille.koan.engine

import dev.nerzouille.koan.data.LoginStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/**
 * Where GeckoView finds logins for a page: Koan's sealed store (the Mozilla account's, read-only,
 * and the phone's own). Saving goes through Koan's "Save in Koan" card, not here.
 */
class LoginAutofill(private val store: LoginStore, private val scope: CoroutineScope) : Autocomplete.StorageDelegate {
    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>> {
        val result = GeckoResult<Array<Autocomplete.LoginEntry>>()
        scope.launch {
            val entries = runCatching { store.forOrigin("https://$domain") }.getOrDefault(emptyList()).map { login ->
                Autocomplete.LoginEntry.Builder()
                    .guid(login.guid)
                    .origin(login.origin)
                    .formActionOrigin(login.formActionOrigin)
                    .httpRealm(login.httpRealm)
                    .username(login.username)
                    .password(login.password)
                    .build()
            }
            result.complete(entries.toTypedArray())
        }
        return result
    }

    override fun onLoginUsed(login: Autocomplete.LoginEntry, usedFields: Int) {
        val guid = login.guid ?: return
        scope.launch { store.touch(guid) }
    }
}
