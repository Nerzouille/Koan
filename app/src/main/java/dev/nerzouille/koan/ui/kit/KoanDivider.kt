package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.theme.KoanTheme

/** A 1dp hairline between rows, starting [inset] from the left (under the rows' text). */
@Composable
fun KoanDivider(inset: Dp = 0.dp, thickness: Dp = 1.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(thickness)
            .background(KoanTheme.colors.CardBorder),
    )
}
