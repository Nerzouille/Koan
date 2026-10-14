// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.sync.PostOutcome
import dev.nerzouille.koan.sync.PutOutcome
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncCrypto
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.SyncHttpRequest
import dev.nerzouille.koan.sync.SyncHttpResponse
import dev.nerzouille.koan.sync.SyncHttpTransport
import dev.nerzouille.koan.sync.TokenServerCreds
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.Base64

/**
 * Hermetic tests for the injectable Sync transport: [FakeHttpTransport]
 * records every request and replays queued responses, so the full
 * [SyncClient] read/write paths run with real crypto and no network.
 * Recorded exchanges from `shared/contract/http/` pin statuses and headers
 * where they fit (SPEC.md §7.3).
 */
class SyncTransportTests {
    private val creds = TokenServerCreds(
        uid = "uid-1",
        apiEndpoint = "https://sync.example.com",
        hawkID = "hawk-id",
        hawkKey = "hawk-key".toByteArray(Charsets.UTF_8),
        expiresAtMillis = Long.MAX_VALUE,
    )

    private val defaultKeys = SyncCrypto.KeyBundle(ByteArray(32) { 0x07 }, ByteArray(32) { 0x09 })
    private val syncKeys = SyncCrypto.syncKeyBundle(ByteArray(32))

    /** Client with preloaded keys: no `crypto/keys` fetch on construction. */
    private fun client(transport: SyncHttpTransport): SyncClient =
        SyncClient(creds, defaultKeys, emptyMap(), transport)

    /** Client that performs the real `crypto/keys` bootstrap against the fake. */
    private fun bootstrappingClient(transport: SyncHttpTransport): SyncClient =
        SyncClient(creds, ByteArray(32), transport)

    // MARK: Fixture wiring

    @Test
    fun httpRecordingsDoNotDrift() {
        assertEquals(11, HttpFixtures.allRecordingNames.size)
        for (name in HttpFixtures.allRecordingNames) {
            HttpFixtures.json(name)
            HttpFixtures.data(name)
            assertTrue(HttpFixtures.request(name).has("method"))
            assertTrue(HttpFixtures.request(name).has("path"))
        }
    }

    // MARK: Pagination

