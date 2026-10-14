// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Port of `Shared/SyncCrypto.swift` (AES-256-CBC + PKCS7, HMAC over the base64 string). */
object SyncCrypto {
    class KeyBundle(val encryptionKey: ByteArray, val hmacKey: ByteArray)

    fun syncKeyBundle(kB: ByteArray): KeyBundle {
        val info = "identity.mozilla.com/picl/v1/oldsync".toByteArray(Charsets.UTF_8)
        val material = FxACrypto.hkdf(kB, info, 64)
        return KeyBundle(
            encryptionKey = material.copyOfRange(0, 32),
            hmacKey = material.copyOfRange(32, 64),
        )
    }

    fun decryptBSO(payloadJSON: String, keys: KeyBundle): ByteArray {
        val env = JSONObject(payloadJSON)
        val ciphertextB64 = env.getString("ciphertext")
        val ivB64 = env.getString("IV")
        val expectedHmac = try {
            FxACrypto.unhex(env.getString("hmac"))
        } catch (_: SyncError.Crypto) {
            // A non-hex tag can never match; report it as a plain mismatch.
            throw SyncError.Crypto("bso hmac mismatch")
        }
        val actualHmac = FxACrypto.hmacSHA256(keys.hmacKey, ciphertextB64.toByteArray(Charsets.UTF_8))
        // Constant-time comparison so the tag check cannot leak timing.
        if (!MessageDigest.isEqual(actualHmac, expectedHmac)) throw SyncError.Crypto("bso hmac mismatch")
        val ciphertext = b64decode(ciphertextB64)
            ?: throw SyncError.Crypto("bso base64")
        val iv = b64decode(ivB64)
            ?: throw SyncError.Crypto("bso base64")
        // Envelope geometry is validated before decryption (SPEC §4).
        if (iv.size != 16) throw SyncError.Crypto("bso iv length")
        if (ciphertext.isEmpty() || ciphertext.size % 16 != 0) throw SyncError.Crypto("bso ciphertext length")
        return try {
            aes256CBC(ciphertext, keys.encryptionKey, iv, decrypt = true)
        } catch (_: GeneralSecurityException) {
            throw SyncError.Crypto("bso cipher")
        }
    }

    fun encryptBSO(plaintext: ByteArray, keys: KeyBundle): String {
        val iv = ByteArray(16).also { random.nextBytes(it) }
        val cipher = try {
            aes256CBC(plaintext, keys.encryptionKey, iv, decrypt = false)
        } catch (_: GeneralSecurityException) {
            throw SyncError.Crypto("bso cipher")
        }
        val ciphertextB64 = Base64.getEncoder().encodeToString(cipher)
        val hmac = FxACrypto.hex(
            FxACrypto.hmacSHA256(keys.hmacKey, ciphertextB64.toByteArray(Charsets.UTF_8))
        )
        val obj = JSONObject()
        obj.put("ciphertext", ciphertextB64)
        obj.put("IV", Base64.getEncoder().encodeToString(iv))
        obj.put("hmac", hmac)
        return obj.toString()
    }

    private fun aes256CBC(data: ByteArray, key: ByteArray, iv: ByteArray, decrypt: Boolean): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            if (decrypt) Cipher.DECRYPT_MODE else Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            IvParameterSpec(iv),
        )
        return cipher.doFinal(data)
    }

    internal fun b64decode(s: String): ByteArray? = try {
        Base64.getDecoder().decode(s)
    } catch (_: IllegalArgumentException) {
        try {
            Base64.getMimeDecoder().decode(s)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private val random = SecureRandom()
}
