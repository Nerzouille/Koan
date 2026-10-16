package dev.nerzouille.koan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginStoreTest {
    @Test fun `base domain of a host`() {
        assertEquals("example.com", LoginStore.baseDomain("accounts.example.com"))
        assertEquals("example.com", LoginStore.baseDomain("www.example.com"))
        assertEquals("example.co.uk", LoginStore.baseDomain("login.example.co.uk"))
        assertEquals("localhost", LoginStore.baseDomain("localhost"))
    }
}
