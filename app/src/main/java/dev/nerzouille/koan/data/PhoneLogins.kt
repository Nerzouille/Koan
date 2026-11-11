package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.PhoneLogin

/** Logins saved on the phone, opened in memory (Keystore) for the upload plan only. */
class RoomPhoneData(private val dao: KoanDao, private val logins: LoginStore) {
    suspend fun logins() = dao.allLogins().filter { it.local }.mapNotNull { row ->
        val login = logins.open(row) ?: return@mapNotNull null
        PhoneLogin(
            row.guid, row.origin, row.formActionOrigin, row.httpRealm, login.username, login.password,
            row.usernameField, row.passwordField, row.timeCreated, row.timeLastUsed, row.timePasswordChanged,
        )
    }
}
