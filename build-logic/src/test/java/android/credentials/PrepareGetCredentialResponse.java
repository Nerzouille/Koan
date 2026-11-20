package android.credentials;
public class PrepareGetCredentialResponse {
    public static class PendingGetCredentialHandle {}
    private final boolean local, authentication, remote;
    public PrepareGetCredentialResponse(boolean local, boolean authentication, boolean remote) {
        this.local = local; this.authentication = authentication; this.remote = remote;
    }
    public boolean hasCredentialResults(String type) { return local; }
    public boolean hasAuthenticationResults() { return authentication; }
    public boolean hasRemoteResults() { return remote; }
}
