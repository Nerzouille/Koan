package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64

/*
 * Two-way sync (M6), docs/SYNC_WRITE.md: what Koan would send to the Mozilla account, planned
 * from the phone's data and the records just read. Pure functions: nothing here talks to a
 * server. A plan's [UploadPlan.summary] is the only thing ever logged: counts and field names,
 * never a password, username, URL or title.
 */

/** What would be uploaded to one collection. [records] are cleartexts, ready to encrypt. */
class UploadPlan(
    val collection: String,
    val records: List<JSONObject>,
    val new: Int,
    val changed: Int,
    /** Phone items that would merge into one the account already has (same site + username…). */
    val mergesIntoExisting: Int,
    /** Phone items left out: invalid for the other clients (reasons, no content). */
    val invalid: List<String>,
    /** Records the account already has that the plan also rewrites (e.g. a bookmark folder). */
    val rewrites: Int = 0,
) {
    val fields: Set<String> get() = records.flatMapTo(sortedSetOf()) { it.keys().asSequence().toList() }

    /** Safe to log: counts and field names only. */
    fun summary(): String =
        "$collection: ${records.size} record(s) to upload (new $new, changed $changed, rewritten $rewrites, " +
            "merging into existing $mergesIntoExisting), fields ${fields.joinToString(",")}, " +
            "${invalid.size} left out" + if (invalid.isEmpty()) "" else " (${invalid.groupingBy { it }.eachCount().entries.joinToString { "${it.key} ×${it.value}" }})"

    override fun toString() = summary()
}

/** A Sync id as Firefox makes them: 12 base64url characters (9 random bytes). */
fun newSyncId(random: SecureRandom = SecureRandom()): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(9).also(random::nextBytes))

private val SYNC_ID = Regex("^[-A-Za-z0-9_]{12}$")
fun isPlacesSyncId(id: String) = SYNC_ID.matches(id)

/**
 * Rule 4 of SYNC_WRITE.md: may Koan write to [collection] at all? Reads the (unencrypted)
 * `meta/global` payload. Null = yes; else why not.
 */
object WriteGuard {
    const val STORAGE_VERSION = 5

    /** Engine versions Koan's records are written for. */
    val ENGINE_VERSIONS = mapOf("passwords" to 1, "history" to 1, "bookmarks" to 2)

    fun refusal(metaGlobal: JSONObject?, collection: String, knownSyncId: String?): String? {
        val meta = metaGlobal ?: return "meta/global unreadable"
        if (meta.optInt("storageVersion", -1) != STORAGE_VERSION) return "storage version ${meta.opt("storageVersion")} != $STORAGE_VERSION"
        val declined = meta.optJSONArray("declined")
        if (declined != null && (0 until declined.length()).any { declined.optString(it) == collection }) return "$collection is declined"
        val engine = meta.optJSONObject("engines")?.optJSONObject(collection) ?: return "$collection engine missing"
        val expected = ENGINE_VERSIONS[collection]
        if (expected != null && engine.optInt("version", -1) != expected) return "$collection engine version ${engine.opt("version")} != $expected"
        val syncId = engine.optString("syncID")
        if (syncId.isEmpty()) return "$collection syncID missing"
        if (knownSyncId != null && syncId != knownSyncId) return "$collection syncID changed since the last read"
        return null
    }
}

/** A login saved on the phone, as the planner sees it (opened in memory, never logged). */
class PhoneLogin(
    val guid: String,
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val username: String,
    val password: String,
    val usernameField: String,
    val passwordField: String,
    val timeCreated: Long,
    val timeLastUsed: Long,
    val timePasswordChanged: Long,
) {
    override fun toString() = "PhoneLogin(<redacted>)"
}

/** Step 1: logins saved on the phone → `passwords` (SYNC_WRITE.md §1). */
object LoginUpload {
    /** Both Firefox engines refuse to sync the Mozilla accounts' own credentials. */
    private val EXCLUDED_ORIGINS = setOf("https://accounts.firefox.com", "chrome://FirefoxAccounts")

    /** The record for [login]: exactly one of formSubmitURL / httpRealm, ms timestamps. */
    fun record(login: PhoneLogin): JSONObject = JSONObject().apply {
        put("id", login.guid)
        put("hostname", origin(login.origin))
        if (login.httpRealm != null) {
            put("httpRealm", login.httpRealm)
            put("usernameField", "")
            put("passwordField", "")
        } else {
            // Null form action = any form on the site ("").
            put("formSubmitURL", login.formActionOrigin?.let(::origin) ?: "")
            put("usernameField", login.usernameField)
            put("passwordField", login.passwordField)
        }
        put("username", login.username)
        put("password", login.password)
        put("timeCreated", login.timeCreated)
        put("timeLastUsed", login.timeLastUsed)
        put("timePasswordChanged", login.timePasswordChanged)
        put("timesUsed", 1)
    }

