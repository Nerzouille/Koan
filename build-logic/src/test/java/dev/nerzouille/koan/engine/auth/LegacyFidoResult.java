package dev.nerzouille.koan.engine.auth;
import com.google.android.gms.tasks.Task;
import org.mozilla.geckoview.GeckoResult;
public class LegacyFidoResult {
    public static GeckoResult.Consumer<Throwable> activityFailure(GeckoResult result) {
        return ignored -> result.error = new org.mozilla.gecko.util.WebAuthnUtils.Exception("ABORT_ERR");
    }
    public static GeckoResult observed;
    public static Task observe(Task task, GeckoResult result) { observed = result; return task; }
    public static Task observeCancellation(Task task, GeckoResult result) { observed = result; return task; }
}
