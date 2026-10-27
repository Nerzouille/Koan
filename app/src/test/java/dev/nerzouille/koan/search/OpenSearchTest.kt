package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenSearchTest {
    @Test
    fun getDescription() {
        val xml = """<?xml version="1.0" encoding="utf-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
              <ShortName>Example Search</ShortName>
              <Url type="text/html" method="get" template="https://search.example.com/search?q={searchTerms}&amp;page={startPage?}"/>
              <Url type="application/x-suggestions+json" template="https://search.example.com/ac?q={searchTerms}"/>
            </OpenSearchDescription>"""
        assertEquals(
            EngineDraft("Example Search", "https://search.example.com/search?q=%s&page=", "https://search.example.com/ac?q=%s"),
            OpenSearch.parse(xml),
        )
    }

    @Test
    fun postDescriptionBecomesGet() {
        // A self-hosted metasearch engine set to POST describes its form fields as Params.
        val xml = """<?xml version="1.0" encoding="utf-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
              <ShortName>Meta</ShortName>
              <Url rel="results" type="text/html" method="POST" template="https://meta.example.net/search">
                <Param name="q" value="{searchTerms}"/>
                <Param name="lang" value="{language?}"/>
                <Param name="theme" value="simple"/>
              </Url>
            </OpenSearchDescription>"""
        assertEquals(EngineDraft("Meta", "https://meta.example.net/search?q=%s&theme=simple"), OpenSearch.parse(xml))
    }

    @Test
    fun httpOrUnusableDescriptionGivesNone() {
        val http = """<OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/"><ShortName>X</ShortName>
            <Url type="text/html" template="http://example.com/?q={searchTerms}"/></OpenSearchDescription>"""
        assertNull(OpenSearch.parse(http))
        val noSearchUrl = """<OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/"><ShortName>X</ShortName></OpenSearchDescription>"""
        assertNull(OpenSearch.parse(noSearchUrl))
    }
}
