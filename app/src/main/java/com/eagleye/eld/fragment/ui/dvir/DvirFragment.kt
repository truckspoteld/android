package com.eagleye.eld.fragment.ui.dvir

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
import com.eagleye.eld.R
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.databinding.FragmentDvirBinding
import com.eagleye.eld.models.DvirReport
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.request.DvirCreateRequest
import com.eagleye.eld.utils.PrefRepository
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
        historyAdapter = DvirHistoryAdapter { report -> showDvirDetail(report) }
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
        // FMCSA §396.11(a)(2) inspection items
        val checklistItems = listOf(
            binding.llCbBrakes to binding.cbBrakes,
            binding.llCbParkingBrake to null,
            binding.llCbSteering to null,
            binding.llCbLights to binding.cbLights,
            binding.llCbTires to binding.cbTires,
            binding.llCbHorn to null,
            binding.llCbWipers to null,
            binding.llCbMirrors to null,
            binding.llCbCoupling to null,
            binding.llCbWheels to null,
            binding.llCbEmergency to null,
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
        // Default every item to OK (selected) — driver UNchecks any defective component.
        checklistItems.forEach { (layout, checkbox) ->
            layout.isSelected = true
            checkbox?.isChecked = true
            (layout.getChildAt(0) as? android.widget.ImageView)?.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.nav_icon_active)
        }
        updateProgress()

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
            binding.llCbBrakes, binding.llCbParkingBrake, binding.llCbSteering,
            binding.llCbLights, binding.llCbTires, binding.llCbHorn,
            binding.llCbWipers, binding.llCbMirrors, binding.llCbCoupling,
            binding.llCbWheels, binding.llCbEmergency, binding.llCbExhaust
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
        // The logged-in driver IS the signer — sign automatically with their name (no prompt).
        // Fall back to username so the signature is never blank (the backend rejects an empty one).
        val signature = prefRepository.getName().trim()
            .ifEmpty { prefRepository.getUserName().trim() }
            .ifEmpty { "Driver" }

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
                "service_brakes" to binding.llCbBrakes.isSelected,
                "parking_brake" to binding.llCbParkingBrake.isSelected,
                "steering" to binding.llCbSteering.isSelected,
                "lights_reflectors" to binding.llCbLights.isSelected,
                "tires" to binding.llCbTires.isSelected,
                "horn" to binding.llCbHorn.isSelected,
                "wipers" to binding.llCbWipers.isSelected,
                "mirrors" to binding.llCbMirrors.isSelected,
                "coupling" to binding.llCbCoupling.isSelected,
                "wheels_rims" to binding.llCbWheels.isSelected,
                "emergency_equipment" to binding.llCbEmergency.isSelected,
                "exhaust" to binding.llCbExhaust.isSelected
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
                // Apps show only the last 8 days of DVIRs (current day + 7 prior) — matches the ELD
                // record-retention window. The carrier portal still keeps the full history.
                val fromDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LocalDate.now().minusDays(7).toString() else null
                val response = truckSpotAPI.getDriverDVIRReports(fromdate = fromDate)
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

    // Inspector-ready DVIR report — the driver taps a history item to show a roadside inspector.
    // Renders the full FMCSA §396.11/§396.13 record: vehicle, checklist, defects, driver
    // signature, and the carrier review / next-driver acknowledgment.
    private fun showDvirDetail(report: DvirReport) {
        if (!isAdded) return
        val ctx = requireContext()
        fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
        val colMain = android.graphics.Color.parseColor("#16263F")
        val colSub = android.graphics.Color.parseColor("#6B7A90")
        val colAccent = android.graphics.Color.parseColor("#146BFF")
        val colOk = android.graphics.Color.parseColor("#0D9369")
        val colBad = android.graphics.Color.parseColor("#D94848")

        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(8))
        }
        fun addText(text: String, size: Float, color: Int, bold: Boolean = false, topMargin: Int = 0) {
            root.addView(android.widget.TextView(ctx).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                (layoutParams ?: android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )).also { lp ->
                    layoutParams = (lp as android.widget.LinearLayout.LayoutParams).apply { setMargins(0, dp(topMargin), 0, 0) }
                }
            })
        }
        fun addSectionHeader(title: String) = addText(title.uppercase(Locale.US), 11f, colAccent, bold = true, topMargin = 16)
        fun addRow(label: String, value: String?) {
            val rowLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, 0) }
            root.addView(android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = rowLp
                addView(android.widget.TextView(ctx).apply {
                    text = label; textSize = 13f; setTextColor(colSub)
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(android.widget.TextView(ctx).apply {
                    text = if (value.isNullOrBlank()) "-" else value; textSize = 13f
                    setTextColor(colMain); setTypeface(typeface, android.graphics.Typeface.BOLD)
                    textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_END
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                })
            })
        }
        fun fmt(v: String?) = if (v.isNullOrBlank()) "-" else v.replace("T", " ").take(16)
        fun pretty(v: String?): String {
            if (v.isNullOrBlank()) return "-"
            val t = v.replace("_", " ").trim()
            return if (t.isEmpty()) "-" else t.substring(0, 1).uppercase(Locale.US) + t.substring(1)
        }

        val isSatisfactory = (report.has_defects != true) && report.vehicle_condition?.lowercase() != "has_defects"
        val tripLabel = if (report.report_type == "post_trip") "Post-Trip" else "Pre-Trip"

        addText("Driver Vehicle Inspection Report", 17f, colMain, bold = true)
        addText("§396.11 / §396.13  ·  $tripLabel", 12f, colSub, topMargin = 2)
        addText(if (isSatisfactory) "● SATISFACTORY" else "● DEFECTS NOTED", 13f, if (isSatisfactory) colOk else colBad, bold = true, topMargin = 10)

        addSectionHeader("Details")
        addRow("Date", report.report_date)
        addRow("Vehicle / Unit", report.vehicle?.truck_no ?: report.vin_no)
        addRow("VIN", report.vin_no)
        addRow("Trailer", report.trailer_number)
        addRow("Odometer", report.odometer)
        addRow("Location", report.location)
        addRow("Status", pretty(report.status))

        val checklist = report.checklist
        if (!checklist.isNullOrEmpty()) {
            addSectionHeader("Inspection Checklist")
            val labels = mapOf(
                "service_brakes" to "Service Brakes", "parking_brake" to "Parking Brake",
                "steering" to "Steering Mechanism", "lights_reflectors" to "Lighting Devices & Reflectors",
                "tires" to "Tires", "horn" to "Horn", "wipers" to "Windshield Wipers",
                "mirrors" to "Rear-Vision Mirrors", "coupling" to "Coupling Devices",
                "wheels_rims" to "Wheels & Rims", "emergency_equipment" to "Emergency Equipment",
                "exhaust" to "Exhaust System"
            )
            checklist.toSortedMap().forEach { (k, ok) ->
                val label = labels[k] ?: pretty(k)
                addText((if (ok) "✓  " else "✗  ") + label, 13f, if (ok) colOk else colBad, topMargin = 4)
            }
        }

        if (!isSatisfactory) {
            addSectionHeader("Defects")
            addText(report.defects_description?.ifBlank { "(see checklist)" } ?: "(see checklist)", 13f, colMain, topMargin = 2)
            addRow("Safe to operate", if (report.safe_to_operate == true) "Yes" else "No")
        }

        addSectionHeader("Driver Certification (§396.11)")
        addText(report.driver_signature?.ifBlank { "-" } ?: "-", 18f, colMain, topMargin = 2)
        addText("Signed" + if (!report.signed_at.isNullOrBlank()) " on ${fmt(report.signed_at)}" else "", 11f, colSub, topMargin = 2)

        if (report.has_defects == true) {
            addSectionHeader("Carrier Review & §396.13 Acknowledgment")
            addRow("Review status", pretty(report.status))
            addRow("Review notes", report.review_notes)
            addRow("Reviewed at", if (!report.reviewed_at.isNullOrBlank()) fmt(report.reviewed_at) else "-")
            addRow("Driver acknowledgment",
                if (!report.driver_ack_signature.isNullOrBlank())
                    report.driver_ack_signature + (if (!report.driver_ack_at.isNullOrBlank()) " (${fmt(report.driver_ack_at)})" else "")
                else "Pending")
        }

        val scroll = android.widget.ScrollView(ctx).apply { addView(root) }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setView(scroll)
            .setPositiveButton("Close") { d, _ -> d.dismiss() }
            .show()
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
            binding.llCbBrakes, binding.llCbParkingBrake, binding.llCbSteering,
            binding.llCbLights, binding.llCbTires, binding.llCbHorn,
            binding.llCbWipers, binding.llCbMirrors, binding.llCbCoupling,
            binding.llCbWheels, binding.llCbEmergency, binding.llCbExhaust
        ).forEach {
            it.isSelected = true   // reset to default OK; driver unchecks defects
            val icon = it.getChildAt(0) as? android.widget.ImageView
            icon?.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.nav_icon_active)
        }
        binding.cbBrakes.isChecked = true
        binding.cbLights.isChecked = true
        binding.cbTires.isChecked = true
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
