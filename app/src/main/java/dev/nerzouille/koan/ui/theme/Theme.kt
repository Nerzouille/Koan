package dev.nerzouille.koan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Visual tokens from docs/UI_SPEC.md. Material 3 is only a base; the look is custom. */
object KoanColors {
    val Base = Color(0xFF0E0E10)
    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassFallback = Color(0xEB1A1A1E) // #1A1A1E @ 92%
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.62f)
    val CardPlaceholder = Color(0xFF18181B)
    val DefaultAccent = Color(0xFF8AB4FF)
}

@Composable
fun KoanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = KoanColors.Base,
            surface = KoanColors.GlassFallback,
            primary = KoanColors.DefaultAccent,
            onBackground = KoanColors.TextPrimary,
            onSurface = KoanColors.TextPrimary,
        ),
        content = content,
    )
}
