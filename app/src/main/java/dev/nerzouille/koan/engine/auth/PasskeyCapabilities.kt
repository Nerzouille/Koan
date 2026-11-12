package dev.nerzouille.koan.engine.auth

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.credentials.CredentialManager
import android.os.Build
import androidx.annotation.RequiresApi

/** Platform support, not a promise that any particular vault will trust this browser. */
object PasskeyCapabilities {
    @JvmStatic
    fun hasPlatformAuthenticator(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return runCatching { Api34.available(context) }.getOrDefault(false)
    }

    @JvmStatic
    fun canAttemptGoogleFido(context: Context): Boolean = runCatching {
        // The original FIDO implementation checks API/version support. Avoid calling it when
        // the service package itself is absent or disabled, where its task may never finish.
        context.packageManager.getApplicationInfo("com.google.android.gms", 0).enabled
    }.getOrDefault(false)

    @RequiresApi(34)
    private object Api34 {
        fun available(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CREDENTIALS) &&
                context.getSystemService(CredentialManager::class.java) != null &&
                context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
    }
}
