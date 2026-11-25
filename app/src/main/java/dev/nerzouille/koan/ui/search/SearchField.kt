package dev.nerzouille.koan.ui.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The address field's text while editing, shared by the field (typing) and the search sheet
 * (results, the "@" picker). Inline completion: the rest of the top hit's address shows after what
 * was typed, highlighted, but it is drawn, not in the field's text ([completion]). Replacing the
 * text while keys were still arriving lost letters when typing fast ("localhost" became
 * "localhst"): the next key landed on the field's older text. So the text is always exactly what
 * was typed; Go takes [text] (typed + completion), Backspace first takes the completion away, and
 * moving the cursor takes it in.
 */
@Stable
class SearchField {
    /** What the field holds: what was typed, and the cursor. Set again even when equal (a refused Backspace). */
    var value by mutableStateOf(TextFieldValue(""), neverEqualPolicy())
        private set

    /** What the owner typed, without the completion: what results are searched for. */
    var typed by mutableStateOf("")
        private set

    /** Something was typed since editing started (until then the page's address shows, nothing is searched). */
    var edited by mutableStateOf(false)
        private set

    /** The rest of the top hit's address after what was typed, drawn after it; "" for none. */
    var completion by mutableStateOf("")
        private set

    /** What Go opens: the text and the completion shown after it. */
    val text: String get() = value.text + completion

    /**
     * Between [start] and [end]: the text is this editing's. After [end] it is the last one's,
     * still shown while the sheet fades out, never when the field opens again.
     */
    var current by mutableStateOf(false)
        private set

    /** Typing added to the end (not a deletion): only then is it completed. */
    private var growing = false

    /** Editing starts on [text] (the page's address), all selected. */
    fun start(text: String) {
        value = TextFieldValue(text, selection = TextRange(0, text.length))
        typed = text
        growing = false
        edited = false
        completion = ""
        current = true
    }

    /** The field is gone (its closing morph done): what it held belongs to the last editing. */
    fun end() {
        current = false
    }

    /** The picker put text in ("@wiki "): the cursor goes after it. */
    fun set(text: String) {
        value = TextFieldValue(text, selection = TextRange(text.length))
        typed = text
        growing = false
        edited = true
        completion = ""
    }

    /** The field changed; [complete] gives the full address for what was typed, if any. */
    fun onChange(new: TextFieldValue, complete: (String) -> String?) {
        val old = value
        when {
            new.text == old.text -> cursorMoved(new)
            // Backspace (or a cut) with a completion showing: only the completion goes.
            completion.isNotEmpty() && isBackspace(old, new) -> {
                completion = ""
                growing = false
                value = old
            }
            else -> {
                edited = true
                growing = new.text.length > typed.length && new.selection.collapsed && new.selection.end == new.text.length
                typed = new.text
                value = new
                completion = suggestion(new.text, complete)
            }
        }
    }

    private fun isBackspace(old: TextFieldValue, new: TextFieldValue) =
        new.selection.collapsed && old.text.startsWith(new.text) && new.text.length == old.text.length - 1

    private fun cursorMoved(new: TextFieldValue) {
        // The keyboard also sends its composing region with the same text and cursor: not a move.
        if (completion.isNotEmpty() && new.selection.collapsed && new.selection != value.selection) {
            accept()
            return
        }
        value = new
        if (new.selection.collapsed) typed = new.text
    }

    /** New results arrived for what was typed: complete it now if typing is still going on. */
    fun refresh(complete: (String) -> String?) {
        if (value.text != typed || !value.selection.collapsed) return
        completion = suggestion(value.text, complete)
    }

    /** The completion becomes text, the cursor at its end (tapping or moving the cursor). */
    fun accept() {
        val full = text
        value = TextFieldValue(full, selection = TextRange(full.length))
        typed = full
        completion = ""
        growing = false
    }

    private fun suggestion(text: String, complete: (String) -> String?): String {
        val full = complete(text)?.takeIf { growing && it.length > text.length && it.startsWith(text, ignoreCase = true) }
        return full?.substring(text.length).orEmpty()
    }
}
