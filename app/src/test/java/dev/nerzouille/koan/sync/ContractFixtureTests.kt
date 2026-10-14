// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.sync.FxACrypto
import dev.nerzouille.koan.sync.FxAClient
import dev.nerzouille.koan.sync.HawkAuth
import dev.nerzouille.koan.sync.SpacesSyncService
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncCrypto
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.ZenSpaces
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Contract conformance suite: one test per fixture group, reading the golden
 * fixtures from `shared/contract/fixtures/` via [FixtureLoader] (SPEC.md,
 * Contract-Version 1). Drift in any fixture fails loudly here.
 */
class ContractFixtureTests {
    private fun fail(message: String): Nothing = throw AssertionError(message)

    private fun decodeInput(name: String): ZenSpaces.DecodedRecord? {
        val input = FixtureLoader.json(name).getJSONObject("input")
        return ZenSpaces.decode(input.optString("id"), input)
    }

    /** The `expect` block of a single-case fixture (canonical expectations). */
    private fun expectOf(name: String): JSONObject =
        FixtureLoader.json(name).getJSONObject("expect")

    /** String array under `key` as a Kotlin list. */
    private fun expectStrings(json: JSONObject, key: String): List<String> {
        val arr = json.getJSONArray(key)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun expectNullableString(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.getString(key)

    private fun expectNullableBool(json: JSONObject, key: String): Boolean? =
        if (json.isNull(key)) null else json.getBoolean(key)

    // MARK: Loader drift (task 1)

    /** Every golden fixture must load with contract == 1 and id == basename. */
    @Test
    fun everyGoldenFixtureLoads() {
        assertEquals(29, FixtureLoader.allFixtureNames.size)
        for (name in FixtureLoader.allFixtureNames) {
            FixtureLoader.json(name)
            FixtureLoader.data(name)
        }
    }

    // MARK: Wire decode tables

    @Test
    fun wireSpaceFixturesDecode() {
        val basicExpect = expectOf("wire-space-basic")
        val basic = (decodeInput("wire-space-basic") as? ZenSpaces.DecodedRecord.Space)?.record
            ?: fail("wire-space-basic must decode as space")
        assertEquals(basicExpect.getString("uuid"), basic.uuid)
        assertEquals(basicExpect.getString("name"), basic.name)
        assertEquals(expectStrings(basicExpect, "gradientColors"), basic.theme?.gradientColors)
        assertEquals(basicExpect.getInt("dotCount"), basic.theme?.dots?.size)
        assertEquals(expectNullableString(basicExpect, "containerGuid"), basic.containerGuid)

        val dotsExpect = expectOf("wire-space-object-dots")
        val dots = (decodeInput("wire-space-object-dots") as? ZenSpaces.DecodedRecord.Space)?.record
            ?: fail("wire-space-object-dots must decode as space")
        val dot = dots.theme?.dots?.single() ?: fail("expected one dot")
        val dotExpect = dotsExpect.getJSONArray("dots").getJSONObject(0)
        assertEquals(dotExpect.getString("hex"), dot.color.hexString)
        assertEquals(dotExpect.getBoolean("isPrimary"), dot.isPrimary)
        assertEquals(dotExpect.getDouble("lightness"), dot.lightness)
        assertEquals(dotExpect.getDouble("positionX"), dot.positionX)
        assertEquals(dotExpect.getDouble("positionY"), dot.positionY)
        assertEquals(dotExpect.getString("type"), dot.type)

        val rgbExpect = expectOf("wire-space-rgb-dots")
        val rgb = (decodeInput("wire-space-rgb-dots") as? ZenSpaces.DecodedRecord.Space)?.record
            ?: fail("wire-space-rgb-dots must decode as space")
        assertEquals(expectStrings(rgbExpect, "gradientColors"), rgb.theme?.gradientColors)
    }

    @Test
    fun wireTabFixturesDecode() {
        val pinnedDefaultExpect = expectOf("wire-tab-pinned-default")
        val pinnedDefault = (decodeInput("wire-tab-pinned-default") as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: fail("wire-tab-pinned-default must decode as tab")
        assertEquals(expectNullableBool(pinnedDefaultExpect, "pinned"), pinnedDefault.pinned)
        assertEquals(pinnedDefaultExpect.getBoolean("isNormalTab"), pinnedDefault.isNormalTab)
        assertEquals(expectNullableString(pinnedDefaultExpect, "folderId"), pinnedDefault.folderId)

        val normalExpect = expectOf("wire-tab-normal-pinned-false")
        val normal = (decodeInput("wire-tab-normal-pinned-false") as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: fail("wire-tab-normal-pinned-false must decode as tab")
        assertEquals(normalExpect.getBoolean("pinned"), normal.pinned)
        assertEquals(normalExpect.getBoolean("isNormalTab"), normal.isNormalTab)

        val stringFalseExpect = expectOf("wire-tab-pinned-string-false")
        val stringFalse = (decodeInput("wire-tab-pinned-string-false") as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: fail("wire-tab-pinned-string-false must decode as tab")
        assertEquals(stringFalseExpect.getBoolean("pinned"), stringFalse.pinned)
        assertEquals(stringFalseExpect.getBoolean("isNormalTab"), stringFalse.isNormalTab)
    }

    @Test
    fun wireFolderFixturesDecode() {
        val basicExpect = expectOf("wire-folder-basic")
        val basic = (decodeInput("wire-folder-basic") as? ZenSpaces.DecodedRecord.Folder)?.record
            ?: fail("wire-folder-basic must decode as folder")
        assertEquals(basicExpect.getString("folderId"), basic.folderId)
        assertEquals(expectStrings(basicExpect, "children"), basic.children)
        assertEquals(expectNullableString(basicExpect, "parentFolderId"), basic.parentFolderId)

        val liveExpect = expectOf("wire-folder-live-object")
        val live = (decodeInput("wire-folder-live-object") as? ZenSpaces.DecodedRecord.Folder)?.record
            ?: fail("wire-folder-live-object must decode as folder")
        assertEquals(expectNullableString(liveExpect, "icon"), live.icon)
        assertEquals(expectStrings(liveExpect, "children"), live.children)

        // Hostile (wire-folder-missing-folderid): folder-shaped for matching,
        // dropped by the decoder, never matches any target request.
        val hostile = FixtureLoader.json("wire-folder-missing-folderid")
        val hostileInput = hostile.getJSONObject("input")
        val hostileExpect = hostile.getJSONObject("expect")
        // non-canonical, platform-local (this fixture's expect has no dropped key).
        assertNull(ZenSpaces.decode(hostileInput.optString("id"), hostileInput))
        val data = hostileInput.getJSONObject("data")
        val requests = hostileInput.getJSONArray("targetRequests")
        val namedTarget = (0 until requests.length()).mapNotNull { requests.opt(it) as? String }.single()
        assertEquals(hostileExpect.getBoolean("matchesNilTarget"), SpacesSyncService.isTargetFolder(null, data))
        assertEquals(hostileExpect.getBoolean("matchesFolder1"), SpacesSyncService.isTargetFolder(namedTarget, data))
    }

    @Test
    fun wireSplitAndLayoutFixturesDecode() {
        val splitExpect = expectOf("wire-split-basic")
        val split = (decodeInput("wire-split-basic") as? ZenSpaces.DecodedRecord.Split)?.record
            ?: fail("wire-split-basic must decode as split")
        assertEquals(expectNullableBool(splitExpect, "pinned"), split.pinned)
        assertEquals(splitExpect.getBoolean("isNormalSplit"), split.isNormalSplit)
        assertEquals(expectStrings(splitExpect, "tabs"), split.tabs)

        val normalSplitExpect = expectOf("wire-split-normal-pinned-false")
        val normalSplit = (decodeInput("wire-split-normal-pinned-false") as? ZenSpaces.DecodedRecord.Split)?.record
            ?: fail("wire-split-normal-pinned-false must decode as split")
        assertEquals(normalSplitExpect.getBoolean("pinned"), normalSplit.pinned)
        assertEquals(normalSplitExpect.getBoolean("isNormalSplit"), normalSplit.isNormalSplit)

        val layoutExpect = expectOf("wire-layout-basic")
        val layout = (decodeInput("wire-layout-basic") as? ZenSpaces.DecodedRecord.Layout)?.record
            ?: fail("wire-layout-basic must decode as layout")
        assertEquals(expectStrings(layoutExpect, "spaces"), layout.spaces)
        val essentialsExpect = layoutExpect.getJSONObject("essentials")
        val expectedEssentials = linkedMapOf<String, List<String>>()
        for (bucket in essentialsExpect.keys()) {
            expectedEssentials[bucket] = expectStrings(essentialsExpect, bucket)
        }
        assertEquals(expectedEssentials, layout.essentials)
    }

    @Test
    fun ignoredRecordsAreDropped() {
        val cases = FixtureLoader.cases("wire-ignored-records")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            assertTrue(
                "case '${case.getString("id")}' must expect dropped",
                case.getJSONObject("expect").getBoolean("dropped"),
            )
            assertNull(
                "case '${case.getString("id")}' must be dropped",
                ZenSpaces.decode(case.getJSONObject("input").optString("id"), case.getJSONObject("input")),
            )
        }
    }

    // MARK: Prefs parser

    @Test
    fun prefsParserMatchesFixtureShapes() {
        val cases = FixtureLoader.cases("wire-prefs-normal-tabs")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val expect = case.getJSONObject("expect")
            val parsed = SpacesSyncService.parsePrefBool(case.getJSONObject("input").opt("value"))
            assertEquals(
                "case '${case.getString("id")}'",
                if (expect.isNull("prefBool")) null else expect.getBoolean("prefBool"),
                parsed,
            )
        }
    }

    /**
     * Tri-state write gate (SPEC §7, `wire-prefs-normal-tabs-capability`):
     * enabled only when the key parses true; present-but-false/null/unparseable
     * is disabled; a missing record or key is absent.
     */
    @Test
    fun normalTabsCapabilityFixtureDerives() {
        val cases = FixtureLoader.cases("wire-prefs-normal-tabs-capability")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val input = case.getJSONObject("input")
            val values = if (input.getBoolean("prefsRecordPresent")) {
                input.optJSONObject("values")
            } else {
                null
            }
            val actual = SpacesSyncService.deriveNormalTabsCapability(values)
            assertEquals(
                "case '${case.getString("id")}'",
                case.getJSONObject("expect").getString("normalTabsCapability"),
                actual.name.lowercase(),
            )
        }
    }

    // MARK: Essentials merge (URL de-duplication across buckets)

    /**
     * `wire-layout-essentials-dup-url`: merging buckets for shared display
     * de-duplicates by URL across buckets (first occurrence wins, default
     * first then stable key order) and preserves duplicates within a bucket.
     */
    @Test
    fun essentialsMergeDedupesUrlAcrossBuckets() {
        val cases = FixtureLoader.cases("wire-layout-essentials-dup-url")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val buckets = case.getJSONObject("input").getJSONObject("buckets")
            val essentials = buckets.keys().asSequence().associateWith { key ->
                val arr = buckets.getJSONArray(key)
                (0 until arr.length()).map { j ->
                    val t = arr.getJSONObject(j)
                    ZenSpaces.ZenTab(t.getString("id"), t.getString("url"), t.getString("title"))
                }
            }
            val space = ZenSpaces.ZenSpace(
                id = "s",
                name = "s",
                icon = null,
                containerGuid = null,
                theme = null,
                pinned = emptyList(),
            )
            val snapshot = ZenSpaces.ZenSnapshot(listOf(space), essentials, 0L)
            val expected = case.getJSONObject("expect").getJSONArray("ids").let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            assertEquals(
                "case '${case.getString("id")}'",
                expected,
                snapshot.essentialsFor(space, ZenSpaces.EssentialsGrouping.SHARED).map { it.id },
            )
        }
    }

    // MARK: D2 — target-folder predicate

    @Test
    fun targetFolderPredicateHostileShapes() {
        val basic = FixtureLoader.json("wire-folder-basic").getJSONObject("input").getJSONObject("data")
        // Basic folder matches only its own id.
        assertTrue(SpacesSyncService.isTargetFolder("folder-1", basic))
        assertFalse(SpacesSyncService.isTargetFolder("folder-2", basic))
        // A nil/empty target never matches a folder.
        assertFalse(SpacesSyncService.isTargetFolder(null, basic))
        assertFalse(SpacesSyncService.isTargetFolder("", basic))

        val hostile = FixtureLoader.json("wire-folder-missing-folderid").getJSONObject("input")
        val hostileData = hostile.getJSONObject("data")
        for (i in 0 until hostile.getJSONArray("targetRequests").length()) {
            val request = hostile.getJSONArray("targetRequests").opt(i) as? String
            assertFalse(
                "missing-folderId record must never match request $request",
                SpacesSyncService.isTargetFolder(request, hostileData),
            )
        }

        // Strict string read: numbers and booleans are never coerced (SPEC §3.1).
        assertFalse(SpacesSyncService.isTargetFolder("42", JSONObject().put("folderId", 42)))
        assertFalse(SpacesSyncService.isTargetFolder("true", JSONObject().put("folderId", true)))
        assertFalse(SpacesSyncService.isTargetFolder("folder-1", JSONObject()))
    }

    // MARK: D13/D14 — hostile record shapes

    @Test
    fun hostileShapesDeletedAndUuidAreStrict() {
        // D13 (wire-space-numeric-uuid): numeric uuid drops the record.
        assertNull(decodeInput("wire-space-numeric-uuid"))
        // Booleans are not coerced either.
        val boolUuid = JSONObject(
            """{"id":"s","kind":"space","data":{"uuid":true,"name":"Bool"}}""",
        )
        assertNull(ZenSpaces.decode("s", boolUuid))

        // D14 (wire-deleted-string): the string "true" is NOT a tombstone.
        val deletedString = (decodeInput("wire-deleted-string") as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: fail("wire-deleted-string must still decode as tab")
        assertEquals("tab-a", deletedString.tabId)
        assertEquals("https://example.com", deletedString.url)
        assertEquals("Example", deletedString.title)

        // Numbers are not tombstones either; the record still decodes.
        val deletedNumber = JSONObject(
            """{"id":"t","kind":"tab","deleted":1,"data":{"tabId":"t","url":"https://x.de"}}""",
        )
        assertTrue(ZenSpaces.decode("t", deletedNumber) is ZenSpaces.DecodedRecord.Tab)

        // A real boolean tombstone drops.
        val tombstone = JSONObject("""{"id":"t","kind":"tab","deleted":true,"data":{}}""")
        assertNull(ZenSpaces.decode("t", tombstone))
    }

    // MARK: D7 — load log summary

    @Test
    fun loadLogSummaryMatchesIOSFields() {
        val line = SpacesSyncService.loadLogSummary(
            recordCount = 10, spaces = 3, tabs = 5, folders = 1, splits = 1,
            essentials = 2, normalTabsOn = true, separateEssentials = false,
            decryptFailures = 2, skippedKinds = 1, gatedNormalItems = 3,
        )
        assertTrue(line.contains("10 records"))
        assertTrue(line.contains("2 decrypt failures"))
        assertTrue(line.contains("1 ignored"))
        assertTrue(line.contains("spaces: 3"))
        assertTrue(line.contains("tabs: 5"))
        assertTrue(line.contains("folders: 1"))
        assertTrue(line.contains("splits: 1"))
        assertTrue(line.contains("essentials: 2"))
        assertTrue(line.contains("normal-tabs pref: true"))
        assertTrue(line.contains("separate-essentials pref: false"))
        assertTrue(line.contains("gated normal items: 3"))
    }

    // MARK: Auth errno mapping (D3)

    /**
     * Fixture-driven errno mapping (SPEC §7.1): HTTP >= 400 + errno 103
     * (two-step auth enabled) maps to TotpRequired, so the rethrow in
     * syncCredentials and the FriendlyError mapping are reachable; any other
     * error body stays a plain Auth error.
     */
    @Test
    fun authErrnoFixtureMapsErrorKinds() {
        val client = FxAClient()
        val cases = FixtureLoader.cases("auth-errno-103")
        for (i in 0 until cases.length()) {
            val fixtureCase = cases.getJSONObject(i)
            val input = fixtureCase.getJSONObject("input")
            val expect = fixtureCase.getJSONObject("expect")
            val body = input.getJSONObject("body").toString().toByteArray(Charsets.UTF_8)
            try {
                client.parseBodyOrThrow(FxAClient.RawResponse(input.getInt("httpStatus"), body))
                fail("case ${fixtureCase.optString("id")}: expected an error")
            } catch (e: SyncError.TotpRequired) {
                assertEquals(fixtureCase.optString("id"), "totpRequired", expect.getString("errorKind"))
            } catch (e: SyncError.Auth) {
                assertEquals(fixtureCase.optString("id"), "auth", expect.getString("errorKind"))
                if (!expect.isNull("message")) {
                    assertEquals(fixtureCase.optString("id"), expect.getString("message"), e.message)
                }
            }
        }
    }

    // MARK: Crypto vectors

    @Test
    fun bsoEnvelopeVectorsMatch() {
        val valid = FixtureLoader.json("crypto-bso-envelope-valid")
        val validInput = valid.getJSONObject("input")
        val validExpect = valid.getJSONObject("expect")
        val keys = SyncCrypto.KeyBundle(
            encryptionKey = FxACrypto.unhex(validInput.getString("encryptionKeyHex")),
            hmacKey = FxACrypto.unhex(validInput.getString("hmacKeyHex")),
        )
        val envelope = validInput.getJSONObject("envelope")
        val plaintext = SyncCrypto.decryptBSO(envelope.toString(), keys)
        assertEquals(validExpect.getString("plaintextUtf8"), String(plaintext))
        // HMAC is computed over the base64 ciphertext STRING bytes (SPEC §4).
        assertEquals(
            envelope.getString("hmac"),
            FxACrypto.hex(
                FxACrypto.hmacSHA256(
                    keys.hmacKey,
                    envelope.getString("ciphertext").toByteArray(Charsets.UTF_8),
                ),
            ).lowercase(),
        )
        // AES-256-CBC/PKCS7 with the fixture IV reproduces the exact ciphertext.
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keys.encryptionKey, "AES"),
            IvParameterSpec(Base64.getDecoder().decode(envelope.getString("IV"))),
        )
        assertEquals(
            validExpect.getString("recomputedCiphertext"),
            Base64.getEncoder()
                .encodeToString(cipher.doFinal(validExpect.getString("plaintextUtf8").toByteArray(Charsets.UTF_8))),
        )

        val tampered = FixtureLoader.json("crypto-bso-envelope-tampered-hmac")
        val tamperedExpect = tampered.getJSONObject("expect")
        try {
            SyncCrypto.decryptBSO(tampered.getJSONObject("input").getJSONObject("envelope").toString(), keys)
            fail("expected ${tamperedExpect.getString("error")}")
        } catch (e: SyncError.Crypto) {
            assertEquals(tamperedExpect.getString("error"), e.message)
        }
    }

