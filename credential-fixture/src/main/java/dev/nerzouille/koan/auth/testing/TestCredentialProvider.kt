package dev.nerzouille.koan.auth.testing

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.*
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Localhost-only fixture. This service and its keys are never included in a user APK. */
class TestCredentialProvider : CredentialProviderService() {
    private fun action(kind: String): PendingIntent = PendingIntent.getActivity(
        this, 0,
        // Candidate queries may overlap. Never cancel an entry handed to an earlier query.
        Intent(this, TestCredentialActivity::class.java).setData(Uri.parse("koan-test://$kind/${UUID.randomUUID()}")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest, cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val json = request.candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        if (json != null && JSONObject(json).optJSONObject("user")?.optString("name") == "no-options") {
            callback.onResult(BeginCreateCredentialResponse.Builder().build())
            return
        }
        callback.onResult(BeginCreateCredentialResponse.Builder()
            .addCreateEntry(CreateEntry.Builder("Koan test account", action("create")).build()).build())
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest, cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        val option = request.beginGetCredentialOptions.filterIsInstance<BeginGetPublicKeyCredentialOption>().first()
        val entry = PublicKeyCredentialEntry.Builder(this, "Koan test account", action("get"), option)
            .setDisplayName("Koan test account").setAutoSelectAllowed(true).build()
        callback.onResult(BeginGetCredentialResponse.Builder().addCredentialEntry(entry).build())
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest, cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) = callback.onResult(null)
}

class TestCredentialActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val result = Intent()
            if (intent.data?.host == "create") {
                val request = requireNotNull(PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent))
                val origin = verifiedOrigin(request.callingAppInfo)
                val options = request.callingRequest as CreatePublicKeyCredentialRequest
                when (JSONObject(options.requestJson).getJSONObject("user").getString("name")) {
                    "provider-error" -> PendingIntentHandler.setCreateCredentialException(result, CreateCredentialUnknownException("Synthetic provider failure"))
                    "malformed-response" -> PendingIntentHandler.setCreateCredentialResponse(result, CreatePublicKeyCredentialResponse("{}"))
                    else -> {
                        val json = TestPasskey(this).create(options.requestJson, origin)
                        PendingIntentHandler.setCreateCredentialResponse(result, CreatePublicKeyCredentialResponse(json))
                    }
                }
            } else {
                val request = requireNotNull(PendingIntentHandler.retrieveProviderGetCredentialRequest(intent))
                val origin = verifiedOrigin(request.callingAppInfo)
                val options = request.credentialOptions.single() as GetPublicKeyCredentialOption
                val json = TestPasskey(this).get(options.requestJson, origin, options.clientDataHash)
                PendingIntentHandler.setGetCredentialResponse(result, GetCredentialResponse(PublicKeyCredential(json)))
            }
            setResult(RESULT_OK, result)
        } catch (error: Exception) {
            android.util.Log.e("KoanCredentialFixture", "Fixture failed", error)
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    private fun verifiedOrigin(caller: CallingAppInfo): String {
        val packages = setOf("dev.nerzouille.koan.debug", "dev.nerzouille.koan.staging")
        check(caller.packageName in packages)
        val signing = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo!!
        val sha = MessageDigest.getInstance("SHA-256").digest(signing.apkContentsSigners.single().toByteArray())
            .joinToString(":") { "%02X".format(it) }
        val info = JSONObject().put("package_name", caller.packageName)
            .put("signatures", JSONArray().put(JSONObject().put("build", "release").put("cert_fingerprint_sha256", sha)))
        val trusted = JSONObject().put("apps", JSONArray().put(JSONObject().put("type", "android").put("info", info)))
        return requireNotNull(caller.getOrigin(trusted.toString())).also { check(it == "http://localhost:8786") }
    }
}
