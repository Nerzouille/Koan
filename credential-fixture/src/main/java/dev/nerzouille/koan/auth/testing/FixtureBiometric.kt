package dev.nerzouille.koan.auth.testing

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Uses only this test app's key. Never reads or changes a real provider's biometric settings. */
object FixtureBiometric {
    fun enabled(context: Context) = context.getSharedPreferences("fixture-options", Context.MODE_PRIVATE)
        .getBoolean("biometric", false)

    fun authenticate(activity: Activity, success: () -> Unit): CancellationSignal {
        val cancel = CancellationSignal()
        val settled = AtomicBoolean()
        val alias = "koan-fixture-biometric"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(alias)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    .build())
                generateKey()
            }
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, store.getKey(alias, null) as SecretKey)
        }
        fun failed(code: Int) {
            if (!settled.compareAndSet(false, true)) return
            Log.w("KoanBioFixture", "authentication error code=$code")
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Koan fixture biometric")
            .setSubtitle("Synthetic provider verification")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButton("Cancel", activity.mainExecutor) { _, _ -> failed(BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) }
            .build()
        Log.i("KoanBioFixture", "authentication started")
        prompt.authenticate(BiometricPrompt.CryptoObject(cipher), cancel, activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = failed(errorCode)
                override fun onAuthenticationFailed() { Log.i("KoanBioFixture", "fingerprint not recognized") }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!settled.compareAndSet(false, true)) return
                    try {
                        requireNotNull(result.cryptoObject?.cipher).doFinal(byteArrayOf(1, 2, 3))
                        Log.i("KoanBioFixture", "Keystore biometric authentication succeeded")
                        success()
                    } catch (e: Exception) {
                        Log.e("KoanBioFixture", "Keystore authentication operation failed", e)
                        activity.setResult(Activity.RESULT_CANCELED)
                        activity.finish()
                    }
                }
            })
        return cancel
    }
}

/** Explicit opt-in, in the disposable fixture package only. No browser setting is changed. */
class BiometricControlFixture : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences("fixture-options", MODE_PRIVATE).edit()
            .putBoolean("biometric", intent.getBooleanExtra("enabled", false)).commit()
        finish()
    }
}
