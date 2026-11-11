package dev.nerzouille.koan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
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
class LoginStore(private val dao: KoanDao, private val box: SecretBox, private val hidden: HiddenLogins) {
    val summaries: Flow<List<LoginSummary>> = combine(dao.observeLogins(), hidden.guids) { rows, gone ->
        rows.filterNot { it.guid in gone }.map { LoginSummary(it.guid, it.origin, it.local) }
    }

    /**
     * Deletes [guids]: a login saved on this phone is deleted; one from the Mozilla account is only
     * hidden here (Koan doesn't change the account's passwords). [restore] undoes it.
     */
    suspend fun remove(guids: List<String>): Removed = withContext(Dispatchers.IO) {
        val rows = dao.loginsByGuid(guids)
        val (local, synced) = rows.partition { it.local }
        dao.deleteLogins(local.map { it.guid })
        hidden.add(synced.map { it.guid })
        Removed(local, synced.map { it.guid })
    }

    suspend fun restore(removed: Removed) = withContext(Dispatchers.IO) {
        dao.upsertLogins(removed.deleted)
        hidden.remove(removed.hidden)
    }

    /** What [remove] did: the phone's logins it deleted, the account's it hid. */
    class Removed(val deleted: List<LoginEntity>, val hidden: List<String>) {
        val count get() = deleted.size + hidden.size
    }

    /**
     * A login typed in by hand (Passwords › +): [site] as typed ("example.com", or a full address),
     * saved on this phone like "Save in Koan". False when [site] isn't a web address.
     */
    suspend fun addManual(site: String, username: String, password: String): Boolean {
        val origin = originOf(site) ?: return false
        saveLocal(origin, formActionOrigin = origin, httpRealm = null, usernameField = "", passwordField = "", username = username, password = password)
        return true
    }

    fun seal(username: String, password: String): ByteArray =
        box.seal(JSONObject().put("u", username).put("p", password).toString().toByteArray(Charsets.UTF_8))

    fun open(row: LoginEntity): Login? = runCatching {
        val json = JSONObject(String(box.open(row.secret), Charsets.UTF_8))
        Login(
            row.guid, row.origin, row.formActionOrigin, row.httpRealm, json.optString("u"), json.optString("p"),
            row.usernameField, row.passwordField, row.timeLastUsed, row.local,
        )
    }.getOrNull()

    /**
     * Every login opened (Settings, after the fingerprint or PIN), a few at a time in parallel
     * (each one is a Keystore round trip): [onBatch] gets them as they come, so the list fills in
     * right away instead of after the last one.
     */
    suspend fun openAll(onBatch: (List<Login>) -> Unit) = withContext(Dispatchers.Default) {
        val rows = dao.allLogins()
        rows.chunked(BATCH).forEach { chunk ->
            val opened = coroutineScope { chunk.map { row -> async(KEYSTORE_POOL) { open(row) } }.awaitAll() }.filterNotNull()
            withContext(Dispatchers.Main) { onBatch(opened) }
        }
    }

    /** One login opened first (the one just tapped), before the rest. */
    suspend fun openOne(guid: String): Login? = withContext(Dispatchers.Default) { dao.allLogins().firstOrNull { it.guid == guid }?.let(::open) }

    /** The logins for a page at [origin] (its site and the site's subdomains), for autofill. */
    suspend fun forOrigin(origin: String): List<Login> = withContext(Dispatchers.Default) {
        val host = android.net.Uri.parse(origin).host ?: return@withContext emptyList()
        val gone = hidden.guids.value
        dao.loginsFor(origin.trimEnd('/'), baseDomain(host)).filterNot { it.guid in gone }.mapNotNull(::open)
    }

    suspend fun touch(guid: String) = dao.touchLogin(guid, System.currentTimeMillis())

    /** "Save in Koan (this phone only)": sealed like the others, never synced (until M6 offers it). */
    suspend fun saveLocal(
        origin: String,
        formActionOrigin: String?,
        httpRealm: String?,
        usernameField: String,
        passwordField: String,
        username: String,
        password: String,
    ) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        val clean = origin.trimEnd('/')
        // Same site and username saved here before: it is an update of that one.
        val existing = dao.loginsFor(clean, baseDomain(android.net.Uri.parse(clean).host ?: clean))
            .filter { it.local && it.origin == clean }
            .firstOrNull { open(it)?.username == username }
        dao.upsertLogins(
            listOf(
                LoginEntity(
                    guid = existing?.guid ?: java.util.UUID.randomUUID().toString(),
                    origin = clean,
                    formActionOrigin = formActionOrigin,
                    httpRealm = httpRealm,
                    usernameField = usernameField,
                    passwordField = passwordField,
                    secret = seal(username, password),
                    timeCreated = existing?.timeCreated ?: now,
                    timeLastUsed = now,
                    timePasswordChanged = now,
                    local = true,
                ),
            ),
        )
    }

    companion object {
        private const val BATCH = 12

        /** "example.com" or "https://example.com/login" → "https://example.com"; null for what isn't a site. */
        fun originOf(site: String): String? {
            val trimmed = site.trim()
            if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val uri = runCatching { java.net.URI(withScheme) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase() ?: return null
            if (scheme != "https" && scheme != "http") return null
            if ('.' !in host && host != "localhost") return null
            val port = if (uri.port != -1) ":${uri.port}" else ""
            return "$scheme://$host$port"
        }

        /** Keystore decryptions in parallel (more just queue in the keystore daemon). */
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        private val KEYSTORE_POOL = Dispatchers.IO.limitedParallelism(4)

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
