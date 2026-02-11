package com.truckspot.fragment.ui.logs
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.truckspot.databinding.FragmentLogsBinding
import com.truckspot.fragment.ui.home.HomeViewModel
import com.truckspot.models.GetLogsByDateRequest
import com.truckspot.utils.*
import com.truckspot.utils.AlertCalculationUtils.setDateAndTimeBasedOnTimezone
import com.truckspot.utils.AlertCalculationUtils.getCurrentDateInTimezone
import com.truckspot.utils.Utils.toHoursMinutesFormate
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@AndroidEntryPoint
class LogsFragment : Fragment() {
    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel by viewModels<HomeViewModel>()
    private val logViewMode by viewModels<LogViewModel>()
    lateinit var prefRepository: PrefRepository

    lateinit var date: Date
    var days = 0

    var timeZone: String = ""
    
    // Timer variables for auto-refresh
    private var autoRefreshJob: Job? = null
    private val AUTO_REFRESH_INTERVAL = 30000L // 30 seconds
    
    // Flag to prevent duplicate data loading
    private var isDataLoaded = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefRepository = PrefRepository(requireContext())

        date = Date()
        _binding = FragmentLogsBinding.inflate(inflater, container, false)

        // Initialize with timezone from preferences first
        if(prefRepository.getTimeZone().isNotEmpty()){
            timeZone = prefRepository.getTimeZone()
            updateDateDisplay()
            if (!isDataLoaded) {
                isDataLoaded = true
                loadInitialData()
            }
        }
        