    @Test
    fun cryptoDerivationVectorsMatch() {
        // HKDF RFC 5869 case 1.
        val hkdf = FixtureLoader.json("crypto-hkdf-rfc5869-case1")
        assertEquals(
            hkdf.getJSONObject("expect").getString("okmHex"),
            FxACrypto.hex(
                FxACrypto.hkdf(
                    secret = FxACrypto.unhex(hkdf.getJSONObject("input").getString("ikmHex")),
                    info = FxACrypto.unhex(hkdf.getJSONObject("input").getString("infoHex")),
                    length = hkdf.getJSONObject("input").getInt("length"),
                    salt = FxACrypto.unhex(hkdf.getJSONObject("input").getString("saltHex")),
                ),
            ),
        )

        // oldsync sync key bundle from zero kB.
        val bundle = FixtureLoader.json("crypto-sync-key-bundle-kb-zero")
        val bundleExpect = bundle.getJSONObject("expect")
        val syncBundle = SyncCrypto.syncKeyBundle(FxACrypto.unhex(bundle.getJSONObject("input").getString("kbHex")))
        assertEquals(bundleExpect.getString("encryptionKeyHex"), FxACrypto.hex(syncBundle.encryptionKey))
        assertEquals(bundleExpect.getString("hmacKeyHex"), FxACrypto.hex(syncBundle.hmacKey))

        // sessionToken token material: id + authKey + bundleKey.
        val token = FixtureLoader.json("crypto-token-material-session-token")
        val tokenExpect = token.getJSONObject("expect")
        val (idHex, authKey, bundleKey) = FxACrypto.tokenMaterial(
            FxACrypto.unhex(token.getJSONObject("input").getString("tokenHex")),
            token.getJSONObject("input").getString("type"),
        )
        assertEquals(tokenExpect.getString("idHex"), idHex)
        assertEquals(tokenExpect.getString("authKeyHex"), FxACrypto.hex(authKey))
        assertEquals(tokenExpect.getString("bundleKeyHex"), FxACrypto.hex(bundleKey))

        // clientStateBytes rendered as hex.
        val state = FixtureLoader.json("crypto-client-state-bytes-kb-zero").getJSONObject("expect")
        assertEquals(state.getString("stateHex"), FxACrypto.hex(FxACrypto.clientStateBytes(ByteArray(32))))

        // account/keys unbundle.
        val unbundle = FixtureLoader.json("crypto-unbundle-account-keys")
        val unbundleInput = unbundle.getJSONObject("input")
        val plain = FxACrypto.unbundle(
            FxACrypto.unhex(unbundleInput.getString("bundleKeyHex")),
            unbundleInput.getString("namespace"),
            FxACrypto.unhex(unbundleInput.getString("payloadHex")),
        )
        assertEquals(unbundle.getJSONObject("expect").getString("plaintextUtf8"), String(plain))
    }

