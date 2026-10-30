package dev.nerzouille.koan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * "Search in Koan" in other apps' text selection menu. Android starts it inside the sending app's
 * task, so it only passes the text on to Koan's own task (with the sender as referrer, so Back on
 * the results returns there) and closes, never shown.
 */
class SearchSelection : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            val koan = Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_PROCESS_TEXT)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            referrer?.host?.takeIf { it != packageName }?.let { koan.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://$it")) }
            startActivity(koan)
        }
        finish()
    }
}
