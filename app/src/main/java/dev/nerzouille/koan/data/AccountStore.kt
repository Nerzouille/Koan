package dev.nerzouille.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.nerzouille.koan.sync.FxAClient
import dev.nerzouille.koan.sync.FxACrypto
import dev.nerzouille.koan.sync.PairingFlow
import dev.nerzouille.koan.sync.ScopedKeys
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
import java.security.interfaces.ECPrivateKey

/**
 * The Mozilla account's secrets. [toString] never prints them, so they can't leak into logs or
 * crash reports by accident (CLAUDE.md rule 5). Signed in either with a password (a session token
 * and kB) or from a computer by QR pairing (an OAuth refresh token and the Sync key, never kB).
 */
class Account private constructor(
    val email: String,
    val uid: String,
    /** Password sign-in. */
    val sessionTokenHex: String?,
    val kBHex: String?,
    /** QR sign-in: long-lived, revoked on sign-out. */
    val refreshToken: String?,
    private val syncKeyB64: String?,
    private val syncKeyId: String?,
) {
    constructor(email: String, uid: String, sessionTokenHex: String, kBHex: String) :
        this(email, uid, sessionTokenHex, kBHex, null, null, null)

    /** The Sync key of a QR sign-in; null for a password one (derived from kB instead). */
    val syncKey: ScopedKeys.SyncKey?
        get() = if (syncKeyB64 != null && syncKeyId != null) ScopedKeys.SyncKey(Base64.getDecoder().decode(syncKeyB64), syncKeyId) else null

    val oauth: Boolean get() = refreshToken != null

    override fun toString() = "Account(email=$email, secrets=<redacted>)"

    internal fun toJson(): String = JSONObject().put("email", email).put("uid", uid).apply {
        if (refreshToken != null) {
            put("refreshToken", refreshToken).put("syncKey", syncKeyB64).put("kid", syncKeyId)
        } else {
            put("sessionToken", sessionTokenHex).put("kB", kBHex)
        }
    }.toString()

    companion object {
        fun oauth(email: String, uid: String, refreshToken: String, syncKey: ScopedKeys.SyncKey) =
            Account(email, uid, null, null, refreshToken, Base64.getEncoder().encodeToString(syncKey.key), syncKey.kid)

        internal fun fromJson(raw: String): Account? = runCatching {
            val o = JSONObject(raw)
            if (o.has("refreshToken")) {
                Account(o.getString("email"), o.getString("uid"), null, null, o.getString("refreshToken"), o.getString("syncKey"), o.getString("kid"))
            } else {
                Account(o.getString("email"), o.getString("uid"), o.getString("sessionToken"), o.getString("kB"))
            }
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

    /** QR sign-in: the current oldsync access token and when it expires (memory only). */
    private var access: Pair<String, Long>? = null

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

    /**
     * Finishes a QR sign-in: the authorization code becomes tokens, the Sync key is decrypted with
     * the flow's key, the profile gives the email. Throws on failure; stores nothing until all is
     * in. Returns whether the key is the one the password sign-in already had (null: no password
     * sign-in to compare with): both must open the same Sync data.
     */
    suspend fun completePairing(flow: PairingFlow, code: String): Boolean? {
        val client = FxAClient(authTransport)
        val tokens = client.exchangeCode(code, flow.codeVerifier, PairingFlow.CLIENT_ID)
        val syncKey = withContext(Dispatchers.Default) {
            ScopedKeys.oldSyncKey(ScopedKeys.decryptJwe(tokens.keysJwe, flow.keyPair.private as ECPrivateKey))
        }
        val (email, uid) = client.profile(tokens.accessToken)
        val previous = load()
        val match = previous?.takeIf { it.uid == uid }?.kBHex?.let { kB -> sameKey(FxACrypto.unhex(kB), syncKey) }
        save(Account.oauth(email, uid, tokens.refreshToken, syncKey))
        mutex.withLock { access = tokens.accessToken to System.currentTimeMillis() + ACCESS_FIRST_MS }
        return match
    }

    suspend fun save(new: Account) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val sealed = runCatching { box.seal(new.toJson().toByteArray(Charsets.UTF_8)) }
                .getOrElse { throw SyncError.StorageUnavailable }
            blobs.write(Base64.getEncoder().encodeToString(sealed))
        }
        account = new
        creds = null
        access = null
        loaded = true
        _state.value = AccountState.SignedIn(new.email)
    }

    suspend fun signOut() = mutex.withLock {
        // A QR sign-in's refresh token is revoked, so it can't be used again (best effort: offline
        // still signs out here).
        val refresh = account?.refreshToken
        if (refresh != null) runCatching { FxAClient(authTransport).destroyToken(refresh, PairingFlow.CLIENT_ID) }
        account = null
        creds = null
        access = null
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
        val valid = cached?.takeIf { it.expiresAtMillis > System.currentTimeMillis() + 45_000 }
        val syncKey = current.syncKey
        val refresh = current.refreshToken
        if (refresh != null && syncKey != null) {
            val fresh = valid ?: FxAClient(authTransport).syncCredentials(accessToken(current, refresh), syncKey.kid)
            mutex.withLock { if (account === current) creds = fresh }
            return@withContext SyncClient(creds = fresh, syncKey = syncKey, transport = syncTransport)
        }
        val kB = FxACrypto.unhex(current.kBHex ?: throw SyncError.NotSignedIn)
        val fresh = valid ?: FxAClient(authTransport).syncCredentials(current.sessionTokenHex ?: throw SyncError.NotSignedIn, kB)
        mutex.withLock { if (account === current) creds = fresh }
        SyncClient(creds = fresh, kB = kB, transport = syncTransport)
    }

    /** A valid oldsync access token, refreshed from the refresh token once it's close to expiring. */
    private suspend fun accessToken(current: Account, refresh: String): String {
        mutex.withLock { access }?.takeIf { it.second > System.currentTimeMillis() + 60_000 }?.let { return it.first }
        val fresh = FxAClient(authTransport).refreshAccessToken(refresh, PairingFlow.CLIENT_ID)
        mutex.withLock { if (account === current) access = fresh }
        return fresh.first
    }

    /** Drops cached token-server credentials, e.g. after the storage server answered 401. */
    suspend fun invalidateCredentials() = mutex.withLock {
        creds = null
        access = null
    }

    private companion object {
        /** The access token a code exchange returns is used for the first sync only, then refreshed. */
        const val ACCESS_FIRST_MS = 5 * 60_000L

        /** The QR sign-in's key is the one the password sign-in derives from kB (both halves and the key id's hash). */
        fun sameKey(kB: ByteArray, key: ScopedKeys.SyncKey): Boolean {
            val legacy = ScopedKeys.legacySyncKey(kB, 0)
            return legacy.key.contentEquals(key.key) && key.kid.substringAfter('-') == legacy.kid.substringAfter('-')
        }
    }
}
