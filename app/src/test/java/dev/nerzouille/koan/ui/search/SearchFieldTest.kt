package dev.nerzouille.koan.ui.search

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFieldTest {
    private val complete: (String) -> String? = { t -> "localhost:8765/sunset.html".takeIf { it.startsWith(t) && t.isNotEmpty() } }

    private fun SearchField.type(text: String) {
        for (c in text) {
            val t = value.text + c
            onChange(TextFieldValue(t, TextRange(t.length)), complete)
        }
    }

    private fun fresh() = SearchField().apply { start("") }

    @Test fun `typing fast keeps every letter, the completion is never in the text`() {
        val f = fresh()
        f.type("localhost")
        assertEquals("localhost", f.value.text)
        assertEquals("localhost", f.typed)
        assertEquals(":8765/sunset.html", f.completion)
        assertEquals("localhost:8765/sunset.html", f.text)
    }

    @Test fun `a key computed on the text before the completion showed still lands`() {
        // The keyboard's next letter, from the field as it was: the text never changed under it.
        val f = fresh()
        f.type("lo")
        f.onChange(TextFieldValue("loc", TextRange(3)), complete)
        assertEquals("loc", f.value.text)
        assertEquals("alhost:8765/sunset.html", f.completion)
    }

    @Test fun `backspace takes the completion away first, then letters`() {
        val f = fresh()
        f.type("local")
        f.onChange(TextFieldValue("loca", TextRange(4)), complete)
        assertEquals("local", f.value.text)
        assertEquals("", f.completion)
        f.onChange(TextFieldValue("loca", TextRange(4)), complete)
        assertEquals("loca", f.value.text)
        // Deleting doesn't complete again.
        assertEquals("", f.completion)
    }

    @Test fun `moving the cursor takes the completion in, the composing region doesn't`() {
        val f = fresh()
        f.type("loc")
        // Same text and cursor, only the keyboard's composing region: nothing changes.
        f.onChange(TextFieldValue("loc", TextRange(3), composition = TextRange(0, 3)), complete)
        assertEquals("alhost:8765/sunset.html", f.completion)
        // The cursor moved: the completion becomes text.
        f.onChange(TextFieldValue("loc", TextRange(1)), complete)
        assertEquals("localhost:8765/sunset.html", f.value.text)
        assertEquals(TextRange(26), f.value.selection)
        assertEquals("", f.completion)
    }

    @Test fun `results arriving later complete what is typed`() {
        val f = fresh()
        var top: String? = null
        f.onChange(TextFieldValue("su", TextRange(2))) { top }
        assertEquals("", f.completion)
        top = "sunset.example"
        f.refresh { t -> top?.takeIf { it.startsWith(t) } }
        assertEquals("nset.example", f.completion)
        assertEquals("su", f.value.text)
    }

    @Test fun `start and set clear the completion`() {
        val f = fresh()
        f.type("loc")
        f.set("@wiki ")
        assertEquals("", f.completion)
        assertEquals("@wiki ", f.text)
        f.start("https://example.com/")
        assertEquals(TextRange(0, 20), f.value.selection)
        assertEquals("", f.completion)
    }

    @Test fun `an "@" typed last time is not this editing's until the field starts again`() {
        val f = fresh()
        f.type("@")
        assertEquals(true, f.current)
        f.end()
        // Kept for the sheet's fade-out, but not current: the picker can't pop in on reopening.
        assertEquals("@", f.typed)
        assertEquals(false, f.current)
        f.start("https://example.com/")
        assertEquals(true, f.current)
        assertEquals("https://example.com/", f.typed)
    }
}
