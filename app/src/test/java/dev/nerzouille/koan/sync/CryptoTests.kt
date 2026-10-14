// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.sync.FxACrypto
import dev.nerzouille.koan.sync.HawkAuth
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncCrypto
import dev.nerzouille.koan.sync.SyncError
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL
import java.util.Base64

/**
 * Crypto and Hawk known-answer vectors, driven by the golden fixtures in
 * `shared/contract/fixtures/` (see SPEC.md §4–§5). Round-trip, hex and
 * error-string tests keep their literals.
 */
class CryptoTests {
    private fun keys() = SyncCrypto.KeyBundle(
        encryptionKey = ByteArray(32) { 7 },
        hmacKey = ByteArray(32) { 9 },
    )

    @Test
    fun syncEnvelopeRoundTrip() {
        val original = """{"hello":"zen"}""".toByteArray()
        val payload = SyncCrypto.encryptBSO(original, keys())
        val back = SyncCrypto.decryptBSO(payloadJSON = payload, keys = keys())
        assertEquals(original.toList(), back.toList())
    }

    /** Known AES vector (crypto-bso-envelope-valid): decrypt must reproduce the plaintext. */
    @Test
    fun decryptKnownAESVector() {
        val fixture = FixtureLoader.json("crypto-bso-envelope-valid")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val keyBundle = SyncCrypto.KeyBundle(
            encryptionKey = FxACrypto.unhex(input.getString("encryptionKeyHex")),
            hmacKey = FxACrypto.unhex(input.getString("hmacKeyHex")),
        )
        val envelope = input.getJSONObject("envelope")
        val plain = SyncCrypto.decryptBSO(envelope.toString(), keyBundle)
        assertEquals(expect.getString("plaintextUtf8"), String(plain))
        assertEquals(expect.getString("plaintextHex"), FxACrypto.hex(plain))
    }

    @Test
    fun tamperedHMACFails() {
        val original = "zen".toByteArray()
        val payload = SyncCrypto.encryptBSO(original, keys())
        val tampered = payload.replace("\"hmac\":\"", "\"hmac\":\"ff")
        try {
            SyncCrypto.decryptBSO(tampered, keys())
            fail("expected hmac mismatch")
        } catch (e: SyncError.Crypto) {
            assertEquals("bso hmac mismatch", e.message)
        }
    }

    /** Valid HMAC, but a non-16-byte IV fails geometry validation (SPEC §4). */
    @Test
    fun shortIVFailsGeometryCheck() {
        val payload = SyncCrypto.encryptBSO("zen".toByteArray(), keys())
        val env = JSONObject(payload)
        // The HMAC covers only the ciphertext string, so a swapped IV stays valid.
        env.put("IV", Base64.getEncoder().encodeToString(ByteArray(8)))
        try {
            SyncCrypto.decryptBSO(env.toString(), keys())
            fail("expected bso iv length")
        } catch (e: SyncError.Crypto) {
            assertEquals("bso iv length", e.message)
        }
    }

    /** Valid HMAC over a ciphertext that is not block-aligned fails geometry (SPEC §4). */
    @Test
    fun unalignedCiphertextFailsGeometryCheck() {
        val badCiphertext = Base64.getEncoder().encodeToString(ByteArray(24))
        val env = JSONObject()
            .put("ciphertext", badCiphertext)
            .put("IV", Base64.getEncoder().encodeToString(ByteArray(16)))
            .put(
                "hmac",
                FxACrypto.hex(FxACrypto.hmacSHA256(keys().hmacKey, badCiphertext.toByteArray(Charsets.UTF_8))),
            )
        try {
            SyncCrypto.decryptBSO(env.toString(), keys())
            fail("expected bso ciphertext length")
        } catch (e: SyncError.Crypto) {
            assertEquals("bso ciphertext length", e.message)
        }
    }

