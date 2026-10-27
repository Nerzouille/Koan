package dev.nerzouille.koan.search

import org.w3c.dom.Element
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

/** An engine as a page describes it, to prefill "Add a search engine". */
data class EngineDraft(val name: String, val url: String, val suggest: String = "", val shortcut: String = "")

/**
 * A page's OpenSearch description (`<link rel="search" type="application/opensearchdescription+xml">`),
 * read as an engine Koan can add. The page hands it over itself (engine/PageEngines).
 */
object OpenSearch {
    private val optionalParam = Regex("\\{[^}]*\\?\\}")

    /**
     * The description's name, search and suggestions urls ("{searchTerms}" → "%s"). A POST url
     * becomes a GET one with its parameters in the query; https only.
     */
    internal fun parse(xml: String): EngineDraft? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val name = doc.getElementsByTagNameNS("*", "ShortName").item(0)?.textContent?.trim().orEmpty()
        val urls = doc.getElementsByTagNameNS("*", "Url").let { list -> (0 until list.length).map { list.item(it) as Element } }
        fun template(type: String) = urls.firstOrNull { it.getAttribute("type").equals(type, ignoreCase = true) }?.let(::template)
        val search = template("text/html")?.takeIf { it.startsWith("https://") } ?: return null
        val suggest = template("application/x-suggestions+json")?.takeIf { it.startsWith("https://") }.orEmpty()
        return EngineDraft(name.ifEmpty { origin(search).substringAfter("://") }, search, suggest)
    }

    private fun template(url: Element): String? {
        var t = url.getAttribute("template").trim().ifEmpty { return null }
        val params = url.getElementsByTagNameNS("*", "Param").let { list -> (0 until list.length).map { list.item(it) as Element } }
            // "{language?}": optional, left out; any other "{…}" but the search words: not supported.
            .filterNot { optionalParam.matches(it.getAttribute("value")) }
        if (params.any { it.getAttribute("value").startsWith("{") && it.getAttribute("value") != "{searchTerms}" }) return null
        if (params.isNotEmpty()) {
            val query = params.joinToString("&") { p ->
                val value = p.getAttribute("value")
                URLEncoder.encode(p.getAttribute("name"), "UTF-8") + "=" +
                    if (value == "{searchTerms}") value else URLEncoder.encode(value, "UTF-8")
            }
            t += (if ('?' in t) "&" else "?") + query
        }
        t = t.replace("{searchTerms}", "%s").replace(optionalParam, "")
        return t.takeIf { "%s" in it && '{' !in it }
    }
}
