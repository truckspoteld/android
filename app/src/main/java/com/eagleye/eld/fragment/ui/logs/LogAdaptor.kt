package com.eagleye.eld.fragment.ui.logs

import LogModalFragment
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.eagleye.eld.R
import com.eagleye.eld.models.UserLog
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.widget.LinearLayout
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.request.AddLogRequest
import com.google.android.material.snackbar.Snackbar
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.utils.AlertCalculationUtils.setDateAndTimeBasedOnTimezone
import com.eagleye.eld.utils.AlertCalculationUtils.formatTimeWithTimezone
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class LogAdaptor  (
    private val dataSet: List<GetLogsByDateResponse.Results.UserLog>, 
    private val fragmentManager: FragmentManager, 
    private val contect : Context,
    private val timeZone: String = "PST"
) :
    RecyclerView.Adapter<LogAdaptor.ViewHolder>() {
    private var previousLog: UserLog? = null

    private lateinit var homeViewModel: HomeViewModel
     private val handler = Handler()

    private fun normalizeEngineMode(modeName: String): String {
        return when (modeName.trim().lowercase(Locale.US)) {
            "eng_on", "e_on", "power_on" -> "eng_on"
            "eng_off", "e_off", "power_off" -> "eng_off"
            else -> ""
        }
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.time_txt)
        val tvStatusPill: TextView = view.findViewById(R.id.status_pill)
        val tvStatusTitle: TextView = view.findViewById(R.id.status_title)
        val tvLocation: TextView = view.findViewById(R.id.location_txt)
        val tvOdo: TextView = view.findViewById(R.id.odo_txt)
        val tvEng: TextView = view.findViewById(R.id.eng_txt)
        val tvorigon: TextView = view.findViewById(R.id.origin_txt)
        val tvUnit: TextView = view.findViewById(R.id.unit_txt)
        val statusIndicator: View = view.findViewById(R.id.status_indicator)
        val tile: View = view.findViewById(R.id.logTIle)
    }

    private fun displaySnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.text_log_item, viewGroup, false)

        return ViewHolder(view)
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (position < 0 || position >= dataSet.size) return
        
        val userLog = dataSet[position] ?: return
        val modeValue = userLog.modename.trim().lowercase(Locale.US)
        val normalizedEngineMode = normalizeEngineMode(modeValue)

        // Item entrance animation with standardized fluid stagger
        val animationDelay = (position % 8) * 40L
        YoYo.with(Techniques.FadeInLeft)
            .duration(400)
            .delay(animationDelay)
            .playOn(viewHolder.itemView)

        viewHolder.itemView.setOnClickListener {
            if (modeValue == "login" || modeValue == "logout" || modeValue == "certification" || normalizedEngineMode.isNotEmpty()) {
                // No action for system/other logs
            } else {
                val modalFragment = LogModalFragment(userLog)
                modalFragment.show(fragmentManager, "LogModalFragment")
            }
        }

        // Time
        val time = userLog.time ?: ""
        viewHolder.tvTime.text = formatTimeWithTimezone(time)
        
        // Status & Colors
        var statusLabel = modeValue.uppercase(Locale.US)
        var statusColor = Color.parseColor("#8DA0B6") // Default OFF Grey
        
        when {
            modeValue == "off" -> {
                statusLabel = "OFF"
                statusColor = Color.parseColor("#8DA0B6")
            }
            modeValue == "sb" -> {
                statusLabel = "SB"
                statusColor = Color.parseColor("#4169E1")
            }
            modeValue == "d" -> {
                statusLabel = if (userLog.discreption == "Intermediate log") "INT" else "DR"
                statusColor = Color.parseColor("#2D7BFE")
            }
            modeValue == "on" -> {
                statusLabel = "ON"
                statusColor = Color.parseColor("#10B981")
            }
            normalizedEngineMode == "eng_on" -> {
                statusLabel = "ENG ON"
                statusColor = Color.parseColor("#0ea5e9")
            }
            normalizedEngineMode == "eng_off" -> {
                statusLabel = "ENG OFF"
                statusColor = Color.parseColor("#64748b")
            }
            userLog.discreption == "yard" -> statusLabel = "YARD"
            userLog.discreption == "personal" -> statusLabel = "PERSONAL"
        }
        
        viewHolder.tvStatusPill.text = statusLabel
        viewHolder.tvStatusTitle.text = when(statusLabel) {
            "OFF" -> "Off Duty"
            "SB" -> "Sleeper Berth"
            "DR" -> "Driving"
            "ON" -> "On Duty"
            "INT" -> "Intermediate"
            else -> statusLabel.replace("_", " ")
        }
        
        viewHolder.statusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(statusColor)
        
        // Odometer
        val odometerValue = userLog.odometerreading.toDoubleOrNull()
        viewHolder.tvOdo.text = if (odometerValue != null) {
            String.format(Locale.US, "%.1f", odometerValue)
        } else {
            userLog.odometerreading.ifEmpty { "0.0" }
        }

        // Engine Hours
        val engHoursValue = userLog.eng_hours.toDoubleOrNull()
        viewHolder.tvEng.text = if (engHoursValue != null) {
            String.format(Locale.US, "%.1f", engHoursValue)
        } else {
            userLog.eng_hours.ifEmpty { "0.0" }
        }

        // Unit Number
        viewHolder.tvUnit.text = if (!userLog.powerunitnumber.isNullOrEmpty()) {
            if (userLog.powerunitnumber.startsWith("#")) userLog.powerunitnumber else "#${userLog.powerunitnumber}"
        } else {
            "N/A"
        }
        
        // Location - Cleaned up
        val fullLocation = userLog.location ?: ""
        val cleanLocation = extractCityAndState(fullLocation)
        viewHolder.tvLocation.text = if (cleanLocation.isNotEmpty()) cleanLocation else fullLocation
        
        // Origin
        viewHolder.tvorigon.text = if (userLog.is_autoinsert == 1) "Auto" else "Manual"
        
        // Violation Highlighting
        if (userLog.authorization_status == "not authorized") {
             viewHolder.tile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF5F5")))
        } else {
             viewHolder.tile.setBackgroundTintList(null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatTimeWithTimezone(time: String): String {
        if (time.isBlank()) return ""
        
        return try {
            // Use the utility function from AlertCalculationUtils
            formatTimeWithTimezone(time, timeZone)
        } catch (e: Exception) {
            Log.e("LogAdaptor", "Error formatting time with timezone: ${e.message}")
            formatTimeToAMPM(time) // Fallback to original method
        }
    }

    private val TAG = "LogAdaptor"

     private fun callAddLogAPIForTesting() {
        // Create an AddLogRequest with sample data for testing
//        val logRequest = AddLogRequest(
//            "on", // Example modename "on" for testing
//            "3000", // Example odometerreading
//            "2000", // Example eng_hours
//            2, // Example authorization_status (you can change this as needed)
//            "Sample location", // Example location (you can change this as needed)
//            2, // Example is_autoinsert (you can change this as needed)
//            2, // Example additional field (you can change this as needed)
//            2 ,
//            "",
//            ""
//            // Example additional field (you can change this as needed)
//        )

        // Call the addLog API here or log the request data for testing
        // For example:
        // truckSpotAPI.addLog(logRequest)

        // Log the request data for testing
//        Log.d(TAG, "Calling Add Log API for Testing with data: $logRequest")
    }

    override fun getItemCount() = dataSet.size

    fun formatTimeToAMPM(time: String): String {
        if (time.isBlank()) return ""
        
        try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // 24-hour format
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour format with AM/PM
            if ("00:00" == time) return ""
            val date = inputFormat.parse(time) // Parse the input string to a Date object
            return outputFormat.format(date ?: return "") // Format the Date object to the desired format
        } catch (e: Exception) {
            return time // Return original time if parsing fails
        }
    }

//    fun showEditLogDialog(context: Context) {
//        AlertDialog.Builder(context)
//            .setTitle("Notice")
//            .setMessage("You cannot edit the log. If you want to edit it, please reach support at support@eagleye.com.")
//            .setPositiveButton("OK") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()
//    }

    fun showEditLogDialog(context: Context) {
        val message = SpannableString("You cannot edit the log. If you want to edit it, please reach support at support@eagleye.com.")

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@eagleye.com")
                }
                context.startActivity(Intent.createChooser(intent, "Send Email"))
            }
        }

        val emailStartIndex = message.indexOf("support@eagleye.com")
        val emailEndIndex = emailStartIndex + "support@eagleye.com".length
        message.setSpan(clickableSpan, emailStartIndex, emailEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textView = TextView(context).apply {
            text = message
            movementMethod = LinkMovementMethod.getInstance()  // Enables clicking
            setPadding(40, 20, 40, 20) // Add padding
        }

        AlertDialog.Builder(context)
            .setTitle("Notice")
            .setView(textView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    fun extractCityAndState(location: String): String {
        if (location.isBlank()) return ""
        
        try {
            val locationParts = location.split(",").map { it.trim() }

            // Assuming the second-to-last part is the city and the third-to-last part is the state
            val city = if (locationParts.size >= 2) locationParts[locationParts.size - 2] else null
            val state = if (locationParts.size >= 3) locationParts[locationParts.size - 3] else null

            return if (city != null && state != null) "$city, $state" else ""
        } catch (e: Exception) {
            return ""
        }
    }
}
