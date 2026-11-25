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

/**
 * The latest find query per tab, for the empty field's second clear: Gecko highlights every match
 * asynchronously, so a highlight for a query typed just before the field was emptied can land after
 * the first clear. [PageTools] clears again a moment later, only if the field is still empty then
 * (the same [Ticket]: nothing typed since).
 */
class FindQueries {
    data class Ticket(val tabId: String, val serial: Long)

    private val latest = HashMap<String, Long>()
    private var serial = 0L

    /** Records [text] as [tabId]'s query; an empty one returns the ticket for the second clear. */
    @Synchronized fun typed(tabId: String, text: String): Ticket? {
        serial++
        latest[tabId] = serial
        return if (text.isEmpty()) Ticket(tabId, serial) else null
    }

    /** Nothing typed in that tab since [ticket]: the field is still empty, clear again. */
    @Synchronized fun stillEmpty(ticket: Ticket): Boolean = latest[ticket.tabId] == ticket.serial
}
