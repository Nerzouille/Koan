package dev.nerzouille.koan.ui.browser

import android.content.Context
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillManager
import android.widget.FrameLayout
import java.lang.ref.WeakReference
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import org.mozilla.geckoview.GeckoView

/** The page's autofill boundary. GeckoView may attach either kind of tab to the same view. */
internal class SystemAutofillView private constructor(
    context: Context,
    val engineView: GeckoEngineView,
) : FrameLayout(context), EngineView by engineView {
    constructor(context: Context) : this(context, GeckoEngineView(context))

    private val gecko get() = engineView.getChildAt(0) as? GeckoView
    private val allowed get() = enabledForRegularTabs && gecko?.session?.settings?.usePrivateMode == false
    private var lastSession = WeakReference<EngineSession>(null)

    var enabledForRegularTabs = false
        set(value) {
            if (field == value) return
            field = value
            updateAutofill(allowed)
            if (!value) cancelAutofill()
        }

    init {
        addView(engineView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        updateAutofill(false)
    }

    override fun asView(): View = this

    override fun render(session: EngineSession) {
        val previous = lastSession.get()
        if (previous !== session) {
            updateAutofill(false)
            if (enabledForRegularTabs && previous != null) cancelAutofill()
        }
        engineView.render(session)
        lastSession = WeakReference(session)
        updateAutofill(allowed)
    }

    override fun release() {
        updateAutofill(false)
        // The presenter also releases on stop, including while the provider unlocks in its own Activity.
        engineView.release()
    }

    override fun dispatchProvideAutofillStructure(structure: ViewStructure, flags: Int) {
        // An explicit Android request can include views marked unimportant.
        if (allowed) super.dispatchProvideAutofillStructure(structure, flags)
    }

    private fun updateAutofill(allow: Boolean) {
        importantForAutofill = if (allow) IMPORTANT_FOR_AUTOFILL_AUTO else IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        gecko?.let { if (it.autofillEnabled != allow) it.autofillEnabled = allow }
    }

    private fun cancelAutofill() {
        context.getSystemService(AutofillManager::class.java)?.cancel()
    }
}
