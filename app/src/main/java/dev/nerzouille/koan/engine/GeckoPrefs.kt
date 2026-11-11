package dev.nerzouille.koan.engine

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.nerzouille.koan.ui.theme.KoanPalette
import java.io.File
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference

/**
 * Gecko preferences Koan sets: find in page in Koan's colours (KoanPalette FindCurrent /
 * FindMatch) instead of Gecko's green, following Koan's theme while the engine runs.
 */
object GeckoPrefs {
    private const val TAG = "GeckoPrefs"

    /**
     * The four find prefs for Koan's theme, each under both of Gecko's names: "ui.<colour>" is used
     * for pages in a light colour scheme, "ui.<colour>.dark" for pages in a dark one. Both get the
     * theme's value, so a light-only page in dark Koan shows the dark colours too, and both text
     * colours are set, so Gecko never picks its own.
     */
    fun findPrefs(dark: Boolean): Map<String, String> {
        val p = if (dark) KoanPalette.Dark else KoanPalette.Light
        val values = mapOf(
            "ui.textSelectAttentionBackground" to p.FindCurrent,
            "ui.textSelectAttentionForeground" to p.FindCurrentText,
            "ui.textHighlightBackground" to p.FindMatch,
            "ui.textHighlightForeground" to p.FindMatchText,
        )
        return buildMap {
            for ((name, color) in values) {
                put(name, hex(color))
                put("$name.dark", hex(color))
            }
        }
    }

    /** Sets the find prefs for [dark] on the running engine (Gecko repaints with them). */
    fun applyFind(dark: Boolean) {
        val prefs = findPrefs(dark).map { (name, value) ->
            SetGeckoPreference.setStringPref(name, value, GeckoPreferenceController.PREF_BRANCH_USER)
        }
        GeckoPreferenceController.setGeckoPrefs(prefs).accept(
            { result -> result?.filterValues { !it }?.keys?.takeIf { it.isNotEmpty() }?.let { Log.w(TAG, "Not set: $it") } },
            { e -> Log.w(TAG, "Find colours not set", e) },
        )
    }

    /** The config file earlier versions passed to GeckoView: no longer used. */
    fun removeOldConfig(context: Context) {
        File(context.filesDir, "geckoview-config.yaml").delete()
    }

    /** "#RRGGBB": the find colours are opaque. */
    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)
}
