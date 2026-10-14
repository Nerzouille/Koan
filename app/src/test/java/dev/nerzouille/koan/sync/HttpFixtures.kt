// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONObject

/**
 * Loads the recorded HTTP exchanges from `shared/contract/http/` (on the test
 * classpath via `android/app/build.gradle.kts`, SPEC.md §7.3). Every load
 * asserts the top-level `contract == 1` and `id == basename`, so an out-of-sync
 * recording fails loudly instead of silently testing the wrong exchange.
 */
internal object HttpFixtures {
    /** All recording basenames; kept static so drift detection can enumerate them. */
    val allRecordingNames = listOf(
        "http-get-404",
        "http-get-500-hawk",
        "http-get-pagination-page1",
        "http-get-pagination-page2",
        "http-get-stuck-offset",
        "http-keys-missing",
        "http-keys-present-default",
        "http-keys-present-per-collection",
        "http-post-200-success-failed",
        "http-put-200",
        "http-put-412",
    )

    /** Parsed recording root; fails loudly on missing resource or contract drift. */
    fun json(name: String): JSONObject {
        val root = JSONObject(String(data(name), Charsets.UTF_8))
        val contract = root.optInt("contract", Int.MIN_VALUE)
        check(contract == 1) {
            "recording '$name': expected contract 1, found $contract (SPEC drift?)"
        }
        val id = root.optString("id")
        check(id == name) {
            "recording '$name': id mismatch, file says '$id'"
        }
        return root
    }

    /** Raw recording bytes (same resource lookup, no contract assertions). */
    fun data(name: String): ByteArray = bytes("$name.json")

    /** Recorded request of a single-exchange recording. */
    fun request(name: String): JSONObject =
        json(name).getJSONObject("input").getJSONObject("request")

    /** Recorded response of a single-exchange recording. */
    fun response(name: String): JSONObject =
        json(name).getJSONObject("expect").getJSONObject("response")

    private fun bytes(resourceName: String): ByteArray {
        val loader: ClassLoader =
            HttpFixtures::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        val stream = loader.getResourceAsStream(resourceName)
            ?: loader.getResourceAsStream("/$resourceName")
            ?: throw IllegalStateException(
                "recording resource '$resourceName' not found on test classpath " +
                    "(classpath=${System.getProperty("java.class.path")})",
            )
        return stream.use { it.readBytes() }
    }
}