        // Observer for company data - only load if not already loaded
        homeViewModel.company.observe(viewLifecycleOwner) {
            val newTimeZone = it.data?.results?.company_timezone ?: ""
            if (newTimeZone.isNotEmpty() && newTimeZone != timeZone) {
                timeZone = newTimeZone
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
        
        updateDateDisplay()

        val root: View = binding.root
        setupClickListeners()
        setupObservers()
        
        // Setup SwipeRefreshLayout for pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("LogsFragment", "Swipe refresh triggered")
            loadInitialData()
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDateDisplay() {
        try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timeZone)
            val currentDate = getDateForDaysOffset(days, timezoneData["date"] ?: "")
            binding.dateTxt.text = currentDate
            
            // Add timezone indicator to the date display
            binding.dateTxt.text = "$currentDate ($timeZone)"
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error updating date display: ${e.message}")
            // Fallback to original method
            binding.dateTxt.text = AlertCalculationUtils.daysDiff(days).toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateForDaysOffset(daysOffset: Int, baseDate: String): String {
        return try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timeZone)
            val currentDate = timezoneData["date"] ?: ""
            
            if (daysOffset == 0) {
                return currentDate
            }
            
            // Calculate the date based on days offset
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
    private fun getCurrentTimezoneInfo(): String {
        return try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timeZone)
            val currentTime = timezoneData["time"] ?: ""
            val currentDate = timezoneData["date"] ?: ""
            "Current $timeZone Time: $currentTime, Date: $currentDate"
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error getting timezone info: ${e.message}")
            "Timezone: $timeZone"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        _binding?.leftArrow?.setOnClickListener {
            if (days <= 9) {
                SLog.detailLogs(
                    "GETTING LOGS FROM LEFT CLICK ",
                    "\n",
                    true
                )
                val currentDate = ++days
                updateDateDisplay()
                val request = GetLogsByDateRequest(
                    prefRepository.getDriverId(), 
                    getDateForDaysOffset(currentDate, ""),
//                    getDateForDaysOffset(currentDate, ""),
                    getDateForDaysOffset(currentDate, "")
                )
                logViewMode.getLogs(request, requireContext())
                updateArrowVisibility()
                
                // Update auto-refresh based on current date
                updateAutoRefreshForCurrentDate()
            } else {
                showValidationErrors("you cannot see more than 8 days old logsheet")
            }
        }
        
        _binding?.rightArrow?.setOnClickListener {
            if (days > 0) {
                SLog.detailLogs(
                    "GETTING LOGS FROM RIGHT CLICK ",
                    "\n",
                    true
                )

                val currentDate = --days
                updateDateDisplay()
                val request = GetLogsByDateRequest(
                    prefRepository.getDriverId(), 
                    getDateForDaysOffset(currentDate, ""),
                    getDateForDaysOffset(currentDate, ""),
//                    getDateForDaysOffset(currentDate, "")
                )
                logViewMode.getLogs(request, requireContext())
                updateArrowVisibility()
                
                // Update auto-refresh based on current date
                updateAutoRefreshForCurrentDate()
            }
        }
        
        // Set up retry button click listener
        binding.retryButton.setOnClickListener {
            loadInitialData()
        }
    }

    private fun setupObservers() {
        logViewMode.logByDateLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoading(true)
                    showError(false)
                    disableNavigation(true)
                }
                is NetworkResult.Success -> {
                    showLoading(false)
                    showError(false)
                    disableNavigation(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    // Safe handling of nested data
                    val logs = result.data?.results?.userLogs ?: emptyList()
                    var logList: MutableList<ELDGraphData>? = mutableListOf()
                    logList?.clear();

                    if( result.data?.results?.previousDayLog?.modename != null){

                        logList?.add(  // adding graph data
                            ELDGraphData(
                                0.toFloat(),
                                result.data.results.previousDayLog.modename,
                                0.toLong()
                            )
                        )
                    }
                    logs.forEach {
                        val time: Float = it.time.let {
                            AlertCalculationUtils.refinedTimeStringToFloat(it!!)
                        }

                        logList?.add(  // adding graph data
                            ELDGraphData(
                                time,
                                it.modename,
                                time.toLong()
                            )
                        )
                    }
                    if( result.data?.results?.latestUpdatedLog?.modename != null){
                        val time: Float =  result.data.results.latestUpdatedLog.time.let {
                                AlertCalculationUtils.refinedTimeStringToFloat(it)
                        }
                        logList?.add(  // adding graph data
                            ELDGraphData(
                                time,
                                result.data.results.latestUpdatedLog.modename,
                                time.toLong()
                            )
                        )
                    }
                    if( result.data?.results?.nextDayLog?.modename != null ){
                        result.data.results.nextDayLog.time.let {
                            if(it != null && it.isNotEmpty())
                            AlertCalculationUtils.refinedTimeStringToFloat(it)
                            else
                                24.0.toFloat()
                        }
                        logList?.add(  // adding graph data
                            ELDGraphData(
                                24.toFloat(),
                                result.data.results.nextDayLog.modename,
                                24.toLong()
                            )
                        )
                    }
                    val filteredList = logList?.filter { it.status != "login" && it.status != "logout" && it.status != "personal" && it.status != "yard" && it.status != "certification" && it.status != "INT" &&  it.status != "eng_off" && it.status != "eng_on" && it.status != "power_on" && it.status != "power_off"}


                    binding.eldPlot.graph.plotGraph(filteredList)
                    binding.eldPlot.graph.invalidate()
                    
//                    if (userLogs != null) {
                        binding.eventLogRv.adapter = LogAdaptor(logs, childFragmentManager, requireContext(), timeZone)
                        binding.eventLogRv.layoutManager = LinearLayoutManager(requireContext())
//                    }
                    
                    // Safe handling of meta data
                    val meta = result.data?.results?.meta
                    val totalTime = (meta?.off ?: 0) + (meta?.d ?: 0) + (meta?.sb ?: 0) + (meta?.on ?: 0)
                    
                    binding.eldPlot.offDutyHours.text = meta?.off?.toHoursMinutesFormate() ?: "0"
                    binding.eldPlot.sbHours.text = meta?.sb?.toHoursMinutesFormate() ?: "0"
                    binding.eldPlot.drivingHours.text = meta?.d?.toHoursMinutesFormate() ?: "0"
                    binding.eldPlot.onDutyHours.text = meta?.on?.toHoursMinutesFormate() ?: "0"
                    binding.eldPlot.totalHours.text = totalTime.toHoursMinutesFormate()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    showError(true, result.message ?: "Unknown error occurred")
                    disableNavigation(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), result.message ?: "Unknown error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadInitialData() {
        if(timeZone.isEmpty()){
            return
        }
        val requestDate = getDateForDaysOffset(days, "")
        loadLogsForDate(requestDate, updateAutoRefresh = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLogsForDate(requestDate: String, updateAutoRefresh: Boolean) {
        val request = GetLogsByDateRequest(
            prefRepository.getDriverId(),
            requestDate,
            requestDate,
//            requestDate
        )
        logViewMode.getLogs(request, requireContext())
        updateArrowVisibility()

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
        binding.eventLogRv.isVisible = !show
    }

    private fun disableNavigation(disable: Boolean) {
        _binding?.leftArrow?.isEnabled = !disable
        _binding?.rightArrow?.isEnabled = !disable
    }

    private fun updateArrowVisibility() {
        if (days == 0) {
            _binding?.rightArrow?.visibility = View.GONE
        } else {
            _binding?.rightArrow?.visibility = View.VISIBLE
        }
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

    private fun showValidationErrors(
        error: String, dialogInterface: Utils.dialogInterface? = null
    ) {
        Utils.dialog(requireContext(), message = error, callback = dialogInterface)
    }
    
    /**
     * Check if the current view is showing today's logs
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isViewingCurrentDay(): Boolean {
        return try {
            if (timeZone.isEmpty()) return false
            if (_binding == null) return false
            
            // Simple check based on days offset - today is always days == 0
            days == 0
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error checking if viewing current day: ${e.message}")
            false
        }
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
                            getDateForDaysOffset(days, "")
                        } catch (e: Exception) {
                            Log.e("LogsFragment", "Error getting date: ${e.message}")
                            null
                        }
                    }

                    if (requestDate != null && _binding != null) {
                        Log.d("LogsFragment", "Auto-refreshing logs for current day")
                        loadLogsForDate(requestDate, updateAutoRefresh = false)
                    }

                    delay(AUTO_REFRESH_INTERVAL)
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
