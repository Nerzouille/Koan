package dev.nerzouille.koan.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Search suggestions from the engine the search goes to (its OpenSearch-style endpoint). Only
 * what is typed is sent, never a url. Quiet by design: a slow or failed request (no network, an
 * engine unreachable without VPN) simply gives none, and local results never wait for it.
 */
object Suggestions {
    private const val TIMEOUT_MS = 1200
    private const val MAX = 4

    /** [url]: the engine's suggestion url for [query] (see SearchEngine / Shortcut). */
    suspend fun fetch(query: String, url: String?): List<String> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (url == null || q.length < 2 || looksLikeUrl(q)) return@withContext emptyList()
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body).filterNot { it.equals(q, ignoreCase = true) }.take(MAX)
        }.getOrDefault(emptyList())
    }

    /** `["query", ["suggestion", …], …]` (the OpenSearch suggestions format all of them use). */
    internal fun parse(body: String): List<String> {
        val list = JSONArray(body).optJSONArray(1) ?: return emptyList()
        return List(list.length()) { list.optString(it) }.filter { it.isNotBlank() }
    }

    private fun looksLikeUrl(q: String) = ' ' !in q && ('/' in q || q.contains("://"))
}
