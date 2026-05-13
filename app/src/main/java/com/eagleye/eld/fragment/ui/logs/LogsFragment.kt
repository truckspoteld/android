package com.eagleye.eld.fragment.ui.logs
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eagleye.eld.databinding.FragmentLogsBinding
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.models.AddLogSuccessResponse
import com.eagleye.eld.models.DriverReviewResponse
import com.eagleye.eld.models.GetCompanyById
import com.eagleye.eld.models.GetLogsByDateRequest
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.LogResponse
import com.eagleye.eld.utils.*
import com.eagleye.eld.utils.AlertCalculationUtils.setDateAndTimeBasedOnTimezone
import com.eagleye.eld.utils.AlertCalculationUtils.getCurrentDateInTimezone
import com.eagleye.eld.utils.Utils.formatTimeFromSeconds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.androidanimations.library.Techniques
import com.eagleye.eld.R

@AndroidEntryPoint
class LogsFragment : Fragment() {
    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var logViewMode: LogViewModel
    lateinit var prefRepository: PrefRepository

    lateinit var date: Date
    var days = 0

    var timeZone: String = ""

    /** Requested date (YYYY-MM-DD) for the current load; used to extend graph to 24 or "now". */
    private var currentRequestDate: String = ""
    
    // Timer variables for auto-refresh
    private var autoRefreshJob: Job? = null
    private val autoRefreshInterval = 120_000L // 2 minutes
    
    // Flag to prevent duplicate data loading
    private var isDataLoaded = false

    private var isReviewMode = false
    private lateinit var daysAdapter: DaysAdapter
    private val dayList = mutableListOf<DayModel>()

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefRepository = PrefRepository(requireContext())
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        logViewMode = ViewModelProvider(this)[LogViewModel::class.java]
        
        // Check for Review Mode
        isReviewMode = arguments?.getBoolean("isReviewMode", false) ?: false

        date = Date()
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        
        if (isReviewMode) {
            Log.d("LogsFragment", "Review Mode ENABLED - showing logs for DOT inspection")
            binding.eventLogRv.visibility = View.VISIBLE
            binding.fabLogReport.visibility = View.GONE
            binding.statusPillsContainer.visibility = View.GONE
            binding.logsTitle.text = getString(R.string.log_review)
            binding.logsSubtitle.text = "Review carrier details and duty chart"
            
            // Auto-expand Carrier Details
            binding.llExpandedContent.visibility = View.VISIBLE
            binding.ivExpandIcon.rotation = 180f
            
            // Fully block scrolling
            binding.nestedScrollView.setOnTouchListener { touchedView, event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> true
                    MotionEvent.ACTION_UP -> {
                        touchedView.performClick()
                        false
                    }
                    else -> false
                }
            }
            binding.swipeRefreshLayout.isEnabled = false
            
            // Reduce title sizes
            binding.logsTitle.textSize = 22f
            binding.logsSubtitle.textSize = 12f
            
            // Adjust padding - keep horizontal breathing room
            val dp12 = (12 * resources.displayMetrics.density).toInt()
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            val dp6 = (6 * resources.displayMetrics.density).toInt()
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            val dp3 = (3 * resources.displayMetrics.density).toInt()
            (binding.nestedScrollView.getChildAt(0) as? ViewGroup)?.setPadding(dp12, dp8, dp12, dp4)
            
