package dev.nerzouille.koan.ui.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The address field's text while editing, shared by the field (typing) and the search sheet
 * (results, the "@" picker). Inline completion: what was typed stays, the rest of the top hit's
 * address follows it selected, so the next letter or Backspace simply replaces it.
 */
@Stable
class SearchField {
    var value by mutableStateOf(TextFieldValue(""))
        private set

    /** What the owner typed, without the completion: what results are searched for. */
    var typed by mutableStateOf("")
        private set

    /** Something was typed since editing started (until then the page's address shows, nothing is searched). */
    var edited by mutableStateOf(false)
        private set

    /** Typing added to the end (not a deletion): only then is it completed. */
    private var growing = false

    /** Editing starts on [text] (the page's address), all selected. */
    fun start(text: String) {
        value = TextFieldValue(text, selection = TextRange(0, text.length))
        typed = text
        growing = false
        edited = false
    }

    /** The picker put text in ("@wiki "): the cursor goes after it. */
    fun set(text: String) {
        value = TextFieldValue(text, selection = TextRange(text.length))
        typed = text
        growing = false
        edited = true
    }

    /** The field changed; [complete] gives the full address for what was typed, if any. */
    fun onChange(new: TextFieldValue, complete: (String) -> String?) {
        if (new.text == value.text) {
            // Only the cursor moved: whatever is shown is now what was typed.
            value = new
            if (new.selection.collapsed) typed = new.text
            return
        }
        edited = true
        growing = new.text.length > typed.length && new.selection.collapsed && new.selection.end == new.text.length
        typed = new.text
        value = completed(new, complete) ?: new
    }

    /** New results arrived for what was typed: complete it now if typing is still going on. */
    fun refresh(complete: (String) -> String?) {
        if (value.text != typed || !value.selection.collapsed) return
        completed(value, complete)?.let { value = it }
    }

    private fun completed(new: TextFieldValue, complete: (String) -> String?): TextFieldValue? {
        if (!growing) return null
        val full = complete(new.text) ?: return null
        if (full.length <= new.text.length) return null
        return TextFieldValue(full, selection = TextRange(new.text.length, full.length))
    }
}
