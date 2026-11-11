package dev.nerzouille.koan.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** A failing background job is reported and the scope keeps working (audit 8.1). */
class KoanScopesTest {
    private val original = KoanScopes.report

    @After fun restore() {
        KoanScopes.report = original
    }

    @Test fun failureIsReportedAndScopeSurvives() = runBlocking {
        val reported = CompletableDeferred<Pair<String, Throwable>>()
        KoanScopes.report = { name, e -> reported.complete(name to e) }
        val scope = KoanScopes.guarded("test", Dispatchers.Default)
        scope.launch { throw IOException("disk full") }
        val (name, e) = withTimeout(2_000) { reported.await() }
        assertEquals("test", name)
        assertTrue(e is IOException)
        // The scope still runs new work after a child failed.
        val after = CompletableDeferred<Int>()
        scope.launch { after.complete(42) }
        assertEquals(42, withTimeout(2_000) { after.await() })
    }
}
