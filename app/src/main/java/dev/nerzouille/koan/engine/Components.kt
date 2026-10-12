package dev.nerzouille.koan.engine

import android.content.Context
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/** Process-wide engine objects. One GeckoRuntime per process. */
class Components(context: Context) {
    private val appContext = context.applicationContext

    val runtime: GeckoRuntime by lazy {
        GeckoRuntime.create(appContext, GeckoRuntimeSettings.Builder().build())
    }

    val engine by lazy {
        GeckoEngine(
            appContext,
            DefaultSettings(
                trackingProtectionPolicy = TrackingProtectionPolicy.recommended(),
                preferredColorScheme = PreferredColorScheme.System,
            ),
            runtime,
        )
    }

    val store by lazy { BrowserStore(middleware = EngineMiddleware.create(engine)) }

    val sessionUseCases by lazy { SessionUseCases(store) }
    val tabsUseCases by lazy { TabsUseCases(store) }
}
