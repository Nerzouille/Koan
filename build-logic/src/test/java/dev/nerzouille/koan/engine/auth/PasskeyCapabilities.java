package dev.nerzouille.koan.engine.auth;

import android.content.Context;

public class PasskeyCapabilities {
    public static boolean available;
    public static boolean googleAvailable;
    public static boolean hasPlatformAuthenticator(Context context) { return available; }
    public static boolean canAttemptGoogleFido(Context context) { return googleAvailable; }
}
