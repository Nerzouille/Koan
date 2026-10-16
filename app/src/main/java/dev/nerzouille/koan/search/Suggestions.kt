package dev.nerzouille.koan.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Search suggestions from DuckDuckGo (whatever the default engine: it keeps no profile). Only
 * what is typed in the address field is sent, never a url.
 */
object Suggestions {
    private const val ENDPOINT = "https://duckduckgo.com/ac/?type=list&q="
    private const val TIMEOUT_MS = 2500
    private const val MAX = 4

    suspend fun fetch(query: String): List<String> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2 || looksLikeUrl(q)) return@withContext emptyList()
        runCatching {
            val connection = (URL(ENDPOINT + URLEncoder.encode(q, Charsets.UTF_8)).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body).filterNot { it.equals(q, ignoreCase = true) }.take(MAX)
        }.getOrDefault(emptyList())
    }

    /** `["query", ["suggestion", …]]`. */
    internal fun parse(body: String): List<String> {
        val list = JSONArray(body).optJSONArray(1) ?: return emptyList()
        return List(list.length()) { list.optString(it) }.filter { it.isNotBlank() }
    }

    private fun looksLikeUrl(q: String) = ' ' !in q && ('/' in q || q.contains("://"))
}
