package dev.nerzouille.koan.sync

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** CLAUDE.md rule 4: nothing writes the Zen spaces collection while writeBackEnabled is off. */
class SpacesWriteGateTest {
    private val creds = TokenServerCreds("uid-1", "https://sync.example.com", "hawk-id", "hawk-key".toByteArray(), Long.MAX_VALUE)
    private val keys = SyncCrypto.KeyBundle(ByteArray(32) { 0x07 }, ByteArray(32) { 0x09 })
    private fun client(fake: FakeHttpTransport) = SyncClient(creds, keys, emptyMap(), fake)

    @After fun off() {
        SpacesWriteGate.writeBackEnabled = false
    }

    private fun refused(fake: FakeHttpTransport, write: () -> Unit) {
        try {
            write()
            fail("expected WriteBackDisabled")
        } catch (e: SyncError.WriteBackDisabled) {
            // expected
        }
        assertTrue("no request may leave Koan", fake.requests.isEmpty())
    }

    @Test fun offByDefault() {
        assertFalse(SpacesWriteGate.writeBackEnabled)
    }

    @Test fun everyWriteMethodIsRefusedForSpaces() {
        val record = JSONObject().put("id", "{abc}").put("hello", "zen")
        FakeHttpTransport().let { f -> refused(f) { client(f).putRecord("spaces", "{abc}", record) } }
        FakeHttpTransport().let { f -> refused(f) { client(f).putRecord("spaces", "{abc}", record, "1700000000.00") } }
        FakeHttpTransport().let { f -> refused(f) { client(f).putTombstone("spaces", "{abc}") } }
        FakeHttpTransport().let { f -> refused(f) { client(f).postRecords("spaces", listOf(record), "1700000000.00") } }
        FakeHttpTransport().let { f -> refused(f) { client(f).postBatch("spaces", listOf(record), "1700000000.00") } }
    }

    @Test fun readsAndOtherCollectionsAreNotAffected() {
        assertFalse(SpacesWriteGate.touchesSpaces("GET", "/storage/spaces?full=1&limit=2500"))
        assertFalse(SpacesWriteGate.touchesSpaces("POST", "/storage/passwords"))
        assertFalse(SpacesWriteGate.touchesSpaces("POST", "/storage/history"))
        assertFalse(SpacesWriteGate.touchesSpaces("PUT", "/storage/spacesextra/x"))
        assertTrue(SpacesWriteGate.touchesSpaces("DELETE", "/storage/spaces"))
        assertTrue(SpacesWriteGate.touchesSpaces("delete", "/storage"))
        assertTrue(SpacesWriteGate.touchesSpaces("PUT", "/storage/spaces/%7Babc%7D"))
    }

    @Test fun onlyWhenTheSettingIsOnDoesTheRequestLeave() {
        SpacesWriteGate.writeBackEnabled = true
        val fake = FakeHttpTransport()
        fake.enqueue(SyncHttpResponse(200, mapOf("x-last-modified" to "1700000001.00"), "1700000001.00".toByteArray()))
        client(fake).putRecord("spaces", "{abc}", JSONObject().put("id", "{abc}"))
        assertEquals(listOf("PUT"), fake.requests.map { it.method })
    }
}
