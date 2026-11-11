package dev.nerzouille.koan.auth.testing

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

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
        callback.onSuccess(FillResponse.Builder().addDataset(dataset).build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) = callback.onSuccess()
}
