package com.eagleye.eld.fragment.ui.home
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.eagleye.eld.R
import com.eagleye.eld.R.drawable.home_bg_deign
import com.eagleye.eld.R.drawable.home_bg_design_selected
import com.eagleye.eld.R.layout.*
import com.eagleye.eld.R.string.shipping_number
import com.eagleye.eld.R.string.trailer_number
import com.eagleye.eld.databinding.FragmentHomeBinding
import com.eagleye.eld.fragment.Dashboard
import com.eagleye.eld.fragment.ui.viewmodels.DashboardViewModel
import com.eagleye.eld.LoginActivity
import com.eagleye.eld.models.DRIVE_MODE.*
import com.eagleye.eld.models.UserLog
import com.eagleye.eld.pt.devicemanager.AppModel
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.updateLogRequest
import com.eagleye.eld.utils.*
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_DRIVING
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_OFF
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_ON
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_PERSONAL
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_SLEEPING
import com.eagleye.eld.utils.Utils.dialogInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.joda.time.DateTime
import java.math.BigDecimal
import java.math.BigDecimal.valueOf
import java.math.RoundingMode.FLOOR
import java.text.SimpleDateFormat
import java.util.*
import androidx.annotation.RequiresApi
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.request.AddOffsetRequest
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_YARD
import com.eagleye.eld.utils.Utils.getDouble
import com.eagleye.eld.utils.Utils.toHoursMinutesFormate
import com.whizpool.supportsystem.SLog
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), OnClickListener {
    companion object {
        private const val TAG = "HomeFragment_VIN"
        val isTesting = true
    }

    private var _binding: FragmentHomeBinding? = null

    @Inject
    lateinit var prefRepository: PrefRepository
    var mActivity: FragmentActivity? = null
    private val binding get() = _binding!!
    private val homeViewModel by viewModels<HomeViewModel>()
    private val dashboardViewModel by activityViewModels<DashboardViewModel>()
    
    // Cached TimeZone objects for performance
    private var cachedTimeZoneId: ZoneId? = null
    private var lastTimeZoneStr: String = ""
    private var blinkAnimation: android.view.animation.Animation? = null

    private var clockJob: Job? = null
    private var locationJob: Job? = null
    private var conditionsRefreshJob: Job? = null
    private val CONDITIONS_REFRESH_INTERVAL_MS = 120_000L // 2 minutes – conditions & remaining times
    private var timeZone: String = ""
    private var lastLogTime: String = ""
    private var lastLogMode: String = ""
    private var lastRelevantLog: HomeDataModel.Log? = null
    private val relevantLogTypes = setOf("d", "off", "sb", "on", "yard", "personal")
    private var lastEngineApiCallTime: Long = 0
    private val ENGINE_API_DEBOUNCE_MS = 30000L // 30 seconds debounce
    private var lastSpeedCheckTime: Long = 0
    private val SPEED_CHECK_THROTTLE_MS = 2000L // 2 seconds throttle
    private var lastPushedEngineState: String = ""
    private var engineStateStableCount: Int = 0
    private val ENGINE_STATE_STABLE_THRESHOLD = 3

    private lateinit var mediaPlayer: MediaPlayer

    // Bluetooth connection state tracking
    private var isBluetoothConnecting = false
    private var receiversRegistered = false
    private var bluetoothConnectionJob: Job? = null

    // API call debouncing
    private var lastApiCallTime: Long = 0
    private var lastPauseTime: Long = 0
    private val API_CALL_DEBOUNCE_MS = 30000L // 30 seconds debounce for API calls
    private val LONG_PAUSE_THRESHOLD_MS = 60000L // 1 minute - considered long pause
    private var isFetchingLogs = false // Flag to prevent concurrent API calls

    var hrs_MODE_OFF = 0.0
    var hrs_MODE_ON = 0.0
    var hrs_MODE_D = 0.0
    var hrs_MODE_SB = 0.0
    var hrs_MODE_YARD = 0.0
    var hrs_MODE_PERSONAL = 0.0

    var miles: BigDecimal? = null
    var mOdometer: String? = null
    var mEngineHours: String? = null
    var location: String? = null

    var isEmptyList = false
    lateinit var yoYo: YoYo.AnimationComposer

    val svcIf = IntentFilter()
    val tmIf = IntentFilter()

    // Simple last log tracking like EagleEye
    // NOTE: Initialized from persisted mode so screen refresh doesn't reset it to empty
    private var lastLog: String = ""

    var lastEngineLogName: String = ""
    var tmRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "tmRefresh broadcast")
            if (!isBluetoothConnecting) {
                updateTelemetryInfo()
            }
        }
    }
    var svcRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "svcRefresh broadcast")
            if (!isBluetoothConnecting) {
                updateTelemetryInfo()
            }
        }
    }
    var viRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateVehicleInfo()
        }
    }

    // Speed monitoring broadcast receiver
    var speedRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            checkAndPrintSpeed()
        }
    }
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var progressBarShift: CircularProgressIndicator
    private lateinit var cycleRemaining: CircularProgressIndicator
    private lateinit var untilBreak: CircularProgressIndicator
    private var selectedLog: String = TRUCK_MODE_OFF
    private var observersSet = false

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        // Track when we paused to detect long minimization
        lastPauseTime = System.currentTimeMillis()
        // Cancel jobs when fragment is not in the foreground
        clockJob?.cancel()
        bluetoothConnectionJob?.cancel()
        speedMonitoringJob?.cancel()  // Stop speed monitoring when paused
        conditionsRefreshJob?.cancel()
    }

    @SuppressLint("SuspiciousIndentation", "ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        val speedIf = IntentFilter()
        speedIf.addAction("REFRESH")

        svcIf.addAction("SVC-BOUND-REFRESH")
        tmIf.addAction("REFRESH")
        tmIf.addAction("TRACKER-REFRESH")

        // NOTE: Receivers are registered in onResume() to avoid duplicate registration

        progressBar = rootView.findViewById(R.id.progressBarMain)
        progressBarShift = rootView.findViewById(R.id.progressBarShift)
        cycleRemaining = rootView.findViewById(R.id.progressBarCycle)
        untilBreak = rootView.findViewById(R.id.progressBarBreak)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.clicksoud)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mActivity = activity
        yoYo = YoYo.with(Techniques.Tada)
        prefRepository = PrefRepository(mActivity!!)
        binding.btnOff.setOnClickListener(this)
        binding.btnSleep.setOnClickListener(this)
        binding.btnDrive.setOnClickListener(this)
        binding.btnOn.setOnClickListener(this)
        binding.tvPerosnaluse.setOnClickListener(this)
        binding.tvYarduse.setOnClickListener(this)

        binding.settingsIcon.setOnClickListener {
            showSettingsPopupMenu(it)
        }

        val defaultVin = "---------"
        val vehicleInfo = AppModel.getInstance().mVehicleInfo
        val vin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: defaultVin
        binding.vinNumber.text = vin
        Log.d("VIN_DEBUG", "Initial VIN set to: $vin")
        homeViewModel.getOffSet(prefRepository, vin, requireContext())
        homeViewModel.getCompanyName(requireContext())
        binding.name.text = prefRepository.getName()

        binding.drivingSpeed.text = "Driving Speed: 0 km/h"
        binding.companyName.text = "Company Name: Loading..."

        val userName = prefRepository.getName()
        if (userName.isNotEmpty()) {
            val firstLetter = userName.first().uppercase()
            binding.profileInitial.text = firstLetter
        } else {
            binding.profileInitial.text = "U"
        }
        homeViewModel.connectSocket(prefRepository.getDriverId())
        homeViewModel.setLogUpdatedCallback {
            requireActivity().runOnUiThread {
                homeViewModel.getHome(requireContext())
            }
        }

        binding.editShipping.setOnClickListener { editShippingNumberPopup() }
        binding.editCoDriver.setOnClickListener { editCoDriverPopup() }
        binding.editTrailerNo.setOnClickListener { editTrailerNumberPopup() }
        
        // Always set observers on view creation
        setupObservers()
        
        // Load animations once
        blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)
        
        // Setup SwipeRefreshLayout for pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe refresh triggered")
            isFetchingLogs = true
            homeViewModel.getHome(requireContext())
            homeViewModel.getCompanyName(requireContext())
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.green,
            R.color.orange,
            R.color.blue
        )
        
        // Set OFF mode as default on initial load
        homeViewModel.trackingMode.set(MODE_OFF)
        updateUI(binding.btnOff)
        
        updateUI()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        homeViewModel.company.observe(viewLifecycleOwner) {
            if (it.data?.results == null) return@observe
            timeZone = it.data.results.company_timezone ?: ""
            prefRepository.setTimeZone(timeZone)
            binding.ManufactureName.text = it.data.results.company_name
            binding.companyName.text = "Company Name: ${it.data.results.company_name}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updateClockDisplay()
            }
        }

        homeViewModel.homeLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success<*> -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    isFetchingLogs = false
                    if (it.data?.logs == null || it.data.logs!!.isEmpty()) {
                        Toast.makeText(context, "Logs are Empty", Toast.LENGTH_SHORT).show()
                        // Only set OFF if user is not already on an active mode
                        val currentMode = homeViewModel.trackingMode.get()
                        if (currentMode == null || currentMode == MODE_OFF) {
                            homeViewModel.trackingMode.set(MODE_OFF)
                            updateUI(binding.btnOff)
                        }
                        return@observe
                    }
                    updateLastRelevantLog(it.data.logs)
                    // Update UI components based on the fetched data
                    updateGauges(it.data)
                    updateViolationTimeCard(it.data.conditions)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        updateClockDisplay()
                    }
                    updateUIBasedOnLogs(it.data)
                }
                is NetworkResult.Error<*> -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    isFetchingLogs = false
                    // Only set OFF if user is not already on an active mode
                    val currentMode = homeViewModel.trackingMode.get()
                    if (currentMode == null || currentMode == MODE_OFF) {
                        homeViewModel.trackingMode.set(MODE_OFF)
                        updateUI(binding.btnOff)
                    }
                    // Don't show error dialog - just log it
                    Log.e(TAG, "homeLiveData ERROR: ${it.message}")
                }
                is NetworkResult.Loading<*> -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }

        // Simple observer for mode change response like EagleEye
        homeViewModel.addLogReponse.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success<*> -> {
                    Log.d(TAG, "✅ addLogReponse SUCCESS")
                    binding.progressBar.visibility = View.GONE
                    
                    // Update UI after mode change
                    updateUIAfterModeChange(selectedLog.ifEmpty { TRUCK_MODE_OFF })
                    
                    // Fetch updated logs
                    homeViewModel.getHome(requireContext())
                }
                is NetworkResult.Error<*> -> {
                    Log.e(TAG, "❌ addLogReponse ERROR: ${it.message}")
                    binding.progressBar.visibility = View.GONE
                    
                    // Still update UI to selected mode even if API fails
                    // This handles the case when data is empty or API has issues
                    updateUIAfterModeChange(selectedLog.ifEmpty { TRUCK_MODE_OFF })
                    
                    // Don't show error dialog - just silently handle it
                    // User can see the mode is activated in UI
                    Log.d(TAG, "Mode UI updated despite API error - user can still use the app")
                }
                is NetworkResult.Loading<*> -> {
                    Log.d(TAG, "⏳ addLogReponse LOADING")
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateGauges(data: HomeDataModel?) {
        if (data?.conditions == null || _binding == null) return

        // Conditions from backend are in seconds; format as HH:MM:SS
        val formatCondition: (Int) -> String = { Utils.formatTimeFromSeconds(it) }
        val driveTotalSec = 11 * 3600
        val cycleTotalSec = 70 * 3600
        val shiftTotalSec = 14 * 3600
        val breakTotalSec = 8 * 3600

        if (data.conditions!!.driveViolation!!) {
            binding.timeText1.text = formatCondition(data.conditions?.drive ?: 0)
            binding.timeText1.setTextColor(Color.RED)
            binding.progressBarMain.startAnimation(blinkAnimation)
            binding.progressBarMain.setIndicatorColor(Color.RED)
            binding.progressBarMain.progress = 100
        } else {
            val remaining = (data.conditions?.drive ?: 0)
            val safeSpent = (driveTotalSec - remaining).coerceIn(0, driveTotalSec)
            val progressPercent = (safeSpent.toFloat() / driveTotalSec * 100).toInt()
            binding.progressBarMain.clearAnimation()
            binding.timeText1.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.progressBarMain.max = 100
            binding.progressBarMain.progress = progressPercent
            val color = when (progressPercent) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                else -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
            }
            if ((data.conditions?.drive ?: 0) < 0) {
                 binding.progressBarMain.setIndicatorColor(Color.RED)
                 binding.timeText1.setTextColor(Color.RED)
            } else {
                 binding.progressBarMain.setIndicatorColor(color)
            }
            binding.timeText1.text = formatCondition(data.conditions?.drive ?: 0)
        }

        if (data.conditions!!.cycleViolation ?: false) {
            binding.timeText3.text = formatCondition(data.conditions?.cycle ?: 0)
            binding.timeText3.setTextColor(Color.RED)
            binding.progressBarCycle.startAnimation(blinkAnimation)
            binding.progressBarCycle.setIndicatorColor(Color.RED)
            binding.progressBarCycle.progress = 100
        } else {
            val remaining = (data.conditions?.cycle ?: 0)
            val safeSpent = (cycleTotalSec - remaining).coerceIn(0, cycleTotalSec)
            val progressPercent = (safeSpent.toFloat() / cycleTotalSec * 100).toInt()
            binding.progressBarCycle.clearAnimation()
            binding.timeText3.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.progressBarCycle.max = 100
            binding.progressBarCycle.progress = progressPercent
            val color = when (progressPercent) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                else -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
            }
            if ((data.conditions?.cycle ?: 0) < 0) {
                 binding.progressBarCycle.setIndicatorColor(Color.RED)
                 binding.timeText3.setTextColor(Color.RED)
            } else {
                 binding.progressBarCycle.setIndicatorColor(color)
            }
            binding.timeText3.text = formatCondition(data.conditions?.cycle ?: 0)
        }

        if (data.conditions!!.shiftViolation ?: false) {
            binding.timeText2.text = formatCondition(data.conditions?.shift ?: 0)
            binding.timeText2.setTextColor(Color.RED)
            binding.progressBarShift.startAnimation(blinkAnimation)
            binding.progressBarShift.setIndicatorColor(Color.RED)
            binding.progressBarShift.progress = 100
        } else {
            val remaining = (data.conditions?.shift ?: 0)
            val safeSpent = (shiftTotalSec - remaining).coerceIn(0, shiftTotalSec)
            val progressPercent = (safeSpent.toFloat() / shiftTotalSec * 100).toInt()
            binding.progressBarShift.clearAnimation()
            binding.timeText2.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.progressBarShift.max = 100
            binding.progressBarShift.progress = progressPercent
            val color = when (progressPercent) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                else -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
            }
            if ((data.conditions?.shift ?: 0) < 0) {
                 binding.progressBarShift.setIndicatorColor(Color.RED)
                 binding.timeText2.setTextColor(Color.RED)
            } else {
                 binding.progressBarShift.setIndicatorColor(color)
            }
            binding.timeText2.text = formatCondition(data.conditions?.shift ?: 0)
        }

        if (data.conditions!!.driveBreakViolation ?: false) {
            binding.timeTextBreak.text = ("Voilation")
            binding.timeTextBreak.setTextColor(Color.RED)
            binding.progressBarBreak.startAnimation(blinkAnimation)
            binding.progressBarBreak.setIndicatorColor(Color.RED)
            binding.progressBarBreak.progress = 0
        } else {
            val remaining = (data.conditions?.drivebreak ?: 0)
            val safeSpent = (breakTotalSec - remaining).coerceIn(0, breakTotalSec)
            val progressPercent = (safeSpent.toFloat() / breakTotalSec * 100).toInt()
            binding.progressBarBreak.clearAnimation()
            binding.timeTextBreak.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.progressBarBreak.max = 100
            binding.progressBarBreak.progress = progressPercent
            val color = when (progressPercent) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                else -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
            }
            binding.progressBarBreak.setIndicatorColor(color)
            binding.timeTextBreak.text = formatCondition(data.conditions?.drivebreak ?: 0)
        }
    }

    private fun updateUIAfterModeChange(mode: String) {
        when (mode) {
            TRUCK_MODE_OFF -> {
                homeViewModel.trackingMode.set(MODE_OFF)
                updateUI(binding.btnOff)
            }
            TRUCK_MODE_ON -> {
                homeViewModel.trackingMode.set(MODE_ON)
                updateUI(binding.btnOn)
            }
            TRUCK_MODE_SLEEPING -> {
                homeViewModel.trackingMode.set(MODE_SB)
                updateUI(binding.btnSleep)
            }
            TRUCK_MODE_DRIVING -> {
                homeViewModel.trackingMode.set(MODE_D)
                updateUI(binding.btnDrive)
            }
            TRUCK_MODE_YARD -> {
                updateUI(binding.tvYarduse)
            }
            TRUCK_MODE_PERSONAL -> {
                updateUI(binding.tvPerosnaluse)
            }
        }
    }

    private fun normalizeEngineMode(modeName: String?): String {
        return when (modeName?.trim()?.lowercase(Locale.US)) {
            "eng_on", "e_on", "power_on" -> "eng_on"
            "eng_off", "e_off", "power_off" -> "eng_off"
            else -> ""
        }
    }

    private fun updateUIBasedOnLogs(data: HomeDataModel?) {
        var logList: MutableList<ELDGraphData>? = mutableListOf()
        logList?.clear()
        data?.previousDayLog?.let {
            logList?.add(
                ELDGraphData(
                    0.0.toFloat(),
                    it.modename,
                    0.0.toLong()
                )
            )
        }
        data?.logs?.forEach {
            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
            logList?.add(
                ELDGraphData(
                    time,
                    it.modename,
                    time.toLong()
                )
            )
        }
        data?.latestUpdatedLog?.let {
            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
            logList?.add(
                ELDGraphData(
                    time,
                    it.modename,
                    time.toLong()
                )
            )
        }
        val latestEngineFromLogs = data?.logs
            ?.asReversed()
            ?.map { normalizeEngineMode(it.modename) }
            ?.firstOrNull { it.isNotEmpty() }
            ?: ""
        val latestEngineFromUpdated = normalizeEngineMode(data?.latestUpdatedLog?.modename)
        val latestEngineFromPrevious = normalizeEngineMode(data?.previousDayLog?.modename)
        lastEngineLogName = when {
            latestEngineFromLogs.isNotEmpty() -> latestEngineFromLogs
            latestEngineFromUpdated.isNotEmpty() -> latestEngineFromUpdated
            latestEngineFromPrevious.isNotEmpty() -> latestEngineFromPrevious
            else -> ""
        }
        if (lastPushedEngineState.isEmpty() && lastEngineLogName.isNotEmpty()) {
            lastPushedEngineState = lastEngineLogName
            Log.d(TAG, "Initialized lastPushedEngineState to: $lastPushedEngineState")
        }

        // Exclude login/logout (and other non-duty) so current mode reflects duty status only
        val filterForSelection = logList?.filter { it.status != "login" && it.status != "logout" && it.status != "certification" && it.status != "INT" }

        if (filterForSelection != null && filterForSelection.isNotEmpty()) {
            val latestMode = filterForSelection.last().status
            // Keep lastLog in sync so Bluetooth auto-switch doesn't override this mode
            lastLog = latestMode
            when (latestMode) {
                TRUCK_MODE_OFF -> updateUI(binding.btnOff)
                TRUCK_MODE_ON -> updateUI(binding.btnOn)
                TRUCK_MODE_SLEEPING -> updateUI(binding.btnSleep)
                TRUCK_MODE_DRIVING -> updateUI(binding.btnDrive)
                TRUCK_MODE_YARD -> updateUI(binding.tvYarduse)
                TRUCK_MODE_PERSONAL -> updateUI(binding.tvPerosnaluse)
            }
        } else {
            // Only set OFF if user is not already on an active mode
            val currentMode = homeViewModel.trackingMode.get()
            if (currentMode == null || currentMode == MODE_OFF) {
                homeViewModel.trackingMode.set(MODE_OFF)
                lastLog = TRUCK_MODE_OFF
                updateUI(binding.btnOff)
            }
        }
    }

    private fun showConnectToDeviceDialog(
        actualOdo: Double,
        actualEngHours: Double,
        mostRecentLogWithValidOdometer: UserLog,
    ) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Unidentified Logs Detected")
        val additionalMessage = "Do you want to authorize these unidentified logs?\n\n"
        val readingsMessage =
            "Unidentified Odometer Reading: $actualOdo\nUnidentified Engine Hours Reading: $actualEngHours"
        val finalMessage = "$additionalMessage$readingsMessage"
        alertDialogBuilder.setMessage(finalMessage)

        alertDialogBuilder.setPositiveButton("Yes") { dialog, _ ->
            prefRepository.setShowUnidentifiedDialog(false)
            prefRepository.setUnauthorized(true)
            val unIdentifiedTime = actualOdo / 45.0
            Log.d(TAG, "45MINUTESTIME:${unIdentifiedTime}")
            var unIdentifiedOnHours = 0.0
            val unIdentifiedDrivingHours: Double
            if (unIdentifiedTime < actualEngHours) {
                unIdentifiedOnHours = actualEngHours - unIdentifiedTime
                unIdentifiedDrivingHours = unIdentifiedTime
            } else
                unIdentifiedDrivingHours = actualEngHours
            val updateLogRequest = updateLogRequest(
                mostRecentLogWithValidOdometer.id,
                unIdentifiedDrivingHours,
                mostRecentLogWithValidOdometer.end_DateTime,
                mostRecentLogWithValidOdometer.modename,
                mostRecentLogWithValidOdometer.odometerreading,
                mostRecentLogWithValidOdometer.eng_hours,
                mostRecentLogWithValidOdometer.time,
                0
            )
            Log.d(TAG, "Unidentified driving hours: $unIdentifiedDrivingHours")
            homeViewModel.updateLog(updateLogRequest, shouldHandleResponse = true, requireContext())
            dialog.dismiss()
        }

        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            prefRepository.setShowUnidentifiedDialog(false)
            var vin = ""
            if (AppModel.getInstance().mVehicleInfo != null && AppModel.getInstance().mVehicleInfo.VIN != null) {
                vin = AppModel.getInstance().mVehicleInfo.VIN
            }
            var odo = prefRepository.getDiffinOdo()
            if (odo.isNullOrEmpty() || odo == "null") {
                odo = "0"
            }
            var eng = prefRepository.getDiffinEng()
            if (eng.isNullOrEmpty() || eng == "null") {
                eng = "0"
            }
            homeViewModel.addOffset(
                prefRepository,
                AddOffsetRequest(
                    odo.toInt().plus(actualOdo.toInt()),
                    eng.toInt().plus(actualEngHours.toInt()),
                    vin
                ),
                requireContext()
            )
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        
        Log.d(TAG, "onResume: START")
        
        try {
            // CRITICAL: Cancel all running jobs first to prevent zombie coroutines
            clockJob?.cancel()
            bluetoothConnectionJob?.cancel()
            
            // Reset flags on resume to prevent stuck states
            isFetchingLogs = false
            
            // Hide progress bar immediately
            binding.progressBar.visibility = View.GONE
            
            // Check if we were paused for a long time (more than 1 minute)
            val currentTime = System.currentTimeMillis()
            val pauseDuration = if (lastPauseTime > 0) currentTime - lastPauseTime else 0
            val wasLongPause = pauseDuration > LONG_PAUSE_THRESHOLD_MS
            
            if (wasLongPause) {
                Log.d(TAG, "onResume: Long pause detected (${pauseDuration/1000}s), will refresh silently")
            }
            
            Log.d(TAG, "onResume: Flags reset, connecting socket...")
            
            // Connect socket (runs on background thread internally)
            try {
                homeViewModel.connectSocket(prefRepository.getDriverId())
            } catch (e: Exception) {
                Log.e(TAG, "onResume: Socket connection error: ${e.message}")
            }
            
            // Register receivers (guarded to prevent duplicate registrations)
            registerLocalReceivers()
            
            // Fetch data with debounce - but skip if long pause to avoid immediate loading
            if (!wasLongPause && !isFetchingLogs && currentTime - lastApiCallTime > API_CALL_DEBOUNCE_MS) {
                isFetchingLogs = true
                lastApiCallTime = currentTime
                Log.d(TAG, "onResume: Fetching home data...")
                try {
                    homeViewModel.getHome(requireContext())
                } catch (e: Exception) {
                    Log.e(TAG, "onResume: getHome error: ${e.message}")
                    isFetchingLogs = false
                }
            } else if (wasLongPause) {
                // For long pauses, just use cached data - don't trigger API call
                Log.d(TAG, "onResume: Using cached data after long pause")
                lastApiCallTime = currentTime // Reset debounce timer
            } else {
                Log.d(TAG, "onResume: Skipping fetch, debounce active")
            }
            
            // Update telemetry (wrapped in try-catch)
            try {
                updateTelemetryInfo()
            } catch (e: Exception) {
                Log.e(TAG, "onResume: updateTelemetryInfo error: ${e.message}")
            }
            
            // Start clock (already cancels previous job internally)
            try {
                startClock()
            } catch (e: Exception) {
                Log.e(TAG, "onResume: startClock error: ${e.message}")
            }

            // Auto-refresh conditions/remaining every 2 min so home stays in sync without socket event
            conditionsRefreshJob?.cancel()
            conditionsRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
                while (isActive) {
                    delay(CONDITIONS_REFRESH_INTERVAL_MS)
                    if (_binding != null && !isFetchingLogs) {
                        try {
                            homeViewModel.getHome(requireContext())
                        } catch (e: Exception) {
                            Log.e(TAG, "Conditions refresh error: ${e.message}")
                        }
                    }
                }
            }
            conditionsRefreshJob?.invokeOnCompletion { conditionsRefreshJob = null }
            
            Log.d(TAG, "onResume: END - All operations completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "onResume: CRITICAL ERROR: ${e.message}", e)
            // Ensure we're in a clean state even if something fails
            isFetchingLogs = false
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun logVinState() {
        val vehicleInfo = AppModel.getInstance().mVehicleInfo
        Log.d(TAG, "VehicleInfo exists: ${vehicleInfo != null}")
        Log.d(TAG, "VIN exists: ${vehicleInfo?.VIN != null}")
        Log.d(TAG, "VIN value: ${vehicleInfo?.VIN ?: "NULL"}")
        Log.d(TAG, "VIN empty: ${vehicleInfo?.VIN?.isEmpty() ?: true}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleLocationUpdate(speed: Int, rpm: Int, name: String) {
        Log.d(TAG, "📍 handleLocationUpdate: Speed=$speed, RPM=$rpm, Event=$name, lastLog=$lastLog")

        // IMPORTANT: Only auto-switch between 'on' and 'd' (driving).
        // Never override manually set modes: off, sb, personal, yard.
        // If lastLog is empty (e.g. after screen refresh), initialize it from the
        // persisted mode so we don't accidentally force the mode to 'on'.
        if (lastLog.isEmpty()) {
            val savedMode = prefRepository.getMode()
            lastLog = if (savedMode.isNotEmpty()) savedMode else "on"
            Log.d(TAG, "📍 lastLog was empty, initialized from saved mode: $lastLog")
        }

        // Only auto-switch when in 'on' or 'd' mode — never touch off/sb/personal/yard
        val autoSwitchableModes = setOf("on", "d")
        if (lastLog !in autoSwitchableModes) {
            Log.d(TAG, "📍 Skipping auto mode switch — current mode '$lastLog' is manually set")
            return
        }

        if (speed > 0 && lastLog == "on") {
            Log.d(TAG, "🚗 Truck is running & in on mode — switching to DRIVE")
            lastLog = "d"
            activity?.runOnUiThread {
                updateUI(binding.btnDrive)
            }
            updateModeChange(hrs_MODE_D, "d", "")
        } else if (speed <= 0 && lastLog == "d") {
            Log.d(TAG, "🛑 Driving mode found & speed is zero — switching to ON")
            activity?.runOnUiThread {
                updateUI(binding.btnOn)
            }
            lastLog = "on"
            updateModeChange(hrs_MODE_ON, "on", "")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation", "DefaultLocale")
    private fun updateModeChange(hoursLast: Double, mode: String, selectedOptionText: String) {
        try {
            if (mode == TRUCK_MODE_DRIVING) {
                prefRepository.setShowUnidentifiedDialog(false)
            }
            Log.d(TAG, "updating mode --> $mode")
            selectedLog = mode
            prefRepository.setMode(mode)
            val vin_no = AppModel.getInstance().mVehicleInfo?.VIN ?: "1HGCM82633A004352"
            val te = AppModel.getInstance().mLastEvent
            val defaultLatitude = (activity as? Dashboard)?.glat ?: 0.0
            val defaultLongitude = (activity as? Dashboard)?.glong ?: 0.0

            val odoact = prefRepository.getDiffinOdo().toDoubleOrNull() ?: 0.0
            val engact = prefRepository.getDiffinEng().toDoubleOrNull() ?: 0.0
            val teOdometer = te?.mOdometer?.toDoubleOrNull() ?: 1.0
            val teEngineHours = te?.mEngineHours?.toDoubleOrNull() ?: 1.0

            var odometer_updated = teOdometer
            var enghour_updated = teEngineHours
            if (odoact != 0.0 && engact != 0.0) {
                odometer_updated = teOdometer - odoact
                enghour_updated = teEngineHours - engact
            }

            val miles = ((odometer_updated - getDouble(prefRepository.getDiffinOdo())) * 0.621371)
            val roundedMiles = String.format("%.2f", miles)
            val logRequest = AddLogRequest(
                modename = mode,
                odometerreading = roundedMiles,
                lat = te?.mGeoloc?.latitude ?: defaultLatitude,
                long = te?.mGeoloc?.longitude ?: defaultLongitude,
                location = true,
                eng_hours = (enghour_updated - getDouble(prefRepository.getDiffinEng())).toString(),
                vin = vin_no,
                is_active = 1,
                is_autoinsert = 1,
                eventcode = 1,
                eventtype = 1
            )

            if (selectedOptionText == "yard") {
                logRequest.discreption = "yard"
            } else if (selectedOptionText == "personal") {
                logRequest.discreption = "personal"
            }

            if (homeViewModel.getUserLogs().isNotEmpty()) {
                val toDayDate = DateFormat.format("dd-MM-yyy", Date()).toString()
                val lastLog = homeViewModel.getUserLogs().last()
                val updateLogRequest = updateLogRequest(
                    lastLog.id,
                    hoursLast,
                    toDayDate,
                    lastLog.modename,
                    lastLog.odometerreading,
                    lastLog.eng_hours,
                    lastLog.time,
                    0
                )
                context?.let { homeViewModel.updateLog(updateLogRequest, false, it) }
            }
            context?.let { homeViewModel.logUser(logRequest, it) }

        } catch (e: Exception) {
            Log.e(TAG, "Error in updateModeChange: ${e.message}", e)
            throw e
        }
    }

    override fun onStop() {
        super.onStop()
        // Reset flags to prevent stuck loader/UI when resuming
        isFetchingLogs = false
        _binding?.progressBar?.visibility = View.GONE
        unregisterLocalReceivers()
        clockJob?.cancel()
        locationJob?.cancel()
        bluetoothConnectionJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release MediaPlayer to prevent memory leak
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startClock() {
        // Cancel any existing clock job before starting a new one
        clockJob?.cancel()
        clockJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    try {
                        updateClockDisplay()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating clock: ${e.message}")
                    }
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        val currentTime = System.currentTimeMillis()
        val dateTime = DateTime(currentTime)
        val hour = dateTime.hourOfDay
        val minute = dateTime.minuteOfHour
        val second = dateTime.secondOfMinute
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%02d:%02d:%02d %s", hour12, minute, second, amPm)
    }

    private fun getCurrentDate(): String {
        val currentTime = System.currentTimeMillis()
        val dateTime = DateTime(currentTime)
        return dateTime.toString("yyyy-MM-dd")
    }

    private val timezoneMappings = mapOf(
        "PST" to "America/Los_Angeles",
        "AKST" to "America/Anchorage",
        "MST" to "America/Denver",
        "HST" to "Pacific/Honolulu",
        "CST" to "America/Chicago",
        "EST" to "America/New_York"
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateClockDisplay() {
        if (_binding == null) {
            return
        }
        if (timeZone.isEmpty()) {
            binding.liveClock.text = getCurrentTime()
            return
        }
        try {
            // Speed optimization: Only reconstruct ZoneId if timezone string changes
            if (timeZone != lastTimeZoneStr || cachedTimeZoneId == null) {
                val mappedTimezone = timezoneMappings[timeZone] ?: "America/Los_Angeles"
                cachedTimeZoneId = ZoneId.of(mappedTimezone)
                lastTimeZoneStr = timeZone
            }
            
            val currentDateTime = LocalDateTime.now()
            val systemZoneId = ZoneId.systemDefault()
            val companyZoneId = cachedTimeZoneId!!
            
            val zonedDateTime = currentDateTime.atZone(systemZoneId).withZoneSameInstant(companyZoneId)
            val companyTime = zonedDateTime.toLocalTime()
            val currentTimezoneTime = String.format("%02d:%02d:%02d", companyTime.hour, companyTime.minute, companyTime.second)
            binding.liveClock.text = currentTimezoneTime

            val lastLog = lastRelevantLog
            if (lastLog != null) {
                lastLogTime = lastLog.time ?: "00:00"
                lastLogMode = lastLog.modename ?: ""
                val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
                binding.timerTv.text = elapsedTime
                binding.timerLabelTv.text = "Time spent in ${lastLogMode.uppercase()}"
            } else {
                if (lastLogTime.isNotEmpty() && lastLogMode.isNotEmpty()) {
                    val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
                    binding.timerTv.text = elapsedTime
                    binding.timerLabelTv.text = "Time spent in ${lastLogMode.uppercase()}"
                } else {
                    binding.timerTv.text = currentTimezoneTime
                    binding.timerLabelTv.text = "Current $timeZone time"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating clock display: ${e.message}", e)
            binding.timerTv.text = getCurrentTime()
            binding.liveClock.text = getCurrentTime()
        }
    }

    private fun updateLastRelevantLog(logs: List<HomeDataModel.Log>?) {
        if (logs.isNullOrEmpty()) {
            lastRelevantLog = null
            return
        }
        for (index in logs.size - 1 downTo 0) {
            val mode = logs[index].modename?.lowercase() ?: continue
            if (relevantLogTypes.contains(mode)) {
                lastRelevantLog = logs[index]
                return
            }
        }
        lastRelevantLog = null
    }

    private fun calculateElapsedTime(currentTime: String, logTime: String): String {
        return try {
            val currentTimeParts = currentTime.split(":")
            val logTimeParts = logTime.split(":")
            if (currentTimeParts.size >= 2 && logTimeParts.size >= 2) {
                val currentHour = currentTimeParts[0].toInt()
                val currentMinute = currentTimeParts[1].toInt()
                val currentSecond = if (currentTimeParts.size > 2) currentTimeParts[2].toInt() else 0
                val logHour = logTimeParts[0].toInt()
                val logMinute = logTimeParts[1].toInt()
                val logSecond = if (logTimeParts.size > 2) logTimeParts[2].toInt() else 0
                val currentTotalSeconds = currentHour * 3600 + currentMinute * 60 + currentSecond
                val logTotalSeconds = logHour * 3600 + logMinute * 60 + logSecond
                var elapsedSeconds = currentTotalSeconds - logTotalSeconds
                if (elapsedSeconds < 0) {
                    elapsedSeconds += 24 * 3600
                }
                val elapsedHours = elapsedSeconds / 3600
                val elapsedMinutes = (elapsedSeconds % 3600) / 60
                val elapsedSecondsRemainder = elapsedSeconds % 60
                String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSecondsRemainder)
            } else {
                "00:00:00"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating elapsed time: ${e.message}", e)
            "00:00:00"
        }
    }

    private fun registerLocalReceivers() {
        if (receiversRegistered) return
        try {
            val instance = getInstance(requireContext())
            logVinState()
            instance.registerReceiver(tmRefresh, tmIf)
            instance.registerReceiver(svcRefresh, svcIf)
            receiversRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Receiver registration error: ${e.message}")
        }
    }

    private fun unregisterLocalReceivers() {
        if (!receiversRegistered) return
        try {
            val instance = getInstance(requireContext())
            instance.unregisterReceiver(tmRefresh)
            instance.unregisterReceiver(svcRefresh)
        } catch (e: Exception) {
            Log.e(TAG, "Receiver unregistration error: ${e.message}")
        } finally {
            receiversRegistered = false
        }
    }

    var isNeedToconnect = true

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSettingsPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.home_settings_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performLogout() {
        val logRequest = AddLogRequest(
            modename = "logout",
            odometerreading = 0.0.toString(),
            lat = 0.0,
            long = 0.0,
            location = true,
            eng_hours = 10.toString(),
            vin = "1111",
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1
        )
        homeViewModel.logUser(logRequest, requireContext())
        prefRepository.setLoggedIn(false)
        prefRepository.setDifferenceinOdo("0")
        prefRepository.setDifferenceinEnghours("0")
        prefRepository.setToken("")
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View) =
        if (mEngineHours.isNullOrEmpty() && isNeedToconnect && !isTesting) {
            Utils.dialog(
                requireContext(),
                message = "Please connect to a device first",
                negativeText = "Cancel",
                callback = object : dialogInterface {
                    override fun positiveClick() {
                        (activity as Dashboard).binding.appBarDashboard.fab.performClick()
                    }
                    override fun negativeClick() {}
                })
        } else {
            when (v.id) {
                R.id.btnDrive -> drivingMode()
                R.id.btnOff -> offMode()
                R.id.btnOn -> onMode()
                R.id.btnSleep -> sbMode()
                R.id.tvYarduse -> yardMode()
                R.id.tvPerosnaluse -> personalMode()
                else -> {}
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sbMode() {
        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_SLEEPING) {
            Utils.dialog(requireContext(), "Error", "Already in sleeping mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        if (MODE_SB != homeViewModel.trackingMode.get()!! || isEmptyList) {
            mediaPlayer.start()
            updateModeChange(hrs_MODE_SB, TRUCK_MODE_SLEEPING, "sb from click")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun yardMode() {
        Log.d(TAG, "Attempting to switch to YARD mode")
        Log.d(TAG, "Current Mode: ${homeViewModel.trackingMode.get()}")
        Log.d(TAG, "Last Log Mode: ${homeViewModel.getLastLogModeName()}")
        Log.d(TAG, "Is Clickable: ${isClickable()}")

        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_YARD) {
            Utils.dialog(requireContext(), "Error", "Already in yard move mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        // if (TRUCK_MODE_YARD != homeViewModel.trackingMode.get()!! || isEmptyList) {
            mediaPlayer.start()
            updateModeChange(hrs_MODE_YARD, TRUCK_MODE_YARD, "yard")
        // }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drivingMode() {
        Log.d(TAG, "🚗 Manual switch to DRIVING mode")
        updateUI(binding.btnDrive)
        mediaPlayer.start()
        updateModeChange(hrs_MODE_D, TRUCK_MODE_DRIVING, "d from click")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMode() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.menu_sb_menu)
        val optionViews = (1..12).map { i ->
            dialog.findViewById<TextView>(resources.getIdentifier("sb$i", "id", requireContext().packageName))
        }
        optionViews.forEach { optionView ->
            optionView?.setOnClickListener {
                val selectedOptionText = optionView.text.toString()
                if (!isClickable() && prefRepository.getMode() == TRUCK_MODE_DRIVING) {
                    Log.d(TAG, "Vehicle is running unable to click")
                    dialog.dismiss()
                    return@setOnClickListener
                }
                mediaPlayer.start()
                if (MODE_ON != homeViewModel.trackingMode.get()!! || isEmptyList) {
                    updateUI(binding.btnOn)
                    updateModeChange(hrs_MODE_ON, TRUCK_MODE_ON, selectedOptionText)
                }
                dialog.dismiss()
            }
        }
        val location = IntArray(2)
        binding.tvOn.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - (dialog.window?.attributes?.height ?: 0)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.TOP or Gravity.START)
        dialog.window?.attributes?.x = x
        dialog.window?.attributes?.y = y
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun offMode() {
        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_OFF) {
            Utils.dialog(requireContext(), "Error", "Already in off mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        val dialog = Dialog(requireContext())
        dialog.setContentView(menu_of_menu)
        val optionViews = (1..3).map { i ->
            dialog.findViewById<TextView>(resources.getIdentifier("option$i", "id", requireContext().packageName))
        }
        optionViews.forEach { optionView ->
            optionView?.setOnClickListener {
                val selectedOptionText = optionView.text.toString()
                updateModeChange(hrs_MODE_OFF, TRUCK_MODE_OFF, selectedOptionText)
                dialog.dismiss()
            }
        }
        val offButton = binding.tvOff
        val location = IntArray(2)
        offButton.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - (dialog.window?.attributes?.height ?: 0)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.TOP or Gravity.START)
        dialog.window?.attributes?.x = x
        dialog.window?.attributes?.y = y
        dialog.show()
        mediaPlayer.start()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun personalMode() {
        Log.d(TAG, "Attempting to switch to PERSONAL mode")
        
        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_PERSONAL) {
            Utils.dialog(requireContext(), "Error", "Already in personal conveyance mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        // if (TRUCK_MODE_PERSONAL != homeViewModel.trackingMode.get()!! || isEmptyList) {
            mediaPlayer.start()
            updateModeChange(hrs_MODE_PERSONAL, TRUCK_MODE_PERSONAL, "personal")
        // }
    }

    private fun isClickable(): Boolean {
        val speed = AppModel.getInstance().mLastEvent?.mGeoloc?.speed ?: 0
        return speed <= 0
    }

    private fun updateUI(viewSelect: View? = null) {
        binding.btnOff.background = getDrawable(resources, home_bg_deign, null)
        binding.btnSleep.background = getDrawable(resources, home_bg_deign, null)
        binding.btnDrive.background = getDrawable(resources, home_bg_deign, null)
        binding.btnOn.background = getDrawable(resources, home_bg_deign, null)
        binding.tvPerosnaluse.background = getDrawable(resources, home_bg_deign, null)
        binding.tvPerosnaluse.setTextColor(getColor(resources, R.color.white, null))
        binding.tvYarduse.background = getDrawable(resources, home_bg_deign, null)
        binding.tvYarduse.setTextColor(getColor(resources, R.color.white, null))
        viewSelect?.let {
            it.background = getDrawable(resources, home_bg_design_selected, null)
        }
    }

    private fun showValidationErrors(error: String, dialogInterface: dialogInterface? = null) {
        Utils.dialog(requireContext(), message = error, callback = dialogInterface)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTelemetryInfo() {
        try {
            val dashboard = activity as? Dashboard ?: return
            val appModel = AppModel.getInstance()
            val lastEvent = appModel.mLastEvent
            if (lastEvent != null) {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_disconnect)
                binding.tvConected.text = "Connected"
                isNeedToconnect = false
                
                // Start continuous speed monitoring if not already running
                startSpeedMonitoring()
            } else {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_bluetooth)
                binding.tvConected.text = "Not Connected"
                isNeedToconnect = true
                prefRepository.setShowUnidentifiedDialog(true)
                
                // Stop speed monitoring when disconnected
                stopSpeedMonitoring()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateTelemetryInfo: ${e.message}", e)
        }
    }
    
    // Speed monitoring job for continuous auto-mode switching
    private var speedMonitoringJob: Job? = null
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startSpeedMonitoring() {
        // Don't start if already running
        if (speedMonitoringJob?.isActive == true) {
            return
        }
        
        speedMonitoringJob = viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "🚗 Speed monitoring started")
            while (isActive) {
                try {
                    withContext(Dispatchers.Main) {
                        checkAndPrintSpeed()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in speed monitoring: ${e.message}")
                }
                delay(SPEED_CHECK_THROTTLE_MS) // Check every 2 seconds
            }
        }
    }
    
    private fun stopSpeedMonitoring() {
        speedMonitoringJob?.cancel()
        speedMonitoringJob = null
        Log.d(TAG, "🚗 Speed monitoring stopped")
    }

    fun updateVehicleInfo() {
        if (AppModel.getInstance().mVehicleInfo != null) {
            val vehicleInfo = AppModel.getInstance().mVehicleInfo
            val vin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: "NOT_AVAILABLE"
            binding.vinNumber.text = vin
            Log.d("VIN_DEBUG", "Vehicle info update - VIN: $vin")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndPrintSpeed() {
        try {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSpeedCheckTime < SPEED_CHECK_THROTTLE_MS) {
                return
            }
            lastSpeedCheckTime = currentTime
            val appModel = AppModel.getInstance()
            val lastEvent = appModel.mLastEvent ?: return
            val speed = lastEvent.mGeoloc.speed
            val dashboard = appModel.dashboard
            val rpm = dashboard?.engineRPM ?: 0
            binding.drivingSpeed.text = "Driving Speed: ${speed} km/h"
            // Use actual Bluetooth speed for mode switching (not dashboardSpeed)
            handleLocationUpdate(speed, rpm, lastEvent.mEvent.name)
            handleEngineStateUpdate(rpm)
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndPrintSpeed: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleEngineStateUpdate(rpm: Int) {
        val currentState = if (rpm > 0) "eng_on" else "eng_off"

        // Don't track engine state until we know the last state from the API
        // Otherwise the very first RPM reading creates a bogus log
        if (lastPushedEngineState.isEmpty()) {
            Log.d(TAG, "Engine state: skipping — lastPushedEngineState not initialized yet")
            return
        }

        if (currentState == lastPushedEngineState) {
            // State hasn't changed — reset the stability counter
            engineStateStableCount = 0
            return
        }

        // State is different from last pushed — count repeated confirmations
        engineStateStableCount++
        if (engineStateStableCount < ENGINE_STATE_STABLE_THRESHOLD) {
            return
        }

        // Time debounce check — avoid flooding the API
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEngineApiCallTime < ENGINE_API_DEBOUNCE_MS) {
            Log.d(TAG, "Engine state: debounce active, skipping API call")
            return
        }

        Log.d(TAG, "Engine state stable at $currentState. Pushing to API.")
        lastEngineApiCallTime = currentTime
        lastPushedEngineState = currentState
        engineStateStableCount = 0  // Reset so we don't fire again until a NEW transition

        val vin_no = AppModel.getInstance().mVehicleInfo?.VIN ?: "1HGCM82633A004352"
        val te = AppModel.getInstance().mLastEvent
        val defaultLatitude = (activity as? Dashboard)?.glat ?: 0.0
        val defaultLongitude = (activity as? Dashboard)?.glong ?: 0.0

        val logRequest = AddLogRequest(
            modename = currentState,
            odometerreading = te?.mOdometer?.takeIf { it.isNotBlank() } ?: "0.0",
            lat = te?.mGeoloc?.latitude ?: defaultLatitude,
            long = te?.mGeoloc?.longitude ?: defaultLongitude,
            location = true,
            eng_hours = te?.mEngineHours?.takeIf { it.isNotBlank() } ?: "0.0",
            vin = vin_no,
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1
        )
        context?.let { homeViewModel.logUser(logRequest, it) }
    }

    private fun updateUI() {
        val numberText = prefRepository.getShippingNumber()
        binding.shippingNumber.text = getString(shipping_number).plus(numberText)
        val trailerText = prefRepository.getTrailerNumber()
        binding.trailerNumber.text = getString(trailer_number).plus(trailerText)
    }

    private fun editShippingNumberPopup() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val dialogView: View = layoutInflater.inflate(edit_shiping_number, null)
        val dialog = builder.create()
        dialog.setView(dialogView)
        val shippingNumber: EditText = dialogView.findViewById(R.id.texture_shipping_number)
        shippingNumber.setText(prefRepository.getShippingNumber())
        dialogView.findViewById<Button>(R.id.cancel_shipping).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.update_shipping).setOnClickListener {
            val number = shippingNumber.text.toString()
            if (number.isEmpty()) {
                makeText(activity, "Shipping number required", LENGTH_LONG).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            prefRepository.setShippingNumber(number)
            updateUI()
        }
        dialog.show()
    }

    private fun editTrailerNumberPopup() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val dialogView: View = layoutInflater.inflate(edit_trailer_number, null)
        val trailerNumber: EditText = dialogView.findViewById(R.id.trailer_number)
        trailerNumber.setText(prefRepository.getTrailerNumber())
        val dialog = builder.create()
        dialogView.findViewById<Button>(R.id.action_cancel_trailer).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.action_update_trailer).setOnClickListener {
            val number = trailerNumber.text.toString()
            if (number.isEmpty()) {
                makeText(activity, "Trailer number required", LENGTH_LONG).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            prefRepository.setTrailerNumber(number)
            updateUI()
        }
        dialog.setView(dialogView)
        dialog.show()
    }

    private fun editCoDriverPopup() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val dialogView: View = layoutInflater.inflate(edit_codriver, null)
        val dialog = builder.create()
        dialog.setView(dialogView)
        val items = arrayOf("No Co-Driver")
        val coDriverSpinner: AppCompatSpinner = dialogView.findViewById(R.id.spinner_co_driver)
        val adapter = ArrayAdapter(requireActivity().applicationContext, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        coDriverSpinner.adapter = adapter
        coDriverSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle selection if needed
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        dialogView.findViewById<Button>(R.id.cancel_codriver).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.action_update_codriver).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateViolationTimeCard(conditions: HomeDataModel.Conditions?) {
        conditions?.let { cond ->
            val closestViolation = findClosestViolation(cond)
            if (closestViolation != null) {
                binding.closestViolationCard.visibility = View.VISIBLE
                binding.closestViolationType.text = closestViolation.type
                binding.closestViolationTime.text = closestViolation.remainingTime
                binding.closestViolationProgress.progress = closestViolation.progress
                binding.closestViolationProgress.setIndicatorColor(getViolationColor(closestViolation.progress, closestViolation.isViolation))
                // remainingMinutes is actually remaining seconds from API
                val totalSeconds = closestViolation.remainingMinutes
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val timeText = when {
                    hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                    hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
                    minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
                    else -> "imminent"
                }
                binding.closestViolationMessage.text = "${closestViolation.type} violation due in $timeText"
            } else {
                binding.closestViolationCard.visibility = View.GONE
            }
        } ?: run {
            binding.closestViolationCard.visibility = View.GONE
        }
    }

    private data class ViolationInfo(
        val type: String,
        val remainingMinutes: Int,
        val remainingTime: String,
        val progress: Int,
        val isViolation: Boolean
    )

    private fun findClosestViolation(conditions: HomeDataModel.Conditions): ViolationInfo? {
        val violations = mutableListOf<ViolationInfo>()
        val driveRemaining = conditions.drive ?: 0
        if (driveRemaining > 0) {
            violations.add(ViolationInfo("Drive Time", driveRemaining, Utils.formatTimeFromSeconds(driveRemaining), calculateProgressFromRemainingSec(driveRemaining, 11 * 3600), conditions.driveViolation ?: false))
        }
        val driveBreakRemaining = conditions.drivebreak ?: 0
        if (driveBreakRemaining > 0) {
            violations.add(ViolationInfo("Drive Break", driveBreakRemaining, Utils.formatTimeFromSeconds(driveBreakRemaining), calculateProgressFromRemainingSec(driveBreakRemaining, 8 * 3600), conditions.driveBreakViolation ?: false))
        }
        val shiftRemaining = conditions.shift ?: 0
        if (shiftRemaining > 0) {
            violations.add(ViolationInfo("Shift Time", shiftRemaining, Utils.formatTimeFromSeconds(shiftRemaining), calculateProgressFromRemainingSec(shiftRemaining, 14 * 3600), conditions.shiftViolation ?: false))
        }
        val cycleRemaining = conditions.cycle ?: 0
        if (cycleRemaining > 0) {
            violations.add(ViolationInfo("Cycle Time", cycleRemaining, Utils.formatTimeFromSeconds(cycleRemaining), calculateProgressFromRemainingSec(cycleRemaining, 70 * 3600), conditions.cycleViolation ?: false))
        }
        return violations.minByOrNull { it.remainingMinutes }
    }

    private fun calculateProgressFromRemainingSec(remainingSeconds: Int, totalSeconds: Int): Int {
        return if (remainingSeconds <= 0) 100 else {
            val spent = totalSeconds - remainingSeconds
            val progress = (spent.toFloat() / totalSeconds * 100).toInt()
            progress.coerceIn(0, 100)
        }
    }

    private fun getViolationColor(progress: Int, isViolation: Boolean): Int {
        return if (isViolation) {
            ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
        } else {
            when (progress) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_green)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_orange)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
                100 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
                else -> ContextCompat.getColor(requireContext(), R.color.dark_progress_green)
            }
        }
    }
}
