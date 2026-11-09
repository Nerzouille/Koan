package dev.nerzouille.koan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Passwords › +: what the Site field accepts and the origin it becomes. */
class LoginOriginTest {
    @Test fun bareHostGetsHttps() = assertEquals("https://example.com", LoginStore.originOf("example.com"))

    @Test fun fullAddressKeepsSchemeHostAndPortOnly() {
        assertEquals("https://login.example.com", LoginStore.originOf(" https://login.example.com/path?q=1 "))
        assertEquals("http://example.com:8080", LoginStore.originOf("http://Example.com:8080/"))
    }

    @Test fun localhostIsASite() = assertEquals("https://localhost", LoginStore.originOf("localhost"))

    @Test fun notASite() {
        assertNull(LoginStore.originOf(""))
        assertNull(LoginStore.originOf("not a site"))
        assertNull(LoginStore.originOf("example"))
        assertNull(LoginStore.originOf("ftp://example.com"))
    }
}
