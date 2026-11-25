package dev.nerzouille.koan.engine

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class WebTracksTest {
    @Test fun `the extension's track list becomes Koan's`() {
        val a = JSONArray("""[{"kind":"subtitles","label":" English ","lang":"en"},{"kind":"captions","label":"","lang":""},"junk"]""")
        assertEquals(
            listOf(WebTrack("subtitles", "English", "en"), WebTrack("captions", "", "")),
            parseTracks(a),
        )
    }
}

class WebFilesTest {
    @Test fun `the extension's file list keeps http files only`() {
        val a = org.json.JSONArray("""[{"src":"https://example.com/a.webm","height":480,"type":"video/webm","original":false},{"src":"blob:x","height":1},{"src":"http://example.com/o.webm","original":true}]""")
        assertEquals(
            listOf(WebFile("https://example.com/a.webm", 480, "video/webm", false), WebFile("http://example.com/o.webm", 0, "", true)),
            parseFiles(a),
        )
    }
}
