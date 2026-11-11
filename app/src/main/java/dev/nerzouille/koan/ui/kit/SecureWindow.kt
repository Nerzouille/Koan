package dev.nerzouille.koan.ui.kit

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** How many screens currently ask for [SecureWindow] (pages stack: the flag goes with the last one). */
private var secureCount = 0

/**
 * While composed: no screenshots, no screen recording, a blank preview in Recents (FLAG_SECURE).
 * For pages showing secrets (Passwords). Main thread only, like composition.
 */
@Composable
fun SecureWindow() {
    val window = (LocalContext.current as? Activity)?.window ?: return
    DisposableEffect(window) {
        if (secureCount++ == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (--secureCount == 0) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
