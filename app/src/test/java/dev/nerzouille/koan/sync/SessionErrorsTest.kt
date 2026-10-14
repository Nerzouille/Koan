package dev.nerzouille.koan.sync

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * Koan's additions to the ported FxA client: an expired session and being offline must stay
 * distinguishable, because one shows "Sign in again" and the other must stay silent.
 */
class SessionErrorsTest {
    private val sessionToken = "01".repeat(32)
    private val kB = ByteArray(32) { 0xAB.toByte() }

    private fun errno(code: Int) =
        JSONObject().put("errno", code).put("message", "Invalid authentication token in request signature").put("code", 401)
            .toString().toByteArray()

    @Test
    fun invalidSessionTokenMeansSignInAgainWithoutBrowserIdFallback() {
        val fake = FakeHttpTransport()
        // Both OAuth endpoints reject the token.
        fake.enqueue(SyncHttpResponse(401, emptyMap(), errno(110)), SyncHttpResponse(401, emptyMap(), errno(110)))
        try {
            runBlocking { FxAClient(fake).syncCredentials(sessionToken, kB) }
            fail("expected SessionExpired")
        } catch (expected: SyncError.SessionExpired) {
            // Expected.
        }
        assertEquals(2, fake.requests.size)
    }

    @Test
    fun offlineStaysANetworkError() {
        val fake = FakeHttpTransport(failWith = UnknownHostException("oauth.accounts.firefox.com"))
        try {
            runBlocking { FxAClient(fake).syncCredentials(sessionToken, kB) }
            fail("expected IOException")
        } catch (expected: IOException) {
            // Expected: SpacesSync shows it as "offline", never as "sign in again".
        }
    }
}
