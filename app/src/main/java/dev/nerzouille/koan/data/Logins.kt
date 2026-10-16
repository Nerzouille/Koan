package dev.nerzouille.koan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        dao.loginsFor(origin.trimEnd('/'), baseDomain(host)).mapNotNull(::open)
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
