package com.eagleye.eld

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen blocking "Update Required" gate. Shown when the backend version gate
 * reports the installed build is no longer supported. Not dismissible — the only way
 * forward is to update. Back press exits the app rather than bypassing the gate.
 */
class UpdateRequiredActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_required)

        val message = intent.getStringExtra(EXTRA_MESSAGE)
        val storeUrl = intent.getStringExtra(EXTRA_STORE_URL)
            ?: "https://play.google.com/store/apps/details?id=$packageName"

        if (!message.isNullOrBlank()) {
            findViewById<TextView>(R.id.updateMessage).text = message
        }

        findViewById<android.view.View>(R.id.btnUpdate).setOnClickListener {
            openStore(storeUrl)
        }
    }

    private fun openStore(storeUrl: String) {
        try {
            // Prefer the Play Store app, fall back to the browser.
            val marketUri = Uri.parse("market://details?id=$packageName")
            startActivity(Intent(Intent.ACTION_VIEW, marketUri).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)))
        }
    }

    @Deprecated("Block back to keep the gate")
    override fun onBackPressed() {
        // Don't let the user back out of the gate — exit instead.
        finishAffinity()
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_update_message"
        const val EXTRA_STORE_URL = "extra_store_url"
    }
}
