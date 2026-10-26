package dev.nerzouille.koan.sync

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ScopedKeysTest {
    private fun b64url(s: String) = Base64.getUrlDecoder().decode(s)

    /**
     * Mozilla's own vector (application-services, fxa-client scoped_keys.rs `test_flow`): a fixed
     * P-256 key and the keys_jwe the server produced for it.
     */
    @Test
    fun decryptsMozillasKeysJwe() {
        val d = b64url("UayD4kn_4QHvLvLLSSaANfDUp9AcQndQu_TohQKoyn8")
        val jwe = "eyJhbGciOiJFQ0RILUVTIiwia2lkIjoiNFBKTTl5dGVGeUtsb21ILWd2UUtyWGZ0a0N3ak9HNHRfTmpYVXhLM1VqSSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IlB3eG9Na1RjSVZ2TFlKWU4wM2R0Y3o2TEJrR0FHaU1hZWlNQ3lTZXEzb2MiLCJ5IjoiLUYtTllRRDZwNUdSQ2ZoYm1hN3NvNkhxdExhVlNub012S0pFcjFBeWlaSSJ9LCJlbmMiOiJBMjU2R0NNIn0..b9FPhjjpmAmo_rP8.ur9jTry21Y2trvtcanSFmAtiRfF6s6qqyg6ruRal7PCwa7PxDzAuMN6DZW5BiK8UREOH08-FyRcIgdDOm5Zq8KwVAn56PGfcH30aNDGQNkA_mpfjx5Tj2z8kI6ryLWew4PGZb-PsL1g-_eyXhktq7dAhetjNYttKwSREWQFokv7N3nJGpukBqnwL1ost-MjDXlINZLVJKAiMHDcu-q7Epitwid2c2JVGOSCJjbZ4-zbxVmZ4o9xhFb2lbvdiaMygH6bPlrjEK99uT6XKtaIZmyDwftbD6G3x4On-CqA2TNL6ILRaJMtmyX--ctL0IrngUIHg_F0Wz94v.zBD8NACkUcZTPLH0tceGnA"
        val plain = ScopedKeys.decryptJwe(jwe, ScopedKeys.privateKey(d))
        assertEquals(
            "{\"https://identity.mozilla.com/apps/oldsync\":{\"kty\":\"oct\",\"scope\":\"https://identity.mozilla.com/apps/oldsync\",\"k\":\"8ek1VNk4sjrNP0DhGC4crzQtwmpoR64zHuFMHb4Tw-exR70Z2SSIfMSrJDTLEZid9lD05-hbA3n2Q4Esjlu1tA\",\"kid\":\"1526414944666-zgTjf5oXmPmBjxwXWFsDWg\"}}",
            String(plain, Charsets.UTF_8),
        )
        val key = ScopedKeys.oldSyncKey(plain)
        assertEquals("1526414944666-zgTjf5oXmPmBjxwXWFsDWg", key.kid)
        assertArrayEquals(b64url("8ek1VNk4sjrNP0DhGC4crzQtwmpoR64zHuFMHb4Tw-exR70Z2SSIfMSrJDTLEZid9lD05-hbA3n2Q4Esjlu1tA"), key.key)
    }

    /**
     * Firefox's vector (services/fxaccounts/tests/xpcshell/test_keys.js,
     * test_derive_legacy_sync_key_test_vector): the key a QR sign-in delivers is what Koan derives
     * from kB after a password sign-in, byte for byte, key id included.
     */
    @Test
    fun passwordAndQrSignInGiveTheSameSyncKey() {
        val kB = FxACrypto.unhex("eaf9570b7219a4187d3d6bf3cec2770c2e0719b7cc0dfbb38243d6f1881675e9")
        val legacy = ScopedKeys.legacySyncKey(kB, 1510726317123)
        assertEquals("1510726317123-IqQv4onc7VcVE1kTQkyyOw", legacy.kid)
        assertArrayEquals(b64url("DW_ll5GwX6SJ5GPqJVAuMUP2t6kDqhUulc2cbt26xbTcaKGQl-9l29FHAQ7kUiJETma4s9fIpEHrt909zgFang"), legacy.key)
        // …and the bundle the sync layer uses is the same either way.
        val fromKb = SyncCrypto.syncKeyBundle(kB)
        assertArrayEquals(fromKb.encryptionKey, legacy.bundle().encryptionKey)
        assertArrayEquals(fromKb.hmacKey, legacy.bundle().hmacKey)
    }

    @Test
    fun roundTripsThroughOurOwnKey() {
        val pair = ScopedKeys.newKeyPair()
        val payload = """{"https://identity.mozilla.com/apps/oldsync":{"kty":"oct","k":"${Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(64) { it.toByte() })}","kid":"1-abc"}}"""
        val jwe = encryptJwe(payload.toByteArray(), pair.public as ECPublicKey)
        val key = ScopedKeys.oldSyncKey(ScopedKeys.decryptJwe(jwe, pair.private as ECPrivateKey))
        assertArrayEquals(ByteArray(64) { it.toByte() }, key.key)
        assertEquals("1-abc", key.kid)
    }

    @Test(expected = SyncError.Crypto::class)
    fun wrongKeyFailsCleanly() {
        val jwe = encryptJwe("{}".toByteArray(), ScopedKeys.newKeyPair().public as ECPublicKey)
        ScopedKeys.decryptJwe(jwe, ScopedKeys.newKeyPair().private as ECPrivateKey)
    }

    @Test(expected = SyncError.Crypto::class)
    fun missingOldSyncKeyIsRejected() {
        ScopedKeys.oldSyncKey("""{"https://identity.mozilla.com/apps/other":{"k":"AA","kid":"1-a"}}""".toByteArray())
    }

    @Test
    fun publicJwkIsAP256Jwk() {
        val pair = ScopedKeys.newKeyPair()
        val jwk = JSONObject(String(b64url(ScopedKeys.publicJwk(pair.public as ECPublicKey))))
        assertEquals("EC", jwk.getString("kty"))
        assertEquals("P-256", jwk.getString("crv"))
        assertEquals(32, b64url(jwk.getString("x")).size)
        assertEquals(32, b64url(jwk.getString("y")).size)
    }

    @Test
    fun secretsDontPrint() {
        val key = ScopedKeys.legacySyncKey(ByteArray(32) { 9 }, 1)
        assertFalse(key.toString().contains(key.kid))
        assertTrue(key.toString().contains("redacted"))
    }

    companion object {
        /** What Mozilla's server does: ECDH-ES (direct) + A256GCM to [recipient]. */
        fun encryptJwe(plain: ByteArray, recipient: ECPublicKey): String {
            val enc = Base64.getUrlEncoder().withoutPadding()
            val eph = ScopedKeys.newKeyPair()
            val epub = eph.public as ECPublicKey
            fun coord(v: java.math.BigInteger) = v.toByteArray().let { if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it }
            val header = JSONObject().put("alg", "ECDH-ES").put("enc", "A256GCM")
                .put("epk", JSONObject().put("kty", "EC").put("crv", "P-256").put("x", enc.encodeToString(coord(epub.w.affineX))).put("y", enc.encodeToString(coord(epub.w.affineY))))
            val protected = enc.encodeToString(header.toString().toByteArray())
            val z = KeyAgreement.getInstance("ECDH").run { init(eph.private); doPhase(recipient, true); generateSecret() }
            fun lp(b: ByteArray) = ByteBuffer.allocate(4).putInt(b.size).array() + b
            val cek = FxACrypto.sha256(byteArrayOf(0, 0, 0, 1) + z + lp("A256GCM".toByteArray()) + lp(ByteArray(0)) + lp(ByteArray(0)) + ByteBuffer.allocate(4).putInt(256).array())
            val iv = ByteArray(12) { (it * 7).toByte() }
            val out = Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, iv))
                updateAAD(protected.toByteArray(Charsets.US_ASCII))
                doFinal(plain)
            }
            val ct = out.copyOfRange(0, out.size - 16)
            val tag = out.copyOfRange(out.size - 16, out.size)
            return "$protected..${enc.encodeToString(iv)}.${enc.encodeToString(ct)}.${enc.encodeToString(tag)}"
        }
    }
}
