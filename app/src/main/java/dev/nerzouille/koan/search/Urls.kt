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

/** "accounts.example.co.uk" → "example.co.uk", roughly: the last two labels (three for short ccSLDs). */
fun baseDomain(host: String): String {
    val labels = host.removePrefix("www.").split('.')
    if (labels.size <= 2) return labels.joinToString(".")
    val secondLevel = labels[labels.size - 2]
    val keep = if (secondLevel.length <= 3 && labels.last().length == 2) 3 else 2
    return labels.takeLast(keep).joinToString(".")
}
