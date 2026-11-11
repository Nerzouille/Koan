package dev.nerzouille.koan.auth.testing

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import org.json.JSONArray
import org.json.JSONObject

/** An actual P-256 signer, scoped to the local test RP. No real vault or user verification. */
class TestPasskey(context: Context) {
    private val prefs = context.getSharedPreferences("passkey-fixture", Context.MODE_PRIVATE)
    private val key: KeyPair = prefs.getString("private", null)?.let { privateKey ->
        val factory = KeyFactory.getInstance("EC")
        KeyPair(factory.generatePublic(X509EncodedKeySpec(decode(prefs.getString("public", "")!!))),
            factory.generatePrivate(PKCS8EncodedKeySpec(decode(privateKey))))
    } ?: KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair().also {
        prefs.edit().putString("private", encode(it.private.encoded)).putString("public", encode(it.public.encoded)).commit()
    }
    private val id = MessageDigest.getInstance("SHA-256").digest(key.public.encoded)

    fun create(optionsJson: String, origin: String): String {
        val options = JSONObject(optionsJson)
        check(options.getJSONObject("rp").getString("id") == "localhost")
        val pub = key.public as ECPublicKey
        val cose = cbor(linkedMapOf(1 to 2, 3 to -7, -1 to 1, -2 to coordinate(pub.w.affineX), -3 to coordinate(pub.w.affineY)))
        val auth = authData(0x45) + ByteArray(16) + byteArrayOf(0, id.size.toByte()) + id + cose
        val attestation = cbor(linkedMapOf("fmt" to "none", "attStmt" to emptyMap<Any, Any>(), "authData" to auth))
        return response(JSONObject().put("clientDataJSON", clientData("webauthn.create", options, origin))
            .put("attestationObject", encode(attestation)).put("transports", JSONArray().put("internal")))
    }

    fun get(optionsJson: String, origin: String, clientDataHash: ByteArray?): String {
        val options = JSONObject(optionsJson)
        check(options.getString("rpId") == "localhost")
        val data = clientData("webauthn.get", options, origin)
        val auth = authData(0x05)
        val hash = clientDataHash ?: MessageDigest.getInstance("SHA-256").digest(decode(data))
        val signature = Signature.getInstance("SHA256withECDSA").apply { initSign(key.private); update(auth + hash) }.sign()
        return response(JSONObject().put("clientDataJSON", data).put("authenticatorData", encode(auth))
            .put("signature", encode(signature)).put("userHandle", encode("test-user".toByteArray())))
    }

    private fun authData(flags: Int) = MessageDigest.getInstance("SHA-256").digest("localhost".toByteArray()) +
        byteArrayOf(flags.toByte(), 0, 0, 0, 0)

    private fun clientData(type: String, options: JSONObject, origin: String) = encode(JSONObject()
        .put("type", type).put("challenge", options.getString("challenge")).put("origin", origin)
        .put("crossOrigin", false).toString().toByteArray())

    private fun response(data: JSONObject) = JSONObject().put("id", encode(id)).put("rawId", encode(id))
        .put("type", "public-key").put("authenticatorAttachment", "platform")
        .put("clientExtensionResults", JSONObject()).put("response", data).toString()

    private fun coordinate(value: BigInteger) = value.toByteArray().takeLast(32).toByteArray().let {
        ByteArray(32 - it.size) + it
    }

    private fun cbor(value: Any): ByteArray {
        val out = ByteArrayOutputStream()
        fun header(major: Int, length: Int) {
            when {
                length < 24 -> out.write(major * 32 + length)
                length < 256 -> { out.write(major * 32 + 24); out.write(length) }
                else -> { out.write(major * 32 + 25); out.write(length ushr 8); out.write(length) }
            }
        }
        when (value) {
            is Int -> if (value >= 0) header(0, value) else header(1, -1 - value)
            is ByteArray -> { header(2, value.size); out.write(value) }
            is String -> { val bytes = value.toByteArray(); header(3, bytes.size); out.write(bytes) }
            is Map<*, *> -> { header(5, value.size); value.forEach { (k, v) -> out.write(cbor(k!!)); out.write(cbor(v!!)) } }
            else -> error("Unsupported fixture CBOR value")
        }
        return out.toByteArray()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
}