            // Reduce carrier details card margins
            (binding.carrierDetailsCard.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.topMargin = dp4
            
            // Carrier header padding
            binding.rlCarrierHeader.setPadding(dp12, dp6, dp12, dp6)
            
            // Reduce expanded content row spacing
            val expandedContent = binding.llExpandedContent
            for (i in 0 until expandedContent.childCount) {
                val child = expandedContent.getChildAt(i)
                if (child is LinearLayout) {
                    child.setPadding(dp12, dp4, dp12, dp4)
                    // Reduce bottom margins on inner rows
                    for (j in 0 until child.childCount) {
                        val row = child.getChildAt(j)
                        if (row is LinearLayout) {
                            (row.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin = dp3
                            // Slightly reduce font sizes in detail rows
                            for (k in 0 until row.childCount) {
                                val tv = row.getChildAt(k)
                                if (tv is TextView) {
                                    tv.textSize = if (tv.currentTextColor == Color.rgb(107, 114, 128)) 11f else 13f
                                }
                            }
                        }
                    }
                }
            }
            
            // Reduce date selector card margin
            (binding.dateCard.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.topMargin = dp6
            
            // Reduce graph card margin and graph container height
            (binding.graphCard.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.topMargin = dp6
            val graphContainer = binding.eldPlot.root.findViewById<View>(R.id.graph_container)
            if (graphContainer != null) {
                val dp130 = (130 * resources.displayMetrics.density).toInt()
                graphContainer.layoutParams.height = dp130
                graphContainer.requestLayout()
            }
        }

        // Initialize with timezone from preferences first
        if(prefRepository.getTimeZone().isNotEmpty()){
            timeZone = prefRepository.getTimeZone()
            setupDaySelector()
            updateDateDisplay()
            if (!isDataLoaded) {
                isDataLoaded = true
                loadInitialData()
            }
        }
        
        // Observer for company data - only load if not already loaded
        homeViewModel.company.observe(viewLifecycleOwner) { result: NetworkResult<GetCompanyById> ->
            val companyResponse = (result as? NetworkResult.Success<*>)?.data as? GetCompanyById
            val newTimeZone = companyResponse?.results?.company_timezone ?: ""
            if (newTimeZone.isNotEmpty() && newTimeZone != timeZone) {
                timeZone = newTimeZone
                setupDaySelector()
                updateDateDisplay()
                if (!isDataLoaded) {
                    isDataLoaded = true
                    loadInitialData()
                }
            }
        }

        // Fetch company name if timezone not set
        if(prefRepository.getTimeZone().isEmpty()){
            homeViewModel.getCompanyName(requireContext())
        }

        val root: View = binding.root
        setupClickListeners()
        
        return root
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private var isFabOpen = false
    private var selectedLog: GetLogsByDateResponse.Results.UserLog? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleFabMenu() {
        val interpolator = android.view.animation.OvershootInterpolator(1.2f)
        if (!isFabOpen) {
            // OPENING
            binding.fabOverlay.visibility = View.VISIBLE
            binding.fabOverlay.animate().alpha(1f).setDuration(400).start()
            
            // Show layouts
            binding.layoutFabException.visibility = View.VISIBLE
            binding.layoutFabAddLog.visibility = View.VISIBLE
            binding.layoutFabAddRemark.visibility = View.VISIBLE
            
            // Animate items one by one with different delays and scaling
            // Remark (closest to main FAB)
            binding.layoutFabAddRemark.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(interpolator)
                .setDuration(450)
                .start()

            // Add Log (middle)
            binding.layoutFabAddLog.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(interpolator)
                .setStartDelay(100)
                .setDuration(450)
                .start()

            // Exception (top)
            binding.layoutFabException.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(interpolator)
                .setStartDelay(200)
                .setDuration(450)
                .start()
            
            // Rotate main FAB
            binding.fabLogReport.animate()
                .rotation(135f)
                .setDuration(400)
                .setInterpolator(interpolator)
                .start()
                
            isFabOpen = true
        } else {
            // CLOSING
            binding.fabOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                binding.fabOverlay.visibility = View.GONE
            }.start()
            
            // Animate items back down to the bottom
            val closeDuration = 300L
            binding.layoutFabException.animate()
                .translationY(100f)
                .alpha(0f)
                .scaleX(0.3f)
                .scaleY(0.3f)
                .setDuration(closeDuration)
                .withEndAction { binding.layoutFabException.visibility = View.GONE }
                .start()
                
            binding.layoutFabAddLog.animate()
                .translationY(100f)
                .alpha(0f)
                .scaleX(0.3f)
                .scaleY(0.3f)
                .setDuration(closeDuration)
                .setStartDelay(50)
                .withEndAction { binding.layoutFabAddLog.visibility = View.GONE }
                .start()
                
            binding.layoutFabAddRemark.animate()
                .translationY(100f)
                .alpha(0f)
                .scaleX(0.3f)
                .scaleY(0.3f)
                .setDuration(closeDuration)
                .setStartDelay(100)
                .withEndAction { binding.layoutFabAddRemark.visibility = View.GONE }
                .start()
            
            // Rotate back
            binding.fabLogReport.animate()
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
                
            isFabOpen = false
        }
    }

    private fun animateDialogShow(view: View) {
        view.scaleX = 0.7f
        view.scaleY = 0.7f
        view.alpha = 0f
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun showAddLogDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext()).create()
        val view = layoutInflater.inflate(R.layout.dialog_add_log, null)
        animateDialogShow(view)
        
        val etDate = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_date)
        val etTime = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_time)
        val modeDropdown = view.findViewById<AutoCompleteTextView>(R.id.mode_dropdown)
        val etOdometer = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_odometer)
        val etRemark = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_remark)
        val btnCancel = view.findViewById<TextView>(R.id.btn_cancel)
        val btnSave = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btn_save)

        // Set up Mode Dropdown
        val modes = arrayOf("on", "off", "sb")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modes)
        modeDropdown.setAdapter(adapter)

        // Date Picker
        etDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                etDate.setText(formattedDate)
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        // Time Picker
        etTime.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                etTime.setText(formattedTime)
            }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val date = etDate.text.toString()
            val time = etTime.text.toString()
            val mode = modeDropdown.text.toString()
            val odometer = etOdometer.text.toString()
            val remark = etRemark.text.toString()

            if (date.isEmpty() || time.isEmpty() || odometer.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.eagleye.eld.request.AddLogWithExceptionRequest(
                date = date,
                time = time,
                modename = mode,
                odometerreading = odometer,
                remark = remark
            )

            logViewMode.addLogWithException(request, requireContext())
            dialog.dismiss()
        }

        dialog.setView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAddLogResult() {
        logViewMode.addLogLiveData.observe(viewLifecycleOwner) { result: NetworkResult<AddLogSuccessResponse> ->
            when (result) {
                is NetworkResult.Loading<*> -> showLoading(true)
                is NetworkResult.Success<*> -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Log added successfully", Toast.LENGTH_SHORT).show()
                    loadInitialData() // Refresh logs
                }
                is NetworkResult.Error<*> -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        logViewMode.updateLogLiveData.observe(viewLifecycleOwner) { result: NetworkResult<LogResponse> ->
            when (result) {
                is NetworkResult.Loading<*> -> showLoading(true)
                is NetworkResult.Success<*> -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Log updated successfully", Toast.LENGTH_SHORT).show()
                    loadInitialData() // Refresh logs
                }
                is NetworkResult.Error<*> -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showActionDialog(title: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext()).create()
        val view = layoutInflater.inflate(R.layout.dialog_exception, null)
        animateDialogShow(view)
        
        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_title)
        val tilException = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_exception)
        val tilNotes = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_notes)
        val btnCancel = view.findViewById<TextView>(R.id.btn_cancel)
        val btnSave = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btn_save)
        
        tvTitle.text = title
        if (title == "Add Remark") {
            tilException.visibility = View.GONE
            tilNotes.hint = "Enter Remark"
        } else {
            tilException.visibility = View.VISIBLE
            tilNotes.hint = "Enter Notes"
            
            val exceptions = arrayOf(
                "Short-Haul Exception (CDL)",
                "Short-Haul Exception (Non-CDL)",
                "16-Hour Exception",
                "Adverse Driving Conditions Exception",
                "30-Minute Break Exception",
                "Split Sleeper Berth Exception",
                "Short-Haul ELD Exception",
                "Driveaway/Towaway Exception",
                "Pre-2000 Engine Exception",
                "Limited Use ELD Exception (≤ 8 days in 30 days)",
                "Covered Farm Vehicle (CFV) Exception",
                "Military Driver Exception",
                "Emergency Response Driver Exception",
                "Recreational Vehicle (RV) Exception",
                "Emergency Relief Exception",
                "Utility Service Vehicle Exception",
                "Oilfield Operations Exception",
                "Agricultural Commodity Exception (150 air-mile)",
                "Railroad Employee Exception",
                "Construction Materials Exception",
                "Ready-Mix Concrete Exception"
            )
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exceptions)
            view.findViewById<AutoCompleteTextView>(R.id.exception_dropdown).setAdapter(adapter)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val log = selectedLog
            if (log == null) {
                Toast.makeText(requireContext(), "No log selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val notes = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_notes).text.toString()
            val exceptionType = if (title == "Exception") {
                view.findViewById<AutoCompleteTextView>(R.id.exception_dropdown).text.toString()
            } else null

            val request = com.eagleye.eld.request.updateLogRequest(
                id = log.id,
                hours = log.hours.toDouble(),
                modename = log.modename,
                odometerreading = log.odometerreading,
                eng_hours = log.eng_hours,
                time = log.time,
                location = log.location,
                event_status = log.event_status,
                remark = if (title == "Add Remark") notes else null,
                exceptionType = exceptionType,
                exceptionNotes = if (title == "Exception") notes else null
            )

            logViewMode.updateLog(request, requireContext())
            dialog.dismiss()
            
            // Disable FAB and clear selection after starting update
            selectedLog = null
            binding.fabLogReport.isEnabled = false
            binding.fabLogReport.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.strockColor)
            )
        }
        
        dialog.setView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.nav_icon_active)
        
        observeAddLogResult()

        binding.fabLogReport.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabOverlay.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabException.setOnClickListener {
            toggleFabMenu()
            showActionDialog("Exception")
        }

        binding.fabAddLog.setOnClickListener {
            toggleFabMenu()
            showAddLogDialog()
        }

        binding.fabAddRemark.setOnClickListener {
            toggleFabMenu()
            showActionDialog("Add Remark")
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("LogsFragment", "Swipe refresh triggered")
            loadInitialData()
        }
        setupObservers()
        loadInitialData()
        // Initial entrance animations
        entranceAnimations()
    }
    private fun entranceAnimations() {
        val duration: Long = 700
        val staggerDelay: Long = 80

        val allViews = mutableListOf(
            binding.logsTitle,
            binding.logsSubtitle,
            binding.statusBadgeDr,
            binding.carrierDetailsCard,
            binding.dateCard
        )
        
        // PST and Events pills removed — not needed
        allViews.add(binding.graphCard)
        if (!isReviewMode) {
            allViews.add(binding.eventLogRv)
            allViews.add(binding.fabLogReport)
        }
        
        allViews.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay(index * staggerDelay)
            
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    val technique = if (index <= 2) Techniques.FadeInDown else Techniques.FadeInUp
                    YoYo.with(technique).duration(duration).playOn(view)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDaySelector() {
        if (timeZone.isEmpty()) return
        
        dayList.clear()
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayNameFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE")
        val dayNumberFormatter = java.time.format.DateTimeFormatter.ofPattern("dd")
        
        val timezoneData = setDateAndTimeBasedOnTimezone(timeZone)
        val todayStr = timezoneData["date"] ?: ""
        val todayDate = java.time.LocalDate.parse(todayStr, dateFormatter)
        
        // Create 8 days in ascending order (Today-7 to Today)
        for (i in 7 downTo 0) {
            val targetDate = todayDate.minusDays(i.toLong())
            val dayOffset = i
            dayList.add(
                DayModel(
                    dayName = targetDate.format(dayNameFormatter),
                    dayNumber = targetDate.format(dayNumberFormatter),
                    fullDate = targetDate.format(dateFormatter),
                    dayOffset = dayOffset,
                    isSelected = dayOffset == days
                )
            )
        }
        
        daysAdapter = DaysAdapter(dayList) { selectedDay ->
            days = selectedDay.dayOffset
            currentRequestDate = selectedDay.fullDate
            updateDateDisplay()
            val request = GetLogsByDateRequest(
                prefRepository.getDriverId(),
                currentRequestDate,
                currentRequestDate
            )
            logViewMode.getLogs(request, requireContext())
        }
        
        binding.rvDays.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 4)
        binding.rvDays.adapter = daysAdapter
        binding.rvDays.scrollToPosition(7)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDateDisplay() {
        try {
            val currentDate = getDateForDaysOffset(days)
            binding.todayIndicator.text = "$currentDate ($timeZone)"
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error updating date display: ${e.message}")
            binding.todayIndicator.text = AlertCalculationUtils.daysDiff(days).toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateForDaysOffset(daysOffset: Int): String {
        return try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timeZone)
            val currentDate = timezoneData["date"] ?: ""
            
            if (daysOffset == 0) {
                return currentDate
            }
            
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = java.time.LocalDate.parse(currentDate, dateFormatter)
            val targetDate = localDate.minusDays(daysOffset.toLong())
            
            targetDate.format(dateFormatter)
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error calculating date offset: ${e.message}")
            AlertCalculationUtils.daysDiff(days).toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        binding.dateCard.setOnClickListener { clickedView -> playClickAnimation(clickedView) }
        binding.graphCard.setOnClickListener { clickedView -> playClickAnimation(clickedView) }
        binding.statusBadgeDr.setOnClickListener { clickedView -> playClickAnimation(clickedView) }
        // Set current mode label dynamically
        val modeLabel = when (prefRepository.getMode().trim().lowercase(java.util.Locale.US)) {
            "d", "drive", "driving" -> "DR"
            "on" -> "ON"
            "off" -> "OFF"
            "sb", "sleeping" -> "SB"
            "personal", "pc" -> "PC"
            "yard", "ym" -> "YM"
            else -> prefRepository.getMode().uppercase(java.util.Locale.US).take(3)
        }
        binding.tvStatusBadgeLabel.text = modeLabel
        
        binding.rlCarrierHeader.setOnClickListener { clickedView ->
            playClickAnimation(clickedView)
            val isExpanded = binding.llExpandedContent.visibility == View.VISIBLE
            if (isExpanded) {
                binding.llExpandedContent.visibility = View.GONE
                binding.ivExpandIcon.animate().rotation(0f).setDuration(200).start()
            } else {
                binding.llExpandedContent.visibility = View.VISIBLE
                binding.ivExpandIcon.animate().rotation(180f).setDuration(200).start()
            }
        }
        
        binding.retryButton.setOnClickListener { clickedView ->
            playClickAnimation(clickedView)
            loadInitialData()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        logViewMode.logByDateLiveData.observe(viewLifecycleOwner) { result: NetworkResult<GetLogsByDateResponse> ->
            Log.d("LogsFragment", "logByDateLiveData observer: result status=${result.javaClass.simpleName}")
            when (result) {
                is NetworkResult.Loading<*> -> {
                    Log.d("LogsFragment", "Logs Loading...")
                    showLoading(true)
                    showError(false)
                    disableNavigation()
                }
                is NetworkResult.Success<*> -> {
                    val response = result.data
                    showLoading(false)
                    showError(false)
                    disableNavigation()
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    // Safe handling of nested data
                    val logs = (response?.results?.userLogs ?: emptyList())
                        .sortedBy { log ->
                            if (log.datetime.isBlank()) "${log.date} ${log.time}" else log.datetime
                        }
                    val graphEvents: MutableList<ELDGraphData> = mutableListOf()

                    val todayStr = if (timeZone.isNotEmpty()) getCurrentDateInTimezone(timeZone)
                        else getCurrentDateInTimezone("America/Los_Angeles")
                    val isViewingToday = currentRequestDate == todayStr
                    val nowFloat = if (timeZone.isNotEmpty()) {
                        AlertCalculationUtils.getCurrentTimeAsFloatInTimezone(timeZone).coerceIn(0f, 24f)
                    } else {
                        24f
                    }

                    fun normalizeDutyStatus(modename: String?): String? {
                        if (modename.isNullOrBlank()) return null
                        return when (modename.trim().lowercase(Locale.US)) {
                            "off", "pc", "personal" -> "off"
                            "sb", "sleeping", "sleeper" -> "sb"
                            "d", "dr", "drive", "driving" -> "d"
                            "on", "ym", "yard" -> "on"
                            else -> null
                        }
                    }

                    fun addGraphPoint(time: Float, modename: String?) {
                        val normalizedStatus = normalizeDutyStatus(modename) ?: return
                        val clampedTime = time.coerceIn(0f, 24f)
                        graphEvents.add(ELDGraphData(clampedTime, normalizedStatus, clampedTime.toLong()))
                    }

                    fun compactGraphPoints(events: List<ELDGraphData>): MutableList<ELDGraphData> {
                        val compacted = mutableListOf<ELDGraphData>()
                        val sortedEvents = events.sortedBy { event -> event.time }
                        val sameSecondThreshold = 1f / 3600f

                        for (event in sortedEvents) {
                            val last = compacted.lastOrNull()
                            if (last == null) {
                                compacted.add(event)
                                continue
                            }

                            if (kotlin.math.abs(last.time - event.time) < sameSecondThreshold) {
                                compacted[compacted.lastIndex] = event
                                continue
                            }

                            if (last.status != event.status) {
                                compacted.add(event)
                            }
                        }

                        return compacted
                    }

                    addGraphPoint(0f, response?.results?.previousDayLog?.modename ?: "off")
                    for (log in logs) {
                        val time = AlertCalculationUtils.refinedTimeStringToFloat(log.time)
                        addGraphPoint(time, log.modename)
                    }
                    val latest = response?.results?.latestUpdatedLog
                    if (latest?.modename != null) {
                        val latestTime = AlertCalculationUtils.refinedTimeStringToFloat(latest.time).coerceIn(0f, 24f)
                        val latestStatus = normalizeDutyStatus(latest.modename)
                        val hasEquivalentPoint = latestStatus != null && graphEvents.any { event ->
                            kotlin.math.abs(event.time - latestTime) < (1f / 3600f) && event.status == latestStatus
                        }
                        if (!hasEquivalentPoint && (!isViewingToday || latestTime <= nowFloat)) {
                            addGraphPoint(latestTime, latest.modename)
                        }
                    }

                    val graphPoints = compactGraphPoints(graphEvents)
                    if (graphPoints.isEmpty()) {
                        graphPoints.add(ELDGraphData(0f, "off", 0L))
                    }
                    if (graphPoints.first().time > 0f) {
                        graphPoints.add(0, ELDGraphData(0f, "off", 0L))
                    }

                    if (!isViewingToday && response?.results?.nextDayLog?.modename != null) {
                        val nextStatus = normalizeDutyStatus(response.results.nextDayLog.modename)
                        val hasEndPoint = graphPoints.any { point -> kotlin.math.abs(point.time - 24f) < (1f / 3600f) }
                        if (nextStatus != null && !hasEndPoint) {
                            graphPoints.add(ELDGraphData(24f, nextStatus, 24L))
                        }
                    }

                    val endFloat = if (isViewingToday) nowFloat else 24f
                    val lastGraphPoint = graphPoints.lastOrNull()
                    if (lastGraphPoint != null && lastGraphPoint.time < endFloat) {
                        graphPoints.add(ELDGraphData(endFloat, lastGraphPoint.status, endFloat.toLong()))
                    }

                    val filteredList = graphPoints.sortedBy { graphPoint -> graphPoint.time }

                    binding.eldPlot.graph.invalidate()
                    
                    binding.eventLogRv.adapter = LogAdaptor(logs, childFragmentManager, requireContext(), timeZone) { log ->
                        if (!isReviewMode) {
                            selectedLog = log
                            binding.fabLogReport.isEnabled = true
                            binding.fabLogReport.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.nav_icon_active)
                            )
                        }
                    }
                    binding.eventLogRv.layoutManager = LinearLayoutManager(requireContext())
                    
                    // Meta and duration are in seconds; graph shows HH:MM (no seconds) for compact width
                    val meta = response?.results?.meta
                    val off = meta?.off ?: 0
                    val sb = meta?.sb ?: 0
                    val d = meta?.d ?: 0
                    val on = meta?.on ?: 0
                    binding.eldPlot.graph.plotGraph(filteredList, off, sb, d, on)
                    binding.eldPlot.graph.startAnimation()
                    binding.eldPlot.offDutyHours.text = formatTimeFromSeconds(off)
                    binding.eldPlot.sbHours.text = formatTimeFromSeconds(sb)
                    binding.eldPlot.drivingHours.text = formatTimeFromSeconds(d)
                    binding.eldPlot.onDutyHours.text = formatTimeFromSeconds(on)
                    binding.eldPlot.totalHours.text = formatTimeFromSeconds(off + sb + d + on)
                }
                is NetworkResult.Error<*> -> {
                    Log.e("LogsFragment", "Logs Error: ${result.message ?: "Unknown error"}")
                    showLoading(false)
                    showError(true, result.message ?: "Unknown error occurred")
                    disableNavigation()
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), result.message ?: "Unknown error", Toast.LENGTH_LONG).show()
                }
            }
        }

        homeViewModel.driverReviewLiveData.observe(viewLifecycleOwner) { result: NetworkResult<DriverReviewResponse> ->
            when (result) {
                is NetworkResult.Success<*> -> {
                    val response = result.data
                    val driver = response?.data?.driver
                    val company = response?.data?.company

                    val carrierName = company?.companyName ?: "--"
                    
                    val addressParts = listOfNotNull(company?.address, company?.city, company?.state, company?.zip).filter { it.isNotBlank() }
                    val addressStr = addressParts.joinToString(", ")
                    
                    val companyContactParts = listOfNotNull(company?.phoneNo, company?.adminEmail).filter { it.isNotBlank() }
                    val companyContact = companyContactParts.joinToString(" | ")
                    
                    val timezone = company?.companyTimezone ?: "--"
                    val dotNumber = company?.dotNo ?: "--"

                    val driverName = if (!driver?.firstName.isNullOrBlank() && !driver?.lastName.isNullOrBlank()) {
                        "${driver!!.firstName} ${driver.lastName}"
                    } else {
                        driver?.name ?: "--"
                    }
                    
                    val driverContactParts = listOfNotNull(driver?.mobile, driver?.email).filter { it.isNotBlank() }
                    val driverContact = driverContactParts.joinToString(" | ")
                    
                    val license = if (!driver?.licenseNumber.isNullOrBlank()) {
                        "${driver!!.licenseNumber} (${driver.licenseState ?: ""})"
                    } else {
                        "--"
                    }
                    val licenseDate = driver?.licensedate?.substringBefore("T") ?: "--"
                    val cycle = company?.multidaybasis ?: "--"

                    Log.d("LogsFragment", "API RESPONSE (/driver/review): $response")
                    binding.tvCarrierName.text = carrierName
                    binding.tvCompanyAddress.text = addressStr.ifEmpty { "--" }
                    binding.tvCompanyContact.text = companyContact.ifEmpty { "--" }
                    binding.tvTimezone.text = timezone
                    binding.tvDot.text = dotNumber
                    
                    binding.tvDriverName.text = driverName
                    binding.tvDriverContact.text = driverContact.ifEmpty { "--" }
                    binding.tvDriverEmail.text = driver?.email ?: "--"
                    binding.tvLicence.text = license
                    binding.tvLicenceDate.text = licenseDate
                    binding.tvCycle.text = cycle
                }
                is NetworkResult.Error<*> -> {
                    Log.e("LogsFragment", "Failed to fetch driver review: ${result.message}")
                }
                else -> {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadInitialData() {
        Log.d("LogsFragment", "loadInitialData: timeZone='$timeZone', days=$days")
        if(timeZone.isEmpty()){
            Log.w("LogsFragment", "loadInitialData: timeZone is empty, skipping load")
            return
        }
        val requestDate = getDateForDaysOffset(days)
        Log.d("LogsFragment", "loadInitialData: requestDate='$requestDate'")
        loadLogsForDate(requestDate, updateAutoRefresh = true)
        
        homeViewModel.getDriverReview(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLogsForDate(requestDate: String, updateAutoRefresh: Boolean) {
        currentRequestDate = requestDate
        val request = GetLogsByDateRequest(
            prefRepository.getDriverId(),
            requestDate,
            requestDate,
//            requestDate
        )
        logViewMode.getLogs(request, requireContext())

        if (updateAutoRefresh) {
            updateAutoRefreshForCurrentDate()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
    }

    private fun showError(show: Boolean, message: String = "") {
        binding.errorLayout.isVisible = show
        if (show && message.isNotEmpty()) {
            binding.errorText.text = message
        }
        // Hide other content when showing error
        binding.eldPlot.root.isVisible = !show
        binding.eventLogHeader.isVisible = !show
        if (!isReviewMode) {
            binding.eventLogRv.isVisible = !show
        }
    }

    private fun disableNavigation() {
        // No-op for new day selector
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoRefresh()
        isDataLoaded = false  // Reset so data loads when fragment is recreated
        _binding = null
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Start/stop auto-refresh based on current date
        updateAutoRefreshForCurrentDate()
    }

    override fun onPause() {
        super.onPause()
        // Stop auto-refresh when fragment is paused
        stopAutoRefresh()
    }
    
    private fun playClickAnimation(view: View) {
        // High-end subtle scale feedback
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(120)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    /**
     * Start the auto-refresh timer for current day logs
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        if (_binding == null) return

        autoRefreshJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                while (isActive && _binding != null) {
                    if (timeZone.isEmpty()) {
                        Log.d("LogsFragment", "Auto-refresh stopped - timezone empty")
                        return@launch
                    }

                    // Check if still viewing current day
                    if (days != 0) {
                        Log.d("LogsFragment", "Stopping auto-refresh - not viewing current day")
                        stopAutoRefresh()
                        return@launch
                    }

                    // Calculate request date on background thread
                    val requestDate = withContext(Dispatchers.Default) {
                        try {
                            getDateForDaysOffset(days)
                        } catch (e: Exception) {
                            Log.e("LogsFragment", "Error getting date: ${e.message}")
                            null
                        }
                    }

                    if (requestDate != null && _binding != null) {
                        Log.d("LogsFragment", "Auto-refreshing logs for current day")
                        loadLogsForDate(requestDate, updateAutoRefresh = false)
                    }

                    delay(autoRefreshInterval)
                }
            } catch (e: Exception) {
                Log.e("LogsFragment", "Error in auto-refresh: ${e.message}")
            }
        }
        autoRefreshJob?.invokeOnCompletion { autoRefreshJob = null }
        Log.d("LogsFragment", "Auto-refresh started for current day logs")
    }
    
    /**
     * Stop the auto-refresh timer
     */
    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        Log.d("LogsFragment", "Auto-refresh stopped")
    }
    
    /**
     * Update auto-refresh based on whether we're viewing current day or not
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAutoRefreshForCurrentDate() {
        if (_binding == null) return
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                val shouldStart = days == 0 && timeZone.isNotEmpty()
                if (shouldStart) {
                    startAutoRefresh()
                } else {
                    stopAutoRefresh()
                }
            } catch (e: Exception) {
                Log.e("LogsFragment", "Error updating auto-refresh: ${e.message}")
            }
        }
    }
}