    @Test
    fun paginationFollowsWeaveNextOffset() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            fixtureResponse("http-get-pagination-page1", body = """[{"id":"a"},{"id":"b"}]"""),
            fixtureResponse("http-get-pagination-page2", body = """[{"id":"c"}]"""),
        )

        val records = client(fake).getRecords("spaces")

        assertEquals(listOf("a", "b", "c"), records.map { it.getString("id") })
        assertEquals(2, fake.requests.size)
        assertEquals("GET", fake.requests[1].method)
        assertEquals(
            HttpFixtures.request("http-get-pagination-page2").getString("path"),
            fake.requests[1].url.file,
        )
    }

    @Test
    fun paginationPercentEncodesOffset() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(200, mapOf("x-weave-next-offset" to "a/b+c d"), "[{\"id\":\"a\"}]".toByteArray(Charsets.UTF_8)),
            SyncHttpResponse(200, emptyMap(), "[]".toByteArray(Charsets.UTF_8)),
        )

        client(fake).getRecords("spaces")

        assertEquals("full=1&limit=2500&offset=a%2Fb%2Bc%20d", fake.requests[1].url.query)
    }

    @Test
    fun paginationStopsWhenServerRepeatsOffset() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            fixtureResponse("http-get-pagination-page1", body = """[{"id":"a"}]"""),
            fixtureResponse("http-get-stuck-offset", body = """[{"id":"b"}]"""),
            fixtureResponse("http-get-pagination-page1", body = """[{"id":"c"}]"""),
        )

        val records = client(fake).getRecords("spaces")

        assertEquals(listOf("a", "b"), records.map { it.getString("id") })
        assertEquals(2, fake.requests.size)
    }

    @Test
    fun paginationStopsOnEmptyPage() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            fixtureResponse("http-get-pagination-page1", body = "[]"),
            fixtureResponse("http-get-pagination-page2", body = """[{"id":"never"}]"""),
        )

        assertTrue(client(fake).getRecords("spaces").isEmpty())
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun paginationCapsAtFiftyPages() {
        val fake = FakeHttpTransport()
        repeat(51) { page ->
            fake.enqueue(
                SyncHttpResponse(
                    200,
                    mapOf("x-weave-next-offset" to "offset-$page"),
                    """[{"id":"r$page"}]""".toByteArray(Charsets.UTF_8),
                ),
            )
        }

        val records = client(fake).getRecords("spaces")

        assertEquals(50, records.size)
        assertEquals("r49", records.last().getString("id"))
        assertEquals(50, fake.requests.size)
    }

    // MARK: Reads

    @Test
    fun getRecentRecordsUsesNewestQuery() {
        val fake = FakeHttpTransport()
        fake.enqueue(SyncHttpResponse(200, emptyMap(), "[]".toByteArray(Charsets.UTF_8)))

        val records = client(fake).getRecentRecords("history", 5)

        assertTrue(records.isEmpty())
        val request = fake.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/storage/history?full=1&limit=5&sort=newest", request.url.file)
        assertEquals(setOf("User-Agent", "Authorization"), request.headers.keys)
        assertTrue(request.headers.getValue("Authorization").startsWith("Hawk "))
        assertFalse(request.headers.keys.any { it.equals("X-If-Unmodified-Since", ignoreCase = true) })
    }

    @Test
    fun collection404ReturnsEmptyList() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-get-404"))

        assertTrue(client(fake).getRecords("spaces").isEmpty())
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun serverErrorSurfacesHawkClockSkewHeaders() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-get-500-hawk"))

        try {
            client(fake).getRecords("spaces")
            fail("expected SyncError.Network")
        } catch (expected: SyncError.Network) {
            val message = expected.message.orEmpty()
            assertTrue(message.contains("HTTP 500 GET /storage/spaces"))
            assertTrue(message.contains("WWW-Authenticate"))
            assertTrue(message.contains("Hawk ts=\"1700000050.00\""))
            assertTrue(message.contains("X-Timestamp"))
            assertTrue(message.contains("1700000050.00"))
        }
    }

    // MARK: Collection keys

    @Test
    fun keysBootstrapCreatesDefaultBundleThenReReads() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            fixtureResponse("http-keys-missing"),
            SyncHttpResponse(200, emptyMap(), "1700000300".toByteArray(Charsets.UTF_8)),
            fixtureResponse("http-keys-present-default", body = keysRecordJson(defaultKeys, emptyMap())),
        )

        val client = bootstrappingClient(fake)

        assertEquals(listOf("GET", "PUT", "GET"), fake.requests.map { it.method })
        assertEquals(listOf("/storage/crypto/keys", "/storage/crypto/keys", "/storage/crypto/keys"), fake.requests.map { it.url.file })

        // The bootstrap PUT body is a §4 envelope whose plaintext carries fresh
        // 32-byte default keys (structure/lengths, not the random bytes).
        val putBody = JSONObject(String(fake.requests[1].body!!, Charsets.UTF_8))
        assertEquals(setOf("payload"), putBody.keys().asSequence().toSet())
        val bootstrapped = JSONObject(
            String(SyncCrypto.decryptBSO(putBody.getString("payload"), syncKeys), Charsets.UTF_8),
        )
        val def = bootstrapped.getJSONArray("default")
        assertEquals(2, def.length())
        assertEquals(32, Base64.getDecoder().decode(def.getString(0)).size)
        assertEquals(32, Base64.getDecoder().decode(def.getString(1)).size)
        assertEquals(0, bootstrapped.getJSONObject("collections").length())

        assertArrayEquals(defaultKeys.encryptionKey, client.keys("spaces").encryptionKey)
    }

    @Test
    fun malformedCollectionsYieldEmptyKeyMapAndDefaultFallback() {
        val fake = FakeHttpTransport()
        // `collections` as an array is malformed; the per-collection map stays
        // empty and the default bundle is used (no throw, no bootstrap).
        fake.enqueue(fixtureResponse("http-keys-present-default", body = keysRecordJson(defaultKeys, null)))

        val client = bootstrappingClient(fake)

        assertEquals(1, fake.requests.size)
        assertEquals("GET", fake.requests.single().method)
        assertArrayEquals(defaultKeys.encryptionKey, client.keys("spaces").encryptionKey)
    }

    @Test
    fun perCollectionKeysArePreferredOverDefault() {
        val spacesKeys = SyncCrypto.KeyBundle(ByteArray(32) { 0x11 }, ByteArray(32) { 0x22 })
        val fake = FakeHttpTransport()
        fake.enqueue(
            fixtureResponse(
                "http-keys-present-per-collection",
                body = keysRecordJson(defaultKeys, mapOf("spaces" to spacesKeys)),
            ),
        )

        val client = bootstrappingClient(fake)

        assertArrayEquals(spacesKeys.encryptionKey, client.keys("spaces").encryptionKey)
        assertArrayEquals(spacesKeys.hmacKey, client.keys("spaces").hmacKey)
        assertArrayEquals(defaultKeys.encryptionKey, client.keys("other").encryptionKey)
    }

    // MARK: Writes

    @Test
    fun putRecordRequestShapeAndRoundTrip() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-put-200"))
        val obj = JSONObject().put("hello", "zen")
        val client = client(fake)

        client.putRecord("spaces", "{abc}", obj)

        val request = fake.requests.single()
        assertEquals("PUT", request.method)
        assertEquals(HttpFixtures.request("http-put-200").getString("path"), request.url.file)
        assertEquals(setOf("User-Agent", "Content-Type", "Authorization"), request.headers.keys)
        assertTrue(request.headers.getValue("Authorization").startsWith("Hawk "))
        assertTrue(request.headers.getValue("Authorization").contains("id=\"hawk-id\""))
        assertFalse(request.headers.keys.any { it.equals("X-If-Unmodified-Since", ignoreCase = true) })

        val body = JSONObject(String(request.body!!, Charsets.UTF_8))
        assertEquals(setOf("payload"), body.keys().asSequence().toSet())
        val envelope = body.getString("payload")
        val plaintext = JSONObject(String(SyncCrypto.decryptBSO(envelope, defaultKeys), Charsets.UTF_8))
        assertEquals("zen", plaintext.getString("hello"))

        // Client-side decrypt is the inverse of the wire envelope.
        val record = JSONObject().put("id", "{abc}").put("payload", envelope)
        assertEquals("zen", client.decryptRecord("spaces", record).getString("hello"))
    }

    @Test
    fun putTombstoneEncryptsBooleanDeleted() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-put-200"))

        client(fake).putTombstone("spaces", "{abc}")

        val body = JSONObject(String(fake.requests.single().body!!, Charsets.UTF_8))
        val payload = JSONObject(
            String(SyncCrypto.decryptBSO(body.getString("payload"), defaultKeys), Charsets.UTF_8),
        )
        assertEquals("{abc}", payload.getString("id"))
        assertTrue(payload.get("deleted") is Boolean)
        assertTrue(payload.getBoolean("deleted"))
    }

    // MARK: Conditional reads and writes (SPEC §7.2)

    @Test
    fun getCollectionWithMetadataConditionsSubsequentPages() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1700000000.00", "x-weave-next-offset" to "2"),
                """[{"id":"a"},{"id":"b"}]""".toByteArray(Charsets.UTF_8),
            ),
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1700000000.00"),
                """[{"id":"c"}]""".toByteArray(Charsets.UTF_8),
            ),
        )

        val read = client(fake).getCollectionWithMetadata("spaces")

        assertEquals("1700000000.00", read.lastModified)
        assertEquals(listOf("a", "b", "c"), read.records.map { it.getString("id") })
        assertEquals(2, fake.requests.size)
        assertNull(fake.requests[0].headers["X-If-Unmodified-Since"])
        assertEquals("1700000000.00", fake.requests[1].headers["X-If-Unmodified-Since"])
    }

    @Test
    fun getCollectionWithMetadataRestartsOnceAfterMidRead412() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1700000000.00", "x-weave-next-offset" to "1"),
                """[{"id":"a"}]""".toByteArray(Charsets.UTF_8),
            ),
            SyncHttpResponse(412, mapOf("x-last-modified" to "1700000100.12"), ByteArray(0)),
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1700000100.12", "x-weave-next-offset" to "1"),
                """[{"id":"a"}]""".toByteArray(Charsets.UTF_8),
            ),
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1700000100.12"),
                """[{"id":"b"}]""".toByteArray(Charsets.UTF_8),
            ),
        )

        val read = client(fake).getCollectionWithMetadata("spaces")

        assertEquals("1700000100.12", read.lastModified)
        assertEquals(listOf("a", "b"), read.records.map { it.getString("id") })
        assertEquals(4, fake.requests.size)
        assertNull(fake.requests[0].headers["X-If-Unmodified-Since"])
        assertEquals("1700000000.00", fake.requests[1].headers["X-If-Unmodified-Since"])
        assertNull(fake.requests[2].headers["X-If-Unmodified-Since"])
        assertEquals("1700000100.12", fake.requests[3].headers["X-If-Unmodified-Since"])
    }

    @Test
    fun getCollectionWithMetadataSecondMidRead412ThrowsConflict() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "1.00", "x-weave-next-offset" to "1"),
                """[{"id":"a"}]""".toByteArray(Charsets.UTF_8),
            ),
            SyncHttpResponse(412, mapOf("x-last-modified" to "2.00"), ByteArray(0)),
            SyncHttpResponse(
                200,
                mapOf("x-last-modified" to "2.00", "x-weave-next-offset" to "1"),
                """[{"id":"a"}]""".toByteArray(Charsets.UTF_8),
            ),
            SyncHttpResponse(412, mapOf("x-last-modified" to "3.00"), ByteArray(0)),
        )

        try {
            client(fake).getCollectionWithMetadata("spaces")
            fail("expected SyncError.Conflict")
        } catch (expected: SyncError.Conflict) {
            // Expected: the single restart also lost the race.
        }
        assertEquals(4, fake.requests.size)
    }

    @Test
    fun putRecordConditional412ReportsLastModified() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-put-412"))

        val outcome = client(fake).putRecord("spaces", "{abc}", JSONObject().put("hello", "zen"), "1700000000.00")

        assertEquals(PutOutcome.PreconditionFailed("1700000100.12"), outcome)
        val request = fake.requests.single()
        assertEquals("PUT", request.method)
        assertEquals(HttpFixtures.request("http-put-412").getString("path"), request.url.file)
        assertEquals("1700000000.00", request.headers["X-If-Unmodified-Since"])
    }

    @Test
    fun putRecordConditionalSuccessApplies() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-put-200"))

        val outcome = client(fake).putRecord("spaces", "{abc}", JSONObject().put("hello", "zen"), "1700000000.00")

        assertEquals(PutOutcome.Applied, outcome)
    }

    @Test
    fun postRecordsParsesSuccessAndFailed() {
        val fake = FakeHttpTransport()
        fake.enqueue(fixtureResponse("http-post-200-success-failed"))

        val outcome = client(fake).postRecords(
            "spaces",
            listOf(JSONObject().put("id", "tab-1"), JSONObject().put("id", "space-1")),
            "1700000000.00",
        )

        assertEquals(PostOutcome.Applied, outcome)
        val request = fake.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/storage/spaces", request.url.file)
        assertEquals("1700000000.00", request.headers["X-If-Unmodified-Since"])
        val arr = JSONArray(String(request.body!!, Charsets.UTF_8))
        assertEquals(2, arr.length())
        assertEquals("tab-1", arr.getJSONObject(0).getString("id"))
        assertEquals(setOf("id", "payload"), arr.getJSONObject(0).keys().asSequence().toSet())
    }

    @Test
    fun postRecordsMissingIdIsPartialFailure() {
        val fake = FakeHttpTransport()
        fake.enqueue(
            SyncHttpResponse(
                200,
                emptyMap(),
                """{"modified":1,"success":["a"],"failed":{"b":"oops"}}""".toByteArray(Charsets.UTF_8),
            ),
        )

        val outcome = client(fake).postRecords(
            "spaces",
            listOf(JSONObject().put("id", "a"), JSONObject().put("id", "b")),
        )

        assertTrue(outcome is PostOutcome.PartialFailure)
        outcome as PostOutcome.PartialFailure
        assertEquals(mapOf("b" to "oops"), outcome.failed)
        assertEquals(listOf("b"), outcome.missingIds)
        assertNull(fake.requests.single().headers["X-If-Unmodified-Since"])
    }

    @Test
    fun postRecords412ReportsLastModified() {
        val fake = FakeHttpTransport()
        fake.enqueue(SyncHttpResponse(412, mapOf("x-last-modified" to "1700000100.12"), ByteArray(0)))

        val outcome = client(fake).postRecords("spaces", listOf(JSONObject().put("id", "a")), "1.00")

        assertEquals(PostOutcome.PreconditionFailed("1700000100.12"), outcome)
        assertEquals("1.00", fake.requests.single().headers["X-If-Unmodified-Since"])
    }

    // MARK: Endpoint enforcement (SPEC §7)

    @Test
    fun insecureEndpointRejectedBeforeAnyRequest() {
        val insecureCreds = TokenServerCreds(
            uid = "uid-1",
            apiEndpoint = "http://example.test/1.5/abc",
            hawkID = "hawk-id",
            hawkKey = "hawk-key".toByteArray(Charsets.UTF_8),
            expiresAtMillis = Long.MAX_VALUE,
        )
        val fake = FakeHttpTransport()

        try {
            SyncClient(insecureCreds, defaultKeys, emptyMap(), fake).infoCollections()
            fail("expected SyncError.Network")
        } catch (expected: SyncError.Network) {
            assertEquals("insecure sync endpoint", expected.message)
        }
        assertTrue(fake.requests.isEmpty())
    }

    // MARK: Failures

    @Test
    fun transportIOExceptionPropagatesWithoutRetry() {
        val fake = FakeHttpTransport(failWith = IOException("boom"))

        try {
            client(fake).getRecords("spaces")
            fail("expected IOException")
        } catch (expected: IOException) {
            assertEquals("boom", expected.message)
        }
        assertEquals(1, fake.requests.size)
    }

    // MARK: Helpers

    /** Queued response derived from a recording, with optional constructed body. */
    private fun fixtureResponse(name: String, body: String? = null): SyncHttpResponse {
        val response = HttpFixtures.response(name)
        val headers = linkedMapOf<String, String>()
        response.optJSONObject("headers")?.let { recorded ->
            for (key in recorded.keys()) {
                headers[key.lowercase()] = recorded.getString(key)
            }
        }
        val bytes = when {
            body != null -> body.toByteArray(Charsets.UTF_8)
            !response.isNull("body") -> response.get("body").toString().toByteArray(Charsets.UTF_8)
            else -> ByteArray(0)
        }
        return SyncHttpResponse(response.getInt("status"), headers, bytes)
    }

    /** §4 envelope wrapping a keys object, encrypted with the sync key bundle. */
    private fun keysRecordJson(
        default: SyncCrypto.KeyBundle,
        collections: Map<String, SyncCrypto.KeyBundle>?,
    ): String {
        val obj = JSONObject().put(
            "default",
            JSONArray().put(b64(default.encryptionKey)).put(b64(default.hmacKey)),
        )
        if (collections == null) {
            obj.put("collections", JSONArray())
        } else {
            val cols = JSONObject()
            for ((name, keys) in collections) {
                cols.put(name, JSONArray().put(b64(keys.encryptionKey)).put(b64(keys.hmacKey)))
            }
            obj.put("collections", cols)
        }
        val payload = SyncCrypto.encryptBSO(obj.toString().toByteArray(Charsets.UTF_8), syncKeys)
        return JSONObject().put("payload", payload).toString()
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}

/**
 * In-memory transport: records every request, replays queued responses in
 * order, and can fail every call with a fixed [IOException]. Shared with
 * [FxAClientAuthTests].
 */
internal class FakeHttpTransport(private val failWith: IOException? = null) : SyncHttpTransport {
    val requests = mutableListOf<SyncHttpRequest>()
    private val queued = ArrayDeque<SyncHttpResponse>()

    fun enqueue(vararg responses: SyncHttpResponse) {
        queued.addAll(responses)
    }

    override fun execute(request: SyncHttpRequest): SyncHttpResponse {
        requests += request
        failWith?.let { throw it }
        check(queued.isNotEmpty()) { "no queued response for ${request.method} ${request.url}" }
        return queued.removeFirst()
    }
}
