package dev.nerzouille.koan.sync

import org.json.JSONObject
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The Sync key as an OAuth flow delivers it (QR pairing): never kB itself, but the oldsync
 * "scoped key", encrypted to an ephemeral P-256 key of ours (JWE, ECDH-ES + A256GCM). The scoped
 * key is exactly what a password sign-in derives from kB ([legacySyncKey]), so the sync layer
 * works the same with either.
 */
object ScopedKeys {
    /** The oldsync key: 64 bytes (encryption half, HMAC half) and its key id for the token server. */
    class SyncKey(val key: ByteArray, val kid: String) {
        fun bundle() = SyncCrypto.KeyBundle(key.copyOfRange(0, 32), key.copyOfRange(32, 64))
        override fun toString() = "SyncKey(kid=<redacted>, key=<redacted>)"
    }

    /** What a password sign-in has: oldsync key = HKDF(kB, "…/oldsync", 64), kid = keysChangedAt-b64url(sha256(kB)[0:16]). */
    fun legacySyncKey(kB: ByteArray, keysChangedAt: Long): SyncKey {
        val bundle = SyncCrypto.syncKeyBundle(kB)
        val kid = "$keysChangedAt-" + base64url(FxACrypto.clientStateBytes(kB))
        return SyncKey(bundle.encryptionKey + bundle.hmacKey, kid)
    }

    /** A fresh ephemeral key pair for one flow. */
    fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    /** Our public key as the base64url-encoded JWK the authorization request carries (`keys_jwk`). */
    fun publicJwk(public: ECPublicKey): String {
        val jwk = JSONObject().put("crv", "P-256").put("kty", "EC")
            .put("x", base64url(coordinate(public.w.affineX)))
            .put("y", base64url(coordinate(public.w.affineY)))
        return base64url(jwk.toString().toByteArray(Charsets.UTF_8))
    }

    /** A P-256 private key from its JWK parts (tests: Mozilla's fixed vectors). */
    fun privateKey(d: ByteArray): ECPrivateKey =
        KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger(1, d), p256())) as ECPrivateKey

    /** Decrypts a compact JWE (ECDH-ES direct key agreement, A256GCM) with [privateKey]. */
    fun decryptJwe(jwe: String, privateKey: ECPrivateKey): ByteArray {
        val parts = jwe.split('.')
        if (parts.size != 5 || parts[1].isNotEmpty()) throw SyncError.Crypto("keys_jwe: not a direct-agreement JWE")
        val header = JSONObject(String(b64(parts[0]), Charsets.UTF_8))
        if (header.optString("alg") != "ECDH-ES" || header.optString("enc") != "A256GCM") {
            throw SyncError.Crypto("keys_jwe: unexpected algorithm")
        }
        val epk = header.getJSONObject("epk")
        if (epk.optString("crv") != "P-256") throw SyncError.Crypto("keys_jwe: unexpected curve")
        val peer = KeyFactory.getInstance("EC").generatePublic(
            ECPublicKeySpec(ECPoint(BigInteger(1, b64(epk.getString("x"))), BigInteger(1, b64(epk.getString("y")))), p256()),
        )
        val shared = KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(peer, true)
            generateSecret()
        }
        val cek = concatKdf(shared, "A256GCM", header.optString("apu"), header.optString("apv"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, b64(parts[2])))
        // The AAD is the protected header exactly as sent (base64url text).
        cipher.updateAAD(parts[0].toByteArray(Charsets.US_ASCII))
        return runCatching { cipher.doFinal(b64(parts[3]) + b64(parts[4])) }
            .getOrElse { throw SyncError.Crypto("keys_jwe: decryption failed") }
    }

    /** The oldsync key out of the decrypted `keys_jwe` JSON. */
    fun oldSyncKey(keysJson: ByteArray): SyncKey {
        val entry = JSONObject(String(keysJson, Charsets.UTF_8)).optJSONObject(FxAClient.OLD_SYNC_SCOPE)
            ?: throw SyncError.Crypto("keys_jwe: no oldsync key")
        val key = b64(entry.getString("k"))
        val kid = entry.optString("kid")
        if (key.size != 64 || !kid.contains('-')) throw SyncError.Crypto("keys_jwe: malformed oldsync key")
        return SyncKey(key, kid)
    }

    /** RFC 7518 §4.6.2 Concat KDF, one SHA-256 round (256-bit key). */
    private fun concatKdf(z: ByteArray, alg: String, apu: String, apv: String): ByteArray {
        fun lengthPrefixed(bytes: ByteArray) = ByteBuffer.allocate(4).putInt(bytes.size).array() + bytes
        val otherInfo = lengthPrefixed(alg.toByteArray(Charsets.US_ASCII)) +
            lengthPrefixed(if (apu.isEmpty()) ByteArray(0) else b64(apu)) +
            lengthPrefixed(if (apv.isEmpty()) ByteArray(0) else b64(apv)) +
            ByteBuffer.allocate(4).putInt(256).array()
        return FxACrypto.sha256(byteArrayOf(0, 0, 0, 1) + z + otherInfo)
    }

    private fun p256(): ECParameterSpec =
        AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec("secp256r1")) }.getParameterSpec(ECParameterSpec::class.java)

    /** A coordinate as exactly 32 big-endian bytes. */
    private fun coordinate(v: BigInteger): ByteArray {
        val raw = v.toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size > 32 -> raw.copyOfRange(raw.size - 32, raw.size)
            else -> ByteArray(32 - raw.size) + raw
        }
    }

    private fun b64(s: String): ByteArray = Base64.getUrlDecoder().decode(s.trimEnd('='))
}
