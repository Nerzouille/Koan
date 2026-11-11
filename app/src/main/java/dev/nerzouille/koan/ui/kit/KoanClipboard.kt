package dev.nerzouille.koan.ui.kit

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.ui.text.AnnotatedString

/**
 * Koan's own copies ("Copy link"…). [sensitive] (Private): flagged so Android 13+ hides the text in
 * its clipboard preview; older versions ignore the flag.
 */
class KoanClipboard(private val context: Context, private val sensitive: Boolean) {
    fun setText(text: AnnotatedString) = copyText(context, "Link", text.text, sensitive)
}

/** Copies [text] under [label]; [sensitive]: hidden in Android 13+'s clipboard preview. */
@SuppressLint("InlinedApi")
fun copyText(context: Context, label: String, text: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, text)
    if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
