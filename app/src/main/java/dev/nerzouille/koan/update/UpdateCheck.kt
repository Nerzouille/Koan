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

    /**
     * The newest version tag in [feed] (GitHub's releases Atom feed) against [current], in semver
     * order (`1.0.0-beta.1 < 1.0.0-beta.2 < 1.0.0-rc.1 < 1.0.0`). The feed doesn't say which
     * releases are pre-releases: a tag with a `-suffix` is one. Someone on a stable 1.0 or later is
     * offered stable releases only; someone on a pre-release or on 0.x is offered both.
     */
    internal fun status(current: String, feed: String): UpdateStatus {
        val mine = parse(current) ?: return UpdateStatus.Failed
        val stableOnly = !mine.isPreRelease && mine.numbers.first() >= 1
        val newest = TAG_LINK.findAll(feed)
            .mapNotNull { m -> parse(m.groupValues[1])?.let { it to m.groupValues[1] } }
            .filter { (v, _) -> !stableOnly || !v.isPreRelease }
            .maxWithOrNull { a, b -> a.first.compareTo(b.first) }
            ?: return UpdateStatus.UpToDate
        if (newest.first <= mine) return UpdateStatus.UpToDate
        return UpdateStatus.Available(newest.first.toString(), "https://github.com/Nerzouille/Koan/releases/tag/${newest.second}")
    }

    /** "v0.2.0" / "1.0.0-beta.1" / "1.0.0-rc.1+build"; null when it isn't a version. */
    internal fun parse(tag: String): Version? {
        val bare = tag.trim().removePrefix("v").substringBefore('+')
        val numbers = bare.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
        if (numbers.isEmpty()) return null
        val pre = if ('-' in bare) bare.substringAfter('-').split('.').filter { it.isNotEmpty() } else emptyList()
        if ('-' in bare && pre.isEmpty()) return null
        return Version(numbers, pre)
    }

    /** A semver version: [numbers] (major.minor.patch…) and the pre-release identifiers, if any. */
    data class Version(val numbers: List<Int>, val preRelease: List<String> = emptyList()) : Comparable<Version> {
        val isPreRelease get() = preRelease.isNotEmpty()

        override fun compareTo(other: Version): Int {
            for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
                val d = numbers.getOrElse(i) { 0 }.compareTo(other.numbers.getOrElse(i) { 0 })
                if (d != 0) return d
            }
            // A release is newer than its own pre-releases.
            if (preRelease.isEmpty() || other.preRelease.isEmpty()) return other.preRelease.size.coerceAtMost(1) - preRelease.size.coerceAtMost(1)
            for (i in 0 until minOf(preRelease.size, other.preRelease.size)) {
                val a = preRelease[i]
                val b = other.preRelease[i]
                val an = a.toIntOrNull()
                val bn = b.toIntOrNull()
                val d = when {
                    an != null && bn != null -> an.compareTo(bn)
                    an != null -> -1 // numeric identifiers sort before alphanumeric ones
                    bn != null -> 1
                    else -> a.compareTo(b)
                }
                if (d != 0) return d
            }
            return preRelease.size.compareTo(other.preRelease.size)
        }

        override fun toString() = numbers.joinToString(".") + if (isPreRelease) "-" + preRelease.joinToString(".") else ""
    }
}
