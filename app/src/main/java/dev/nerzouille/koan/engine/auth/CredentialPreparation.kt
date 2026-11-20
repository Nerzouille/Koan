package dev.nerzouille.koan.engine.auth

import android.credentials.PrepareGetCredentialResponse
import androidx.annotation.RequiresApi

/** A remote provider entry is usable even when no local passkey has been enumerated. */
@RequiresApi(34)
object CredentialPreparation {
    @JvmStatic
    fun hasCredentialOrRemote(response: PrepareGetCredentialResponse, type: String): Boolean =
        response.hasCredentialResults(type) || response.hasRemoteResults()
}
