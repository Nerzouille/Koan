package dev.nerzouille.koan.engine.auth

import android.content.Intent
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PendingPasskeyResultTest {
    @Test fun resultIsDeliveredOnlyOnce() {
        val state = PendingPasskeyResult()
        val response = Intent()
        val received = mutableListOf<Intent?>()
        val id = requireNotNull(state.begin(received::add))
        state.complete(id, response)
        state.complete(id, null)
        assertEquals(1, received.size)
        assertSame(response, received.single())
    }

    @Test fun concurrentRequestDoesNotReplaceFirstCallback() {
        val state = PendingPasskeyResult()
        var firstCalls = 0
        var secondCalls = 0
        val id = requireNotNull(state.begin { firstCalls++ })
        assertNull(state.begin { assertNull(it); secondCalls++ })
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
        state.complete(id, null)
        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test fun cancellationAllowsAnotherRequest() {
        val state = PendingPasskeyResult()
        var cancellations = 0
        val first = requireNotNull(state.begin { assertNull(it); cancellations++ })
        state.complete(first, null)
        val second = requireNotNull(state.begin { assertNull(it); cancellations++ })
        state.complete(second, null)
        assertEquals(2, cancellations)
    }

    @Test fun finishingActivityCancelsPendingRequest() {
        val store = ViewModelStore()
        val state = PendingPasskeyResult()
        store.put("passkey", state)
        var calls = 0
        val id = requireNotNull(state.begin { assertNull(it); calls++ })
        store.clear()
        state.complete(id, null)
        assertEquals(1, calls)
    }

    @Test fun lateResultCannotCompleteRetry() {
        val state = PendingPasskeyResult()
        val first = requireNotNull(state.begin {})
        state.complete(first, null)
        var retryCalls = 0
        val second = requireNotNull(state.begin { retryCalls++ })
        assertNotEquals(first, second)
        assertFalse(state.complete(first, Intent()))
        assertEquals(0, retryCalls)
        assertTrue(state.complete(second, Intent()))
        assertEquals(1, retryCalls)
    }

    @Test fun callbackCanStartAnotherRequestWithoutBeingCleared() {
        val state = PendingPasskeyResult()
        var next: String? = null
        var calls = 0
        val first = requireNotNull(state.begin {
            next = state.begin { calls++ }
        })
        state.complete(first, null)
        assertNotNull(next)
        assertEquals(next, state.requestId)
        state.complete(requireNotNull(next), Intent())
        assertEquals(1, calls)
        assertNull(state.requestId)
    }

    @Test fun clearedOwnerCannotBeResurrectedByItsCallback() {
        val store = ViewModelStore()
        val state = PendingPasskeyResult()
        store.put("passkey", state)
        var rejected = 0
        state.begin {
            assertNull(state.begin { assertNull(it); rejected++ })
        }
        store.clear()
        assertNull(state.requestId)
        assertEquals(1, rejected)
        assertNull(state.begin { assertNull(it); rejected++ })
        assertEquals(2, rejected)
    }
}