    /** Why the record would be dropped or rejected by Firefox desktop or Fenix; null = fine. */
    fun problem(r: JSONObject): String? {
        val id = r.optString("id")
        if (id.isEmpty() || id.length > 64 || id.contains(',') || id.any { it.code !in 0x20..0x7e }) return "bad id"
        val host = r.optString("hostname")
        if (host.isEmpty() || !host.contains("://")) return "bad origin"
        if (host in EXCLUDED_ORIGINS) return "Mozilla account login"
        if (r.has("formSubmitURL") == r.has("httpRealm")) return "needs exactly one of form action / realm"
        if (r.optString("password").isEmpty()) return "empty password"
        if (r.optString("usernameField") == ".") return "bad username field"
        val noNewline = listOf("hostname", "formSubmitURL", "httpRealm", "usernameField", "passwordField")
        if (noNewline.any { k -> r.optString(k).any { it == '\u0000' || it == '\n' || it == '\r' } }) return "control character"
        if (listOf("username", "password").any { k -> r.optString(k).contains('\u0000') }) return "control character"
        if (listOf("timeCreated", "timePasswordChanged", "timeLastUsed").any { r.optLong(it) !in MIN_MS..MAX_MS }) return "timestamp not in ms"
        return null
    }

    /**
     * The upload: every phone login, [serverIds] = the account's login ids, [serverLogins] = the
     * account's (origin, username) pairs, to say which ones Firefox will merge into an existing login.
     */
    fun plan(phone: List<PhoneLogin>, serverIds: Set<String>, serverLogins: Set<Pair<String, String>>): UploadPlan {
        val records = mutableListOf<JSONObject>()
        val invalid = mutableListOf<String>()
        var new = 0
        var changed = 0
        var merges = 0
        for (login in phone) {
            val r = record(login)
            val why = problem(r)
            if (why != null) {
                invalid += why
                continue
            }
            records += r
            when {
                login.guid in serverIds -> changed++
                (r.getString("hostname") to login.username) in serverLogins -> merges++
                else -> new++
            }
        }
        return UploadPlan("passwords", records, new, changed, merges, invalid)
    }

    /** `scheme://host[:port]`: what Firefox calls an origin. */
    fun origin(url: String): String {
        val u = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return url.trimEnd('/')
        if (u.scheme == null || u.host == null) return url.trimEnd('/')
        return u.scheme.lowercase() + "://" + u.host.lowercase() + if (u.port != -1) ":${u.port}" else ""
    }

    /** 2001 … 2100 in ms: anything outside is seconds or microseconds by mistake. */
    private const val MIN_MS = 978_307_200_000L
    private const val MAX_MS = 4_102_444_800_000L
}

/** A page visited on the phone, as the planner sees it. */
class PhoneVisit(val url: String, val title: String, val lastVisitMs: Long)

/** Step 2a: the phone's visits → `history` (SYNC_WRITE.md §2). Never a tombstone. */
object HistoryUpload {
    const val MAX_VISITS = 20

    /** Firefox's history TTL: 60 days, in seconds (sent as the BSO `ttl`). */
    const val TTL_SECONDS = 5_184_000

    /** Transition "link": the phone doesn't know better. */
    private const val LINK = 1

    /**
     * [server]: the account's history records already read (cleartexts, by URL). A known URL keeps
     * its id and every field, the phone's visit is added (µs, newest 20 kept); a new URL gets a
     * fresh 12-character id. Only http(s) pages; a visit already there is not "changed".
     */
    fun plan(phone: List<PhoneVisit>, server: Map<String, JSONObject>, random: SecureRandom = SecureRandom()): UploadPlan {
        val records = mutableListOf<JSONObject>()
        val invalid = mutableListOf<String>()
        var new = 0
        var changed = 0
        for (visit in phone) {
            if (!visit.url.startsWith("http://") && !visit.url.startsWith("https://")) {
                invalid += "not a web page"
                continue
            }
            if (visit.url.length > 65_536) {
                invalid += "URL too long"
                continue
            }
            val micros = visit.lastVisitMs * 1000
            val known = server[visit.url]
            val record = if (known != null) JSONObject(known.toString()) else JSONObject()
                .put("id", newSyncId(random)).put("histUri", visit.url).put("title", visit.title)
            val visits = record.optJSONArray("visits") ?: JSONArray()
            val dates = (0 until visits.length()).mapNotNull { visits.optJSONObject(it) }
            if (dates.any { it.optLong("date") == micros }) continue
            val merged = (dates + JSONObject().put("date", micros).put("type", LINK))
                .sortedByDescending { it.optLong("date") }.take(MAX_VISITS)
            record.put("visits", JSONArray(merged))
            if (problem(record) != null) {
                invalid += problem(record)!!
                continue
            }
            records += record
            if (known != null) changed++ else new++
        }
        return UploadPlan("history", records, new, changed, 0, invalid)
    }

