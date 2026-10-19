package dev.nerzouille.koan.engine

import androidx.core.app.NotificationManagerCompat
import dev.nerzouille.koan.KoanApp
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.feature.media.service.AbstractMediaSessionService
import mozilla.components.support.base.android.NotificationsDelegate

/**
 * A page playing audio or video (feature-media): the media notification and the lock screen
 * controls (play/pause, and next/previous when the page offers them), as a foreground service so
 * playback keeps going with Koan in the background (HyperOS kills background apps otherwise).
 */
class KoanMediaService : AbstractMediaSessionService() {
    override val store: BrowserStore get() = (application as KoanApp).components.store
    override val crashReporter: CrashReporting? = null
    override val notificationsDelegate: NotificationsDelegate by lazy { NotificationsDelegate(NotificationManagerCompat.from(this)) }
}
