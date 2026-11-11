package dev.nerzouille.koan.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * "Hide status bar" (Settings > Display): the page and the overview take the status bar's row,
 * right up to the camera. Everything else (sheets, settings, sign-in) keeps clear of the camera
 * cutout instead, so it looks the same with or without the status bar.
 */

/** The top of the screen nothing may cover: the status bar when shown, else the camera cutout. */
val WindowInsets.Companion.topSafe: WindowInsets
    @Composable get() = statusBars.union(displayCutout).only(WindowInsetsSides.Top)

/** Padding for [WindowInsets.Companion.topSafe]. */
@Composable
fun Modifier.topSafePadding(): Modifier = windowInsetsPadding(WindowInsets.topSafe)

