package com.truckspot
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//
//class MyDialogReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context?, intent: Intent?) {
//        if (context != null) {
//            Log.v("MyDialogReceiver", "Received broadcast")
//            // Show the alert when the alarm triggers
//            val alertDialog = AlertDialog.Builder(context)
//                .setTitle("Logs Certification")
//                .setMessage("Please certify")
//                .setPositiveButton("OK") { dialog, _ ->
//                    dialog.dismiss()
//                }
//                .create()
//
//            alertDialog.show()
//        }
//    }
//}
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
            Log.v("MyDialogReceiver", "Received broadcast - Alarm triggered")
            // Note: Cannot show Dialog from Background Receiver.
            // TODO: Implement Notification or start Activity if needed.
            // val intent = Intent(context, HomeActivity::class.java)
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // context.startActivity(intent)
        }
    }
}
