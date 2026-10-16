package dev.nerzouille.koan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A login as Koan uses it, opened (in memory only, never logged). */
class Login(
    val guid: String,
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val username: String,
    val password: String,
    val usernameField: String,
    val passwordField: String,
    val timeLastUsed: Long,
    val local: Boolean,
) {
    override fun toString() = "Login(guid=$guid, origin=$origin, <redacted>)"
}

/** Where a login is for, without opening it: Settings' list. */
class LoginSummary(val guid: String, val origin: String, val local: Boolean)

/**
 * Saved logins: the Mozilla account's (read-only, replaced at each pull) and the phone's own.
 * Username and password are sealed together with a Keystore key ([box]); only the site is stored
 * in the clear.
 */
class LoginStore(private val dao: KoanDao, private val box: SecretBox) {
    val summaries: Flow<List<LoginSummary>> = dao.observeLogins().map { rows -> rows.map { LoginSummary(it.guid, it.origin, it.local) } }

    fun seal(username: String, password: String): ByteArray =
        box.seal(JSONObject().put("u", username).put("p", password).toString().toByteArray(Charsets.UTF_8))

    fun open(row: LoginEntity): Login? = runCatching {
        val json = JSONObject(String(box.open(row.secret), Charsets.UTF_8))
        Login(
            row.guid, row.origin, row.formActionOrigin, row.httpRealm, json.optString("u"), json.optString("p"),
            row.usernameField, row.passwordField, row.timeLastUsed, row.local,
        )
    }.getOrNull()

    /** Every login opened (Settings, after the fingerprint or PIN). */
    suspend fun all(): List<Login> = withContext(Dispatchers.Default) { dao.allLogins().mapNotNull(::open) }

    /** The logins for a page at [origin] (its site and the site's subdomains), for autofill. */
    suspend fun forOrigin(origin: String): List<Login> = withContext(Dispatchers.Default) {
        val host = android.net.Uri.parse(origin).host ?: return@withContext emptyList()
        dao.loginsFor(origin.trimEnd('/'), baseDomain(host)).mapNotNull(::open)
    }

    suspend fun touch(guid: String) = dao.touchLogin(guid, System.currentTimeMillis())

    companion object {
        /** "accounts.example.co.uk" → "example.co.uk", roughly: the last two labels (three for short ccSLDs). */
        fun baseDomain(host: String): String {
            val labels = host.removePrefix("www.").split('.')
            if (labels.size <= 2) return labels.joinToString(".")
            val secondLevel = labels[labels.size - 2]
            val keep = if (secondLevel.length <= 3 && labels.last().length == 2) 3 else 2
            return labels.takeLast(keep).joinToString(".")
        }
    }
}
