package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.nerzouille.koan.sync.FxAClient
import dev.nerzouille.koan.sync.FxACrypto
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.SyncHttpTransport
import dev.nerzouille.koan.sync.TokenServerCreds
import dev.nerzouille.koan.sync.UrlConnectionTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64

/**
 * The Mozilla account's secrets. [toString] never prints them, so they can't leak into logs or
 * crash reports by accident (CLAUDE.md rule 5).
 */
class Account(
    val email: String,
    val uid: String,
    val sessionTokenHex: String,
    val kBHex: String,
) {
    override fun toString() = "Account(email=$email, secrets=<redacted>)"

    internal fun toJson(): String = JSONObject()
        .put("email", email).put("uid", uid).put("sessionToken", sessionTokenHex).put("kB", kBHex)
        .toString()

    internal companion object {
        fun fromJson(raw: String): Account? = runCatching {
            val o = JSONObject(raw)
            Account(o.getString("email"), o.getString("uid"), o.getString("sessionToken"), o.getString("kB"))
        }.getOrNull()
    }
}

sealed interface AccountState {
    /** Not read from disk yet: show nothing account-related (no first-run card flash). */
    data object Loading : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val email: String) : AccountState
}

/** One opaque, already-encrypted blob on disk. */
interface BlobStore {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun clear()
}

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account")

class DataStoreBlob(context: Context) : BlobStore {
    private val store = context.applicationContext.accountDataStore
    private val key = stringPreferencesKey("sealed")
    override suspend fun read(): String? = store.data.map { it[key] }.first()
    override suspend fun write(value: String) { store.edit { it[key] = value } }
    override suspend fun clear() { store.edit { it.remove(key) } }
}

/**
 * Mozilla account session, sealed with an Android Keystore key and kept in DataStore. Token-server
 * credentials stay in memory only and are refreshed 45 s before they expire (as Z-Sync does).
 */
class AccountStore(
    private val blobs: BlobStore,
    private val box: SecretBox,
    private val authTransport: SyncHttpTransport = UrlConnectionTransport(readTimeoutMs = 30_000),
    private val syncTransport: SyncHttpTransport = UrlConnectionTransport(),
) {
    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    val state: StateFlow<AccountState> = _state

    private val mutex = Mutex()
    private var account: Account? = null
    private var loaded = false
    private var creds: TokenServerCreds? = null

    /** Reads the stored account once. Unreadable data (e.g. a wiped Keystore) counts as signed out. */
    suspend fun load(): Account? = mutex.withLock { loadLocked() }

    private suspend fun loadLocked(): Account? {
        if (!loaded) {
            account = withContext(Dispatchers.IO) {
                blobs.read()?.let { sealed ->
                    runCatching { String(box.open(Base64.getDecoder().decode(sealed)), Charsets.UTF_8) }
                        .getOrNull()
                        ?.let(Account::fromJson)
                }
            }
            loaded = true
            _state.value = account?.let { AccountState.SignedIn(it.email) } ?: AccountState.SignedOut
        }
        return account
    }

    /** Finishes a web login: fetches kB, then stores the session. Throws on failure. */
    suspend fun completeWebLogin(email: String, uid: String, sessionToken: String, keyFetchToken: String, unwrapBKey: String) {
        val result = FxAClient(authTransport).completeWebLogin(email, uid, sessionToken, keyFetchToken, unwrapBKey)
        save(Account(email, result.uid, result.session, FxACrypto.hex(result.kB)))
    }

    suspend fun save(new: Account) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val sealed = runCatching { box.seal(new.toJson().toByteArray(Charsets.UTF_8)) }
                .getOrElse { throw SyncError.StorageUnavailable }
            blobs.write(Base64.getEncoder().encodeToString(sealed))
        }
        account = new
        creds = null
        loaded = true
        _state.value = AccountState.SignedIn(new.email)
    }

    suspend fun signOut() = mutex.withLock {
        account = null
        creds = null
        loaded = true
        withContext(Dispatchers.IO) {
            runCatching { blobs.clear() }
            box.destroy()
        }
        _state.value = AccountState.SignedOut
    }

    /** A Sync storage client with fresh token-server credentials. Throws [SyncError.NotSignedIn]. */
    suspend fun connect(): SyncClient = withContext(Dispatchers.IO) {
        val (current, cached) = mutex.withLock { loadLocked() to creds }
        current ?: throw SyncError.NotSignedIn
        val kB = FxACrypto.unhex(current.kBHex)
        val valid = cached?.takeIf { it.expiresAtMillis > System.currentTimeMillis() + 45_000 }
        val fresh = valid ?: FxAClient(authTransport).syncCredentials(current.sessionTokenHex, kB)
        mutex.withLock { if (account === current) creds = fresh }
        SyncClient(creds = fresh, kB = kB, transport = syncTransport)
    }

    /** Drops cached token-server credentials, e.g. after the storage server answered 401. */
    suspend fun invalidateCredentials() = mutex.withLock { creds = null }
}
