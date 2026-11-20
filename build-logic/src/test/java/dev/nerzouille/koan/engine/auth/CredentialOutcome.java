package dev.nerzouille.koan.engine.auth;

import org.mozilla.geckoview.GeckoResult;

public class CredentialOutcome {
    public static GeckoResult normalize(GeckoResult result) {
        result.normalized = true;
        return result;
    }
}
