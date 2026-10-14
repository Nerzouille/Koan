package dev.nerzouille.koan

import android.app.Application
import dev.nerzouille.koan.engine.Components

class KoanApp : Application() {
    /**
     * Lazily created so GeckoView's child processes (which also run Application.onCreate)
     * never spin up a second runtime.
     */
    val components by lazy { Components(this) }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (componentsReady) components.tabs.onTrimMemory(level)
    }

    /** Set by MainActivity: only the main process ever builds the components. */
    var componentsReady = false
}
