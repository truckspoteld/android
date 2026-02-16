import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.eagleye.eld.R
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.UserLog
import com.eagleye.eld.request.updateLogRequest

class LogModalFragment(private val userLog: GetLogsByDateResponse.Results.UserLog) : DialogFragment() {
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.log_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        val editTextTime = view.findViewById<EditText>(R.id.editTextTime)
        editTextTime.setText(userLog.time)

        val editTextModename = view.findViewById<EditText>(R.id.editTextModename)
        editTextModename.setText(userLog.modename)

        val editTextLocation = view.findViewById<EditText>(R.id.editTextlocation)
//        editTextLocation.setText(userLog.location)

        val editTextOdometer = view.findViewById<EditText>(R.id.editTextodometer)
        editTextOdometer.setText(userLog.odometerreading)

        val editTextEngineHours = view.findViewById<EditText>(R.id.editTextenginehours)
        editTextEngineHours.setText(userLog.eng_hours)


        val btnEdit = view.findViewById<Button>(R.id.btnEdit)

        btnEdit.setOnClickListener {
            val updatedTime = editTextTime.text.toString()
            val updatedModename = editTextModename.text.toString()
            val updatedLocation = editTextLocation.text.toString()
            val updatedOdometer = editTextOdometer.text.toString()
            val updatedEngineHours = editTextEngineHours.text.toString()

            // Create an UpdatedLogData object
            val updatedLogData = updateLogRequest(
                userLog.id,
                0.0,
                userLog.datetime,
                updatedModename,
                updatedOdometer,
                updatedEngineHours,
                updatedTime,
                1
            )

            try {
                homeViewModel.updateLog(updatedLogData, context = requireContext())
                Log.v("checkupdateapi", "Update successful")

                // Close the dialog
                dismiss()

                // Display a toast message
                Toast.makeText(requireContext(), "Log updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Handle the error here, you can log the error message or return an error message
                Log.e("API Error", "Error updating log: ${e.message}", e)

                // Return an error message or code, depending on your needs
                Toast.makeText(requireContext(), "Error updating log: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun updateLog(updatedLogData: updateLogRequest, homeViewModel: HomeViewModel): String {
    try {
//        homeViewModel.updateLog(updatedLogData, true, requireContext())

         return "Updated successfully"
    } catch (e: Exception) {
        // Handle the error here, you can log the error message or return an error message
        Log.e("API Error", "Error updating log: ${e.message}", e)

        // Return an error message or code, depending on your needs
        return "Error updating log: ${e.message}"
    }
}

