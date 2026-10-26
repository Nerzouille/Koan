package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.FxACrypto
import dev.nerzouille.koan.sync.PairingFlow
import dev.nerzouille.koan.sync.ScopedKeys
import dev.nerzouille.koan.sync.ScopedKeysTest
import dev.nerzouille.koan.sync.SyncCrypto
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.SyncHttpRequest
import dev.nerzouille.koan.sync.SyncHttpResponse
import dev.nerzouille.koan.sync.SyncHttpTransport
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64
import javax.crypto.KeyGenerator

class AccountStoreTest {
    private class MemoryBlob : BlobStore {
        var value: String? = null
        override suspend fun read() = value
        override suspend fun write(value: String) { this.value = value }
        override suspend fun clear() { value = null }
    }

    /** Same AES-GCM box as production, with a plain JCE key instead of the Android Keystore. */
    private class TestBox : AesGcmBox({ key }) {
        var destroyed = false
        override fun destroy() { destroyed = true }

        companion object {
            val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        }
    }

    private val secretSession = "a1".repeat(32)
    private val secretKb = "b2".repeat(32)
    private val account = Account("noa@example.com", "uid-1", secretSession, secretKb)

    @Test
    fun savedAccountSurvivesANewProcess() = runTest {
        val blob = MemoryBlob()
        AccountStore(blob, TestBox()).save(account)

        val fresh = AccountStore(blob, TestBox())
        val loaded = fresh.load()!!
        assertEquals(account.email, loaded.email)
        assertEquals(secretSession, loaded.sessionTokenHex)
        assertEquals(secretKb, loaded.kBHex)
        assertEquals(AccountState.SignedIn(account.email), fresh.state.value)
    }

    @Test
    fun secretsNeverReachDiskOrLogsInClear() = runTest {
        val blob = MemoryBlob()
        AccountStore(blob, TestBox()).save(account)
        val onDisk = String(Base64.getDecoder().decode(blob.value!!), Charsets.ISO_8859_1)
        assertFalse(onDisk.contains(secretSession))
        assertFalse(onDisk.contains(secretKb))
        assertFalse(onDisk.contains(account.email))
        assertFalse(account.toString().contains(secretSession))
        assertFalse(account.toString().contains(secretKb))
    }

    @Test
    fun signOutForgetsEverything() = runTest {
        val blob = MemoryBlob()
        val box = TestBox()
        val store = AccountStore(blob, box)
        store.save(account)
        store.signOut()
        assertNull(blob.value)
        assertTrue(box.destroyed)
        assertNull(store.load())
        assertEquals(AccountState.SignedOut, store.state.value)
    }

    @Test
    fun unreadableBlobCountsAsSignedOut() = runTest {
        val blob = MemoryBlob().apply { value = Base64.getEncoder().encodeToString(ByteArray(40) { 7 }) }
        val store = AccountStore(blob, TestBox())
        assertNull(store.load())
        assertEquals(AccountState.SignedOut, store.state.value)
    }

    @Test(expected = SyncError.NotSignedIn::class)
    fun connectWithoutAccountFails() = runTest {
        AccountStore(MemoryBlob(), TestBox()).connect()
    }

    // MARK: - QR sign-in (OAuth): fake Mozilla servers, fake data only.

    /** Answers by path; records what was asked (never a real server). */
    private class FakeServer(val routes: (SyncHttpRequest) -> Pair<Int, String>) : SyncHttpTransport {
        val seen = mutableListOf<SyncHttpRequest>()
        override fun execute(request: SyncHttpRequest): SyncHttpResponse {
            seen += request
            val (code, body) = routes(request)
            return SyncHttpResponse(code, emptyMap(), body.toByteArray())
        }
        fun bodyOf(path: String) = seen.last { it.url.path.endsWith(path) }.body?.let { JSONObject(String(it)) }
    }

    private val fakeKb = ByteArray(32) { (it * 3).toByte() }
    private val pairLink = "https://accounts.firefox.com/pair#channel_id=chan&channel_key=a2V5"

    /** The keys_jwe Mozilla would send for [flow]: the oldsync key of [fakeKb]. */
    private fun keysJweFor(flow: PairingFlow): String {
        val key = ScopedKeys.legacySyncKey(fakeKb, 1700000000000)
        val json = JSONObject().put(
            "https://identity.mozilla.com/apps/oldsync",
            JSONObject().put("kty", "oct").put("k", Base64.getUrlEncoder().withoutPadding().encodeToString(key.key)).put("kid", key.kid),
        )
        return ScopedKeysTest.encryptJwe(json.toString().toByteArray(), flow.keyPair.public as ECPublicKey)
    }

