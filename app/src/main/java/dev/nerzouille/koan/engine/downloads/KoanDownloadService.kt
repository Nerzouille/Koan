package dev.nerzouille.koan.engine.downloads

import androidx.core.app.NotificationManagerCompat
import dev.nerzouille.koan.KoanApp
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.DefaultFileSizeFormatter
import mozilla.components.feature.downloads.DefaultPackageNameProvider
import mozilla.components.feature.downloads.DownloadEstimator
import mozilla.components.feature.downloads.filewriter.DefaultDownloadFileWriter
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.utils.DefaultDateTimeProvider

/**
 * Downloads run here, in a foreground service, so they finish with Koan in the background. The
 * engine's own HTTP client keeps the page's cookies; files go where Settings says (Components).
 */
class KoanDownloadService : AbstractFetchDownloadService() {
    private val components get() = (application as KoanApp).components

    override val store get() = components.store
    override val httpClient by lazy { GeckoViewFetchClient(applicationContext, components.runtime) }
    override val packageNameProvider by lazy { DefaultPackageNameProvider(applicationContext) }
    override val notificationsDelegate by lazy { NotificationsDelegate(NotificationManagerCompat.from(applicationContext)) }
    override val fileSizeFormatter by lazy { DefaultFileSizeFormatter(applicationContext) }
    override val downloadEstimator by lazy { DownloadEstimator(DefaultDateTimeProvider()) }
    override val downloadFileUtils get() = components.downloadFiles
    override val downloadFileWriter by lazy { DefaultDownloadFileWriter(applicationContext, components.downloadFiles) }
}
