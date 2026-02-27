package com.eagleye.eld.fragment.ui.dvir

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eagleye.eld.R
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.databinding.FragmentDvirBinding
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.request.DvirCreateRequest
import com.eagleye.eld.utils.PrefRepository
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DvirFragment : Fragment() {

    private var _binding: FragmentDvirBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var truckSpotAPI: TruckSpotAPI

    @Inject
    lateinit var prefRepository: PrefRepository

    private lateinit var historyAdapter: DvirHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDvirBinding.inflate(inflater, container, false)
        setupTripTypeSpinner()
        setupRecycler()
        setupListeners()
        applyConditionUIState()
        prefillDefaults()
        loadHistory()
        return binding.root
    }

    private fun setupTripTypeSpinner() {
        val values = listOf("Pre Trip", "Post Trip")
        val adapter =
            ArrayAdapter(requireContext(), R.layout.item_spinner_trip_selected, values).apply {
                setDropDownViewResource(R.layout.item_spinner_trip_dropdown)
            }
        binding.spinnerTripType.adapter = adapter
        binding.spinnerTripType.post {
            binding.spinnerTripType.dropDownWidth = binding.spinnerTripType.width
            binding.spinnerTripType.dropDownVerticalOffset =
                resources.getDimensionPixelOffset(R.dimen._4dp)
        }
        binding.spinnerTripType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                animateTripTypeSelection(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupRecycler() {
        historyAdapter = DvirHistoryAdapter()
        binding.rvDvirHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshLayoutDvir.setOnRefreshListener {
            prefillDefaults()
            loadHistory()
        }

        binding.rbDefects.setOnCheckedChangeListener { _, _ ->
            applyConditionUIState()
        }

        binding.rbSatisfactory.setOnCheckedChangeListener { _, _ ->
            applyConditionUIState()
        }

        binding.btnSubmitDvir.setOnClickListener {
            submitDvir()
        }
    }

    private fun applyConditionUIState() {
        val hasDefects = binding.rbDefects.isChecked
        animateDefectsField(hasDefects)
        if (!hasDefects) {
            binding.etDefects.setText("")
            binding.cbSafeToOperate.isChecked = true
            binding.cbSafeToOperate.isEnabled = false
        } else {
            binding.cbSafeToOperate.isEnabled = true
            if (binding.cbSafeToOperate.isChecked) {
                binding.cbSafeToOperate.isChecked = false
            }
        }
    }

    private fun animateTripTypeSelection(position: Int) {
        val accent = if (position == 0) R.color.dvir_success else R.color.dvir_warning
        val spinner = binding.spinnerTripType
        spinner.animate().cancel()
        spinner.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(140)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                spinner.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
        spinner.backgroundTintList = ContextCompat.getColorStateList(requireContext(), accent)
    }

    private fun animateDefectsField(show: Boolean) {
        val field = binding.etDefects
        if (show) {
            if (field.visibility == View.VISIBLE) return
            field.alpha = 0f
            field.translationY = -18f
            field.visibility = View.VISIBLE
            field.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            if (field.visibility != View.VISIBLE) return
            field.animate()
                .alpha(0f)
                .translationY(-12f)
                .setDuration(180)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        field.visibility = View.GONE
                        field.alpha = 1f
                        field.translationY = 0f
                        field.animate().setListener(null)
                    }
                })
                .start()
        }
    }

    private fun prefillDefaults() {
        val trailerFromPref = prefRepository.getTrailerNumber().trim()
        if (trailerFromPref.isNotBlank() && binding.etTrailer.text.isNullOrBlank()) {
            binding.etTrailer.setText(trailerFromPref)
        }

        lifecycleScope.launch {
            fetchTrailerFromShipmentIfNeeded()
            fetchDefaultsFromLatestLog()
            fetchLocationFromDeviceIfStillEmpty()
        }
    }

    private suspend fun fetchTrailerFromShipmentIfNeeded() {
        if (!binding.etTrailer.text.isNullOrBlank()) return
        try {
            val shipmentRes = truckSpotAPI.getActiveDriverShipment()
            val trailer = shipmentRes.body()?.data?.trailerNumber.orEmpty().trim()
            if (shipmentRes.isSuccessful && trailer.isNotBlank()) {
                binding.etTrailer.setText(trailer)
                prefRepository.setTrailerNumber(trailer)
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun fetchDefaultsFromLatestLog() {
        try {
            val homeRes = truckSpotAPI.getHomeData()
            val payload = homeRes.body()
            if (!homeRes.isSuccessful || payload == null) return

            val latest = selectLatestLog(payload)

            val odo = latest?.odometerreading?.trim().orEmpty()
            if (odo.isNotBlank() && binding.etOdometer.text.isNullOrBlank()) {
                binding.etOdometer.setText(odo)
            }

            val location = latest?.location?.trim().orEmpty()
            if (location.isNotBlank() && binding.etLocation.text.isNullOrBlank()) {
                binding.etLocation.setText(location)
            }

            if (binding.etTrailer.text.isNullOrBlank()) {
                val trailer = latest?.trailer_number?.toString()?.trim().orEmpty()
                if (trailer.isNotBlank() && trailer != "null") {
                    binding.etTrailer.setText(trailer)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun selectLatestLog(home: HomeDataModel): HomeDataModel.Log? {
        val fromLatestUpdated = home.latestUpdatedLog
        if (fromLatestUpdated != null && fromLatestUpdated.id != null && fromLatestUpdated.id != 0) {
            return fromLatestUpdated
        }
        val logs = home.logs.orEmpty()
        return logs.maxByOrNull { it.id ?: 0 }
    }

    private fun fetchLocationFromDeviceIfStillEmpty() {
        if (!binding.etLocation.text.isNullOrBlank()) return

        val hasFine = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && binding.etLocation.text.isNullOrBlank()) {
                val lat = String.format(Locale.US, "%.5f", location.latitude)
                val lng = String.format(Locale.US, "%.5f", location.longitude)
                binding.etLocation.setText("$lat, $lng")
            }
        }
    }

    private fun submitDvir() {
        val signature = binding.etSignature.text?.toString()?.trim().orEmpty()
        if (signature.isEmpty()) {
            toast("Driver signature is required")
            return
        }

        val hasDefects = binding.rbDefects.isChecked
        val defects = binding.etDefects.text?.toString()?.trim().orEmpty()
        if (hasDefects && defects.isEmpty()) {
            toast("Please describe the defects")
            return
        }

        val reportType = if (binding.spinnerTripType.selectedItemPosition == 1) "post_trip" else "pre_trip"
        val request = DvirCreateRequest(
            reportType = reportType,
            reportDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now().toString() else "",
            odometer = binding.etOdometer.text?.toString()?.trim().orEmpty(),
            trailerNumber = binding.etTrailer.text?.toString()?.trim().orEmpty(),
            location = binding.etLocation.text?.toString()?.trim().orEmpty(),
            vehicleCondition = if (hasDefects) "has_defects" else "satisfactory",
            hasDefects = hasDefects,
            defectsDescription = if (hasDefects) defects else null,
            checklist = mapOf(
                "brakes" to binding.cbBrakes.isChecked,
                "lights" to binding.cbLights.isChecked,
                "tires" to binding.cbTires.isChecked
            ),
            safeToOperate = if (hasDefects) binding.cbSafeToOperate.isChecked else true,
            driverSignature = signature
        )

        setSubmitting(true)
        lifecycleScope.launch {
            try {
                val response = truckSpotAPI.submitDVIR(request)
                val body = response.body()
                if (response.isSuccessful && body?.status == true) {
                    toast(body.message ?: "DVIR submitted")
                    clearForm()
                    prefillDefaults()
                    loadHistory()
                } else {
                    toast(body?.message ?: "Failed to submit DVIR")
                }
            } catch (e: Exception) {
                toast("Network error: ${e.message}")
            } finally {
                setSubmitting(false)
            }
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            try {
                val response = truckSpotAPI.getDriverDVIRReports()
                val items = if (response.isSuccessful && response.body()?.status == true) {
                    response.body()?.results?.reports.orEmpty()
                } else {
                    emptyList()
                }
                historyAdapter.submitList(items)
            } catch (e: Exception) {
                toast("Failed to load DVIR history")
            } finally {
                binding.swipeRefreshLayoutDvir.isRefreshing = false
            }
        }
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        binding.progressSubmit.visibility = if (isSubmitting) View.VISIBLE else View.GONE
        binding.btnSubmitDvir.isEnabled = !isSubmitting
    }

    private fun clearForm() {
        binding.spinnerTripType.setSelection(0)
        binding.etSignature.setText("")
        binding.cbBrakes.isChecked = false
        binding.cbLights.isChecked = false
        binding.cbTires.isChecked = false
        binding.rbSatisfactory.isChecked = true
        applyConditionUIState()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
