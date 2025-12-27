package com.truckspot

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment


class MyDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create and configure your custom dialog here
        val builder: AlertDialog.Builder = AlertDialog.Builder(getActivity())
        builder.setTitle("Logs Certification")
            .setMessage("Your Last 24 hours logs are ready to be certified")
            .setPositiveButton(
                "OK",
                DialogInterface.OnClickListener { dialog, id -> // Handle the "OK" button click
                    dialog.dismiss()
                })
        return builder.create()
    }
}