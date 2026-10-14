// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import java.net.URL
import java.security.SecureRandom
import java.util.Base64

/**
 * Port of `Shared/HawkAuth.swift`. Signs the verbatim request-line resource
 * (percent-encoding preserved), exactly like the Swift implementation.
 */
object HawkAuth {
    private val random = SecureRandom()

    fun authorization(
        method: String,
        url: URL,
        id: String,
        key: ByteArray,
        payloadHash: String? = null,
        resource: String? = null,
        fixedTimestamp: Long? = null,
        fixedNonce: String? = null,
    ): String {
        val ts = fixedTimestamp ?: (System.currentTimeMillis() / 1000)
        val nonce = fixedNonce ?: randomNonce()
        val host = url.host
        val port = if (url.port != -1) url.port else if (url.protocol == "https") 443 else 80
        // Swift's `URL.path` decodes percent-escapes, but Hawk must sign the
        // exact encoded resource the request line carries. Callers that build
        // paths with percent-encoded segments pass `resource` explicitly.
        //
        // On Android, `URL.path` keeps escapes as written; to stay behaviorally
        // identical with the Swift call sites we still prefer an explicit
        // `resource`, and otherwise use the path + query verbatim.
        val path: String = if (resource != null) {
            if (resource.startsWith("/")) resource else "/$resource"
        } else {
            val p = url.path.ifEmpty { "/" }
            val q = url.query
            if (!q.isNullOrEmpty()) "$p?$q" else p
        }
        val hash = payloadHash ?: ""
        val ext = ""
        val normalized = listOf(
            "hawk.1.header",
            ts.toString(),
            nonce,
            method.uppercase(),
            path,
            host.lowercase(),
            port.toString(),
            hash,
            ext,
            "",
        ).joinToString("\n")
        val macB64 = macFor(normalized, key)
        val parts = mutableListOf(
            "id=\"$id\"",
            "ts=\"$ts\"",
            "nonce=\"$nonce\"",
            "mac=\"$macB64\"",
        )
        if (hash.isNotEmpty()) parts.add("hash=\"$hash\"")
        return "Hawk " + parts.joinToString(", ")
    }

    fun macFor(normalized: String, key: ByteArray): String =
        Base64.getEncoder().encodeToString(FxACrypto.hmacSHA256(key, normalized.toByteArray(Charsets.UTF_8)))

    fun payloadHash(body: ByteArray, contentType: String = "application/json"): String {
        val normalizedType = contentType.split(";").firstOrNull()?.trim() ?: contentType
        val data = "hawk.1.payload\n$normalizedType\n".toByteArray(Charsets.UTF_8) +
            body + byteArrayOf(0x0A)
        return Base64.getEncoder().encodeToString(FxACrypto.sha256(data))
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
            .replace("+", "")
            .replace("/", "")
            .replace("=", "")
    }
}
