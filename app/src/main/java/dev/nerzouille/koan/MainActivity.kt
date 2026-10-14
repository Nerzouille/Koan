package dev.nerzouille.koan

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.ui.KoanRoot
import dev.nerzouille.koan.ui.RefreshBooster
import dev.nerzouille.koan.ui.theme.KoanTheme

class MainActivity : FragmentActivity() {
    private val refresh = RefreshBooster(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val components = (application as KoanApp).components
        if (components.store.state.tabs.isEmpty()) {
            components.tabsUseCases.addTab("about:blank", selectTab = true)
        }
        IconStore.initialize(this)
        components.spaces.ensureLocalSpace()
        components.sync.start()

        setContent {
            KoanTheme {
                KoanRoot(components)
            }
        }
    }

    // High refresh only while the user interacts (and the animations that follow); see RefreshBooster.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        refresh.onInput()
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        refresh.onInput()
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        (application as KoanApp).components.sync.onForeground()
    }

    override fun onStop() {
        (application as KoanApp).components.sync.onBackground()
        super.onStop()
    }

    override fun onPause() {
        refresh.stop()
        super.onPause()
    }
}
