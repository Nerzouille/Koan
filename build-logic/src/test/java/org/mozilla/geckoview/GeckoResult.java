package org.mozilla.geckoview;

public class GeckoResult {
    public interface Consumer<T> { void accept(T value); }
    public final Object value;
    public Throwable error;
    private GeckoResult(Object value) { this.value = value; }
    public static GeckoResult fromValue(Object value) { return new GeckoResult(value); }
    public static GeckoResult fromException(Throwable error) {
        GeckoResult result = new GeckoResult(null); result.error = error; return result;
    }
}
