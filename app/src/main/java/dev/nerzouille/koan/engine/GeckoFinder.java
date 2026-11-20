package dev.nerzouille.koan.engine;

import androidx.annotation.Nullable;
import mozilla.components.browser.engine.gecko.GeckoEngineSession;
import mozilla.components.concept.engine.EngineSession;
import org.mozilla.geckoview.GeckoSession;

/**
 * Find in page, straight on GeckoView's finder: it highlights every match, not only the current
 * one, and its result says whether the text was found at all (Android Components passes only the
 * count, and Gecko's count can be 0 for a match it found and highlighted). Android Components
 * keeps the GeckoSession internal (Kotlin can't reach it); from Java its accessor is a plain
 * public method.
 */
final class GeckoFinder {
    private GeckoFinder() {}

    interface Result {
        void onResult(String searchString, boolean found, int current, int total);
    }

    /**
     * Finds [text] (null: the next / previous match of the current search) with every match
     * highlighted. False when the session isn't Gecko's: nothing was done.
     */
    static boolean find(EngineSession session, @Nullable String text, boolean backwards, Result result) {
        if (!(session instanceof GeckoEngineSession)) return false;
        GeckoSession gecko = ((GeckoEngineSession) session).getGeckoSession$browser_engine_gecko();
        gecko.getFinder().setDisplayFlags(GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL);
        int flags = backwards ? GeckoSession.FINDER_FIND_BACKWARDS : 0;
        gecko.getFinder().find(text, flags).accept(r -> {
            if (r != null) result.onResult(r.searchString, r.found, r.current, r.total);
        });
        return true;
    }
}
