package com.truckspot.eld.fragment.ui.dvir

import android.Manifest
import android.animation.Animator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.androidanimations.library.Techniques
import com.truckspot.eld.R
import com.truckspot.eld.api.TruckSpotAPI
import com.truckspot.eld.databinding.FragmentDvirBinding
import com.truckspot.eld.models.HomeDataModel
import com.truckspot.eld.request.DvirCreateRequest
import com.truckspot.eld.utils.PrefRepository
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
    private var isPreTrip = true
    private var isSatisfactory = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDvirBinding.inflate(inflater, container, false)
        setupRecycler()
        setupListeners()
        updateTripUI(true)
        updateConditionUI(true)
        updateProgress()
        prefillDefaults()
        loadHistory()
        
        binding.root.post {
            startEntranceAnimations()
        }
        
        return binding.root
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

        // Screen Toggle
        binding.tvToggleNew.setOnClickListener {
            playClickAnimation(it)
            showNewTab(true)
        }
        binding.tvToggleHistory.setOnClickListener {
            playClickAnimation(it)
            showNewTab(false)
        }

        // Trip Type Selection
        binding.tvPreTrip.setOnClickListener {
            playClickAnimation(it)
            updateTripUI(true)
        }
        binding.tvPostTrip.setOnClickListener {
            playClickAnimation(it)
            updateTripUI(false)
        }

        // Vehicle Condition Selection
        binding.llSatisfactory.setOnClickListener {
            playClickAnimation(it)
            updateConditionUI(true)
        }
        binding.llHasDefects.setOnClickListener {
            playClickAnimation(it)
            updateConditionUI(false)
        }

        // Checklist Selection
        val checklistItems = listOf(
            binding.llCbBrakes to binding.cbBrakes,
            binding.llCbTires to binding.cbTires,
            binding.llCbLights to binding.cbLights,
            binding.llCbMirrors to null,
            binding.llCbHorn to null,
            binding.llCbWipers to null,
            binding.llCbSteering to null,
            binding.llCbEmergency to null,
            binding.llCbCoupling to null,
            binding.llCbExhaust to null
        )

        checklistItems.forEach { (layout, checkbox) ->
            layout.setOnClickListener {
                playClickAnimation(it)
                it.isSelected = !it.isSelected
                checkbox?.isChecked = it.isSelected
                val icon = layout.getChildAt(0) as? android.widget.ImageView
                icon?.imageTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    if (it.isSelected) R.color.nav_icon_active else R.color.home_text_sub
                )
                updateProgress()
            }
        }

        binding.btnSubmitDvir.setOnClickListener {
            playClickAnimation(it)
            submitDvir()
        }
    }

    private fun showNewTab(isNew: Boolean) {
        binding.tvToggleNew.isSelected = isNew
        binding.tvToggleHistory.isSelected = !isNew
        binding.tvToggleNew.setTextColor(ContextCompat.getColor(requireContext(), if (isNew) R.color.home_text_main else R.color.home_text_sub))
        binding.tvToggleHistory.setTextColor(ContextCompat.getColor(requireContext(), if (!isNew) R.color.home_text_main else R.color.home_text_sub))
        
        binding.formContainer.visibility = if (isNew) View.VISIBLE else View.GONE
        binding.historyContainer.visibility = if (isNew) View.GONE else View.VISIBLE
        
        if (!isNew) {
            YoYo.with(Techniques.FadeInUp).duration(500).playOn(binding.historyContainer)
        } else {
            YoYo.with(Techniques.FadeInUp).duration(500).playOn(binding.formContainer)
        }
    }

    private fun updateTripUI(preTrip: Boolean) {
        isPreTrip = preTrip
        binding.tvPreTrip.isSelected = preTrip
        binding.tvPostTrip.isSelected = !preTrip
        
        binding.tvPreTrip.setTextColor(ContextCompat.getColor(requireContext(), if (preTrip) R.color.home_text_main else R.color.home_text_sub))
        binding.tvPostTrip.setTextColor(ContextCompat.getColor(requireContext(), if (!preTrip) R.color.home_text_main else R.color.home_text_sub))
        
        val activeIcon = R.drawable.baseline_radio_button_checked_24
        val inactiveIcon = R.drawable.baseline_radio_button_unchecked_24
        
        binding.tvPreTrip.setCompoundDrawablesWithIntrinsicBounds(if (preTrip) activeIcon else inactiveIcon, 0, 0, 0)
        binding.tvPostTrip.setCompoundDrawablesWithIntrinsicBounds(if (!preTrip) activeIcon else inactiveIcon, 0, 0, 0)
        
        val activeTint = ContextCompat.getColorStateList(requireContext(), R.color.nav_icon_active)
        val inactiveTint = ContextCompat.getColorStateList(requireContext(), R.color.home_text_sub)
        
        binding.tvPreTrip.compoundDrawableTintList = if (preTrip) activeTint else inactiveTint
        binding.tvPostTrip.compoundDrawableTintList = if (!preTrip) activeTint else inactiveTint
        
        binding.tvTripChip.text = if (preTrip) "Pre-Trip" else "Post-Trip"
    }

    private fun updateConditionUI(satisfactory: Boolean) {
        isSatisfactory = satisfactory
        
        // Satisfactory Card
        binding.llSatisfactory.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (satisfactory) R.color.status_on_bg else android.R.color.transparent)
        binding.llSatisfactory.apply {
            if (satisfactory) {
                setBackgroundResource(R.drawable.bg_status_pill)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_on_bg)
            } else {
                setBackgroundResource(R.drawable.bg_dvir_checklist_item)
                backgroundTintList = null
            }
        }
        binding.ivSatisfactoryCheck.setImageResource(if (satisfactory) R.drawable.baseline_check_circle_24 else R.drawable.baseline_radio_button_unchecked_24)
        binding.ivSatisfactoryCheck.imageTintList = ContextCompat.getColorStateList(requireContext(), if (satisfactory) R.color.status_on_text else R.color.home_text_sub)

        // Has Defects Card
        binding.llHasDefects.apply {
            if (!satisfactory) {
                setBackgroundResource(R.drawable.bg_status_pill)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.home_bg_blue_light) // Using a light blue for defects selection
            } else {
                setBackgroundResource(R.drawable.bg_dvir_checklist_item)
                backgroundTintList = null
            }
        }
        binding.vDefectsCheck.setBackgroundResource(if (!satisfactory) R.drawable.baseline_check_circle_24 else R.drawable.baseline_radio_button_unchecked_24)
        binding.vDefectsCheck.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (!satisfactory) R.color.nav_icon_active else R.color.home_text_sub)

        animateDefectsField(!satisfactory)
        
        if (satisfactory) {
            binding.etDefects.setText("")
            binding.cbSafeToOperate.isChecked = true
        }
    }

    private fun updateProgress() {
        var checkedCount = 0
        val items = listOf(
            binding.llCbBrakes, binding.llCbTires, binding.llCbLights,
            binding.llCbMirrors, binding.llCbHorn, binding.llCbWipers,
            binding.llCbSteering, binding.llCbEmergency, binding.llCbCoupling, binding.llCbExhaust
        )
        items.forEach { if (it.isSelected) checkedCount++ }
        
        val percent = (checkedCount.toFloat() / items.size.toFloat() * 100).toInt()
        binding.tvProgressPercent.text = "$percent%"
        binding.pbInspection.progress = percent
        binding.tvCheckChip.text = "$checkedCount/${items.size} Checks"
    }

    private fun animateDefectsField(show: Boolean) {
        val field = binding.etDefects
        if (show) {
            if (field.visibility == View.VISIBLE) return
            field.visibility = View.VISIBLE
            YoYo.with(Techniques.FadeInDown).duration(300).playOn(field)
        } else {
            if (field.visibility != View.VISIBLE) return
            YoYo.with(Techniques.FadeOutUp).duration(300).withListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    field.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            }).playOn(field)
        }
    }

    private fun startEntranceAnimations() {
        val duration = 700L
        val stagger = 80L
        
        val views = listOf(
            binding.llHeader,
            binding.llToggle,
            binding.cvProgress,
            binding.cvTripType,
            binding.cvVehicleInfo,
            binding.cvCondition,
            binding.cvChecklist,
            binding.cvSafety,
            binding.etSignature,
            binding.btnSubmitDvir
        )
        
        views.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay(index * stagger)
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    // Header descends, cards rise
                    val technique = if (index == 0) Techniques.FadeInDown else Techniques.FadeInUp
                    YoYo.with(technique)
                        .duration(duration)
                        .playOn(view)
                }
            }
        }
    }

    private fun playClickAnimation(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
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

        val hasDefects = !isSatisfactory
        val defects = binding.etDefects.text?.toString()?.trim().orEmpty()
        if (hasDefects && defects.isEmpty()) {
            toast("Please describe the defects")
            return
        }

        val reportType = if (isPreTrip) "pre_trip" else "post_trip"
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
            safeToOperate = binding.cbSafeToOperate.isChecked,
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
        binding.btnSubmitDvir.alpha = if (isSubmitting) 0.5f else 1.0f
    }

    private fun clearForm() {
        updateTripUI(true)
        binding.etSignature.setText("")
        listOf(
            binding.llCbBrakes, binding.llCbTires, binding.llCbLights,
            binding.llCbMirrors, binding.llCbHorn, binding.llCbWipers,
            binding.llCbSteering, binding.llCbEmergency, binding.llCbCoupling, binding.llCbExhaust
        ).forEach {
            it.isSelected = false
            val icon = it.getChildAt(0) as? android.widget.ImageView
            icon?.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.home_text_sub)
        }
        binding.cbBrakes.isChecked = false
        binding.cbLights.isChecked = false
        binding.cbTires.isChecked = false
        updateProgress()
        updateConditionUI(true)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
