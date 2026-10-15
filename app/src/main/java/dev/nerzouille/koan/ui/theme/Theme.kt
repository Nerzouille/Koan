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
            primary = palette.Primary,
            onBackground = palette.TextPrimary,
            onSurface = palette.TextPrimary,
        )
    } else {
        lightColorScheme(
            background = palette.Base,
            surface = palette.Card,
            primary = palette.Primary,
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
 * A space's colour as a tiny space indicator (docs/UI_SPEC.md "Themes"): the active space pill in
 * the switcher and its name. Derived from the Zen theme gradient; hue kept, pulled into a range
 * that reads on [KoanPalette.Base]. Everything else is neutral or [BrandTokens].
 */
@Immutable
data class SpaceColors(
    val accent: Color,
    /** Text on the pill tint. */
    val ink: Color,
    val pill: Color,
) {
    companion object {
        fun of(gradient: List<Color>, palette: KoanPalette): SpaceColors {
            val accent = gradient.firstOrNull()?.let { normalise(it, palette.AccentLightness) } ?: palette.DefaultAccent
            return SpaceColors(
                accent = accent,
                ink = withLightness(accent, palette.InkLightness),
                pill = accent.copy(alpha = palette.PillAlpha),
            )
        }

        /** Null for a grey colour: it carries no hue worth showing. */
        private fun normalise(c: Color, lightness: ClosedFloatingPointRange<Float>): Color? {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(c.toArgb(), hsl)
            if (hsl[1] < SpaceColorTokens.MIN_SATURATION) return null
            hsl[1] = hsl[1].coerceAtLeast(SpaceColorTokens.ACCENT_SATURATION)
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
