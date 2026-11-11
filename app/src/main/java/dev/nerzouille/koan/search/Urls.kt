package dev.nerzouille.koan.search

/*
 * One place for what a web address's host means (audit 3.28): the site a login, an app link
 * choice or an icon belongs to. Pure Kotlin, JVM-tested (UrlsTest).
 */

/** An http(s) address. */
fun isWebUrl(url: String): Boolean = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

/**
 * One site for "www.", "m." and "mobile." hosts ("en.m.example.org" → "en.example.org"): a mobile
 * label is dropped anywhere before the site name; a name and a suffix always stay. Lower case.
 */
fun siteHost(host: String): String {
    val labels = host.lowercase().split('.').toMutableList()
    if (labels.size > 2 && labels.first() == "www") labels.removeAt(0)
    var i = 0
    while (i < labels.size - 2) {
        if (labels[i] in MOBILE_LABELS) labels.removeAt(i) else i++
    }
    return labels.joinToString(".")
}

private val MOBILE_LABELS = setOf("m", "mobile")

/**
 * "accounts.example.co.uk" → "example.co.uk", roughly: the last two labels (three for short
 * ccSLDs). Under a shared hosting suffix ([SHARED_SUFFIXES]) each site is its own: no public
 * suffix list, only the common ones.
 */
fun baseDomain(host: String): String {
    val labels = host.removePrefix("www.").split('.')
    SHARED_SUFFIXES.firstOrNull { host.endsWith(".$it", ignoreCase = true) }?.let { suffix ->
        val keep = suffix.count { it == '.' } + 2
        return labels.takeLast(keep).joinToString(".")
    }
    if (labels.size <= 2) return labels.joinToString(".")
    val secondLevel = labels[labels.size - 2]
    val keep = if (secondLevel.length <= 3 && labels.last().length == 2) 3 else 2
    return labels.takeLast(keep).joinToString(".")
}

/** Hosting suffixes where every subdomain is someone else's site (a login is never shared across them). */
private val SHARED_SUFFIXES = listOf(
    "github.io", "gitlab.io", "pages.dev", "workers.dev", "netlify.app", "vercel.app", "web.app",
    "firebaseapp.com", "herokuapp.com", "blogspot.com", "glitch.me", "neocities.org", "codeberg.page",
)
