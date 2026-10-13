package dev.nerzouille.koan

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.ui.KoanRoot
import dev.nerzouille.koan.ui.requestMaxRefreshRate
import dev.nerzouille.koan.ui.theme.KoanTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        requestMaxRefreshRate()

        val components = (application as KoanApp).components
        if (components.store.state.tabs.isEmpty()) {
            components.tabsUseCases.addTab("about:blank", selectTab = true)
        }
        components.spaces.seedIfEmpty()

        setContent {
            KoanTheme {
                KoanRoot(components)
            }
        }
    }
}