    /** BSO ids percent-encode to the RFC 3986 unreserved set (bso-ids-percent-encoding). */
    @Test
    fun bracedBSOIdIsPercentEncoded() {
        val cases = FixtureLoader.cases("bso-ids-percent-encoding")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            assertEquals(
                case.getJSONObject("expect").getString("encoded"),
                SyncClient.encodedBSOId(case.getJSONObject("input").getString("id")),
            )
        }
    }

    /** Hawk signs the verbatim request-line resource, not a decoded path. */
    @Test
    fun hawkSignsExplicitResourceVerbatim() {
        val url = URL("https://sync.example.com/1.5/1/storage/spaces/%7Babc%7D")
        val a = HawkAuth.authorization(
            method = "PUT",
            url = url,
            id = "id",
            key = ByteArray(32) { 1 },
            payloadHash = null,
            resource = "/1.5/1/storage/spaces/%7Babc%7D",
            fixedTimestamp = 1_700_000_000L,
            fixedNonce = "nonce",
        )
        val expectedMAC = HawkAuth.macFor(
            normalized = listOf(
                "hawk.1.header", "1700000000", "nonce", "PUT",
                "/1.5/1/storage/spaces/%7Babc%7D", "sync.example.com", "443", "", "", "",
            ).joinToString("\n"),
            key = ByteArray(32) { 1 },
        )
        assertTrue(a.contains("mac=\"$expectedMAC\""))
        // Unlike Swift's URL.path (which decodes escapes), java.net.URL keeps
        // them, so the derived resource matches the explicit one on Android.
        // Call sites still pass `resource` explicitly, mirroring Swift.
        val b = HawkAuth.authorization(
            method = "PUT",
            url = url,
            id = "id",
            key = ByteArray(32) { 1 },
            payloadHash = null,
            fixedTimestamp = 1_700_000_000L,
            fixedNonce = "nonce",
        )
        assertEquals(expectedMAC, b.substringAfter("mac=\"").substringBefore("\""))
    }

    /** hawk-authorization-resource: exact MAC bytes and authorization header. */
    @Test
    fun hawkMACMatchesReferenceVector() {
        val fixture = FixtureLoader.json("hawk-authorization-resource")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val key = FxACrypto.unhex(input.getString("keyHex"))

        val lines = expect.getJSONArray("normalizedLines")
        val joined = (0 until lines.length()).joinToString("\n") { lines.getString(it) }
        assertEquals(expect.getString("normalized"), joined)
        assertEquals(expect.getString("macBase64"), HawkAuth.macFor(joined, key))

        val authorization = HawkAuth.authorization(
            method = input.getString("method"),
            url = URL(input.getString("url")),
            id = input.getString("id"),
            key = key,
            payloadHash = if (input.isNull("payloadHash")) null else input.getString("payloadHash"),
            resource = input.getString("resource"),
            fixedTimestamp = input.getLong("fixedTimestamp"),
            fixedNonce = input.getString("fixedNonce"),
        )
        assertEquals(expect.getString("authorization"), authorization)
    }

    /** hawk-payload-hash: base64(SHA-256) over the normalized payload preimage. */
    @Test
    fun hawkPayloadHashMatchesReferenceVector() {
        val fixture = FixtureLoader.json("hawk-payload-hash")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val body = input.getString("bodyUtf8").toByteArray(Charsets.UTF_8)
        assertEquals(
            expect.getString("hashBase64"),
            HawkAuth.payloadHash(body, input.getString("contentType")),
        )
        // Independent check of the preimage structure from the fixture.
        val preimage = "hawk.1.payload\n" + expect.getString("normalizedType") +
            "\n" + input.getString("bodyUtf8") + "\n"
        assertEquals(expect.getString("preimageUtf8"), preimage)
        assertEquals(
            expect.getString("hashBase64"),
            Base64.getEncoder().encodeToString(FxACrypto.sha256(preimage.toByteArray(Charsets.UTF_8))),
        )
    }

    /** RFC 5869 test case 1 (crypto-hkdf-rfc5869-case1). */
    @Test
    fun hkdfMatchesRFC5869TestCase1() {
        val fixture = FixtureLoader.json("crypto-hkdf-rfc5869-case1")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val okm = FxACrypto.hkdf(
            secret = FxACrypto.unhex(input.getString("ikmHex")),
            info = FxACrypto.unhex(input.getString("infoHex")),
            length = input.getInt("length"),
            salt = FxACrypto.unhex(input.getString("saltHex")),
        )
        assertEquals(expect.getString("okmHex"), FxACrypto.hex(okm))
    }

    @Test
    fun hexRoundTrip() {
        val data = byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte(), 0x00, 0x01)
        assertEquals("deadbeef0001", FxACrypto.hex(data))
        assertTrue(FxACrypto.unhex("DEADBEEF0001").contentEquals(data))
        try {
            FxACrypto.unhex("abc")
            fail("expected invalid hex")
        } catch (e: SyncError.Crypto) {
            assertEquals(SyncError.CRYPTO_INVALID_HEX, e.message)
        }
    }

    /** PICL unbundle of account/keys (crypto-unbundle-account-keys). */
    @Test
    fun unbundleRoundTripWithReferenceVector() {
        val fixture = FixtureLoader.json("crypto-unbundle-account-keys")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val plain = FxACrypto.unbundle(
            FxACrypto.unhex(input.getString("bundleKeyHex")),
            input.getString("namespace"),
            FxACrypto.unhex(input.getString("payloadHex")),
        )
        assertEquals(expect.getString("plaintextUtf8"), String(plain))
    }

    /** clientStateBytes = SHA-256(kB)[0..16] (crypto-client-state-bytes-kb-zero). */
    @Test
    fun clientStateBytesVector() {
        val expect = FixtureLoader.json("crypto-client-state-bytes-kb-zero").getJSONObject("expect")
        val state = FxACrypto.clientStateBytes(ByteArray(32))
        assertEquals(expect.getString("stateHex"), FxACrypto.hex(state))
        assertEquals(
            expect.getString("stateBase64Url"),
            Base64.getUrlEncoder().withoutPadding().encodeToString(state),
        )
    }

    /** 96-byte token material split (crypto-token-material-session-token). */
    @Test
    fun tokenMaterialVector() {
        val fixture = FixtureLoader.json("crypto-token-material-session-token")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val (idHex, authKey, bundleKey) =
            FxACrypto.tokenMaterial(FxACrypto.unhex(input.getString("tokenHex")), input.getString("type"))
        assertEquals(expect.getString("idHex"), idHex)
        assertEquals(expect.getString("authKeyHex"), FxACrypto.hex(authKey))
        assertEquals(expect.getString("bundleKeyHex"), FxACrypto.hex(bundleKey))
    }

    /** oldsync key bundle from zero kB; empty salt == 32 zero bytes (SPEC §4). */
    @Test
    fun syncKeyBundleFromKBVector() {
        val fixture = FixtureLoader.json("crypto-sync-key-bundle-kb-zero")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        val kB = FxACrypto.unhex(input.getString("kbHex"))
        // Direct RFC derivation with the fixture's info and salt.
        val material = FxACrypto.hkdf(
            kB,
            input.getString("infoUtf8").toByteArray(Charsets.UTF_8),
            input.getInt("length"),
            salt = FxACrypto.unhex(input.getString("saltHex")),
        )
        assertEquals(expect.getString("encryptionKeyHex"), FxACrypto.hex(material.copyOfRange(0, 32)))
        assertEquals(expect.getString("hmacKeyHex"), FxACrypto.hex(material.copyOfRange(32, 64)))
        // The app entry point must agree exactly.
        val bundle = SyncCrypto.syncKeyBundle(kB)
        assertEquals(expect.getString("encryptionKeyHex"), FxACrypto.hex(bundle.encryptionKey))
        assertEquals(expect.getString("hmacKeyHex"), FxACrypto.hex(bundle.hmacKey))
    }

    @Test
    fun xorLengthMismatchThrows() {
        try {
            FxACrypto.xor(ByteArray(4), ByteArray(5))
            fail("expected mismatch")
        } catch (e: SyncError.Crypto) {
            assertNotNull(e.message)
        }
    }
}
