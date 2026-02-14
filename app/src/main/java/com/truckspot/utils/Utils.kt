package com.truckspot.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import androidx.appcompat.app.AlertDialog


object Utils {


    @JvmStatic
    fun getDouble(timeString: String): Double {
        return if (timeString.isNullOrEmpty()) {
            0.0 // Return a default value if the time is not set or empty
        } else {
            try {
                timeString.toDouble() // Parse the retrieved string to a double value
            } catch (e: NumberFormatException) {
                0.0 // Return default value if parsing fails
            }
        }
    }

    fun Int.toHoursMinutesFormate(): String{
        val absMinutes = kotlin.math.abs(this)
        val hours = absMinutes / 60
        val min = absMinutes % 60
        val sign = if (this < 0) "-" else ""
        return String.format("%s%02d:%02d", sign, hours, min)
    }

     @JvmStatic
    fun dialog(context:Context,title:String?=null,message:String?,positiveText:String?="Ok",negativeText:String?=null,callback: dialogInterface?=null) {

        val alertDialog = AlertDialog.Builder(context).create()
         if(title!=null)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, positiveText
        ) { dialog, which ->
            dialog.dismiss()
            callback?.positiveClick()
        }

        if(!negativeText.isNullOrEmpty())
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negativeText
        ) { dialog, which ->
            dialog.dismiss()
            callback?.negativeClick()
        }
        alertDialog.show()

//        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
//        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
//
//        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
//        layoutParams.weight = 10f
//        btnPositive.layoutParams = layoutParams
//        btnNegative.layoutParams = layoutParams
    }

    fun getScreenWidth(context: Activity): Int
    {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        return width
    }
    interface dialogInterface {
        fun positiveClick()
        fun negativeClick()
    }

}