package dev.nerzouille.koan

import android.app.Application
import dev.nerzouille.koan.engine.Components

class KoanApp : Application() {
    /**
     * Lazily created so GeckoView's child processes (which also run Application.onCreate)
     * never spin up a second runtime.
     */
    val components by lazy { Components(this) }
}
