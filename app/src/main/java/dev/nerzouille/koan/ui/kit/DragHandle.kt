package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** The grab handle at the top of a sheet and of the space drawer: a short pill, 8dp from the top. */
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = SheetTokens.HandleTop)
            .size(BarTokens.HandleWidth, BarTokens.HandleHeight)
            .clip(CircleShape)
            .background(KoanTheme.colors.DrawerHandle),
    )
}
