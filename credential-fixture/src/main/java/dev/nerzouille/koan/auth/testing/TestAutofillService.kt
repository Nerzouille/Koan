package dev.nerzouille.koan.auth.testing

import android.app.Activity
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.autofill.AutofillManager
import android.widget.RemoteViews
import java.util.UUID

/** Only fills the two synthetic fields on the localhost fixture page. */
class TestAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        var local = false
        var username: AutofillId? = null
        var password: AutofillId? = null
        fun visit(node: AssistStructure.ViewNode) {
            if (node.webDomain == "localhost" || android.net.Uri.parse(node.webDomain.orEmpty()).host == "localhost") local = true
            val attributes = node.htmlInfo?.attributes.orEmpty().associate { it.first to it.second }
            val id = attributes["id"] ?: attributes["name"]
            if (id == "username") username = node.autofillId
            if (id == "password") password = node.autofillId
            repeat(node.childCount) { visit(node.getChildAt(it)) }
        }
        repeat(structure.windowNodeCount) { visit(structure.getWindowNodeAt(it).rootViewNode) }
        if (!local || username == null || password == null) { callback.onSuccess(null); return }
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Koan test login")
        }
        val dataset = Dataset.Builder(presentation)
            .setValue(username!!, AutofillValue.forText("test-user"))
            .setValue(password!!, AutofillValue.forText("test-password")).build()
        val response = FillResponse.Builder()
        if (FixtureBiometric.enabled(this)) {
            val intent = Intent(this, TestAutofillAuthActivity::class.java)
                .setData(Uri.parse("koan-test://autofill-auth/${UUID.randomUUID()}"))
                .putExtra("username-id", username).putExtra("password-id", password)
            val auth = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            response.setAuthentication(arrayOf(username!!, password!!), auth.intentSender, presentation)
        } else response.addDataset(dataset)
        callback.onSuccess(response.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) = callback.onSuccess()
}

class TestAutofillAuthActivity : Activity() {
    private var biometric: CancellationSignal? = null
    private var started = false

    override fun onPostResume() {
        super.onPostResume()
        if (started) return
        started = true
        biometric = FixtureBiometric.authenticate(this) {
            val username = requireNotNull(intent.getParcelableExtra("username-id", AutofillId::class.java))
            val password = requireNotNull(intent.getParcelableExtra("password-id", AutofillId::class.java))
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "Koan test login")
            }
            val dataset = Dataset.Builder(presentation)
                .setValue(username, AutofillValue.forText("test-user"))
                .setValue(password, AutofillValue.forText("test-password")).build()
            val response = FillResponse.Builder().addDataset(dataset).build()
            setResult(RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response))
            finish()
        }
    }

    override fun onDestroy() {
        biometric?.cancel()
        super.onDestroy()
    }
}
