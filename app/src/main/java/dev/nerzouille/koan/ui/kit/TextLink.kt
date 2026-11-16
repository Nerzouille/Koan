package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.TextUnit
import dev.nerzouille.koan.ui.theme.KoanTheme

/** Secondary text that acts ("Delete", "Change"): a round ripple around it; plain text without [onClick]. */
@Composable
fun TextLink(text: String, onClick: (() -> Unit)?, size: TextUnit, padding: PaddingValues) {
    Text(
        text,
        color = KoanTheme.colors.TextSecondary,
        fontSize = size,
        modifier = if (onClick == null) Modifier else Modifier.clip(CircleShape).clickable(onClick = onClick).padding(padding),
    )
}
