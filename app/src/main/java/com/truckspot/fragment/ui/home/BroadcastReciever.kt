package com.truckspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MyDialogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            Log.v("MyDialogReceiver", "Received broadcast")
            // Show the alert when the alarm triggers
            val alertDialog = AlertDialog.Builder(context)
                .setTitle("Logs Certification")
                .setMessage("Please certify")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            alertDialog.show()
        }
    }
}
