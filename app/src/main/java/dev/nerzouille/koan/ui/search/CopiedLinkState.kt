package dev.nerzouille.koan.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * The link to offer while the search field is open ([editing]), checked once each time it opens
 * (and again when the window gets focus: Android hides the clipboard from an app without it).
 * [current]: the open page's address, never offered.
 */
@Composable
fun rememberCopiedLink(editing: Boolean, current: String?): CopiedLink? {
    val context = LocalContext.current
    val links = remember { CopiedLinks(context.applicationContext) }
    val focused = LocalWindowInfo.current.isWindowFocused
    var link by remember { mutableStateOf<CopiedLink?>(null) }
    val page by rememberUpdatedState(current)
    LaunchedEffect(editing, focused) {
        if (!editing) link = null else if (focused && link == null) link = links.check(page)
    }
    return link
}
