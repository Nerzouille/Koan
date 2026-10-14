// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONObject
import java.math.BigInteger
import java.net.URL
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.Signature
import java.util.Base64

/**
 * Port of `Shared/FxAClient.swift`: official Mozilla Accounts auth server +
 * Sync token server protocol (onepw + BrowserID), same path Firefox uses.
 */
class FxAClient(
    private val transport: SyncHttpTransport = UrlConnectionTransport(readTimeoutMs = 30_000),
) {
    suspend fun completeWebLogin(
        email: String,
        uid: String,
        sessionToken: String,
        keyFetchToken: String,
        unwrapBKeyHex: String,
    ): CompleteLoginResult {
        val unwrap = FxACrypto.unhex(unwrapBKeyHex)
        val kB = fetchKB(keyFetchToken, unwrap)
        return CompleteLoginResult(uid = uid, session = sessionToken, kB = kB)
    }

    class CompleteLoginResult(val uid: String, val session: String, val kB: ByteArray)

    @Throws(Exception::class)
    suspend fun syncCredentials(sessionToken: String, kB: ByteArray): TokenServerCreds {
        var last = "no token"
        try {
            val oauth = oauthAccessToken(sessionToken)
            val rotation = scopedKeyData(sessionToken)
            return tokenServerCreds("Bearer $oauth", kB, rotation)
        } catch (e: Exception) {
            if (e is SyncError.TotpRequired) throw e
            // Koan: offline or an expired session must not look like a generic auth failure, and
            // the BrowserID fallback would fail the same way.
            if (e is java.io.IOException) throw e
            // Koan: errno 110 = invalid/expired session token, 102 = account gone. Only those mean
            // "sign in again"; anything else keeps Z-Sync's BrowserID fallback.
            if (e is SyncError.Auth && (e.errno == 110 || e.errno == 102)) throw SyncError.SessionExpired
            last = e.message ?: e.toString()
        }
        try {
            val assertion = browserIDAssertion(sessionToken, audience = TOKEN_SERVER_AUDIENCE)
            return tokenServerCreds("BrowserID $assertion", kB, 0)
        } catch (e: Exception) {
            throw SyncError.Auth("Sync sign-in failed. $last / ${e.message ?: e.toString()}")
        }
    }

    /** oldsync scoped-key metadata; raw millisecond keyRotationTimestamp. */
    private suspend fun scopedKeyData(sessionToken: String): Long {
        val token = FxACrypto.unhex(sessionToken)
        val url = URL("$authBase/account/scoped-key-data")
        val json = hawkJSON(
            url = url,
            method = "POST",
            token = token,
            tokenType = "sessionToken",
            json = JSONObject()
                .put("client_id", OAUTH_CLIENT_ID)
                .put("scope", OLD_SYNC_SCOPE),
        )
        val meta = json.optJSONObject(OLD_SYNC_SCOPE)
            ?: throw SyncError.Auth("missing keyRotationTimestamp")
        val ts = meta.optLong("keyRotationTimestamp", Long.MIN_VALUE)
        if (ts == Long.MIN_VALUE || !meta.has("keyRotationTimestamp")) {
            throw SyncError.Auth("missing keyRotationTimestamp")
        }
        return ts
    }

    /** Firefox-desktop client, fxa-credentials grant. OAuth wants Hawk, not a raw Bearer. */
    private suspend fun oauthAccessToken(sessionToken: String): String {
        val token = FxACrypto.unhex(sessionToken)
        val body = JSONObject()
            .put("client_id", OAUTH_CLIENT_ID)
            .put("grant_type", "fxa-credentials")
            .put("scope", OLD_SYNC_SCOPE)
            .put("access_type", "offline")
        val urls = listOf(
            URL("https://oauth.accounts.firefox.com/v1/token"),
            URL("$authBase/oauth/token"),
        )
        var last = "OAuth failed"
        var lastError: Exception? = null
        for (url in urls) {
            try {
                val obj = hawkJSON(url = url, method = "POST", token = token, tokenType = "sessionToken", json = body)
                obj.optString("access_token").takeIf { it.isNotEmpty() }?.let { return it }
                last = "OAuth response without access_token"
            } catch (e: Exception) {
                last = e.message ?: e.toString()
                lastError = e
            }
        }
        // Koan: when every endpoint failed the same telling way (offline, or an auth error with an
        // errno), surface that instead of a generic message.
        when (val e = lastError) {
            is java.io.IOException -> throw e
            is SyncError.Auth -> if (e.errno != null) throw SyncError.Auth(last, e.errno)
            else -> Unit
        }
        throw SyncError.Auth(last)
    }

    private suspend fun hawkJSON(
        url: URL,
        method: String,
        token: ByteArray,
        tokenType: String,
        json: JSONObject?,
    ): JSONObject {
        val any = hawkAny(url, method, token, tokenType, json)
        return any as? JSONObject ?: JSONObject()
    }

    private suspend fun hawkAny(
        url: URL,
        method: String,
        token: ByteArray,
        tokenType: String,
        json: JSONObject?,
    ): Any {
        val material = FxACrypto.tokenMaterial(token, tokenType)
        val body: ByteArray = json?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val hash = if (body.isEmpty()) null else HawkAuth.payloadHash(body)
        val authorization = HawkAuth.authorization(
            method = method,
            url = url,
            id = material.first,
            key = material.second,
            payloadHash = hash,
        )
        val response = sendRequest(url, method, body, mapOf("Authorization" to authorization))
        return parseBodyOrThrow(response) ?: JSONObject()
    }

    private suspend fun tokenServerCreds(
        authorization: String,
        kB: ByteArray,
        keysChangedAt: Long,
    ): TokenServerCreds {
        val kid = "$keysChangedAt-" + base64url(FxACrypto.clientStateBytes(kB))
        val response = sendRequest(
            tokenServerURL,
            "GET",
            ByteArray(0),
            mapOf("Authorization" to authorization, "X-KeyID" to kid),
        )
        val text = String(response.body, Charsets.UTF_8)
        if (response.code !in 200..299) throw SyncError.Auth("Token server: ${text.take(300)}")
        val obj = runCatching { JSONObject(text) }.getOrElse { throw SyncError.Auth("Incomplete token server response.") }
        val uid = when (val u = obj.opt("uid")) {
            is String -> u
            is Int -> u.toString()
            is Long -> u.toString()
            else -> null
        }
        val endpoint = obj.optString("api_endpoint").ifEmpty { null }
        val id = obj.optString("id").ifEmpty { null }
        val key = obj.optString("key").ifEmpty { null }
        val duration = obj.optInt("duration", Int.MIN_VALUE)
        if (uid == null || endpoint == null || id == null || key == null || duration == Int.MIN_VALUE) {
            throw SyncError.Auth("Incomplete token server response.")
        }
        return TokenServerCreds(
            uid = uid,
            apiEndpoint = endpoint,
            hawkID = id,
            hawkKey = key.toByteArray(Charsets.UTF_8),
            expiresAtMillis = System.currentTimeMillis() + (duration - 60) * 1000L,
        )
    }

    private suspend fun fetchKB(keyFetchToken: String, unwrapBKey: ByteArray): ByteArray {
        val token = FxACrypto.unhex(keyFetchToken)
        val material = FxACrypto.tokenMaterial(token, "keyFetchToken")
        // An unconfirmed account answers errno 104. While the user is still
        // waiting for the confirmation email, poll instead of failing —
        // the keys become fetchable the moment they confirm.
        var lastError: Exception = SyncError.Auth("keys missing")
        repeat(40) { attempt ->
            try {
                val json = getJSON("/account/keys", bearer = "fxk_${material.first}")
                val bundleHex = json.optString("bundle").ifEmpty { throw SyncError.Auth("keys missing") }
                val bundle = FxACrypto.unhex(bundleHex)
                val raw = FxACrypto.unbundle(material.third, "account/keys", bundle)
                if (raw.size < 64) throw SyncError.Crypto("keys short")
                return FxACrypto.xor(raw.copyOfRange(32, 64), unwrapBKey)
            } catch (e: SyncError.Auth) {
                lastError = e
                val message = e.message ?: ""
                if (!message.contains("Unconfirmed account") && !message.contains("\"errno\":104")) throw e
                if (attempt < 39) kotlinx.coroutines.delay(3_000)
            }
        }
        throw lastError
    }

    /** Fallback BrowserID assertion when the OAuth path fails. */
    private suspend fun browserIDAssertion(sessionToken: String, audience: String): String {
        val pair = RSAKeyPair.generate()
        val token = FxACrypto.unhex(sessionToken)
        val json = hawkJSON(
            url = URL("$authBase/certificate/sign"),
            method = "POST",
            token = token,
            tokenType = "sessionToken",
            json = JSONObject()
                .put(
                    "publicKey",
                    JSONObject()
                        .put("algorithm", "RS")
                        .put("n", pair.modulusB64URL)
                        .put("e", pair.exponentB64URL),
                )
                .put("duration", 60_000),
        )
        val cert = json.optString("cert").ifEmpty { throw SyncError.Auth("missing certificate") }
        val assertion = pair.signAssertion(audience, expiresInSecs = 60)
        return "$cert~$assertion"
    }

    private suspend fun getJSON(path: String, bearer: String): JSONObject {
        val response = sendRequest(URL(authBase + path), "GET", ByteArray(0), mapOf("Authorization" to "Bearer $bearer"))
        val any = parseBodyOrThrow(response)
        return any as? JSONObject ?: JSONObject()
    }

    // MARK: - HTTP plumbing

    internal class RawResponse(val code: Int, val body: ByteArray)

    private suspend fun sendRequest(url: URL, method: String, body: ByteArray, headers: Map<String, String>): RawResponse =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val requestHeaders = linkedMapOf<String, String>()
            requestHeaders["User-Agent"] = userAgent
            if (body.isNotEmpty()) requestHeaders["Content-Type"] = "application/json"
            requestHeaders["Accept"] = "application/json"
            requestHeaders.putAll(headers)
            val response = transport.execute(
                SyncHttpRequest(
                    method = method,
                    url = url,
                    headers = requestHeaders,
                    // GET stays body-less (no `doOutput`); every write call
                    // carries a JSON body, matching the previous inline logic.
                    body = if (method == "GET") null else body,
                ),
            )
            RawResponse(response.statusCode, response.body)
        }

    internal fun parseBodyOrThrow(response: RawResponse): Any? {  // nullable: callers coerce

        val text = String(response.body, Charsets.UTF_8)
        val parsed: Any? = runCatching { org.json.JSONTokener(text).nextValue() }.getOrNull()
        if (response.code !in 200..299) {
            val obj = parsed as? JSONObject
            // errno 103 = two-step authentication enabled; the caller must
            // complete TOTP (same as iOS). Throwing here keeps the rethrow in
            // syncCredentials and the FriendlyError mapping reachable.
            if (obj?.optInt("errno", -1) == 103) throw SyncError.TotpRequired
            val message = obj?.optString("message")?.takeIf { it.isNotEmpty() }
                ?: obj?.optString("error")?.takeIf { it.isNotEmpty() }
                ?: text.ifEmpty { "HTTP ${response.code}" }
            throw SyncError.Auth(message, errno = obj?.optInt("errno", -1)?.takeIf { it >= 0 })
        }
        return parsed ?: JSONObject()
    }

    companion object {
        const val authBase = "https://api.accounts.firefox.com/v1"
        val tokenServerURL = URL("https://token.services.mozilla.com/1.0/sync/1.5")
        const val TOKEN_SERVER_AUDIENCE = "https://token.services.mozilla.com"

        /** Firefox desktop client_id, fxa-credentials grant, oldsync scope. */
        const val OAUTH_CLIENT_ID = "5882386c6d801776"
        const val OLD_SYNC_SCOPE = "https://identity.mozilla.com/apps/oldsync"

        /** Spec allows `Z-Sync/1.0 (Android)`; Hawk math is unaffected. */
        const val userAgent = "Z-Sync/1.0 (Android)"
    }
}

