package dev.nerzouille.koan.engine

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Koan's own small requests outside the engine (suggestions, link preview, video poster, video
 * check). Android's default User-Agent names the phone model and its build; these send the
 * engine's own User-Agent instead, the one every page already sees (no device details).
 */
object KoanHttp {
    /** Gecko's mobile User-Agent: Android version and engine version only. */
    val USER_AGENT: String by lazy {
        runCatching { org.mozilla.geckoview.GeckoSession.getDefaultUserAgent() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Mozilla/5.0 (Android; Mobile; rv:${GECKO_MAJOR}.0) Gecko/${GECKO_MAJOR}.0 Firefox/${GECKO_MAJOR}.0"
    }

    private val GECKO_MAJOR = org.mozilla.geckoview.BuildConfig.MOZILLA_VERSION.substringBefore('.')

    /** A connection with Koan's User-Agent and both timeouts set. */
    fun open(url: String, timeoutMs: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
        }

    /** The whole body, or null as soon as it grows past [maxBytes] (never buffered beyond that). */
    fun readCapped(input: InputStream, maxBytes: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return out.toByteArray()
            if (out.size() + n > maxBytes) return null
            out.write(buffer, 0, n)
        }
    }
}
