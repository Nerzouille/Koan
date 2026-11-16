package dev.nerzouille.koan.ui.kit

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Asks for the fingerprint or the device PIN / pattern ([title] on the prompt). True once it is the
 * owner; false if cancelled or failed. A phone with no screen lock at all can't be asked: true.
 */
suspend fun deviceAuth(activity: FragmentActivity, title: String): Boolean {
    val how = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
    if (BiometricManager.from(activity).canAuthenticate(how) != BiometricManager.BIOMETRIC_SUCCESS) return true
    return suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            },
        )
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(how).build())
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
