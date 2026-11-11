package dev.nerzouille.koan.auth.testing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class ActivityResultFixture : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fun reply() {
            setResult(if (intent.getBooleanExtra("cancel", false)) RESULT_CANCELED else RESULT_OK,
                Intent().putExtra("fixture-result", "returned"))
            finish()
        }
        if (intent.getBooleanExtra("hold", false)) {
            setContentView(Button(this).apply { text = "Return test result"; setOnClickListener { reply() } })
        } else reply()
    }
}
