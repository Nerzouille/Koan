package dev.nerzouille.koan.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** What About shows under the version. */
sealed interface UpdateStatus {
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val version: String, val url: String) : UpdateStatus
    /** No network, GitHub unreachable or rate-limited: quiet, tap to try again. */
    data object Failed : UpdateStatus
}

/**
 * One look at Koan's GitHub releases, only when About opens (never in the background). Reads the
 * repo's releases feed, not the REST API: the API's anonymous limit (60 an hour per IP) is used up
 * at once behind a shared VPN address. The feed lists every version tag, pre-releases included, so
 * a version tag counts as released as soon as it is pushed.
 */
object UpdateCheck {
    private const val FEED = "https://github.com/Nerzouille/Koan/releases.atom"
    private const val TIMEOUT_MS = 5000
    private val TAG_LINK = Regex("""/releases/tag/([^"/?#]+)"""")

    suspend fun check(current: String): UpdateStatus = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(FEED).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "Koan/$current")
            }
            try {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                status(current, body)
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(UpdateStatus.Failed)
    }

    /** The newest version tag in [feed] (GitHub's releases Atom feed) against [current]. */
    internal fun status(current: String, feed: String): UpdateStatus {
        val newest = TAG_LINK.findAll(feed)
            .mapNotNull { m -> parse(m.groupValues[1])?.let { it to m.groupValues[1] } }
            .maxWithOrNull { a, b -> compare(a.first, b.first) }
            ?: return UpdateStatus.UpToDate
        val mine = parse(current) ?: return UpdateStatus.Failed
        if (compare(newest.first, mine) <= 0) return UpdateStatus.UpToDate
        return UpdateStatus.Available(newest.first.joinToString("."), "https://github.com/Nerzouille/Koan/releases/tag/${newest.second}")
    }

    /** "v0.2.0" / "0.2.0-rc1" -> [0, 2, 0]; null when it isn't a version. */
    internal fun parse(tag: String): List<Int>? {
        val parts = tag.trim().removePrefix("v").substringBefore('-').split('.')
        return parts.map { it.toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }
    }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