    private fun authServer(flow: PairingFlow, refreshOk: Boolean = true) = FakeServer { req ->
        when {
            req.url.path.endsWith("/oauth/token") -> {
                val grant = JSONObject(String(req.body!!)).getString("grant_type")
                when {
                    grant == "authorization_code" -> 200 to JSONObject().put("access_token", "access-1").put("refresh_token", "refresh-secret")
                        .put("keys_jwe", keysJweFor(flow)).put("expires_in", 3600).toString()
                    refreshOk -> 200 to JSONObject().put("access_token", "access-2").put("expires_in", 3600).toString()
                    else -> 400 to JSONObject().put("errno", 110).put("message", "Invalid token").toString()
                }
            }
            req.url.host == "profile.accounts.firefox.com" -> 200 to JSONObject().put("email", "noa@example.com").put("uid", "uid-1").toString()
            req.url.path.endsWith("/oauth/destroy") -> 200 to "{}"
            req.url.host == "token.services.mozilla.com" -> 200 to JSONObject().put("uid", 42).put("api_endpoint", "https://sync.example.com/1.5/42")
                .put("id", "hawk-id").put("key", "hawk-key").put("duration", 3600).toString()
            else -> 404 to "{}"
        }
    }

    /** A storage server whose crypto/keys only opens with the Sync key of [fakeKb]. */
    private fun storageServer() = FakeServer { req ->
        if (req.url.path.endsWith("/storage/crypto/keys")) {
            val keys = JSONObject().put("default", JSONArray().put(Base64.getEncoder().encodeToString(ByteArray(32) { 1 })).put(Base64.getEncoder().encodeToString(ByteArray(32) { 2 })))
            val payload = SyncCrypto.encryptBSO(keys.toString().toByteArray(), SyncCrypto.syncKeyBundle(fakeKb))
            200 to JSONObject().put("id", "keys").put("payload", payload).toString()
        } else {
            404 to "{}"
        }
    }

    @Test
    fun qrSignInStoresTheRefreshTokenSealedAndSyncsWithIt() = runTest {
        val flow = PairingFlow(pairLink)
        val auth = authServer(flow)
        val storage = storageServer()
        val blob = MemoryBlob()
        val store = AccountStore(blob, TestBox(), auth, storage)

        assertNull(store.completePairing(flow, "code-1"))
        assertEquals(AccountState.SignedIn("noa@example.com"), store.state.value)
        assertEquals(flow.codeVerifier, auth.bodyOf("/oauth/token")!!.getString("code_verifier"))
        // Sealed on disk: neither the token nor the key in clear.
        val onDisk = String(Base64.getDecoder().decode(blob.value!!), Charsets.ISO_8859_1)
        assertFalse(onDisk.contains("refresh-secret"))
        assertFalse(store.load().toString().contains("refresh-secret"))

        // A new process: the stored token gives a fresh access token, the token server gets the
        // key id, and crypto/keys opens with the decrypted Sync key.
        val again = AccountStore(blob, TestBox(), auth, storage)
        again.connect()
        assertEquals("refresh_token", auth.bodyOf("/oauth/token")!!.getString("grant_type"))
        val tokenCall = auth.seen.last { it.url.host == "token.services.mozilla.com" }
        assertEquals("Bearer access-2", tokenCall.headers["Authorization"])
        assertEquals(ScopedKeys.legacySyncKey(fakeKb, 1700000000000).kid, tokenCall.headers["X-KeyID"])
    }

    @Test
    fun qrSignInChecksTheKeyAgainstThePasswordSignIn() = runTest {
        val flow = PairingFlow(pairLink)
        val blob = MemoryBlob()
        val store = AccountStore(blob, TestBox(), authServer(flow), storageServer())
        store.save(Account("noa@example.com", "uid-1", secretSession, FxACrypto.hex(fakeKb)))
        assertEquals(true, store.completePairing(flow, "code-1"))
        assertTrue(store.load()!!.oauth)
    }

    @Test
    fun aRevokedRefreshTokenMeansSignInAgain() = runTest {
        val flow = PairingFlow(pairLink)
        val blob = MemoryBlob()
        AccountStore(blob, TestBox(), authServer(flow), storageServer()).completePairing(flow, "code-1")
        val store = AccountStore(blob, TestBox(), authServer(flow, refreshOk = false), storageServer())
        try {
            store.connect()
            fail("expected SessionExpired")
        } catch (e: SyncError.SessionExpired) {
            // expected
        }
    }

    @Test
    fun signOutRevokesTheRefreshToken() = runTest {
        val flow = PairingFlow(pairLink)
        val auth = authServer(flow)
        val blob = MemoryBlob()
        val store = AccountStore(blob, TestBox(), auth, storageServer())
        store.completePairing(flow, "code-1")
        store.signOut()
        assertEquals("refresh-secret", auth.bodyOf("/oauth/destroy")!!.getString("token"))
        assertNull(blob.value)
    }

    @Test
    fun aPasswordSignInStoredBeforeStillLoads() = runTest {
        // The format of 0.3 and before.
        val box = TestBox()
        val old = JSONObject().put("email", "noa@example.com").put("uid", "uid-1").put("sessionToken", secretSession).put("kB", secretKb).toString()
        val blob = MemoryBlob().apply { value = Base64.getEncoder().encodeToString(box.seal(old.toByteArray())) }
        val loaded = AccountStore(blob, box).load()!!
        assertEquals(secretSession, loaded.sessionTokenHex)
        assertFalse(loaded.oauth)
    }
}
