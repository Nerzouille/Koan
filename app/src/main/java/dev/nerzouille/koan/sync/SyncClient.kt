// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.security.SecureRandom
import java.util.Base64

/**
 * A consistent collection read: every page was served under one
 * `X-Last-Modified` condition (SPEC §7.2).
 */
data class CollectionRead(
    val records: List<JSONObject>,
    val lastModified: String?,
)

/** Outcome of a conditional single-record PUT. */
sealed class PutOutcome {
    object Applied : PutOutcome()
    data class PreconditionFailed(val lastModified: String?) : PutOutcome()
}

/** Outcome of an atomic multi-record POST (SPEC §7.2). */
sealed class PostOutcome {
    object Applied : PostOutcome()
    data class PreconditionFailed(val lastModified: String?) : PostOutcome()

    /** Server rejected some requested ids (`failed` and/or absent from `success`). */
    data class PartialFailure(
        val failed: Map<String, String>,
        val missingIds: List<String>,
    ) : PostOutcome()
}

/**
 * Port of `Shared/SyncClient.swift`. Hawk signs the exact request-line
 * resource with percent-encoding preserved; BSO ids are percent-encoded to
 * the same unreserved set as the Swift implementation.
 *
 * All network entry points block; callers must stay off the main thread.
 */
class SyncClient internal constructor(
    private val creds: TokenServerCreds,
    syncKeys: SyncCrypto.KeyBundle?,
    preloadedDefaultKeys: SyncCrypto.KeyBundle?,
    preloadedCollectionKeys: Map<String, SyncCrypto.KeyBundle>?,
    private val transport: SyncHttpTransport,
) {
    private val defaultKeys: SyncCrypto.KeyBundle
    private val collectionKeys: Map<String, SyncCrypto.KeyBundle>

    init {
        if (preloadedDefaultKeys != null && preloadedCollectionKeys != null) {
            defaultKeys = preloadedDefaultKeys
            collectionKeys = preloadedCollectionKeys
        } else {
            val loaded = fetchCollectionKeys(creds, requireNotNull(syncKeys) { "Sync key required when keys are not preloaded" })
            defaultKeys = loaded.first
            collectionKeys = loaded.second
        }
    }

    constructor(
        creds: TokenServerCreds,
        kB: ByteArray,
        transport: SyncHttpTransport = UrlConnectionTransport(),
    ) : this(creds, SyncCrypto.syncKeyBundle(kB), null, null, transport)

    /** Koan: with the oldsync scoped key an OAuth sign-in (QR pairing) delivers instead of kB. */
    constructor(
        creds: TokenServerCreds,
        syncKey: ScopedKeys.SyncKey,
        transport: SyncHttpTransport = UrlConnectionTransport(),
    ) : this(creds, syncKey.bundle(), null, null, transport)

    /** Hermetic-test constructor: preloaded key bundles skip the `crypto/keys` fetch. */
    internal constructor(
        creds: TokenServerCreds,
        defaultKeys: SyncCrypto.KeyBundle,
        collectionKeys: Map<String, SyncCrypto.KeyBundle>,
        transport: SyncHttpTransport = UrlConnectionTransport(),
    ) : this(creds, null, defaultKeys, collectionKeys, transport)

    fun keys(forCollection: String): SyncCrypto.KeyBundle =
        collectionKeys[forCollection] ?: defaultKeys

    fun infoCollections(): Map<String, Double> {
        val obj = requestJSONObject(method = "GET", path = "/info/collections")
        val out = linkedMapOf<String, Double>()
        for (key in obj.keys()) {
            when (val v = obj.opt(key)) {
                is Double -> out[key] = v
                is Int -> out[key] = v.toDouble()
                is Long -> out[key] = v.toDouble()
            }
        }
        return out
    }

    /** Follows `X-Weave-Next-Offset` pagination like the Swift version. */
    fun getRecords(collection: String): List<JSONObject> {
        val records = mutableListOf<JSONObject>()
        var offset: String? = null
        var pages = 0
        while (pages < 50) {
            var path = "/storage/$collection?full=1&limit=2500"
            if (!offset.isNullOrEmpty()) {
                path += "&offset=" + encodeUnreserved(offset)
            }
            val result = requestPage(path)
            records += result.page
            pages++
            // Stop unless the server signals a next page AND delivered content.
            if (result.nextOffset.isNullOrEmpty() || result.page.isEmpty() || result.nextOffset == offset) break
            offset = result.nextOffset
        }
        return records
    }

    /**
     * Conflict-safe full read (SPEC §7.2): captures the collection's
     * `X-Last-Modified` on the first page and conditions every subsequent page
     * on it, so a concurrent writer makes the read fail cleanly instead of
     * splicing two versions together. One mid-read 412 restarts the whole read
     * once; a second one surfaces as [SyncError.Conflict].
     */
    fun getCollectionWithMetadata(collection: String): CollectionRead {
        var restarts = 0
        while (true) {
            val records = mutableListOf<JSONObject>()
            var offset: String? = null
            var pages = 0
            var lastModified: String? = null
            var preconditionFailed = false
            while (pages < 50) {
                var path = "/storage/$collection?full=1&limit=2500"
                if (!offset.isNullOrEmpty()) {
                    path += "&offset=" + encodeUnreserved(offset)
                }
                val response = requestRaw(
                    creds = this.creds,
                    method = "GET",
                    path = path,
                    ifUnmodifiedSince = if (offset.isNullOrEmpty()) null else lastModified,
                    allowPreconditionFailed = true,
                )
                if (response.statusCode == 412) {
                    preconditionFailed = true
                    break
                }
                if (lastModified == null) lastModified = response.header("X-Last-Modified")
                val result = pageFrom(response)
                records += result.page
                pages++
                if (result.nextOffset.isNullOrEmpty() || result.page.isEmpty() || result.nextOffset == offset) break
                offset = result.nextOffset
            }
            if (!preconditionFailed) return CollectionRead(records, lastModified)
            if (restarts >= 1) throw SyncError.Conflict("collection '$collection' changed while reading")
            restarts++
        }
    }

    /**
     * The newest page of a collection, decrypted by callers. Large read-only
     * collections (history) don't need full pagination — the server's
     * `sort=newest` orders by last-modified, so the first page holds the
     * most recently changed records.
     */
    fun getRecentRecords(collection: String, limit: Int): List<JSONObject> {
        val result = requestPage("/storage/$collection?full=1&limit=$limit&sort=newest")
        return result.page
    }

    fun decryptRecord(collection: String, record: JSONObject): JSONObject {
        val payload = record.optString("payload")
        if (payload.isEmpty()) return JSONObject()
        val data = SyncCrypto.decryptBSO(payloadJSON = payload, keys = keys(collection))
        return JSONObject(String(data, Charsets.UTF_8))
    }

    fun putRecord(collection: String, id: String, obj: JSONObject) {
        val body = SyncCrypto.encryptBSO(obj.toString().toByteArray(Charsets.UTF_8), keys(collection))
        val bso = JSONObject().put("payload", body)
        requestJSONObject(
            method = "PUT",
            path = "/storage/$collection/${encodedBSOId(id)}",
            json = bso,
            allowMissing = false,
        )
    }

    /**
     * Conditional single-record PUT (SPEC §7.2). With a null condition the
     * legacy unconditional request runs unchanged; otherwise a 412 is reported
     * instead of thrown, carrying the server's current `X-Last-Modified`.
     */
    fun putRecord(
        collection: String,
        id: String,
        obj: JSONObject,
        ifUnmodifiedSince: String?,
    ): PutOutcome {
        if (ifUnmodifiedSince == null) {
            putRecord(collection, id, obj)
            return PutOutcome.Applied
        }
        val body = SyncCrypto.encryptBSO(obj.toString().toByteArray(Charsets.UTF_8), keys(collection))
        val bso = JSONObject().put("payload", body)
        val response = requestRaw(
            creds = this.creds,
            method = "PUT",
            path = "/storage/$collection/${encodedBSOId(id)}",
            json = bso,
            allowMissing = false,
            ifUnmodifiedSince = ifUnmodifiedSince,
            allowPreconditionFailed = true,
        )
        if (response.statusCode == 412) {
            return PutOutcome.PreconditionFailed(response.header("X-Last-Modified"))
        }
        return PutOutcome.Applied
    }

    /**
     * Atomic multi-record write (SPEC §7.2): one `POST /storage/<collection>`
     * with a JSON array of encrypted BSOs `[{id, payload}]`, optionally
     * conditioned on the collection timestamp. A 200 response reports the
     * per-record outcome; any requested id missing from `success` is a failure.
     */
    fun postRecords(
        collection: String,
        records: List<JSONObject>,
        ifUnmodifiedSince: String? = null,
    ): PostOutcome {
        val array = JSONArray()
        val requestedIds = mutableListOf<String>()
        for (record in records) {
            val id = record.optString("id")
            require(id.isNotEmpty()) { "postRecords: every record needs an id" }
            requestedIds += id
            val payload = SyncCrypto.encryptBSO(record.toString().toByteArray(Charsets.UTF_8), keys(collection))
            array.put(JSONObject().put("id", id).put("payload", payload))
        }
        val response = requestRaw(
            creds = this.creds,
            method = "POST",
            path = "/storage/$collection",
            json = array,
            allowMissing = false,
            ifUnmodifiedSince = ifUnmodifiedSince,
            allowPreconditionFailed = true,
        )
        if (response.statusCode == 412) {
            return PostOutcome.PreconditionFailed(response.header("X-Last-Modified"))
        }
        val parsed = runCatching { JSONObject(String(response.body, Charsets.UTF_8)) }.getOrNull()
            ?: JSONObject()
        val successArr = parsed.optJSONArray("success") ?: JSONArray()
        val success = buildSet {
            for (i in 0 until successArr.length()) {
                (successArr.opt(i) as? String)?.let { add(it) }
            }
        }
        val failedObj = parsed.optJSONObject("failed") ?: JSONObject()
        val failed = linkedMapOf<String, String>()
        for (key in failedObj.keys()) {
            failed[key] = failedObj.optString(key)
        }
        val missing = requestedIds.filter { !success.contains(it) }
        if (failed.isNotEmpty() || missing.isNotEmpty()) {
            return PostOutcome.PartialFailure(failed = failed, missingIds = missing)
        }
        return PostOutcome.Applied
    }

    /**
     * Weave tombstone: encrypted `{ id, deleted: true }` payload.
     * Zen's Spaces engine reads `CryptoWrapper.deleted` from *cleartext*
     * after decrypt; a raw WBO `{deleted:true}` with no ciphertext never
     * applies and the desktop re-uploads the pin on the next sync.
     */
    fun putTombstone(collection: String, id: String) {
        putRecord(
            collection = collection,
            id = id,
            obj = JSONObject().put("id", id).put("deleted", true),
        )
    }

    private data class PageResult(val page: List<JSONObject>, val nextOffset: String?)

    // MARK: - Collection keys

    private fun fetchCollectionKeys(
        creds: TokenServerCreds,
        syncKeys: SyncCrypto.KeyBundle,
    ): Pair<SyncCrypto.KeyBundle, Map<String, SyncCrypto.KeyBundle>> {
        val record = requestJSONObject(creds = creds, method = "GET", path = "/storage/crypto/keys", allowMissing = true)
        if (!record.has("payload")) {
            // First write on an empty Sync node: create the default collection keys.
            bootstrapCryptoKeys(creds, syncKeys)
            return fetchCollectionKeys(creds, syncKeys)
        }
        val payload = record.optString("payload")
        if (payload.isEmpty()) throw SyncError.Crypto("crypto/keys")
        val plain = SyncCrypto.decryptBSO(payloadJSON = payload, keys = syncKeys)
        val obj = JSONObject(String(plain, Charsets.UTF_8))

        fun bundle(array: Any?): SyncCrypto.KeyBundle {
            val arr = array as? JSONArray ?: throw SyncError.Crypto("collection key")
            val encB64 = arr.optString(0)
            val hmacB64 = arr.optString(1)
            val enc = SyncCrypto.b64decode(encB64) ?: throw SyncError.Crypto("collection key")
            val hmac = SyncCrypto.b64decode(hmacB64) ?: throw SyncError.Crypto("collection key")
            return SyncCrypto.KeyBundle(enc, hmac)
        }

        val def = bundle(obj.opt("default"))
        val map = linkedMapOf<String, SyncCrypto.KeyBundle>()
        val cols = obj.optJSONObject("collections")
        if (cols != null) {
            for (name in cols.keys()) {
                map[name] = bundle(cols.opt(name))
            }
        }
        return def to map
    }

    private fun bootstrapCryptoKeys(creds: TokenServerCreds, syncKeys: SyncCrypto.KeyBundle) {
        val enc = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hmac = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val objectJson = JSONObject()
            .put(
                "default",
                JSONArray()
                    .put(Base64.getEncoder().encodeToString(enc))
                    .put(Base64.getEncoder().encodeToString(hmac)),
            )
            .put("collections", JSONObject())
        val payload = SyncCrypto.encryptBSO(objectJson.toString().toByteArray(Charsets.UTF_8), syncKeys)
        requestRaw(
            creds = creds,
            method = "PUT",
            path = "/storage/crypto/keys",
            json = JSONObject().put("payload", payload),
            allowMissing = false,
            // Create-if-absent (SPEC §7.2): a 412 means another client won the
            // bootstrap race; the caller's re-read recursion decrypts the
            // winner's keys instead of overwriting them.
            ifUnmodifiedSince = "0",
            allowPreconditionFailed = true,
        )
    }

    // MARK: - HTTP

    private fun requestJSONObject(
        creds: TokenServerCreds? = null,
        method: String,
        path: String,
        json: JSONObject? = null,
        allowMissing: Boolean = true,
    ): JSONObject {
        val raw = requestRaw(creds ?: this.creds, method, path, json, allowMissing)
        if (raw.body.isEmpty()) return JSONObject()
        val text = String(raw.body, Charsets.UTF_8)
        val trimmed = text.trim()
        // PUT/DELETE return the new collection timestamp as a bare number.
        if (trimmed.isNotEmpty() && (trimmed.first().isDigit() || trimmed.first() == '-')) {
            return JSONObject()
        }
        return runCatching { org.json.JSONTokener(text).nextValue() }
            .getOrNull() as? JSONObject ?: JSONObject()
    }

    private fun requestPage(path: String): PageResult =
        pageFrom(requestRaw(this.creds, "GET", path))

    private fun pageFrom(raw: SyncHttpResponse): PageResult {
        if (raw.statusCode == 404 || raw.body.isEmpty()) return PageResult(emptyList(), null)
        val list = mutableListOf<JSONObject>()
        runCatching {
            val arr = JSONArray(String(raw.body, Charsets.UTF_8))
            for (i in 0 until arr.length()) {
                (arr.opt(i) as? JSONObject)?.let { list.add(it) }
            }
        }
        return PageResult(list, raw.header("X-Weave-Next-Offset"))
    }

    private fun requestRaw(
        creds: TokenServerCreds,
        method: String,
        path: String,
        json: Any? = null,
        allowMissing: Boolean = true,
        ifUnmodifiedSince: String? = null,
        allowPreconditionFailed: Boolean = false,
    ): SyncHttpResponse {
        val fullURLString = creds.apiEndpoint.trimEnd('/') + path
        val url = URL(fullURLString)
        // SPEC §7: reject non-HTTPS endpoints before signing or transport.
        if (url.protocol != "https") throw SyncError.Network("insecure sync endpoint")
        // Hawk must sign the exact request-line resource (path + query) with
        // percent-encoding preserved — derived from the verbatim URL string
        // exactly as in Swift.
        var resource = fullURLString
        val schemeEnd = resource.indexOf("://")
        if (schemeEnd >= 0) {
            val afterScheme = resource.substring(schemeEnd + 3)
            val slash = afterScheme.indexOf('/')
            resource = if (slash >= 0) afterScheme.substring(slash) else "/"
        }
        val body: ByteArray = json?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val headers = linkedMapOf<String, String>()
        headers["User-Agent"] = FxAClient.userAgent
        if (json != null) headers["Content-Type"] = "application/json"
        ifUnmodifiedSince?.let { headers["X-If-Unmodified-Since"] = it }
        val hash = if (body.isEmpty()) null else HawkAuth.payloadHash(body)
        headers["Authorization"] = HawkAuth.authorization(
            method = method,
            url = url,
            id = creds.hawkID,
            key = creds.hawkKey,
            payloadHash = hash,
            resource = resource,
        )
        val response = transport.execute(
            SyncHttpRequest(
                method = method,
                url = url,
                headers = headers,
                body = if (json != null) body else null,
            ),
        )
        if (response.statusCode == 412 && allowPreconditionFailed) return response
        if (response.statusCode == 404 && allowMissing) return response
        if (response.statusCode !in 200..299) {
            // Hawk auth failures carry the server's verdict here.
            val detailText = String(response.body, Charsets.UTF_8).take(400)
            var detail = "HTTP ${response.statusCode} $method $path: $detailText"
            for (header in listOf("WWW-Authenticate", "X-Timestamp")) {
                response.header(header)?.let { detail += " | $header: $it" }
            }
            throw SyncError.Network(detail)
        }
        return response
    }

    companion object {
        /**
         * BSO ids are opaque server-side strings; Zen's spaces engine uses
         * braced UUIDs (`{…}`). Braces are not valid raw in a URL, so
         * percent-encode every id to the exact form both the wire and the
         * Hawk signature use.
         */
        fun encodedBSOId(id: String): String {
            val sb = StringBuilder()
            for (b in id.toByteArray(Charsets.UTF_8)) {
                val c = b.toInt() and 0xFF
                val unreserved =
                    c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code ||
                        c in '0'.code..'9'.code || c == '-'.code || c == '.'.code ||
                        c == '_'.code || c == '~'.code
                if (unreserved) {
                    sb.append(c.toChar())
                } else {
                    sb.append('%').append("%02X".format(c))
                }
            }
            return sb.toString()
        }

        private fun encodeUnreserved(value: String): String =
            java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
                .replace("+", "%20")
                .replace("%7E", "~")

        /** True when a thrown error means the request was cancelled mid-flight. */
        fun isCancellationError(e: Throwable): Boolean =
            e is kotlinx.coroutines.CancellationException ||
                (e.message?.contains("canceled", ignoreCase = true) == true &&
                    (e is java.io.IOException || e.cause is java.io.IOException))
    }
}

data class TokenServerCreds(
    val uid: String,
    val apiEndpoint: String,
    val hawkID: String,
    val hawkKey: ByteArray,
    val expiresAtMillis: Long,
) {
    override fun equals(other: Any?): Boolean = other is TokenServerCreds &&
        other.uid == uid && other.apiEndpoint == apiEndpoint && other.hawkID == hawkID &&
        other.expiresAtMillis == expiresAtMillis

    override fun hashCode(): Int = uid.hashCode() * 31 + expiresAtMillis.hashCode()

    // Koan: never print the Hawk key (CLAUDE.md rule 5).
    override fun toString(): String = "TokenServerCreds(uid=$uid, expiresAtMillis=$expiresAtMillis, key=<redacted>)"
}
