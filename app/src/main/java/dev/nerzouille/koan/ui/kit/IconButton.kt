package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

/**
 * A glyph on a round touch target ([touch], glyph [glyph]). [enabled] false: [disabledTint], no
 * click; [interactive] false: normal tint, no click (a sheet closing). [alignment]: where the
 * glyph sits (page bars put the back arrow at the start).
 */
@Composable
fun KoanIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    touch: Dp,
    glyph: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactive: Boolean = true,
    disabledTint: Color = tint,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(touch)
            .clip(CircleShape)
            .then(if (enabled && interactive) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = alignment,
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else disabledTint, modifier = Modifier.size(glyph))
    }
}
