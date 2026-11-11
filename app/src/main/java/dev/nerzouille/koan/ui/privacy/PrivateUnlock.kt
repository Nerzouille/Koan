package dev.nerzouille.koan.ui.privacy

import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Asks for the fingerprint or the device PIN before the private tabs show. True once unlocked;
 * false if cancelled or failed. A phone with no screen lock at all can't be asked: true.
 */
suspend fun unlockPrivate(activity: FragmentActivity): Boolean {
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
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock private tabs").setAllowedAuthenticators(how).build())
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

/**
 * Keyboards don't learn from what is typed in Koan's own fields while [on] (Private): every input
 * connection asks for IME_FLAG_NO_PERSONALIZED_LEARNING. Fields inside pages get it from GeckoView
 * in private tabs. Always composed (switching it never rebuilds [content]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoPersonalizedLearning(on: Boolean, content: @Composable () -> Unit) {
    val private by rememberUpdatedState(on)
    InterceptPlatformTextInput(
        interceptor = { request, next ->
            val modified = PlatformTextInputMethodRequest { outAttributes ->
                request.createInputConnection(outAttributes).also {
                    if (private) outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                }
            }
            next.startInputMethod(modified)
        },
        content = content,
    )
}
