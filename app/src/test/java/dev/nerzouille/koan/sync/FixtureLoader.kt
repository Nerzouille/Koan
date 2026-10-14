// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the golden contract fixtures from `shared/contract/fixtures/{crypto,wire,auth}/`
 * (on the test classpath via `android/app/build.gradle.kts`). Every load asserts the
 * top-level `contract == 1` and `id == basename`, so an out-of-sync fixture
 * fails loudly instead of silently testing the wrong vector.
 */
internal object FixtureLoader {
    /** All fixture basenames; kept static so drift detection can enumerate them. */
    val allFixtureNames = listOf(
        "auth-errno-103",
        "bso-ids-percent-encoding",
        "crypto-bso-envelope-tampered-hmac",
        "crypto-bso-envelope-valid",
        "crypto-client-state-bytes-kb-zero",
        "crypto-hkdf-rfc5869-case1",
        "crypto-sync-key-bundle-kb-zero",
        "crypto-token-material-session-token",
        "crypto-unbundle-account-keys",
        "hawk-authorization-resource",
        "hawk-payload-hash",
        "wire-deleted-string",
        "wire-folder-basic",
        "wire-folder-live-object",
        "wire-folder-missing-folderid",
        "wire-ignored-records",
        "wire-layout-basic",
        "wire-layout-essentials-dup-url",
        "wire-prefs-normal-tabs",
        "wire-prefs-normal-tabs-capability",
        "wire-space-basic",
        "wire-space-numeric-uuid",
        "wire-space-object-dots",
        "wire-space-rgb-dots",
        "wire-split-basic",
        "wire-split-normal-pinned-false",
        "wire-tab-normal-pinned-false",
        "wire-tab-pinned-default",
        "wire-tab-pinned-string-false",
    )

    /** Parsed fixture root; fails loudly on missing resource or contract drift. */
    fun json(name: String): JSONObject {
        val root = JSONObject(String(data(name), Charsets.UTF_8))
        val contract = root.optInt("contract", Int.MIN_VALUE)
        check(contract == 1) {
            "fixture '$name': expected contract 1, found $contract (SPEC drift?)"
        }
        val id = root.optString("id")
        check(id == name) {
            "fixture '$name': id mismatch, file says '$id'"
        }
        return root
    }

    /** Raw fixture bytes (same resource lookup, no contract assertions). */
    fun data(name: String): ByteArray = bytes("$name.json")

    /**
     * Cases array of a multi-case fixture; each case must carry `input` and
     * `expect` so a malformed case fails at load time, not mid-test.
     */
    fun cases(name: String): JSONArray {
        val root = json(name)
        val cases = root.optJSONArray("cases")
            ?: throw IllegalStateException("fixture '$name' has no 'cases' array (single-case fixture?)")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            check(case.has("input") && case.has("expect")) {
                "fixture '$name' case $i ('${case.optString("id")}'): missing input/expect"
            }
        }
        return cases
    }

    private fun bytes(resourceName: String): ByteArray {
        val loader: ClassLoader =
            FixtureLoader::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        val stream = loader.getResourceAsStream(resourceName)
            ?: loader.getResourceAsStream("/$resourceName")
            ?: throw IllegalStateException(
                "fixture resource '$resourceName' not found on test classpath " +
                    "(classpath=${System.getProperty("java.class.path")})",
            )
        return stream.use { it.readBytes() }
    }
}
