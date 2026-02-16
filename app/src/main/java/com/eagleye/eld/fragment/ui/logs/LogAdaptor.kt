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


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView
        val tvStatus: TextView
        val tvLocation: TextView
        val tvOdo: TextView
        val tvEng: TextView
        val tvorigon: TextView
        val tile: LinearLayout

        init {
            tvTime = view.findViewWithTag("binding_1")
            tvStatus = view.findViewWithTag("binding_2")
            tvLocation = view.findViewWithTag("binding_3")
            tvOdo = view.findViewWithTag("binding_4")
            tvEng = view.findViewWithTag("binding_5")
            tvorigon = view.findViewWithTag("binding_6")
            tile = view.findViewById(R.id.logTIle)
        }
    }

    private fun displaySnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
    private fun getLocationDetails(context: Context, latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        var locationDetails: String? = null

        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                locationDetails = address.getAddressLine(0)
             }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return locationDetails
    }
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.text_log_item, viewGroup, false)

        return ViewHolder(view)
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Safety check for position bounds
        if (position < 0 || position >= dataSet.size) {
            return
        }
        
        val userLog = dataSet[position] ?: return
        
        viewHolder.itemView.setOnClickListener {
            if (userLog.modename == "d") {
                showEditLogDialog(contect)
            } else if(userLog.modename == "login" || userLog.modename == "logout"|| userLog.modename == "yard"|| userLog.modename == "personal" || userLog.modename == "certification"){

            }else {
                val modalFragment = LogModalFragment(userLog)
                modalFragment.show(fragmentManager, "LogModalFragment")
            }
        }

        // Safe handling of time with timezone conversion
        val time = userLog.time ?: ""
        viewHolder.tvTime.text = formatTimeWithTimezone(time)
        
        // Safe handling of status
        var statusText = userLog.modename.uppercase() ?: ""
        if (userLog.discreption == "yard") {
            statusText = "YARD"
        } else if (userLog.discreption == "personal") {
            statusText = "PERSONAL"
        } else if (userLog.modename == "d" && userLog.discreption == "Intermediate log") {
            statusText = "INT"
        }
        viewHolder.tvStatus.text = statusText
        
        val border = GradientDrawable()
        border.cornerRadius = 16f
        when (userLog.modename) {
            "off" -> border.setColor(Color.RED)
            "d" -> border.setColor(Color.parseColor("#00CC00"))
            "on" -> border.setColor(Color.parseColor("#FFA500"))
            "sb" -> border.setColor(Color.BLUE)
            else -> border.setColor(Color.GRAY)
        }

        viewHolder.tvStatus.setPadding(0, 10, 0, 10)
        viewHolder.tvStatus.background = border
        
        if (userLog.authorization_status == "not authorized") {
            viewHolder.itemView.setBackgroundColor(Color.RED)
        } else {
            // All authorized logs use dark background - no more white/transparent alternation
            viewHolder.tile.setBackgroundResource(R.drawable.modern_log_card)
        }
        
        // Safe handling of odometer and engine hours
        viewHolder.tvOdo.text = String.format("%.2f", userLog.odometerreading.toDouble()) ?: ""
        viewHolder.tvEng.text = userLog.eng_hours ?: ""
        
        // Safe handling of location
        viewHolder.tvLocation.text = userLog.location ?: ""
        
        // Safe handling of auto/manual
        viewHolder.tvorigon.text = if (userLog.is_autoinsert == 1) "Auto" else "Manual"
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