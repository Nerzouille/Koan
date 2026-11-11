package dev.nerzouille.koan.engine.auth

import android.content.Intent
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPasskeyResultTest {
    @Test fun resultIsDeliveredOnlyOnce() {
        val state = PendingPasskeyResult()
        val response = Intent()
        val received = mutableListOf<Intent?>()
        assertTrue(state.begin(received::add))
        state.complete(response)
        state.complete(null)
        assertEquals(1, received.size)
        assertSame(response, received.single())
    }

    @Test fun concurrentRequestDoesNotReplaceFirstCallback() {
        val state = PendingPasskeyResult()
        var firstCalls = 0
        var secondCalls = 0
        assertTrue(state.begin { firstCalls++ })
        assertFalse(state.begin { assertNull(it); secondCalls++ })
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
        state.complete(null)
        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test fun cancellationAllowsAnotherRequest() {
        val state = PendingPasskeyResult()
        var cancellations = 0
        assertTrue(state.begin { assertNull(it); cancellations++ })
        state.complete(null)
        assertTrue(state.begin { assertNull(it); cancellations++ })
        state.complete(null)
        assertEquals(2, cancellations)
    }

    @Test fun finishingActivityCancelsPendingRequest() {
        val store = ViewModelStore()
        val state = PendingPasskeyResult()
        store.put("passkey", state)
        var calls = 0
        state.begin { assertNull(it); calls++ }
        store.clear()
        state.complete(null)
        assertEquals(1, calls)
    }
}
