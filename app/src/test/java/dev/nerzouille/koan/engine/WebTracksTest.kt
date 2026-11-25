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
