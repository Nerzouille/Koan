package dev.nerzouille.koan.engine.auth

import org.mozilla.gecko.util.WebAuthnUtils
import org.mozilla.geckoview.GeckoResult

/** A provider's user dismissal is NotAllowedError; AbortError belongs to the website's signal. */
object CredentialOutcome {
    @JvmStatic
    fun <T> normalize(source: GeckoResult<T>): GeckoResult<T> {
        val result = GeckoResult<T>()
        source.accept(
            { result.complete(it) },
            { error ->
                result.completeExceptionally(
                    if (error is WebAuthnUtils.Exception && error.message == "ABORT_ERR")
                        WebAuthnUtils.Exception("NOT_ALLOWED_ERR")
                    else error ?: WebAuthnUtils.Exception("UNKNOWN_ERR"),
                )
            },
        )
        return result
    }
}
