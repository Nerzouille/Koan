// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.sync.FxAClient
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.SyncHttpResponse
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Transport-driven tests for [FxAClient.syncCredentials] (SPEC §7.1): an
 * errno 103 failure on the primary OAuth/oldsync path must surface as
 * [SyncError.TotpRequired] and must not fall through to the BrowserID flow,
 * exactly like iOS. Error bodies come from the shared `auth-errno-103.json`
 * fixture via [FixtureLoader].
 */
class FxAClientAuthTests {
    private val sessionToken = "01".repeat(32)
    private val kB = ByteArray(32) { 0xAB.toByte() }

    /** `input.body` of a named case in the `auth-errno-103` fixture. */
    private fun errnoBody(caseId: String): ByteArray {
        val cases = FixtureLoader.cases("auth-errno-103")
        for (i in 0 until cases.length()) {
            val fixtureCase = cases.getJSONObject(i)
            if (fixtureCase.getString("id") == caseId) {
                return fixtureCase.getJSONObject("input").getJSONObject("body")
                    .toString().toByteArray(Charsets.UTF_8)
            }
        }
        error("fixture case $caseId missing")
    }

    private fun json(vararg entries: Pair<String, Any>): ByteArray {
        val obj = JSONObject()
        for ((key, value) in entries) obj.put(key, value)
        return obj.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * HTTP 401 + errno 103 from `scoped-key-data` — the OAuth token call
     * succeeds first, so the failure lands on the primary path — must throw
     * [SyncError.TotpRequired] and stop. The fallback stub is a sentinel that
     * is only consumed if the BrowserID flow runs.
     */
    @Test
    fun syncCredentialsRethrowsTotpRequiredWithoutBrowserIdFallback() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(200, emptyMap(), json("access_token" to "access-token")),
            SyncHttpResponse(401, emptyMap(), errnoBody("errno-103-totp")),
            SyncHttpResponse(401, emptyMap(), errnoBody("errno-103-totp")),
        )

        try {
            runBlocking { FxAClient(fake).syncCredentials(sessionToken, kB) }
            fail("expected SyncError.TotpRequired")
        } catch (expected: SyncError.TotpRequired) {
            // Expected.
        } catch (other: Exception) {
            fail("expected SyncError.TotpRequired, got $other")
        }

        assertEquals(2, fake.requests.size)
        assertEquals("oauth.accounts.firefox.com", fake.requests[0].url.host)
        assertEquals("api.accounts.firefox.com", fake.requests[1].url.host)
        assertEquals("/v1/account/scoped-key-data", fake.requests[1].url.path)
    }

    /**
     * A non-TOTP auth failure (errno 104, fixture case `errno-104-plain-auth`)
     * still falls back to BrowserID, mirroring Android's conditional rethrow.
     */
    @Test
    fun syncCredentialsFallsBackToBrowserIdForNonTotpFailure() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(200, emptyMap(), json("access_token" to "access-token")),
            SyncHttpResponse(401, emptyMap(), errnoBody("errno-104-plain-auth")),
            SyncHttpResponse(200, emptyMap(), json("cert" to "cert")),
            SyncHttpResponse(
                200,
                emptyMap(),
                json(
                    "uid" to "uid-1",
                    "api_endpoint" to "https://sync.example.com/1.0/sync/1.5",
                    "id" to "hawk-id",
                    "key" to "hawk-key",
                    "duration" to 3600,
                ),
            ),
        )

        val creds = runBlocking { FxAClient(fake).syncCredentials(sessionToken, kB) }

        assertEquals("uid-1", creds.uid)
        assertEquals(4, fake.requests.size)
        assertEquals("/v1/account/scoped-key-data", fake.requests[1].url.path)
        assertEquals("/v1/certificate/sign", fake.requests[2].url.path)
        assertEquals("token.services.mozilla.com", fake.requests[3].url.host)
        assertTrue(fake.requests[3].headers.getValue("Authorization").startsWith("BrowserID "))
    }
}
