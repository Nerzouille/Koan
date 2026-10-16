package dev.nerzouille.koan.engine;

import mozilla.components.browser.engine.gecko.GeckoEngineSession;
import mozilla.components.concept.engine.EngineSession;
import org.mozilla.geckoview.GeckoSession;

/**
 * Find in page highlights every match, not only the current one. Android Components keeps the
 * GeckoSession internal (Kotlin can't reach it); from Java its accessor is a plain public method.
 */
final class GeckoFinder {
    private GeckoFinder() {}

    static void highlightAll(EngineSession session) {
        if (!(session instanceof GeckoEngineSession)) return;
        GeckoSession gecko = ((GeckoEngineSession) session).getGeckoSession$browser_engine_gecko();
        gecko.getFinder().setDisplayFlags(GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL);
    }
}
