package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Takes every touch that lands here, so nothing reaches what lies under it (a panel's empty parts). */
fun Modifier.blockTouches(): Modifier = pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
