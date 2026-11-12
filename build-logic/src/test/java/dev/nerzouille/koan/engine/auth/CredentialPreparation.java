package dev.nerzouille.koan.engine.auth;

import android.credentials.PrepareGetCredentialResponse;

public class CredentialPreparation {
    public static boolean hasCredentialOrRemote(PrepareGetCredentialResponse response, String type) {
        return response.hasCredentialResults(type) || response.hasRemoteResults();
    }
}
