package dev.nerzouille.koan.engine.auth;
import android.content.Context;
public class CredentialUiHost {
    public static final Context UI = new Context();
    public static Context context(Context fallback) { return UI; }
}
