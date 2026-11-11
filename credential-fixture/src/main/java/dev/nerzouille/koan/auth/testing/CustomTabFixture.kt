package dev.nerzouille.koan.auth.testing

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection

/** A real external client, including binding and a Custom Tabs session. */
class CustomTabFixture : Activity() {
    private var bound = false
    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            check(client.warmup(0))
            val session = requireNotNull(client.newSession(null))
            CustomTabsIntent.Builder(session).build().launchUrl(this@CustomTabFixture, Uri.parse("http://localhost:8786/"))
        }
        override fun onServiceDisconnected(name: ComponentName) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val browser = intent.getStringExtra("browser") ?: "dev.nerzouille.koan.debug"
        check(browser in setOf("dev.nerzouille.koan.debug", "dev.nerzouille.koan.staging"))
        bound = CustomTabsClient.bindCustomTabsService(this, browser, connection)
        check(bound) { "Browser did not expose a Custom Tabs service" }
    }

    override fun onDestroy() {
        if (bound) unbindService(connection)
        super.onDestroy()
    }
}
