package dev.nerzouille.koan.ui.player

import kotlinx.coroutines.runBlocking
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCheckTest {
    private class Fake(private val type: String) : Client() {
        val asked = mutableListOf<Request>()
        override fun fetch(request: Request): Response {
            asked += request
            return Response(request.url, 200, MutableHeaders("Content-Type" to type), Response.Body.empty())
        }
    }

    @Test fun `a private tab asks through the private session, with HEAD`() = runBlocking {
        val client = Fake("video/mp4")
        assertTrue(isVideoFile(client, "https://example.com/v?id=1", private = true))
        val r = client.asked.single()
        assertTrue(r.private)
        assertEquals(Request.Method.HEAD, r.method)
    }

    @Test fun `a normal tab asks outside the private session`() = runBlocking {
        val client = Fake("video/webm")
        assertTrue(isVideoFile(client, "https://example.com/v", private = false))
        assertFalse(client.asked.single().private)
    }

    @Test fun `private never asks over plain http`() = runBlocking {
        val client = Fake("video/mp4")
        assertFalse(isVideoFile(client, "http://example.com/v", private = true))
        assertTrue(client.asked.isEmpty())
    }

    @Test fun `a page is not a video`() = runBlocking {
        assertFalse(isVideoFile(Fake("text/html"), "https://example.com/v", private = false))
    }
}
