package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * A tiny Sync storage server in memory (fake data only): records per collection as the server keeps
 * them (encrypted payloads), a timestamp per collection, conditional POSTs (412 when
 * `X-If-Unmodified-Since` is stale), `meta/global`. Every request is kept in [log].
 */
internal class FakeStorage(val keys: SyncCrypto.KeyBundle = testKeys()) : SyncHttpTransport {
    val log = mutableListOf<SyncHttpRequest>()
    private val records = mutableMapOf<String, LinkedHashMap<String, JSONObject>>()
    private val modified = mutableMapOf<String, Double>()
    private var clock = 1_700_000_000.0

    var metaGlobal: JSONObject? = meta()

    /** Answers the next POST with this status instead of applying it (then forgets it). */
    var nextPostStatus: Int? = null

    /** Throws this on the next POST (then forgets it). */
    var nextPostThrows: IOException? = null

    /** Someone else writes [collection] just before Koan's next POST (so it gets a 412). */
    var concurrentWriteBeforeNextPost: String? = null

    fun put(collection: String, cleartext: JSONObject) {
        val id = cleartext.getString("id")
        val payload = SyncCrypto.encryptBSO(cleartext.toString().toByteArray(Charsets.UTF_8), keys)
        records.getOrPut(collection) { LinkedHashMap() }[id] = JSONObject().put("id", id).put("payload", payload).put("modified", tick(collection))
    }

    /** What the server holds for [collection], decrypted. */
    fun cleartexts(collection: String): Map<String, JSONObject> = records[collection].orEmpty().mapValues { (_, r) ->
        JSONObject(String(SyncCrypto.decryptBSO(r.getString("payload"), keys), Charsets.UTF_8))
    }

    val posts get() = log.filter { it.method == "POST" }

    /** The cleartexts of one POST's records (decrypted with the fake account's keys). */
    fun postedCleartexts(request: SyncHttpRequest): List<JSONObject> {
        val array = JSONArray(String(request.body!!, Charsets.UTF_8))
        return (0 until array.length()).map {
            JSONObject(String(SyncCrypto.decryptBSO(array.getJSONObject(it).getString("payload"), keys), Charsets.UTF_8))
        }
    }

    fun lastModified(collection: String): String? = modified[collection]?.let(::stamp)

    override fun execute(request: SyncHttpRequest): SyncHttpResponse {
        log += request
        val path = request.url.path.substringAfter("/1.5/1")
        val query = request.url.query.orEmpty()
        val parts = path.removePrefix("/storage/").split('/')
        return when {
            path == "/storage/meta/global" && request.method == "GET" ->
                metaGlobal?.let { ok(JSONObject().put("id", "global").put("payload", it.toString())) } ?: missing()
            request.method == "GET" && parts.size == 2 -> {
                records[parts[0]]?.get(java.net.URLDecoder.decode(parts[1], "UTF-8"))?.let { ok(it, parts[0]) } ?: missing()
            }
            request.method == "GET" && parts.size == 1 -> {
                val all = records[parts[0]].orEmpty().values.toList()
                val limit = Regex("limit=(\\d+)").find(query)?.groupValues?.get(1)?.toInt() ?: all.size
                val page = (if ("sort=newest" in query) all.reversed() else all).take(limit)
                if (records[parts[0]] == null) missing() else ok(JSONArray(page), parts[0])
            }
            request.method == "POST" && parts.size == 1 -> post(parts[0], request)
            else -> SyncHttpResponse(400, emptyMap(), "unexpected ${request.method} $path".toByteArray())
        }
    }

    private fun post(collection: String, request: SyncHttpRequest): SyncHttpResponse {
        nextPostThrows?.let { nextPostThrows = null; throw it }
        nextPostStatus?.let { nextPostStatus = null; return SyncHttpResponse(it, emptyMap(), "{}".toByteArray()) }
        concurrentWriteBeforeNextPost?.let { other ->
            concurrentWriteBeforeNextPost = null
            put(other, JSONObject().put("id", "someone-else").put("note", "written by another device"))
        }
        val since = request.headers["X-If-Unmodified-Since"]
        val current = lastModified(collection)
        if (since != null && current != null && since.toDouble() < current.toDouble()) {
            return SyncHttpResponse(412, mapOf("x-last-modified" to current), ByteArray(0))
        }
        val array = JSONArray(String(request.body!!, Charsets.UTF_8))
        val now = tick(collection)
        val success = JSONArray()
        for (i in 0 until array.length()) {
            val bso = array.getJSONObject(i)
            records.getOrPut(collection) { LinkedHashMap() }[bso.getString("id")] = JSONObject(bso.toString()).put("modified", now)
            success.put(bso.getString("id"))
        }
        val body = JSONObject().put("success", success).put("failed", JSONObject()).put("modified", now)
        return SyncHttpResponse(200, mapOf("x-last-modified" to stamp(now)), body.toString().toByteArray())
    }

    private fun tick(collection: String): Double {
        clock += 1.0
        modified[collection] = clock
        return clock
    }

    private fun ok(body: Any, collection: String? = null): SyncHttpResponse {
        val headers = collection?.let { c -> lastModified(c)?.let { mapOf("x-last-modified" to it) } }.orEmpty()
        return SyncHttpResponse(200, headers, body.toString().toByteArray())
    }

    private fun missing() = SyncHttpResponse(404, emptyMap(), "{}".toByteArray())

    private fun stamp(v: Double) = String.format(Locale.ROOT, "%.2f", v)

    companion object {
        fun testKeys() = SyncCrypto.KeyBundle(ByteArray(32) { 7 }, ByteArray(32) { 9 })

        fun meta(passwordsVersion: Int = 1, historyVersion: Int = 1, passwordsSyncId: String = "pw-sync-id", historySyncId: String = "hist-sync-id"): JSONObject =
            JSONObject().put("storageVersion", 5).put("syncID", "global-sync-id").put(
                "engines",
                JSONObject()
                    .put("passwords", JSONObject().put("version", passwordsVersion).put("syncID", passwordsSyncId))
                    .put("history", JSONObject().put("version", historyVersion).put("syncID", historySyncId)),
            )

        val creds = TokenServerCreds("1", "https://sync.example.com/1.5/1", "hawk-id", ByteArray(32) { 3 }, Long.MAX_VALUE)
    }

    fun client() = SyncClient(creds, keys, emptyMap(), this)
}
