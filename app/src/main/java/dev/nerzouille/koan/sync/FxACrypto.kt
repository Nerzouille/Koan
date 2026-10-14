// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Ports of `Shared/SyncError` cases; UI maps these to friendly strings. */
sealed class SyncError(message: String?) : Exception(message) {
    class Crypto(detail: String? = null) : SyncError(detail)
    /** Koan: [errno] = the FxA error number when the server sent one. */
    class Auth(detail: String? = null, val errno: Int? = null) : SyncError(detail)
    class Network(detail: String? = null) : SyncError(detail)

    /** Conditional write lost its race twice (SPEC §7.2); caller re-reads. */
    class Conflict(detail: String? = null) : SyncError(detail)
    object NotSignedIn : SyncError(null)
    object TotpRequired : SyncError(null)

    /** Koan: Mozilla accounts no longer accepts the session token: the user must sign in again. */
    object SessionExpired : SyncError("session expired")
    object StorageUnavailable : SyncError(null)

    companion object {
        const val CRYPTO_INVALID_HEX = "invalid hex"
    }
}

/** Ports of `Shared/FxACrypto.swift` — byte-for-byte identical outputs. */
object FxACrypto {
    const val NAMESPACE = "identity.mozilla.com/picl/v1/"

    fun hex(data: ByteArray): String = data.joinToString("") { "%02x".format(it) }

    fun unhex(string: String): ByteArray {
        val raw = string.trim()
        if (raw.length % 2 != 0) throw SyncError.Crypto(SyncError.CRYPTO_INVALID_HEX)
        val out = ByteArray(raw.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(raw[i * 2], 16)
            val lo = Character.digit(raw[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) throw SyncError.Crypto(SyncError.CRYPTO_INVALID_HEX)
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    fun hmacSHA256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    /**
     * HKDF-SHA256 per RFC 5869. The app always calls this with the default
     * empty salt, which behaves exactly like CryptoKit's `Data()` salt
     * (HMAC zero-pads short keys); tests additionally pass RFC vectors.
     */
    fun hkdf(secret: ByteArray, info: ByteArray, length: Int, salt: ByteArray = ByteArray(32)): ByteArray {
        val effectiveSalt = if (salt.isEmpty()) ByteArray(32) else salt
        val prk = hmacSHA256(effectiveSalt, secret)
        val okm = ByteArray(length)
        var t = ByteArray(0)
        var counter = 1
        var offset = 0
        while (offset < length) {
            t = hmacSHA256(prk, t + info + byteArrayOf(counter.toByte()))
            val n = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, n)
            offset += n
            counter++
        }
        return okm
    }

    fun namespacedHKDF(secret: ByteArray, name: String, length: Int): ByteArray =
        hkdf(secret, (NAMESPACE + name).toByteArray(Charsets.UTF_8), length)

    fun xor(a: ByteArray, b: ByteArray): ByteArray {
        if (a.size != b.size) throw SyncError.Crypto("xor length mismatch")
        return ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }

    /** HKDF-unbundle: derive [hmacKey | xorKey], verify trailing HMAC, XOR-decrypt. */
    fun unbundle(bundleKey: ByteArray, namespaceName: String, payload: ByteArray): ByteArray {
        if (payload.size < 32) throw SyncError.Crypto("bundle too short")
        val ciphertext = payload.copyOfRange(0, payload.size - 32)
        val expected = payload.copyOfRange(payload.size - 32, payload.size)
        val material = namespacedHKDF(bundleKey, namespaceName, 32 + ciphertext.size)
        val hmacKey = material.copyOfRange(0, 32)
        val xorKey = material.copyOfRange(32, material.size)
        val actual = hmacSHA256(hmacKey, ciphertext)
        // Constant-time comparison so the tag check cannot leak timing.
        if (!MessageDigest.isEqual(actual, expected)) throw SyncError.Crypto("bundle hmac mismatch")
        return xor(ciphertext, xorKey)
    }

    fun clientStateBytes(kB: ByteArray): ByteArray = sha256(kB).copyOfRange(0, 16)

    /** Returns (id as hex, authKey, bundleKey). */
    fun tokenMaterial(token: ByteArray, type: String): Triple<String, ByteArray, ByteArray> {
        val material = namespacedHKDF(token, type, 96)
        return Triple(
            hex(material.copyOfRange(0, 32)),
            material.copyOfRange(32, 64),
            material.copyOfRange(64, 96),
        )
    }
}
