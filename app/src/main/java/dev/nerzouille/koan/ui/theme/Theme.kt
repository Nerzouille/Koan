package dev.nerzouille.koan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

private val LocalKoanPalette = staticCompositionLocalOf { KoanPalette.Dark }

@Composable
fun KoanTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) KoanPalette.Dark else KoanPalette.Light
    val scheme = if (dark) {
        darkColorScheme(
            background = palette.Base,
            surface = palette.Card,
            primary = palette.DefaultAccent,
            onBackground = palette.TextPrimary,
            onSurface = palette.TextPrimary,
        )
    } else {
        lightColorScheme(
            background = palette.Base,
            surface = palette.Card,
            primary = palette.DefaultAccent,
            onBackground = palette.TextPrimary,
            onSurface = palette.TextPrimary,
        )
    }
    CompositionLocalProvider(LocalKoanPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object KoanTheme {
    /** The current theme's colours. */
    val colors: KoanPalette
        @Composable @ReadOnlyComposable
        get() = LocalKoanPalette.current
}

/**
 * A space's colour as a small accent (docs/UI_SPEC.md "Visual tokens"): the active space pill,
 * the current-tab dot, the cursor and focus ring, the loading bar, and (light) the background wash.
 * Derived from the Zen theme gradient; hue kept, pulled into a range that reads on [KoanPalette.Base].
 */
@Immutable
data class SpaceColors(
    val accent: Color,
    /** Text on the pill tint. */
    val ink: Color,
    val pill: Color,
    val washTop: Color,
    val washMid: Color,
    /** What back cards fade towards in light: the wash colour where the pile sits. */
    val haze: Color,
    /** Loading bar gradient, always three stops. */
    val progress: List<Color>,
) {
    companion object {
        fun of(gradient: List<Color>, palette: KoanPalette): SpaceColors {
            val accent = gradient.firstOrNull()?.let { normalise(it, palette.AccentLightness) } ?: palette.DefaultAccent
            val ink = withLightness(accent, palette.InkLightness)
            val progress = gradient.mapNotNull { normalise(it, palette.AccentLightness) }.let {
                when {
                    it.size >= 3 -> it.take(3)
                    it.size == 2 -> listOf(it[0], lerp(it[0], it[1], 0.5f), it[1])
                    else -> WashTokens.DefaultProgress
                }
            }
            val washMid = lerp(palette.Base, accent, palette.WashMid)
            return SpaceColors(
                accent = accent,
                ink = ink,
                pill = accent.copy(alpha = palette.PillAlpha),
                washTop = lerp(palette.Base, accent, palette.WashTop),
                washMid = washMid,
                haze = washMid,
                progress = progress,
            )
        }

        /** Null for a grey colour: it carries no hue worth showing. */
        private fun normalise(c: Color, lightness: ClosedFloatingPointRange<Float>): Color? {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(c.toArgb(), hsl)
            if (hsl[1] < WashTokens.MIN_SATURATION) return null
            hsl[1] = hsl[1].coerceAtLeast(WashTokens.ACCENT_SATURATION)
            hsl[2] = hsl[2].coerceIn(lightness)
            return Color(ColorUtils.HSLToColor(hsl))
        }

        private fun withLightness(c: Color, l: Float): Color {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(c.toArgb(), hsl)
            hsl[2] = l
            return Color(ColorUtils.HSLToColor(hsl))
        }
    }
}
