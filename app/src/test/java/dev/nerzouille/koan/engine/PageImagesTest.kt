package dev.nerzouille.koan.engine

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageImagesTest {
    @Test
    fun aWebAddressIsTheFullImage() =
        assertEquals("https://example.com/full.jpg", fullImageUrl(JSONObject().put("id", 1).put("url", "https://example.com/full.jpg")))

    @Test
    fun aReasonOrAnythingElseIsNone() {
        assertNull(fullImageUrl(JSONObject().put("id", 1).put("reason", "not-loaded").put("images", 2)))
        assertNull(fullImageUrl(JSONObject().put("url", "data:image/png;base64,AAAA")))
        assertNull(fullImageUrl(JSONObject().put("url", JSONObject.NULL)))
    }
}
