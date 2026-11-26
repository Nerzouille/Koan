package dev.nerzouille.koan.ui.browser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dev.nerzouille.koan.ui.overview.StackLab

/** How pull to refresh looks: the current circle, or one of the three prototypes (owner, 2026-11-26). */
enum class PullStyle(val short: String) {
    /** The small circle under the top edge: an arc while pulling, the brand spinner while reloading. */
    Current("Current"),

    /** The page slides down over soft pink, violet and blue blobs, a white K in them. */
    Arc("Arc"),

    /** The page slides down over Koan's base; pink and blue glows rise from its edge, brighter as you pull. */
    Haze("Haze"),
    ;

    /** The page itself moves (the prototypes), rather than a circle over it. */
    val slides: Boolean get() = this != Current
}

/**
 * The pull-to-refresh look: [PullStyle.Current] in every build; dev, debug and staging builds (never a
 * release build) can switch in Settings › Debug › Pull to refresh, kept in the app's preferences.
 */
object PullLab {
    var style by mutableStateOf(PullStyle.Current)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        if (!StackLab.available || prefs != null) return
        val p = context.getSharedPreferences("stack_lab", Context.MODE_PRIVATE)
        prefs = p
        style = PullStyle.entries.firstOrNull { it.name == p.getString("pull", null) } ?: PullStyle.Current
    }

    fun select(s: PullStyle) {
        if (!StackLab.available) return
        style = s
        prefs?.edit { putString("pull", s.name) }
    }
}
