package dev.nerzouille.koan.sync

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** `SyncClient.postBatch`, the only write Koan has (audit 9.1), against [FakeStorage]. */
class SyncClientPostBatchTest {
    private val server = FakeStorage()
    private val client = server.client()

    @After
    fun gateOff() {
        SpacesWriteGate.writeBackEnabled = false
    }

    private fun record(id: String) = JSONObject().put("id", id).put("histUri", "https://example.com/$id").put("title", "Example $id")

    @Test
    fun appliedBatchIsConditionalEncryptedAndReturnsTheNewTimestamp() {
        server.put("history", record("old"))
        val since = server.lastModified("history")!!

        val (outcome, modified) = client.postBatch("history", listOf(record("a"), record("b")), since)

        assertEquals(PostOutcome.Applied, outcome)
        assertEquals(server.lastModified("history"), modified)
        val post = server.posts.single()
        assertEquals(since, post.headers["X-If-Unmodified-Since"])
        assertTrue(post.headers.getValue("Authorization").startsWith("Hawk "))
        val body = String(post.body!!, Charsets.UTF_8)
        assertTrue("records go encrypted", "example.com" !in body && "Example" !in body)
        assertEquals(listOf("a", "b"), server.postedCleartexts(post).map { it.getString("id") })
        assertEquals("https://example.com/a", server.cleartexts("history").getValue("a").getString("histUri"))
    }

    @Test
    fun staleTimestampGivesPreconditionFailedAndWritesNothing() {
        server.put("history", record("old"))
        val stale = server.lastModified("history")!!
        server.put("history", record("someone-else"))

        val (outcome, modified) = client.postBatch("history", listOf(record("a")), stale)

        assertTrue(outcome is PostOutcome.PreconditionFailed)
        assertEquals(null, modified)
        assertEquals(setOf("old", "someone-else"), server.cleartexts("history").keys)
    }

    @Test
    fun idsMissingFromSuccessArePartialFailure() {
        val fake = FakeHttpTransport()
        fake.enqueue(SyncHttpResponse(200, mapOf("x-last-modified" to "12.00"), """{"success":["a"],"failed":{"b":"invalid"}}""".toByteArray()))
        val c = SyncClient(FakeStorage.creds, FakeStorage.testKeys(), emptyMap(), fake)

        val (outcome, _) = c.postBatch("history", listOf(record("a"), record("b"), record("c")), "10.00")

        outcome as PostOutcome.PartialFailure
        assertEquals(mapOf("b" to "invalid"), outcome.failed)
        assertEquals(listOf("b", "c"), outcome.missingIds)
    }

    @Test
    fun serverErrorThrowsNetworkWithTheStatus() {
        server.put("history", record("old"))
        server.nextPostStatus = 503
        try {
            client.postBatch("history", listOf(record("a")), server.lastModified("history")!!)
            fail("expected SyncError.Network")
        } catch (e: SyncError.Network) {
            assertTrue(e.message.orEmpty().startsWith("HTTP 503"))
        }
    }

    @Test
    fun spacesAreNeverPostedWhileWriteBackIsOff() {
        SpacesWriteGate.writeBackEnabled = false
        val fake = FakeHttpTransport()
        val c = SyncClient(FakeStorage.creds, FakeStorage.testKeys(), emptyMap(), fake)
        try {
            c.postBatch("spaces", listOf(JSONObject().put("id", "{space}")), "1.00")
            fail("expected WriteBackDisabled")
        } catch (e: SyncError) {
            assertEquals(SyncError.WriteBackDisabled, e)
        }
        assertTrue("the transport is never reached", fake.requests.isEmpty())
    }

    @Test
    fun spacesGoThroughOnlyWithWriteBackOn() {
        SpacesWriteGate.writeBackEnabled = true
        server.put("spaces", JSONObject().put("id", "{space}"))
        val (outcome, _) = client.postBatch("spaces", listOf(JSONObject().put("id", "{space}").put("name", "Example")), server.lastModified("spaces")!!)
        assertEquals(PostOutcome.Applied, outcome)
    }

    @Test
    fun readsOfSpacesStayAllowedWithWriteBackOff() {
        SpacesWriteGate.writeBackEnabled = false
        server.put("spaces", JSONObject().put("id", "{space}"))
        assertEquals(1, client.getRecords("spaces").size)
    }
}
