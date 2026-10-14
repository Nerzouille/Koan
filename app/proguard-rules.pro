# GeckoView and android-components ship their own consumer rules.
# Add app-specific keep rules here only when R8 actually breaks something.

# android-components declares Glean (Mozilla telemetry) as compileOnly; Fenix ships it, we don't.
# Only Nimbus experiment code references it, and Koan never initialises Nimbus/Glean.
-dontwarn mozilla.telemetry.glean.**

# The Mozilla sign-in bridge (ui/signin) is only called from JavaScript: keep its method names.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