    fun problem(r: JSONObject): String? {
        if (!isPlacesSyncId(r.optString("id"))) return "bad id"
        if (r.optString("histUri").isEmpty()) return "no URL"
        val visits = r.optJSONArray("visits") ?: return "no visits"
        if (visits.length() !in 1..MAX_VISITS) return "visit count"
        for (i in 0 until visits.length()) {
            val v = visits.optJSONObject(i) ?: return "bad visit"
            // Microseconds since 2001 at least: a value in ms would be read as 1970.
            if (v.optLong("date") < 978_307_200_000_000L) return "visit date not in µs"
            if (v.optInt("type") !in 1..9) return "bad visit type"
        }
        return null
    }
}

/** A bookmark added on the phone, as the planner sees it. */
class PhoneBookmark(val url: String, val title: String, val createdAtMs: Long)

/** Step 2b: bookmarks added on the phone → `bookmarks`, under "Mobile Bookmarks" (SYNC_WRITE.md §2). */
object BookmarkUpload {
    const val MOBILE = "mobile"

    /**
     * [server]: the account's bookmark records (cleartexts). Phone bookmarks whose URL the account
     * already has are skipped (dogear would de-duplicate them anyway). The new ones go under
     * `mobile`, and the `mobile` folder is rewritten with its complete children list (new ones
     * appended), every other field kept: one batch.
     */
    fun plan(phone: List<PhoneBookmark>, server: List<JSONObject>, random: SecureRandom = SecureRandom()): UploadPlan {
        val known = server.mapNotNullTo(HashSet()) { r -> r.optString("bmkUri").takeIf { r.optString("type") == "bookmark" && it.isNotEmpty() } }
        val invalid = mutableListOf<String>()
        val adds = mutableListOf<JSONObject>()
        var merges = 0
        for (b in phone) {
            if (!b.url.startsWith("http://") && !b.url.startsWith("https://")) {
                invalid += "not a web page"
                continue
            }
            if (b.url in known) {
                merges++
                continue
            }
            adds += JSONObject()
                .put("id", newSyncId(random))
                .put("type", "bookmark")
                .put("parentid", MOBILE)
                .put("parentName", "mobile")
                .put("title", b.title)
                .put("bmkUri", b.url)
                .put("dateAdded", b.createdAtMs)
                .put("hasDupe", true)
        }
        if (adds.isEmpty()) return UploadPlan("bookmarks", emptyList(), 0, 0, merges, invalid)
        val existing = server.firstOrNull { it.optString("id") == MOBILE && !it.optBoolean("deleted") }
        val folder = if (existing != null) JSONObject(existing.toString()) else JSONObject()
            .put("id", MOBILE).put("type", "folder").put("parentid", "places").put("parentName", "").put("title", "mobile")
        val children = folder.optJSONArray("children") ?: JSONArray()
        adds.forEach { children.put(it.getString("id")) }
        folder.put("children", children).put("hasDupe", true)
        return UploadPlan("bookmarks", adds + folder, adds.size, 0, merges, invalid, rewrites = if (existing != null) 1 else 0)
    }

    /** The whole batch must form a valid tree for dogear: every new item listed by its parent, no `places`. */
    fun problem(batch: List<JSONObject>): String? {
        if (batch.any { it.optString("id") == "places" }) return "places root"
        val folders = batch.filter { it.optString("type") == "folder" }.associateBy { it.optString("id") }
        for (r in batch) {
            if (r.optString("type") != "bookmark") continue
            if (!isPlacesSyncId(r.optString("id"))) return "bad id"
            if (r.optString("bmkUri").isEmpty()) return "no URL"
            val parent = folders[r.optString("parentid")] ?: return "parent folder not in the batch"
            val kids = parent.optJSONArray("children") ?: return "parent without children"
            if ((0 until kids.length()).none { kids.optString(it) == r.optString("id") }) return "not listed by its parent"
        }
        return null
    }
}
