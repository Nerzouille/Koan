package dev.nerzouille.koan.engine

/** Gecko's answer to one find in page, for the tab [tabId]. [current] is 1-based, 0 when unknown. */
data class FindHit(val tabId: String, val query: String, val found: Boolean, val current: Int, val total: Int)

/**
 * The counter in the find bar for [query] in [tabId]: "3/12", "0/0" when nothing matches, empty
 * before an answer for this query (an older query's late answer doesn't count) and when Gecko
 * found the text but couldn't count it (its counter skips some text the find reaches, such as
 * collapsed sections: "0/0" there was wrong while a match was highlighted).
 */
fun findCount(tabId: String, query: String, hit: FindHit?): String {
    if (query.isEmpty() || hit == null || hit.tabId != tabId || hit.query != query) return ""
    if (!hit.found) return "0/0"
    if (hit.total <= 0 || hit.current <= 0) return ""
    return "${hit.current.coerceAtMost(hit.total)}/${hit.total}"
}
