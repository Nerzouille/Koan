package dev.nerzouille.koan.sync

import java.net.URI
import java.net.URLEncoder
import java.security.KeyPair
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey

/**
 * Sign-in by scanning the "Connect another device" QR code of Zen/Firefox desktop (Mozilla's
 * pairing). Koan is the "supplicant": it opens Mozilla's pairing page with its OAuth request
 * (PKCE + an ephemeral key for the Sync key), the desktop approves, and the page hands back an
 * authorization code over the WebChannel. Mozilla only pairs its own apps, so Koan asks as Firefox
 * for Android (the same way it signs in as Firefox desktop with a password). docs/SYNC.md.
 */
class PairingFlow(
    /** The scanned or pasted `https://accounts.firefox.com/pair#channel_id=…&channel_key=…`. */
    pairLink: String,
    random: SecureRandom = SecureRandom(),
    val keyPair: KeyPair = ScopedKeys.newKeyPair(),
) {
    /** The channel part of the link (id + key): only ever passed on in the URL fragment. */
    private val channel: String = parseChannel(pairLink) ?: throw IllegalArgumentException("not a pairing link")
    val state: String = base64url(ByteArray(16).also(random::nextBytes))
    val codeVerifier: String = base64url(ByteArray(32).also(random::nextBytes))
    val codeChallenge: String get() = base64url(FxACrypto.sha256(codeVerifier.toByteArray(Charsets.US_ASCII)))

    /** Our public key for the Sync key (`keys_jwk`). */
    val keysJwk: String get() = ScopedKeys.publicJwk(keyPair.public as ECPublicKey)

    /** The supplicant page, as Fenix's application-services builds it (WebChannel redirect = `context`). */
    fun supplicantUrl(): String {
        val query = listOf(
            "entrypoint" to "pairing",
            "service" to "sync",
            "client_id" to CLIENT_ID,
            "state" to state,
            "code_challenge_method" to "S256",
            "code_challenge" to codeChallenge,
            "access_type" to "offline",
            "keys_jwk" to keysJwk,
            "scope" to SCOPE,
            "context" to "oauth_webchannel_v1",
        ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return "https://$HOST/pair/supp?$query#$channel"
    }

    override fun toString() = "PairingFlow(<redacted>)"

    companion object {
        const val HOST = "accounts.firefox.com"

        /** Firefox for Android: one of the clients Mozilla's pairing accepts. */
        const val CLIENT_ID = "a2270f727f45f648"
        const val SCOPE = "profile ${FxAClient.OLD_SYNC_SCOPE}"

        /** What the pairing page must see to treat Koan as Firefox for Android. */
        const val USER_AGENT = "Mozilla/5.0 (Android 14; Mobile; rv:144.0) Gecko/144.0 Firefox/144.0"

        /** The channel fragment of a pairing link, or null if [link] isn't one. */
        fun parseChannel(link: String): String? {
            val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return null
            if (uri.scheme != "https" || uri.host != HOST || uri.path?.startsWith("/pair") != true) return null
            val fragment = uri.rawFragment ?: return null
            val params = fragment.split('&').associate { it.substringBefore('=') to it.substringAfter('=', "") }
            if (params["channel_id"].isNullOrEmpty() || params["channel_key"].isNullOrEmpty()) return null
            return fragment
        }

        fun isPairingLink(link: String?) = link != null && parseChannel(link) != null
    }
}
