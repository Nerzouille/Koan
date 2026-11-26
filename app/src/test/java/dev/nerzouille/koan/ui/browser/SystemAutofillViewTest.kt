package dev.nerzouille.koan.ui.browser

import android.app.Activity
import android.view.View
import mozilla.components.browser.engine.gecko.GeckoEngineSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.gecko.GeckoAppShell
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoView
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class SystemAutofillViewTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).setup()
    private val view = SystemAutofillView(activity.get())
    private val gecko = view.engineView.getChildAt(0) as GeckoView
    init {
        GeckoAppShell.setApplicationContext(activity.get().applicationContext)
    }
    private val regular = GeckoEngineSession(runtime, privateMode = false, openGeckoSession = false)
    private val privateTab = GeckoEngineSession(runtime, privateMode = true, openGeckoSession = false)

    @After fun close() {
        view.release()
        regular.close()
        privateTab.close()
        activity.close()
    }

    @Test fun optInCanBeChangedWithoutReloadingARegularTab() {
        view.render(regular)
        assertBlocked()

        view.enabledForRegularTabs = true
        assertTrue(gecko.autofillEnabled)

        view.enabledForRegularTabs = false
        assertBlocked()
    }

    @Test fun movingBetweenPrivateAndRegularTabsUpdatesTheReusedView() {
        view.enabledForRegularTabs = true
        view.render(regular)
        assertTrue(gecko.autofillEnabled)

        view.render(privateTab)
        assertBlocked()
        view.enabledForRegularTabs = false
        view.enabledForRegularTabs = true
        assertBlocked()

        view.render(regular)
        assertTrue(gecko.autofillEnabled)
    }

    @Test fun releasingTheViewClosesTheAutofillBoundaryUntilARender() {
        view.enabledForRegularTabs = true
        view.render(regular)
        view.release()
        assertBlocked()

        view.render(regular)
        assertTrue(gecko.autofillEnabled)
    }

    private fun assertBlocked() {
        assertFalse(gecko.autofillEnabled)
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS, view.importantForAutofill)
    }

    private companion object {
        // The view can attach unopened sessions without starting the native engine.
        val runtime: GeckoRuntime by lazy { ReflectionHelpers.callConstructor(GeckoRuntime::class.java) }
    }
}
