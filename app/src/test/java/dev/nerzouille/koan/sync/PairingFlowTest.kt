package dev.nerzouille.koan.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.util.Base64

class PairingFlowTest {
    private val link = "https://accounts.firefox.com/pair#channel_id=abc123&channel_key=a2V5LWZha2U"

    @Test
    fun recognisesPairingLinksOnly() {
        assertEquals("channel_id=abc123&channel_key=a2V5LWZha2U", PairingFlow.parseChannel(link))
        assertEquals("channel_id=1&channel_key=2&v=2", PairingFlow.parseChannel("https://accounts.firefox.com/pair#channel_id=1&channel_key=2&v=2"))
        assertNull(PairingFlow.parseChannel("https://accounts.firefox.com/pair"))
        assertNull(PairingFlow.parseChannel("https://accounts.firefox.com/pair#channel_id=1"))
        assertNull(PairingFlow.parseChannel("https://example.com/pair#channel_id=1&channel_key=2"))
        assertNull(PairingFlow.parseChannel("http://accounts.firefox.com/pair#channel_id=1&channel_key=2"))
        assertNull(PairingFlow.parseChannel("https://accounts.firefox.com/settings#channel_id=1&channel_key=2"))
        assertNull(PairingFlow.parseChannel("not a link"))
        assertTrue(PairingFlow.isPairingLink("  $link  "))
    }

    @Test
    fun supplicantUrlIsWhatFenixSends() {
        val flow = PairingFlow(link)
        val uri = URI(flow.supplicantUrl())
        assertEquals("accounts.firefox.com", uri.host)
        assertEquals("/pair/supp", uri.path)
        // The channel key stays in the fragment: never sent to a server.
        assertEquals("channel_id=abc123&channel_key=a2V5LWZha2U", uri.rawFragment)
        assertFalse(uri.rawQuery.contains("channel_key"))
        val q = uri.rawQuery.split('&').associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8") }
        assertEquals(PairingFlow.CLIENT_ID, q["client_id"])
        assertEquals("oauth_webchannel_v1", q["context"])
        // FxA rejects the WebChannel redirect as a query parameter (seen on the real server).
        assertFalse(q.containsKey("redirect_uri"))
        assertEquals("S256", q["code_challenge_method"])
        assertEquals("offline", q["access_type"])
        assertEquals(flow.state, q["state"])
        assertEquals("profile https://identity.mozilla.com/apps/oldsync", q["scope"])
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(FxACrypto.sha256(flow.codeVerifier.toByteArray()))
        assertEquals(challenge, q["code_challenge"])
        assertTrue(q["keys_jwk"]!!.isNotEmpty())
    }

    @Test
    fun everyFlowIsFresh() {
        val a = PairingFlow(link)
        val b = PairingFlow(link)
        assertFalse(a.state == b.state)
        assertFalse(a.codeVerifier == b.codeVerifier)
        assertFalse(a.toString().contains(a.codeVerifier))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesOtherLinks() {
        PairingFlow("https://example.com/")
    }
}
