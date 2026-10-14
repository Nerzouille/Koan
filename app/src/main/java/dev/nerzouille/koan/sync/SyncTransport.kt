// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import java.net.HttpURLConnection
import java.net.URL

/**
 * Blocking HTTP seam for Sync storage and token-server calls, mirroring the
 * `HttpURLConnection` usage that used to live inline in `SyncClient` and
 * `FxAClient`. Callers must stay off the main thread.
 */
data class SyncHttpRequest(
    val method: String,
    val url: URL,
    val headers: Map<String, String>,
    val body: ByteArray?,
)

data class SyncHttpResponse(
    val statusCode: Int,
    /** Response headers with lowercased names, copied before disconnect. */
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    fun header(name: String): String? = headers[name.lowercase()]
}

/** Executes one HTTP request; blocking, exactly like today's call sites. */
interface SyncHttpTransport {
    fun execute(request: SyncHttpRequest): SyncHttpResponse
}

/**
 * Production transport: one `HttpURLConnection` per request, mechanically the
 * same calls `SyncClient.requestRaw`/`FxAClient.sendRequest` made inline.
 * Non-2xx statuses are returned raw; callers keep their existing error mapping.
 */
class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 30_000,
    private val readTimeoutMs: Int = 60_000,
) : SyncHttpTransport {
    override fun execute(request: SyncHttpRequest): SyncHttpResponse {
        val connection = request.url.openConnection() as HttpURLConnection
        try {
            // Sync endpoints must not be followed through redirects (SPEC §7); a 3xx fails closed.
            connection.instanceFollowRedirects = false
            connection.requestMethod = request.method
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            for ((name, value) in request.headers) {
                connection.setRequestProperty(name, value)
            }
            val body = request.body
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val data = stream?.use { it.readBytes() } ?: ByteArray(0)
            // Copy every header eagerly, before `disconnect()` invalidates the
            // connection's header fields.
            val responseHeaders = linkedMapOf<String, String>()
            for ((key, values) in connection.headerFields) {
                if (key != null && values.isNotEmpty()) {
                    responseHeaders[key.lowercase()] = values.first()
                }
            }
            return SyncHttpResponse(code, responseHeaders, data)
        } finally {
            connection.disconnect()
        }
    }
}
