package dev.nerzouille.koan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
