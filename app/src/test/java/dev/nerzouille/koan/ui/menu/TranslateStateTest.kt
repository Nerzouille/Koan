package dev.nerzouille.koan.ui.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateStateTest {
    @Test fun `a page translated again is done even if the processing flag stayed up`() {
        // Translate, Show original, Translate again: Gecko holds the pair, the flag is stale.
        assertEquals(true, translatedNow(isTranslated = false, hasPair = true))
        assertEquals(false, workingNow(isTranslated = false, hasPair = true, translating = true, restoring = false))
    }

    @Test fun `a restored page is done even if the restore flag stayed up`() {
        assertEquals(false, translatedNow(isTranslated = true, hasPair = false))
        assertEquals(false, workingNow(isTranslated = true, hasPair = false, translating = false, restoring = true))
    }

    @Test fun `still translating while the page has no pair yet`() {
        assertEquals(true, workingNow(isTranslated = false, hasPair = false, translating = true, restoring = false))
    }

    @Test fun `before Gecko said anything, the flags decide`() {
        assertEquals(true, translatedNow(isTranslated = true, hasPair = null))
        assertEquals(true, workingNow(isTranslated = false, hasPair = null, translating = true, restoring = false))
    }
}
