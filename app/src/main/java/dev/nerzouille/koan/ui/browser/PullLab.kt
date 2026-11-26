package dev.nerzouille.koan.ui.browser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dev.nerzouille.koan.ui.overview.StackLab

/** How pull to refresh looks: Arc (owner, 2026-11-26), or the old circle as a fallback in Debug. */
enum class PullStyle(val short: String) {
    /** The small circle under the top edge: an arc while pulling, the brand spinner while reloading. */
    Current("Current"),

    /** The page slides down over soft pink, violet and blue blobs, a white K in them. */
    Arc("Arc"),
    ;

    /** The page itself moves (the prototypes), rather than a circle over it. */
    val slides: Boolean get() = this != Current
}

/**
 * The pull-to-refresh look: [PullStyle.Arc] in every build; dev, debug and staging builds (never a
 * release build) can fall back to Current in Settings › Debug › Pull to refresh, kept in the app's
 * preferences.
 */
object PullLab {
    var style by mutableStateOf(PullStyle.Arc)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        if (!StackLab.available || prefs != null) return
        val p = context.getSharedPreferences("stack_lab", Context.MODE_PRIVATE)
        prefs = p
        style = PullStyle.entries.firstOrNull { it.name == p.getString("pull", null) } ?: PullStyle.Arc
    }

    fun select(s: PullStyle) {
        if (!StackLab.available) return
        style = s
        prefs?.edit { putString("pull", s.name) }
    }
}
