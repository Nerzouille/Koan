package dev.nerzouille.koan.engine

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/**
 * Back on the first page of a tab another app opened: that tab is closed, and we go back to that
 * app ([Opener.App.referrer], when Android said which), not just to whatever task lies under Koan.
 * Its own launch intent brings its task back as it was. Except when it only relayed the link:
 * an app that is the verified handler of the link's own site (the GitHub app for a github.com link
 * from Discord) can't show it and forwards it; the app the link really came from is unknown, so
 * Koan just steps back as before.
 */
fun Activity.returnToApp(app: Opener.App) {
    val referrer = app.referrer
    val launch = referrer?.takeIf { !relays(it, app.link) }?.let { packageManager.getLaunchIntentForPackage(it) }
    if (launch != null) {
        try {
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    moveTaskToBack(true)
}

/** Whether [pkg] opens [link]'s site itself (an app link), so it only passed the link on. */
private fun Activity.relays(pkg: String, link: String?): Boolean {
    val uri = link?.let(Uri::parse)?.takeIf { it.scheme == "http" || it.scheme == "https" } ?: return false
    val view = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(pkg)
    return packageManager.queryIntentActivities(view, 0).isNotEmpty()
}