/** Port of the Swift `RSAKeyPair` helper used for the BrowserID fallback. */
internal class RSAKeyPair private constructor(
    private val privateKey: java.security.PrivateKey,
    val modulusB64URL: String,
    val exponentB64URL: String,
) {
    fun signAssertion(audience: String, expiresInSecs: Long): String {
        val header = base64url(JSONObject().put("alg", "RS256").toString().toByteArray(Charsets.UTF_8))
        val expMs = System.currentTimeMillis() + expiresInSecs * 1000
        val payloadJson = JSONObject().put("aud", audience).put("exp", expMs)
        val payload = base64url(payloadJson.toString().toByteArray(Charsets.UTF_8))
        val signingInput = "$header.$payload".toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(privateKey)
            update(signingInput)
            sign()
        }
        return "$header.$payload.${base64url(signature)}"
    }

    companion object {
        fun generate(): RSAKeyPair {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, SecureRandom())
            val pair = generator.generateKeyPair()
            val pub = pair.public as RSAPublicKey

            // BigInteger.toByteArray may include a leading zero for the sign bit;
            // strip it so the value matches what the server expects.
            var modulusBytes = pub.modulus.toByteArray()
            if (modulusBytes.size > 1 && modulusBytes.first() == 0.toByte()) {
                modulusBytes = modulusBytes.copyOfRange(1, modulusBytes.size)
            }
            var exponentBytes = pub.publicExponent.toByteArray()
            if (exponentBytes.size > 1 && exponentBytes.first() == 0.toByte()) {
                exponentBytes = exponentBytes.copyOfRange(1, exponentBytes.size)
            }
            return RSAKeyPair(
                privateKey = pair.private,
                modulusB64URL = base64url(modulusBytes),
                exponentB64URL = base64url(exponentBytes),
            )
        }

        internal fun bigIntegerFrom(bytes: ByteArray): BigInteger = BigInteger(1, bytes)
    }
}

internal fun base64url(data: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(data)
