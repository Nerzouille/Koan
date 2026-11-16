package dev.nerzouille.koan.ui.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateOfferTest {
    private val phone = listOf("en", "fr")

    @Test fun `a language set on the phone is never offered, even when it isn't the first`() {
        assertFalse(offerFor("fr", phone, offerOn = true))
        assertFalse(offerFor("en", phone, offerOn = true))
    }

    @Test fun `regional variants count as the language`() {
        assertFalse(offerFor("fr-CA", phone, offerOn = true))
        assertFalse(offerFor("EN-gb", phone, offerOn = true))
    }

    @Test fun `another language is offered`() {
        assertTrue(offerFor("de", phone, offerOn = true))
        assertTrue(offerFor("ja", listOf("en"), offerOn = true))
    }

    @Test fun `the switch off offers nothing`() {
        assertFalse(offerFor("de", phone, offerOn = false))
    }
}