    // MARK: Hawk vectors

    @Test
    fun hawkVectorsMatch() {
        val auth = FixtureLoader.json("hawk-authorization-resource")
        val authInput = auth.getJSONObject("input")
        val authExpect = auth.getJSONObject("expect")
        val key = FxACrypto.unhex(authInput.getString("keyHex"))
        val lines = authExpect.getJSONArray("normalizedLines")
        val normalized = (0 until lines.length()).joinToString("\n") { lines.getString(it) }
        assertEquals(authExpect.getString("normalized"), normalized)
        assertEquals(authExpect.getString("macBase64"), HawkAuth.macFor(normalized, key))
        assertEquals(
            authExpect.getString("authorization"),
            HawkAuth.authorization(
                method = authInput.getString("method"),
                url = URL(authInput.getString("url")),
                id = authInput.getString("id"),
                key = key,
                payloadHash = if (authInput.isNull("payloadHash")) null else authInput.getString("payloadHash"),
                resource = authInput.getString("resource"),
                fixedTimestamp = authInput.getLong("fixedTimestamp"),
                fixedNonce = authInput.getString("fixedNonce"),
            ),
        )

        val hash = FixtureLoader.json("hawk-payload-hash")
        assertEquals(
            hash.getJSONObject("expect").getString("hashBase64"),
            HawkAuth.payloadHash(
                hash.getJSONObject("input").getString("bodyUtf8").toByteArray(Charsets.UTF_8),
                hash.getJSONObject("input").getString("contentType"),
            ),
        )
    }

    // MARK: BSO ids

    @Test
    fun bsoIdEncodingMatchesFixture() {
        val cases = FixtureLoader.cases("bso-ids-percent-encoding")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            assertEquals(
                case.getJSONObject("expect").getString("encoded"),
                SyncClient.encodedBSOId(case.getJSONObject("input").getString("id")),
            )
        }
    }
}
