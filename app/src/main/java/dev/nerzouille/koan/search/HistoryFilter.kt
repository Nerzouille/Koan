package dev.nerzouille.koan.search

/**
 * Pages that are plumbing, not places: redirect pages ("Redirection", Google's SetSID hop) never
 * become history, and sign-in endpoints never become an inline completion.
 */
object HistoryFilter {
    /** A page that only sends the browser on ("Redirecting…", "302 Found"). */
    fun isRedirect(url: String, title: String): Boolean =
        REDIRECT_TITLE.matches(title.trim()) || REDIRECT_PATHS.any { it.containsMatchIn(url) }

    /** Signing in or out, an OAuth hop: fine in the history list, never typed-to. */
    fun isAuth(url: String): Boolean {
        val bare = bareUrl(url).lowercase()
        val host = bare.substringBefore('/')
        val path = bare.substring(host.length)
        return AUTH_HOSTS.any { host.startsWith(it) } || AUTH_PATHS.any { it.containsMatchIn(path) }
    }

    private val REDIRECT_TITLE = Regex(
        "(redirect(ing|ion)?|moved( permanently| temporarily)?|object moved|found|30[1278]( \\w+)*|please wait)[.…\\s]*",
        RegexOption.IGNORE_CASE,
    )

    private val REDIRECT_PATHS = listOf(
        Regex("/accounts/Set[O]?SID", RegexOption.IGNORE_CASE),
        Regex("/accounts/CheckCookie", RegexOption.IGNORE_CASE),
        Regex("^https?://(www\\.)?google\\.[a-z.]+/url\\?", RegexOption.IGNORE_CASE),
    )

    private val AUTH_HOSTS = listOf("accounts.", "login.", "auth.", "sso.", "signin.", "oauth.")

    private val AUTH_PATHS = listOf(
        Regex("/(log-?in|log-?out|sign-?in|sign-?out|signup|oauth2?|authorize|sso|saml2?|ServiceLogin)([/?#.]|$)", RegexOption.IGNORE_CASE),
        Regex("/(auth|callback)([/?#]|$)", RegexOption.IGNORE_CASE),
    )
}
