package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.SyncError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.KeyGenerator

class AccountStoreTest {
    private class MemoryBlob : BlobStore {
        var value: String? = null
        override suspend fun read() = value
        override suspend fun write(value: String) { this.value = value }
        override suspend fun clear() { value = null }
    }

    /** Same AES-GCM box as production, with a plain JCE key instead of the Android Keystore. */
    private class TestBox : AesGcmBox({ key }) {
        var destroyed = false
        override fun destroy() { destroyed = true }

        companion object {
            val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        }
    }

    private val secretSession = "a1".repeat(32)
    private val secretKb = "b2".repeat(32)
    private val account = Account("noa@example.com", "uid-1", secretSession, secretKb)

    @Test
    fun savedAccountSurvivesANewProcess() = runTest {
        val blob = MemoryBlob()
        AccountStore(blob, TestBox()).save(account)

        val fresh = AccountStore(blob, TestBox())
        val loaded = fresh.load()!!
        assertEquals(account.email, loaded.email)
        assertEquals(secretSession, loaded.sessionTokenHex)
        assertEquals(secretKb, loaded.kBHex)
        assertEquals(AccountState.SignedIn(account.email), fresh.state.value)
    }

    @Test
    fun secretsNeverReachDiskOrLogsInClear() = runTest {
        val blob = MemoryBlob()
        AccountStore(blob, TestBox()).save(account)
        val onDisk = String(Base64.getDecoder().decode(blob.value!!), Charsets.ISO_8859_1)
        assertFalse(onDisk.contains(secretSession))
        assertFalse(onDisk.contains(secretKb))
        assertFalse(onDisk.contains(account.email))
        assertFalse(account.toString().contains(secretSession))
        assertFalse(account.toString().contains(secretKb))
    }

    @Test
    fun signOutForgetsEverything() = runTest {
        val blob = MemoryBlob()
        val box = TestBox()
        val store = AccountStore(blob, box)
        store.save(account)
        store.signOut()
        assertNull(blob.value)
        assertTrue(box.destroyed)
        assertNull(store.load())
        assertEquals(AccountState.SignedOut, store.state.value)
    }

    @Test
    fun unreadableBlobCountsAsSignedOut() = runTest {
        val blob = MemoryBlob().apply { value = Base64.getEncoder().encodeToString(ByteArray(40) { 7 }) }
        val store = AccountStore(blob, TestBox())
        assertNull(store.load())
        assertEquals(AccountState.SignedOut, store.state.value)
    }

    @Test(expected = SyncError.NotSignedIn::class)
    fun connectWithoutAccountFails() = runTest {
        AccountStore(MemoryBlob(), TestBox()).connect()
    }
}
