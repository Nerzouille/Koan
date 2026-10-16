package dev.nerzouille.koan.engine

import android.content.Context
import dev.nerzouille.koan.ui.theme.BrandTokens
import java.io.File

/**
 * Gecko preferences Koan sets at every start, through GeckoView's config file (read before the
 * engine starts, in release builds too since the path is given explicitly).
 */
object GeckoPrefs {
    /**
     * Find in page in Koan's blue (the brand gradient's blue end) instead of Gecko's green: every
     * match softly (translucent, so it reads on light and dark pages alike; see GeckoFinder), the
     * current one strongly.
     */
    private fun prefs(): Map<String, String> {
        val blue = BrandTokens.Blue
        val (r, g, b) = listOf(blue.red, blue.green, blue.blue).map { (it * 255).toInt() }
        return mapOf(
            "ui.textHighlightBackground" to "rgba($r, $g, $b, 0.32)",
            "ui.textSelectAttentionBackground" to "rgb($r, $g, $b)",
        )
    }

    fun write(context: Context): File {
        val file = File(context.filesDir, "geckoview-config.yaml")
        val yaml = buildString {
            appendLine("prefs:")
            prefs().forEach { (name, value) -> appendLine("  $name: \"$value\"") }
        }
        if (!file.exists() || file.readText() != yaml) file.writeText(yaml)
        return file
    }
}
