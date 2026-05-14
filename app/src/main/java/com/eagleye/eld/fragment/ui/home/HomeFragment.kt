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
import android.os.SystemClock
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
import com.eagleye.eld.pt.devicemanager.BleProfileService
import com.eagleye.eld.pt.devicemanager.TrackerManagerActivity
import com.eagleye.eld.pt.devicemanager.TrackerService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import com.eagleye.eld.models.CodriverItem
import com.eagleye.eld.request.AddOffsetRequest
import com.eagleye.eld.models.RejectUnidentifiedRequest
import com.eagleye.eld.request.DriverShipmentRequest
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_YARD
import com.eagleye.eld.utils.Utils.getDouble
import com.eagleye.eld.utils.Utils.toHoursMinutesFormate
import com.whizpool.supportsystem.SLog
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import org.json.JSONObject

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
    private var warningAnimation: android.view.animation.Animation? = null

    private var clockJob: Job? = null
    private var locationJob: Job? = null
    private var conditionsRefreshJob: Job? = null
    private val CONDITIONS_REFRESH_INTERVAL_MS = 120_000L // 2 minutes – conditions & remaining times
    private var timeZone: String = ""
    private var lastLogTime: String = ""
    private var lastLogMode: String = ""
    private var lastRelevantLog: HomeDataModel.Log? = null
    private var latestConditionsSnapshot: HomeDataModel.Conditions? = null
    private var latestConditionsSnapshotAtMs: Long = SystemClock.elapsedRealtime()
    private val relevantLogTypes = setOf("d", "off", "sb", "on", "yard", "personal")
    private var lastSpeedCheckTime: Long = 0
    private val SPEED_CHECK_THROTTLE_MS = 2000L // 2 seconds throttle
    private var latestEldAttention: com.eagleye.eld.models.EldAttentionSummary? = null
    private var pendingModeSelection: String? = null
    private var pendingModeSelectionAtMs: Long = 0L
    private val MODE_SELECTION_GRACE_MS = 20_000L

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
    private val DRIVE_DETECTION_THRESHOLD_KMH = 8 // ~5 mph threshold for auto-driving
    private val STOPPED_DURATION_BEFORE_ON_MS = 5 * 60 * 1000L // 5 min stopped before Drive→ON
    private var stoppedSinceMs: Long? = null
    private val DRIVE_LIMIT_SECONDS = 11 * 3600
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
    val connectionStateIf = IntentFilter(BleProfileService.BROADCAST_CONNECTION_STATE)
    val disconnectedMilesIf = IntentFilter(TrackerService.ACTION_DISCONNECTED_DRIVING_MILES_READY)
    val vinRefreshIf = IntentFilter("TRACKER-VIN-REFRESH")

    // Simple last log tracking like EagleEye
    // NOTE: Initialized from persisted mode so screen refresh doesn't reset it to empty
    private var lastLog: String = ""

    private var vinCheckedThisConnection = false
    var tmRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "tmRefresh broadcast")
            if (!isBluetoothConnecting) {
                updateTelemetryInfo()
                // Check VIN once per connection after PT-40 has synced vehicle info
                if (!vinCheckedThisConnection) {
                    val vin = AppModel.getInstance().mVehicleInfo?.VIN?.takeIf { it.isNotEmpty() }
                    if (vin != null) {
                        vinCheckedThisConnection = true
                        updateVehicleInfo()
                    }
                }
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

    var connectionStateRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val connectionState = intent.getIntExtra(
                BleProfileService.EXTRA_CONNECTION_STATE,
                BleProfileService.STATE_DISCONNECTED
            )
            Log.d(TAG, "Bluetooth connection state changed: $connectionState")

            if (connectionState == BleProfileService.STATE_DISCONNECTED ||
                connectionState == BleProfileService.STATE_LINK_LOSS
            ) {
                prefRepository.setEldConnected(false)
                vinCheckedThisConnection = false // reset so next connect re-checks VIN
                stopSpeedMonitoring()
                (activity as? Dashboard)?.showEldReconnectDialog()
            } else if (connectionState == BleProfileService.STATE_CONNECTED) {
                prefRepository.setEldConnected(true)
                (activity as? Dashboard)?.dismissEldReconnectDialog()
            }

            if (!isBluetoothConnecting) {
                updateTelemetryInfo()
            }
        }
    }
    var disconnectedDrivingMilesRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getBooleanExtra(TrackerService.EXTRA_DISCONNECTED_DRIVING_AUTO_SUBMITTED, false)) {
                prefRepository.clearPendingDisconnectedDrivingMilesDialog()
                prefRepository.clearPendingDisconnectedDrivingSegmentsJson()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1500)
                    if (_binding != null) {
                        runCatching { homeViewModel.getHome(requireContext()) }
                    }
                }
                return
            }
            maybeShowDisconnectedDrivingMilesDialog(
                intent.getStringExtra(TrackerService.EXTRA_DISCONNECTED_DRIVING_MILES)
            )
        }
    }
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var progressBarShift: CircularProgressIndicator
    private lateinit var cycleRemaining: CircularProgressIndicator
    private lateinit var untilBreak: CircularProgressIndicator
    private var selectedLog: String = TRUCK_MODE_OFF
    private var observersSet = false
    private var codrivers: List<CodriverItem> = emptyList()
    private var disconnectedDrivingMilesDialog: androidx.appcompat.app.AlertDialog? = null

    private data class PendingDisconnectedDrivingSegment(
        val start: PendingDisconnectedDrivingPoint? = null,
        val end: PendingDisconnectedDrivingPoint? = null
    )

    private data class PendingDisconnectedDrivingPoint(
        val date: String? = "",
        val time: String? = "",
        val datetime: String? = "",
        val odometerKm: String? = "",
        val engineHours: String? = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val hasLocation: Boolean = false
    )

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
        context?.let {
            mediaPlayer = MediaPlayer.create(it, R.raw.clicksoud)
        }
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mActivity = activity
        yoYo = YoYo.with(Techniques.Tada)
        prefRepository = PrefRepository(mActivity!!)
        binding.btnOff.setOnClickListener(this)
        binding.btnSleep.setOnClickListener(this)
        binding.btnDrive.setOnClickListener(this)
        binding.btnOn.setOnClickListener(this)
        binding.btnPersonal.setOnClickListener(this)
        binding.btnYard.setOnClickListener(this)

        binding.settingsIcon.setOnClickListener {
            playClickAnimation(it)
            val dashboard = activity as? Dashboard
            dashboard?.binding?.drawerLayout?.open()
        }

        binding.cvGaugeDrive.setOnClickListener { playClickAnimation(it) }
        binding.cvGaugeShift.setOnClickListener { playClickAnimation(it) }
        binding.cvGaugeCycle.setOnClickListener { playClickAnimation(it) }
        binding.cvGaugeBreak.setOnClickListener { playClickAnimation(it) }
        binding.cvProfile.setOnClickListener { playClickAnimation(it) }

        binding.llBluetoothStatusPill.setOnClickListener {
            playClickAnimation(it)
            if (isNeedToconnect) {
                startActivity(Intent(requireContext(), TrackerManagerActivity::class.java))
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bluetooth Disconnection")
                    .setMessage("Are you sure you want to disconnect the Bluetooth device?")
                    .setPositiveButton("Disconnect") { _, _ ->
                        val disconnectIntent = Intent(TrackerService.ACTION_DISCONNECT).apply {
                            setPackage(requireContext().packageName)
                            putExtra(
                                TrackerService.EXTRA_SOURCE,
                                TrackerService.SOURCE_NOTIFICATION
                            )
                        }
                        requireContext().sendBroadcast(disconnectIntent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val defaultVin = "---------"
        val vehicleInfo = AppModel.getInstance().mVehicleInfo
        val vin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: defaultVin
        // binding.vinNumber.text = vin // Removed as we replaced this card
        Log.d("VIN_DEBUG", "Initial VIN set to: $vin")
        context?.let { ctx ->
            homeViewModel.getOffSet(prefRepository, vin, ctx)
            homeViewModel.getCompanyName(ctx)
        }
        binding.name.text = prefRepository.getName()

        // binding.drivingSpeed.text = "Driving Speed: 0 km/h" // Removed
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
            activity?.runOnUiThread {
                context?.let { ctx ->
                    homeViewModel.getHome(ctx)
                }
            }
        }

        binding.editShipping.setOnClickListener { playClickAnimation(it); showShipmentDialog() }
        binding.editCoDriver.setOnClickListener { playClickAnimation(it); showShipmentDialog() }
        binding.editTrailerNo.setOnClickListener { playClickAnimation(it); showShipmentDialog() }
        binding.cvProfile.setOnClickListener { playClickAnimation(it); showDriverProfileDialog() }
        binding.btnUpdateShipment.setOnClickListener { playClickAnimation(it); showShipmentDialog() }

        binding.tvMalfunction.setOnClickListener { showEldAttentionDialog(showMalfunction = true) }
        binding.tvDiagnostic.setOnClickListener { showEldAttentionDialog(showMalfunction = false) }
        
        // Always set observers on view creation
        setupObservers()
        
        // Load animations once
        warningAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_warning)
        
        // Setup SwipeRefreshLayout for pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe refresh triggered")
            isFetchingLogs = true
            context?.let { ctx ->
                homeViewModel.getHome(ctx)
                homeViewModel.getCompanyName(ctx)
            }
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.green,
            R.color.orange,
            R.color.blue
        )
        
        // Restore the last known duty mode so disconnected sessions don't jump back to OFF.
        updateDrivingButtonAvailability()
        restoreSelectedModeFromState()
        
        updateShipmentInfoUI()
        loadShipmentContext()
        setupCodriverPanel()
        startEntranceAnimations()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        homeViewModel.company.observe(viewLifecycleOwner) {
            if (it.data?.results == null) return@observe
            val rawTz = it.data.results.company_timezone ?: ""
            timeZone = try {
                java.util.TimeZone.getTimeZone(rawTz)
                    .getDisplayName(java.util.TimeZone.getTimeZone(rawTz)
                        .inDaylightTime(java.util.Date()), java.util.TimeZone.SHORT)
            } catch (e: Exception) { rawTz }
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
                        if (!shouldPreserveManualSelection() &&
                            (currentMode == null || currentMode == MODE_OFF)
                        ) {
                            homeViewModel.trackingMode.set(MODE_OFF)
                            updateUI(binding.btnOff)
                        }
                        return@observe
                    }
                    updateLastRelevantLog(it.data.logs)
                    cacheConditionsSnapshot(it.data.conditions)
                    // Update UI components based on the fetched data
                    updateGauges(it.data)
                    updateViolationTimeCard(it.data.conditions)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        updateClockDisplay()
                    }
                    updateUIBasedOnLogs(it.data)

                    // Update M and D indicators using server-computed eldAttention (tracks active vs cleared)
                    val att = it.data.eldAttention
                    latestEldAttention = att
                    val hasMalfunction = att?.hasMalfunction == true
                    val hasDiagnostic = att?.hasDiagnostic == true

                    val ghostBg = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_indicator_ghost)
                    val redBg   = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_indicator_red)
                    val orangeBg = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_indicator_orange)

                    if (hasMalfunction) {
                        binding.tvMalfunction.background = redBg
                        binding.tvMalfunction.setTextColor(android.graphics.Color.WHITE)
                        YoYo.with(Techniques.Flash).duration(1500).repeat(YoYo.INFINITE).playOn(binding.tvMalfunction)
                    } else {
                        binding.tvMalfunction.background = ghostBg
                        binding.tvMalfunction.setTextColor(android.graphics.Color.parseColor("#55000000"))
                        binding.tvMalfunction.clearAnimation()
                    }

                    if (hasDiagnostic) {
                        binding.tvDiagnostic.background = orangeBg
                        binding.tvDiagnostic.setTextColor(android.graphics.Color.WHITE)
                        YoYo.with(Techniques.Flash).duration(1500).repeat(YoYo.INFINITE).playOn(binding.tvDiagnostic)
                    } else {
                        binding.tvDiagnostic.background = ghostBg
                        binding.tvDiagnostic.setTextColor(android.graphics.Color.parseColor("#55000000"))
                        binding.tvDiagnostic.clearAnimation()
                    }

                    Log.d("M_D_DEBUG", "Malfunction: $hasMalfunction codes=${att?.malfunctionCodesActive}, Diagnostic: $hasDiagnostic codes=${att?.diagnosticCodesActive}")
                }
                is NetworkResult.Error<*> -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    isFetchingLogs = false
                    // Only set OFF if user is not already on an active mode
                    val currentMode = homeViewModel.trackingMode.get()
                    if (!shouldPreserveManualSelection() &&
                        (currentMode == null || currentMode == MODE_OFF)
                    ) {
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
                    context?.let { homeViewModel.getHome(it) }
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
                    Log.d(TAG, "⏳ addLogRepose LOADING")
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateGauges(data: HomeDataModel?) {
        if (data?.conditions == null || _binding == null) return

        // Conditions from backend are in seconds; match iOS gauges with compact HH:MM.
        val formatCondition: (Int) -> String = { Utils.formatTimeFromSeconds(it) }
        val driveTotalSec = 11 * 3600
        val cycleTotalSec = 70 * 3600
        val shiftTotalSec = 14 * 3600
        val breakTotalSec = 8 * 3600

        if (data.conditions!!.driveViolation!!) {
            binding.timeText1.text = formatCondition(data.conditions?.drive ?: 0)
            binding.timeText1.setTextColor(Color.RED)
            binding.progressBarMain.startAnimation(warningAnimation)
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
            binding.progressBarCycle.startAnimation(warningAnimation)
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
            binding.progressBarShift.startAnimation(warningAnimation)
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
            binding.timeText4.text = "Violation"
            binding.timeText4.setTextColor(Color.RED)
            binding.progressBarBreak.startAnimation(warningAnimation)
            binding.progressBarBreak.setIndicatorColor(Color.RED)
            binding.progressBarBreak.progress = 0
        } else {
            val remaining = (data.conditions?.drivebreak ?: 0)
            val safeSpent = (breakTotalSec - remaining).coerceIn(0, breakTotalSec)
            val progressPercent = (safeSpent.toFloat() / breakTotalSec * 100).toInt()
            binding.progressBarBreak.clearAnimation()
            binding.timeText4.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.progressBarBreak.max = 100
            binding.progressBarBreak.progress = progressPercent
            val color = when (progressPercent) {
                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                else -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
            }
            binding.progressBarBreak.setIndicatorColor(color)
            binding.timeText4.text = formatCondition(data.conditions?.drivebreak ?: 0)
        }
    }

    private fun gaugeColor(remainingPercent: Int): Int = when {
        remainingPercent > 50 -> android.graphics.Color.parseColor("#4CAF50") // green
        remainingPercent > 25 -> android.graphics.Color.parseColor("#FF9800") // orange
        else                  -> android.graphics.Color.parseColor("#F44336") // red
    }

    private fun isSpecialManualMode(mode: String): Boolean {
        return mode == TRUCK_MODE_PERSONAL || mode == TRUCK_MODE_YARD
    }

    private fun shouldPreserveManualSelection(): Boolean {
        return isSpecialManualMode(selectedLog) ||
            isSpecialManualMode(lastLog) ||
            isSpecialManualMode(prefRepository.getMode())
    }

    private fun updateTrackingModeForSelection(mode: String) {
        when (mode) {
            TRUCK_MODE_OFF,
            TRUCK_MODE_PERSONAL -> homeViewModel.trackingMode.set(MODE_OFF)
            TRUCK_MODE_ON,
            TRUCK_MODE_YARD -> homeViewModel.trackingMode.set(MODE_ON)
            TRUCK_MODE_SLEEPING -> homeViewModel.trackingMode.set(MODE_SB)
            TRUCK_MODE_DRIVING -> homeViewModel.trackingMode.set(MODE_D)
        }
    }

    private fun resolveActualMode(modeName: String?, description: Any? = null): String {
        val normalizedDescription = description?.toString()?.trim()?.lowercase(Locale.US).orEmpty()
        val normalizedMode = modeName?.trim()?.lowercase(Locale.US).orEmpty()

        return when {
            normalizedDescription == TRUCK_MODE_PERSONAL || normalizedMode == TRUCK_MODE_PERSONAL -> TRUCK_MODE_PERSONAL
            normalizedDescription == TRUCK_MODE_YARD || normalizedMode == TRUCK_MODE_YARD -> TRUCK_MODE_YARD
            normalizedMode == "drive" || normalizedMode == "driving" -> TRUCK_MODE_DRIVING
            else -> normalizedMode
        }
    }

    private fun resolveActualMode(log: HomeDataModel.Log?): String {
        if (log == null) return ""
        return resolveActualMode(log.modename, log.discreption)
    }

    private fun formatModeLabel(mode: String): String {
        return when (resolveActualMode(mode)) {
            TRUCK_MODE_OFF -> "OFF"
            TRUCK_MODE_SLEEPING -> "SB"
            TRUCK_MODE_DRIVING -> "DR"
            TRUCK_MODE_ON -> "ON"
            TRUCK_MODE_YARD -> "YM"
            TRUCK_MODE_PERSONAL -> "PC"
            else -> mode.trim().uppercase(Locale.US)
        }
    }

    private data class LogLocationPayload(
        val latitude: Double,
        val longitude: Double,
        val shouldSendLocation: Boolean
    )

    private fun hasValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        return !(latitude == 0.0 && longitude == 0.0)
    }

    private fun resolveTrackerLocationPayload(): LogLocationPayload {
        val lastTelemetryEvent = AppModel.getInstance().mLastEvent
        val trackerLatitude = lastTelemetryEvent?.mGeoloc?.latitude?.toDouble()
        val trackerLongitude = lastTelemetryEvent?.mGeoloc?.longitude?.toDouble()

        if (hasValidCoordinates(trackerLatitude, trackerLongitude)) {
            return LogLocationPayload(
                latitude = trackerLatitude!!,
                longitude = trackerLongitude!!,
                shouldSendLocation = true
            )
        }

        return LogLocationPayload(
            latitude = 0.0,
            longitude = 0.0,
            shouldSendLocation = false
        )
    }

    private fun resolvePhoneLocationPayload(phoneLocation: Pair<Double, Double>?): LogLocationPayload {
        if (phoneLocation != null && hasValidCoordinates(phoneLocation.first, phoneLocation.second)) {
            return LogLocationPayload(
                latitude = phoneLocation.first,
                longitude = phoneLocation.second,
                shouldSendLocation = true
            )
        }

        return LogLocationPayload(
            latitude = 0.0,
            longitude = 0.0,
            shouldSendLocation = false
        )
    }

    private fun getButtonForMode(mode: String): com.google.android.material.card.MaterialCardView? {
        return when (mode) {
            TRUCK_MODE_OFF -> binding.btnOff
            TRUCK_MODE_ON -> binding.btnOn
            TRUCK_MODE_SLEEPING -> binding.btnSleep
            TRUCK_MODE_DRIVING -> binding.btnDrive
            TRUCK_MODE_YARD -> binding.btnYard
            TRUCK_MODE_PERSONAL -> binding.btnPersonal
            else -> null
        }
    }

    private fun isDrivingModeAllowed(): Boolean {
        return !isNeedToconnect && AppModel.getInstance().mLastEvent != null
    }

    private fun sanitizeModeForCurrentConnection(mode: String): String {
        val normalizedMode = resolveActualMode(mode)
        return if (!isDrivingModeAllowed() && normalizedMode == TRUCK_MODE_DRIVING) {
            TRUCK_MODE_ON
        } else {
            normalizedMode
        }
    }

    private fun updateDrivingButtonAvailability() {
        if (_binding == null) return

        val isEnabled = isDrivingModeAllowed()
        binding.btnDrive.isEnabled = isEnabled
        binding.btnDrive.isClickable = isEnabled
        binding.btnDrive.isFocusable = isEnabled
        binding.btnDrive.alpha = if (isEnabled) 1f else 0.45f
        binding.tvDrive.alpha = if (isEnabled) 1f else 0.6f
    }

    private fun restoreSelectedModeFromState() {
        val persistedMode = prefRepository.getMode().trim().lowercase(Locale.US)
        val modeToRestore = if (persistedMode in relevantLogTypes) {
            persistedMode
        } else {
            TRUCK_MODE_OFF
        }
        applySelectedMode(sanitizeModeForCurrentConnection(modeToRestore))
    }

    private fun applySelectedMode(mode: String) {
        val sanitizedMode = sanitizeModeForCurrentConnection(mode)
        selectedLog = sanitizedMode
        lastLog = sanitizedMode
        prefRepository.setMode(sanitizedMode)
        updateTrackingModeForSelection(sanitizedMode)
        updateDrivingButtonAvailability()
        updateUI(getButtonForMode(sanitizedMode))
    }

    private fun markPendingModeSelection(mode: String) {
        pendingModeSelection = mode.trim().lowercase(Locale.US)
        pendingModeSelectionAtMs = SystemClock.elapsedRealtime()
    }

    private fun clearPendingModeSelection() {
        pendingModeSelection = null
        pendingModeSelectionAtMs = 0L
    }

    private fun shouldHoldPendingModeSelection(latestMode: String): Boolean {
        val pendingMode = pendingModeSelection ?: return false
        val isWithinGraceWindow =
            SystemClock.elapsedRealtime() - pendingModeSelectionAtMs <= MODE_SELECTION_GRACE_MS

        if (!isWithinGraceWindow) {
            clearPendingModeSelection()
            return false
        }

        if (latestMode == pendingMode) {
            clearPendingModeSelection()
            return false
        }

        Log.d(
            TAG,
            "Holding local mode '$pendingMode' because server still reports '$latestMode'"
        )
        return true
    }

    private fun rememberTimerStartForModeChange(mode: String) {
        val normalizedMode = resolveActualMode(mode)
        if (normalizedMode.isEmpty()) return

        val currentMode = getCurrentModeForLiveConditions()
        if (normalizedMode != currentMode) {
            prefRepository.setTimerStartTime(System.currentTimeMillis())
        }
    }

    private fun cacheConditionsSnapshot(conditions: HomeDataModel.Conditions?) {
        latestConditionsSnapshot = conditions?.copy()
        latestConditionsSnapshotAtMs = SystemClock.elapsedRealtime()
    }

    private fun getCurrentModeForLiveConditions(): String {
        val selectedMode = selectedLog.trim().lowercase(Locale.US)
        if (selectedMode.isNotEmpty()) {
            return selectedMode
        }

        val relevantMode = resolveActualMode(lastRelevantLog)
        if (relevantMode.isNotEmpty()) {
            return relevantMode
        }

        val persistedMode = prefRepository.getMode().trim().lowercase(Locale.US)
        return if (persistedMode.isNotEmpty()) persistedMode else TRUCK_MODE_OFF
    }

    private fun getLiveConditions(): HomeDataModel.Conditions? {
        val snapshot = latestConditionsSnapshot ?: return null
        val elapsedSeconds =
            ((SystemClock.elapsedRealtime() - latestConditionsSnapshotAtMs) / 1000L).toInt()

        if (elapsedSeconds <= 0) {
            return snapshot.copy()
        }

        val currentMode = getCurrentModeForLiveConditions()
        val isDriving = currentMode == TRUCK_MODE_DRIVING
        val isOnDuty = currentMode == TRUCK_MODE_ON || currentMode == TRUCK_MODE_YARD
        val snapshotDrive = snapshot.drive ?: 0
        val snapshotDriveBreak = snapshot.drivebreak ?: 0
        val snapshotShift = snapshot.shift ?: 0
        val snapshotCycle = snapshot.cycle ?: 0
        val liveDrive = if (isDriving) (snapshotDrive - elapsedSeconds).coerceAtLeast(0) else snapshotDrive
        val liveDriveBreak = if (isDriving) {
            (snapshotDriveBreak - elapsedSeconds).coerceAtLeast(0)
        } else {
            snapshotDriveBreak
        }
        val liveShift = if (isDriving || isOnDuty) {
            (snapshotShift - elapsedSeconds).coerceAtLeast(0)
        } else {
            snapshotShift
        }
        val liveCycle = if (isDriving || isOnDuty) {
            (snapshotCycle - elapsedSeconds).coerceAtLeast(0)
        } else {
            snapshotCycle
        }

        return snapshot.copy(
            drive = liveDrive,
            shift = liveShift,
            cycle = liveCycle,
            drivebreak = liveDriveBreak,
            driveViolation = snapshot.driveViolation == true ||
                (isDriving && snapshotDrive > 0 && liveDrive <= 0),
            shiftViolation = snapshot.shiftViolation == true ||
                ((isDriving || isOnDuty) && snapshotShift > 0 && liveShift <= 0),
            cycleViolation = snapshot.cycleViolation == true ||
                ((isDriving || isOnDuty) && snapshotCycle > 0 && liveCycle <= 0),
            driveBreakViolation = snapshot.driveBreakViolation == true ||
                (isDriving && snapshotDriveBreak > 0 && liveDriveBreak <= 0)
        )
    }

    private fun updateLiveConditionsDisplay() {
        val liveConditions = getLiveConditions() ?: return
        updateGauges(HomeDataModel(conditions = liveConditions))
        updateViolationTimeCard(liveConditions)
        updateDrivingOverlayForCurrentMode()
    }

    private fun updateUIAfterModeChange(mode: String) {
        if (mode.isNotEmpty()) {
            applySelectedMode(mode)
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
            val actualMode = resolveActualMode(it.modename, it.discreption)
            logList?.add(
                ELDGraphData(
                    0.0.toFloat(),
                    actualMode,
                    0.0.toLong()
                )
            )
        }
        data?.logs?.forEach {
            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
            val actualMode = resolveActualMode(it.modename, it.discreption)
            logList?.add(
                ELDGraphData(
                    time,
                    actualMode,
                    time.toLong()
                )
            )
        }
        data?.latestUpdatedLog?.let {
            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
            val actualMode = resolveActualMode(it.modename, it.discreption)
            logList?.add(
                ELDGraphData(
                    time,
                    actualMode,
                    time.toLong()
                )
            )
        }

        // Exclude login/logout (and other non-duty) so current mode reflects duty status only
        val filterForSelection = logList?.filter { it.status != "login" && it.status != "logout" && it.status != "certification" && it.status != "INT" }

        if (filterForSelection != null && filterForSelection.isNotEmpty()) {
            val latestMode = filterForSelection.last().status
            if (shouldHoldPendingModeSelection(latestMode)) {
                updateUIAfterModeChange(selectedLog.ifEmpty { TRUCK_MODE_OFF })
                return
            }
            applySelectedMode(latestMode)
        } else {
            // Only set OFF if user is not already on an active mode
            val currentMode = homeViewModel.trackingMode.get()
            if (!shouldPreserveManualSelection() &&
                (currentMode == null || currentMode == MODE_OFF)
            ) {
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
                mostRecentLogWithValidOdometer.location,
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
            // Record rejection on server for FMCSA unidentified driver profile records (ICD §4.5.1)
            val minutes = actualEngHours * 60.0
            homeViewModel.rejectUnidentifiedDriving(
                RejectUnidentifiedRequest(
                    minutes = minutes,
                    vin = vin.ifEmpty { null },
                    odometer = actualOdo,
                    eng_hours = actualEngHours,
                    start_datetime = null,
                    end_datetime = null,
                )
            )
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

    private fun maybeShowDisconnectedDrivingMilesDialog(rawMiles: String?) {
        if (!isAdded || _binding == null) return

        val pendingSegments = readPendingDisconnectedDrivingSegments()
        val milesCovered = rawMiles?.trim()?.toDoubleOrNull() ?: 0.0

        if (milesCovered <= 0.0 && pendingSegments.isEmpty()) {
            prefRepository.clearPendingDisconnectedDrivingMilesDialog()
            prefRepository.clearPendingDisconnectedDrivingSegmentsJson()
            return
        }

        if (disconnectedDrivingMilesDialog?.isShowing == true) {
            return
        }

        val milesText = String.format(Locale.US, "%.2f", milesCovered)
        val recoverableLogCount = pendingSegments.sumOf { segment ->
            listOfNotNull(segment.start, segment.end).size
        }
        val message = if (recoverableLogCount > 0) {
            "You covered $milesText miles while the ELD was disconnected. The ELD stored $recoverableLogCount driving log(s). Do you want to add them to your record?"
        } else {
            "You covered $milesText miles while the ELD was disconnected. Do you want to add this to your record?"
        }

        disconnectedDrivingMilesDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Disconnected Driving")
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, _ ->
                if (pendingSegments.isNotEmpty()) {
                    submitPendingDisconnectedDrivingSegments(pendingSegments)
                }
                prefRepository.clearPendingDisconnectedDrivingMilesDialog()
                prefRepository.clearPendingDisconnectedDrivingSegmentsJson()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                prefRepository.clearPendingDisconnectedDrivingMilesDialog()
                prefRepository.clearPendingDisconnectedDrivingSegmentsJson()
                dialog.dismiss()
            }
            .setOnDismissListener {
                disconnectedDrivingMilesDialog = null
            }
            .show()
    }

    private fun readPendingDisconnectedDrivingSegments(): List<PendingDisconnectedDrivingSegment> {
        val json = prefRepository.getPendingDisconnectedDrivingSegmentsJson()
        if (json.isBlank()) return emptyList()
        return runCatching {
            Gson().fromJson(json, Array<PendingDisconnectedDrivingSegment>::class.java)
                ?.filter { it.start != null || it.end != null }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun submitPendingDisconnectedDrivingSegments(
        segments: List<PendingDisconnectedDrivingSegment>
    ) {
        for (segment in segments) {
            segment.start?.let { point ->
                submitRecoveredStoredEventLog(TRUCK_MODE_DRIVING, point)
            }
            segment.end?.let { point ->
                submitRecoveredStoredEventLog(TRUCK_MODE_ON, point)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            if (_binding != null) {
                runCatching { homeViewModel.getHome(requireContext()) }
            }
        }
    }

    private fun submitRecoveredStoredEventLog(
        mode: String,
        point: PendingDisconnectedDrivingPoint
    ) {
        val vin = resolveTrackerVinForRecoveredLog()
        val logRequest = AddLogRequest(
            modename = mode,
            odometerreading = TelemetryLogValueUtils.normalizeOdometerForLog(
                point.odometerKm,
                prefRepository.getDiffinOdo()
            ),
            lat = point.latitude,
            long = point.longitude,
            location = point.hasLocation,
            eng_hours = TelemetryLogValueUtils.normalizeEngineHoursForLog(
                point.engineHours,
                prefRepository.getDiffinEng()
            ),
            vin = vin,
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1,
            date = point.date.orEmpty(),
            time = point.time.orEmpty(),
            connection_status = "disconnected",
            datetime = point.datetime.orEmpty(),
            codriverid = prefRepository.getCoDriverId().takeIf { it > 0 }
        )
        context?.let { homeViewModel.logUser(logRequest, it) }
    }

    private fun resolveTrackerVinForRecoveredLog(): String {
        val appModel = AppModel.getInstance()
        val pt30Vin = appModel.mPT30Vin
        if (!pt30Vin.isNullOrBlank() && pt30Vin != "n/a") {
            return pt30Vin
        }
        val vehicleVin = appModel.mVehicleInfo?.VIN
        return if (!vehicleVin.isNullOrBlank()) vehicleVin else "1111"
    }

    private fun showPendingDisconnectedDrivingMilesDialogIfNeeded() {
        val pendingMiles = prefRepository.getPendingDisconnectedDrivingMilesDialog()
        if (pendingMiles.isBlank()) return
        maybeShowDisconnectedDrivingMilesDialog(pendingMiles)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        
        Log.d(TAG, "onResume: START")
        
        try {
            // CRITICAL: Cancel all running jobs first to prevent zombie coroutines
            clockJob?.cancel()
            bluetoothConnectionJob?.cancel()

            // Reset snapshot to full/default conditions so clocks show correct values
            // immediately on login before the fresh server response arrives
            latestConditionsSnapshotAtMs = SystemClock.elapsedRealtime()
            latestConditionsSnapshot = HomeDataModel.Conditions(
                drive = 39600, shift = 50400, cycle = 252000, drivebreak = 28800,
                driveViolation = false, shiftViolation = false,
                cycleViolation = false, driveBreakViolation = false
            )

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
                registerCodriverRequestListener()
            } catch (e: Exception) {
                Log.e(TAG, "onResume: Socket connection error: ${e.message}")
            }

            // Register receivers (guarded to prevent duplicate registrations)
            registerLocalReceivers()

            // Show reconnect dialog if coming back from background and ELD is disconnected.
            // Check AFTER receiver is registered so any live STATE_CONNECTED broadcast is caught first.
            if (wasLongPause) {
                val savedAddress = prefRepository.getLastEldDeviceAddress().trim()
                if (savedAddress.isNotEmpty() && !prefRepository.isEldConnected()) {
                    activity?.let { act ->
                        act.findViewById<View>(android.R.id.content)?.postDelayed({
                            if (!prefRepository.isEldConnected()) {
                                (act as? com.eagleye.eld.fragment.Dashboard)?.showEldReconnectDialog()
                            }
                        }, 1500L)
                    }
                }
            }

            // Send login log here so the ViewModel is alive (LoginActivity dies too fast)
            if (prefRepository.getNeedsLoginLog()) {
                prefRepository.setNeedsLoginLog(false)
                fetchLocationAndSendLoginLog()
            }

            showPendingDisconnectedDrivingMilesDialogIfNeeded()
            
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

            loadShipmentContext()

            // Auto-refresh conditions/remaining every 2 min — silent (no spinner) to avoid UI flicker
            conditionsRefreshJob?.cancel()
            conditionsRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
                while (isActive) {
                    delay(CONDITIONS_REFRESH_INTERVAL_MS)
                    if (_binding != null && !isFetchingLogs) {
                        try {
                            context?.let { ctx ->
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    homeViewModel.refreshConditionsSilently(ctx)
                                }
                            }
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

        val currentMode = resolveModeForAutoSwitch()
        lastLog = currentMode

        val modesThatMustSwitchToDriving = setOf(
            TRUCK_MODE_OFF,
            TRUCK_MODE_ON,
            TRUCK_MODE_SLEEPING,
            TRUCK_MODE_PERSONAL,
            TRUCK_MODE_YARD
        )

        if (speed >= DRIVE_DETECTION_THRESHOLD_KMH) {
            // Vehicle moving — always cancel stop timer regardless of current mode
            stoppedSinceMs = null
            if (currentMode in modesThatMustSwitchToDriving) {
                Log.d(TAG, "🚗 Truck is moving ($speed km/h) in '$currentMode' mode — switching to DRIVE")
                lastLog = TRUCK_MODE_DRIVING
                activity?.runOnUiThread { updateUI(binding.btnDrive) }
                updateModeChange(hrs_MODE_D, TRUCK_MODE_DRIVING, "")
            }
        } else if (speed <= 0 && currentMode == TRUCK_MODE_DRIVING) {
            val now = System.currentTimeMillis()
            if (stoppedSinceMs == null) stoppedSinceMs = now
            val elapsedMs = now - (stoppedSinceMs ?: now)
            if (elapsedMs >= STOPPED_DURATION_BEFORE_ON_MS) {
                Log.d(TAG, "🛑 Stopped for ${elapsedMs/1000}s — switching to ON")
                stoppedSinceMs = null
                lastLog = TRUCK_MODE_ON
                activity?.runOnUiThread { updateUI(binding.btnOn) }
                updateModeChange(hrs_MODE_ON, TRUCK_MODE_ON, "")
            } else {
                Log.d(TAG, "⏱ Stopped for ${elapsedMs/1000}s — waiting 5 min before switching to ON")
            }
        }
        // Only reset stopped timer if speed is high enough to re-enter Drive
        // (don't reset on minor ECM sensor noise 1-7 km/h)
    }

    private fun resolveModeForAutoSwitch(): String {
        val candidates = listOf(
            lastLog,
            selectedLog,
            prefRepository.getMode(),
            lastRelevantLog?.modename.orEmpty()
        )

        return candidates
            .map { it.trim().lowercase(Locale.US) }
            .firstOrNull { it in relevantLogTypes }
            ?: TRUCK_MODE_ON
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation", "DefaultLocale")
    private fun updateModeChange(hoursLast: Double, mode: String, selectedOptionText: String) {
        try {
            if (mode == TRUCK_MODE_DRIVING && !isDrivingModeAllowed()) {
                Log.d(TAG, "Blocked DR mode change because tracker is disconnected")
                if (isAdded) {
                    showValidationErrors("Driving mode is only available when a tracker is connected.")
                }
                updateDrivingButtonAvailability()
                return
            }
            if (mode == TRUCK_MODE_DRIVING) {
                prefRepository.setShowUnidentifiedDialog(false)
            }
            Log.d(TAG, "updating mode --> $mode")
            rememberTimerStartForModeChange(mode)
            applySelectedMode(mode)
            markPendingModeSelection(mode)
            if (_binding != null) {
                updateClockDisplay()
            }
            val vin_no = AppModel.getInstance().mVehicleInfo?.VIN
            val te = AppModel.getInstance().mLastEvent

            val normalizedOdometer = TelemetryLogValueUtils.normalizeOdometerForLog(
                te?.mOdometer,
                prefRepository.getDiffinOdo()
            )
            val normalizedEngineHours = TelemetryLogValueUtils.normalizeEngineHoursForLog(
                te?.mEngineHours,
                prefRepository.getDiffinEng()
            )
            val finalMode = mode
            val eventCode = if (selectedOptionText == "yard" || mode == TRUCK_MODE_YARD) 4 else 1

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
                    lastLog.location,
                    0
                )
                context?.let { homeViewModel.updateLog(updateLogRequest, false, it) }
            }

            val submitModeChangeLog: (LogLocationPayload) -> Unit = { logLocationPayload ->
                val logRequest = AddLogRequest(
                    modename = finalMode,
                    odometerreading = normalizedOdometer,
                    lat = logLocationPayload.latitude,
                    long = logLocationPayload.longitude,
                    location = logLocationPayload.shouldSendLocation,
                    eng_hours = normalizedEngineHours,
                    vin = vin_no.toString(),
                    is_active = 1,
                    is_autoinsert = 0,
                    eventcode = eventCode,
                    eventtype = 1,
                    connection_status = if (isNeedToconnect) "disconnected" else "connected",
                    codriverid = prefRepository.getCoDriverId().takeIf { it > 0 }
                )

                if (selectedOptionText == "yard") {
                    logRequest.discreption = "yard"
                } else if (selectedOptionText == "personal") {
                    logRequest.discreption = "personal"
                } else if (selectedOptionText.isNotEmpty()) {
                    logRequest.discreption = selectedOptionText
                }

                context?.let { homeViewModel.logUser(logRequest, it) }
            }

            if (!isNeedToconnect) {
                val trackerPayload = resolveTrackerLocationPayload()
                Log.d(TAG, "📍 Mode change location (TRACKER): lat=${trackerPayload.latitude}, lng=${trackerPayload.longitude}, valid=${trackerPayload.shouldSendLocation}")
                submitModeChangeLog(trackerPayload)
            } else {
                val dashboard = activity as? Dashboard
                if (dashboard != null) {
                    Log.d(TAG, "📍 Mode change location: Requesting PHONE geolocation (disconnected)...")
                    dashboard.fetchCurrentGeoLocation { phoneLocation ->
                        if (!isAdded) return@fetchCurrentGeoLocation
                        Log.d(TAG, "📍 Mode change location (PHONE): received=${phoneLocation != null}, lat=${phoneLocation?.first}, lng=${phoneLocation?.second}")
                        val phoneLocationPayload = resolvePhoneLocationPayload(phoneLocation)
                        Log.d(TAG, "📍 Mode change location (PHONE payload): lat=${phoneLocationPayload.latitude}, lng=${phoneLocationPayload.longitude}, valid=${phoneLocationPayload.shouldSendLocation}")
                        activity?.runOnUiThread {
                            submitModeChangeLog(phoneLocationPayload)
                        }
                    }
                } else {
                    Log.w(TAG, "📍 Mode change location: Dashboard is null, sending empty location")
                    submitModeChangeLog(resolvePhoneLocationPayload(null))
                }
            }

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
        homeViewModel.stopListeningForCodriverRequest()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

    private fun registerCodriverRequestListener() {
        homeViewModel.listenForCodriverRequest { fromDriverId, fromDriverName, fromUsername, companyName ->
            val activity = activity ?: return@listenForCodriverRequest
            activity.runOnUiThread {
                val displayName = if (fromDriverName.isNotBlank()) fromDriverName else fromUsername.ifBlank { "A driver" }
                val companyText = if (companyName.isNotBlank()) " from $companyName" else ""
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Co-Driver Request")
                    .setMessage("$displayName$companyText wants to add you as a co-driver. Accept?")
                    .setPositiveButton("Accept") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                homeViewModel.respondToCodriver(fromDriverId, true)
                                // Load requesting driver's clocks at bottom
                                prefRepository.setCoDriverId(fromDriverId)
                                prefRepository.setCoDriverName(fromDriverName.ifBlank { fromUsername })
                                prefRepository.setCodriverUsername(fromUsername)
                                prefRepository.setIsCodriverLoggedIn(true)
                                homeViewModel.getHome(requireContext())
                            } catch (e: Exception) {
                                Log.e(TAG, "respondToCodriver accept error: ${e.message}")
                            }
                        }
                    }
                    .setNegativeButton("Reject") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try { homeViewModel.respondToCodriver(fromDriverId, false) } catch (_: Exception) {}
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
        }
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
                        updateLiveConditionsDisplay()
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

            val currentMode = getCurrentModeForLiveConditions()
            val localTimerStartMs = prefRepository.getTimerStartTime()
            val lastLog = lastRelevantLog
            val lastServerMode = resolveActualMode(lastLog)
            val shouldUseLocalTimer =
                currentMode in relevantLogTypes &&
                    localTimerStartMs > 0L &&
                    (lastServerMode.isEmpty() || lastServerMode != currentMode)

            if (shouldUseLocalTimer) {
                val elapsedSeconds =
                    ((System.currentTimeMillis() - localTimerStartMs) / 1000L).coerceAtLeast(0L)
                binding.timerTv.text = formatElapsedDuration(elapsedSeconds)
                binding.timerLabelTv.text = "Time spent in ${formatModeLabel(currentMode)}"
            } else if (lastLog != null) {
                lastLogTime = lastLog.time ?: "00:00"
                lastLogMode = lastServerMode
                val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
                binding.timerTv.text = elapsedTime
                binding.timerLabelTv.text = "Time spent in ${formatModeLabel(lastLogMode)}"
            } else {
                if (localTimerStartMs > 0L && currentMode in relevantLogTypes) {
                    val elapsedSeconds =
                        ((System.currentTimeMillis() - localTimerStartMs) / 1000L).coerceAtLeast(0L)
                    binding.timerTv.text = formatElapsedDuration(elapsedSeconds)
                    binding.timerLabelTv.text = "Time spent in ${formatModeLabel(currentMode)}"
                } else if (lastLogTime.isNotEmpty() && lastLogMode.isNotEmpty()) {
                    val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
                    binding.timerTv.text = elapsedTime
                    binding.timerLabelTv.text = "Time spent in ${formatModeLabel(lastLogMode)}"
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

    private fun formatElapsedDuration(totalSeconds: Long): String {
        val normalizedSeconds = totalSeconds.coerceAtLeast(0L)
        val elapsedHours = normalizedSeconds / 3600
        val elapsedMinutes = (normalizedSeconds % 3600) / 60
        val elapsedSecondsRemainder = normalizedSeconds % 60
        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            elapsedHours,
            elapsedMinutes,
            elapsedSecondsRemainder
        )
    }

    private fun updateLastRelevantLog(logs: List<HomeDataModel.Log>?) {
        if (logs.isNullOrEmpty()) {
            lastRelevantLog = null
            return
        }
        for (index in logs.size - 1 downTo 0) {
            val mode = resolveActualMode(logs[index])
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
            instance.registerReceiver(connectionStateRefresh, connectionStateIf)
            instance.registerReceiver(disconnectedDrivingMilesRefresh, disconnectedMilesIf)
            instance.registerReceiver(viRefresh, vinRefreshIf)
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
            instance.unregisterReceiver(connectionStateRefresh)
            instance.unregisterReceiver(disconnectedDrivingMilesRefresh)
            instance.unregisterReceiver(viRefresh)
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
            location = false,
            eng_hours = 0.toString(),
            vin = "1111",
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1,
            connection_status = if (isNeedToconnect) "disconnected" else "connected",
            codriverid = prefRepository.getCoDriverId().takeIf { it > 0 }
        )
        context?.let { homeViewModel.logUser(logRequest, it) }
        // Clear stale ELD device/VIN cache so next session starts fresh
        com.eagleye.eld.pt.devicemanager.AppModel.getInstance().invalidate()
        // Clear current_token on server so next login on this device doesn't trigger force-login
        lifecycleScope.launch {
            homeViewModel.driverLogout()
            prefRepository.setLoggedIn(false)
            prefRepository.setDifferenceinOdo("0")
            prefRepository.setDifferenceinEnghours("0")
            prefRepository.setToken("")
            context?.let { ctx ->
                val intent = Intent(ctx, LoginActivity::class.java)
                startActivity(intent)
            }
            activity?.finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View) {
        playClickAnimation(v)
        if (v.id == R.id.btnDrive && !isDrivingModeAllowed()) {
            updateDrivingButtonAvailability()
            showValidationErrors("Driving mode is only available when a tracker is connected.")
            return
        }
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
                R.id.btnYard -> yardMode()
                R.id.btnPersonal -> personalMode()
            }
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
        if (selectedLog != TRUCK_MODE_SLEEPING || isEmptyList) {
            mediaPlayer.start()
            updateModeChange(hrs_MODE_SB, TRUCK_MODE_SLEEPING, "")
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
        if (!isDrivingModeAllowed()) {
            Log.d(TAG, "Blocked manual DRIVING mode while tracker is disconnected")
            showValidationErrors("Driving mode is only available when a tracker is connected.")
            return
        }
        Log.d(TAG, "🚗 Manual switch to DRIVING mode")
        updateUI(binding.btnDrive)
        mediaPlayer.start()
        updateModeChange(hrs_MODE_D, TRUCK_MODE_DRIVING, "")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMode() {
        val dialog = Dialog(requireContext(), R.style.ModernDialogStyle)
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
                if (selectedLog != TRUCK_MODE_ON || isEmptyList) {
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
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Staggered Entrance Animations for Menu Items
        optionViews.forEachIndexed { index, view ->
            view?.visibility = View.INVISIBLE
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                view?.visibility = View.VISIBLE
                YoYo.with(Techniques.FadeInRight).duration(300).playOn(view)
            }, index * 40L)
        }
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
        val dialog = Dialog(requireContext(), R.style.ModernDialogStyle)
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
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        
        // Staggered Entrance Animations for Menu Items
        optionViews.forEachIndexed { index, view ->
            view?.visibility = View.INVISIBLE
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                view?.visibility = View.VISIBLE
                YoYo.with(Techniques.FadeInRight).duration(300).playOn(view)
            }, index * 50L)
        }
        
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
        val allButtons = listOf(
            binding.btnOff, binding.btnSleep, binding.btnDrive, 
            binding.btnOn, binding.btnPersonal, binding.btnYard
        )

        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.card_white)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.onduty_secondary)
        val strokeUnselected = ContextCompat.getColor(requireContext(), R.color.strockColor)
        val strokeSelected = ContextCompat.getColor(requireContext(), R.color.nav_icon_active)

        allButtons.forEach { card ->
            card.setCardBackgroundColor(unselectedColor)
            card.strokeColor = strokeUnselected
            card.strokeWidth = 1
            
            // Fix text visibility for specific buttons if they were forced to white previously
            if (card == binding.btnPersonal) binding.tvPerosnaluse.setTextColor(ContextCompat.getColor(requireContext(), R.color.home_text_main))
            if (card == binding.btnYard) binding.tvYarduse.setTextColor(ContextCompat.getColor(requireContext(), R.color.home_text_main))
        }

        viewSelect?.let {
            if (it is com.google.android.material.card.MaterialCardView) {
                it.setCardBackgroundColor(selectedColor)
                it.strokeColor = strokeSelected
                it.strokeWidth = 2
            }
        }

        if (viewSelect == binding.btnDrive) {
            if (!isChangingStatus) {
                showDrivingOverlay()
            } else {
                updateDrivingOverlayContent(animateProgress = false)
            }
        } else {
            isChangingStatus = false
            hideDrivingOverlay()
        }
    }

    private var isChangingStatus = false

    private fun updateDrivingOverlayForCurrentMode() {
        if (_binding == null) return

        if (getCurrentModeForLiveConditions() == TRUCK_MODE_DRIVING) {
            if (!isChangingStatus) {
                showDrivingOverlay()
            } else {
                updateDrivingOverlayContent(animateProgress = false)
            }
        } else {
            isChangingStatus = false
            hideDrivingOverlay()
        }
    }

    private fun showDrivingOverlay() {
        val overlay = binding.clDrOverlay
        val shouldAnimateEntry = overlay.visibility != View.VISIBLE
        if (overlay.visibility != View.VISIBLE) {
            overlay.visibility = View.VISIBLE
            overlay.alpha = 0f
            overlay.animate().alpha(1f).setDuration(300).start()
        }

        binding.btnDrChangeStatus.setOnClickListener {
            isChangingStatus = true
            hideDrivingOverlay()
        }

        updateDrivingOverlayContent(animateProgress = shouldAnimateEntry)
    }

    private fun updateDrivingOverlayContent(animateProgress: Boolean) {
        if (_binding == null) return

        val c = getLiveConditions() ?: homeViewModel.homeLiveData.value?.data?.conditions
        val closest = c?.let { findClosestViolation(it) }

        if (closest != null) {
            val label = when (closest.type) {
                "Drive Break" -> "30-MIN BREAK"
                "Drive Time"  -> "DRIVE LIMIT"
                "Shift Time"  -> "SHIFT LIMIT"
                "Cycle Time"  -> "CYCLE LIMIT"
                else          -> closest.type.uppercase()
            }
            binding.tvDrTimeRemaining.text = formatDrivingOverlayTime(closest.remainingMinutes)
            binding.tvDrViolationLabel.text = label
            binding.drProgressCircle.setProgressCompat(closest.progress, animateProgress)
            binding.drProgressCircle.setIndicatorColor(gaugeColor(closest.progress))
        } else {
            binding.tvDrTimeRemaining.text = "0:00"
            binding.tvDrViolationLabel.text = "VIOLATION"
            binding.drProgressCircle.setProgressCompat(0, animateProgress)
            binding.drProgressCircle.setIndicatorColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    private fun formatDrivingOverlayTime(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val totalMinutes = safeSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.US, "%d:%02d", hours, minutes)
    }

    private fun hideDrivingOverlay() {
        val overlay = binding.clDrOverlay
        if (overlay.visibility == View.VISIBLE) {
            overlay.animate().alpha(0f).setDuration(200).withEndAction {
                overlay.visibility = View.GONE
            }.start()
        }
    }

    private fun playClickAnimation(view: View) {
        // High-end subtle scale feedback
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

    private fun showValidationErrors(error: String, dialogInterface: dialogInterface? = null) {
        Utils.dialog(requireContext(), message = error, callback = dialogInterface)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTelemetryInfo() {
        try {
            val dashboard = activity as? Dashboard ?: return
            val appModel = AppModel.getInstance()
            val lastEvent = appModel.mLastEvent
            
            // Manage pulsing animation for the status indicator
            val pulseAnimation = android.view.animation.AlphaAnimation(1.0f, 0.5f).apply {
                duration = 800
                repeatMode = android.view.animation.Animation.REVERSE
                repeatCount = android.view.animation.Animation.INFINITE
            }

            if (lastEvent != null) {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_disconnect)
                // binding.tvConected.text = "Connected" // Removed
                isNeedToconnect = false
                updateDrivingButtonAvailability()
                
                // Update top-left pill to CONNECTED (Green)
                binding.tvBluetoothStatusText.text = "Connected"
                binding.tvBluetoothStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_on_text))
                binding.ivBluetoothStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_on_text))
                binding.ivBluetoothStatusIcon.setImageResource(R.drawable.ic_flash_on)
                binding.llBluetoothStatusPill.setBackgroundResource(R.drawable.bg_status_pill_active)
                
                // Remove pulse animation for stable connection or add a subtle one if preferred
                binding.llBluetoothStatusPill.clearAnimation()
                binding.ivBluetoothStatusIcon.startAnimation(pulseAnimation)
                
                // Start continuous speed monitoring if not already running
                startSpeedMonitoring()
            } else {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_bluetooth)
                // binding.tvConected.text = "Not Connected" // Removed
                isNeedToconnect = true
                prefRepository.setShowUnidentifiedDialog(true)
                updateDrivingButtonAvailability()
                
                // Update top-left pill to DISCONNECTED (Red)
                binding.tvBluetoothStatusText.text = "Disconnected"
                binding.tvBluetoothStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_off_text))
                binding.ivBluetoothStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_off_text))
                binding.ivBluetoothStatusIcon.setImageResource(R.drawable.ic_status_off_dot) // A dot icon
                binding.llBluetoothStatusPill.setBackgroundResource(R.drawable.bg_status_pill_off)
                
                // Add blinking animation for OFF state
                binding.ivBluetoothStatusIcon.startAnimation(pulseAnimation)
                
                // Stop speed monitoring when disconnected
                stopSpeedMonitoring()
                // Reset speed pill to zero
                updateSpeedPill(0)
                restoreSelectedModeFromState()
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
            val newVin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: return
            Log.d("VIN_DEBUG", "Vehicle info update - VIN: $newVin")

            val storedVin = prefRepository.getLastKnownVin()
            if (storedVin.isNotEmpty() && storedVin != newVin) {
                Log.d("VIN_DEBUG", "Vehicle changed: $storedVin → $newVin")
                activity?.runOnUiThread {
                    showVehicleChangedAlert(storedVin, newVin)
                }
            } else {
                prefRepository.setLastKnownVin(newVin)
            }
        }
    }

    private fun showVehicleChangedAlert(oldVin: String, newVin: String) {
        if (_binding == null) return
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Vehicle Changed")
            .setMessage("A different truck has been detected.\n\nPrevious: $oldVin\nNew: $newVin\n\nYou must confirm the vehicle change to continue logging.")
            .setPositiveButton("Confirm Switch") { _, _ ->
                prefRepository.setLastKnownVin(newVin)
                prefRepository.setDifferenceinOdo("0")
                prefRepository.setDifferenceinEnghours("0")
                Log.d("VIN_DEBUG", "Vehicle change accepted — offsets reset for $newVin")
            }
            .setNegativeButton("Reject") { _, _ ->
                showVehicleChangeRejectedError(oldVin, newVin)
            }
            .setCancelable(false)
            .show()
    }

    private fun showVehicleChangeRejectedError(oldVin: String, newVin: String) {
        if (_binding == null) return
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Action Required")
            .setMessage("You are connected to a different vehicle (VIN: $newVin).\n\nPlease contact your administrator before continuing. You cannot log duty status until the vehicle change is acknowledged.")
            .setPositiveButton("Try Again") { _, _ ->
                showVehicleChangedAlert(oldVin, newVin)
            }
            .setCancelable(false)
            .show()
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
            val lastEvent = appModel.mLastEvent ?: run {
                // No event — show 0
                updateSpeedPill(0)
                return
            }
            val speed = lastEvent.mGeoloc.speed
            val dashboard = appModel.dashboard
            val rpm = dashboard?.engineRPM ?: 0
            // Update speed pill on home screen
            updateSpeedPill(speed)
            // Use actual Bluetooth speed for mode switching (not dashboardSpeed)
            handleLocationUpdate(speed, rpm, lastEvent.mEvent.name)
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndPrintSpeed: ${e.message}", e)
        }
    }

    private fun updateSpeedPill(speed: Int) {
        try {
            if (_binding == null) return
            val mph = (speed * 0.621371).toInt()
            binding.tvSpeedValue.text = "$mph mph"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating speed pill: ${e.message}")
        }
    }


    private fun updateShipmentInfoUI() {
        val shippingText = prefRepository.getShippingNumber().ifBlank { "Not set" }
        val trailerText = prefRepository.getTrailerNumber().ifBlank { "Not set" }
        val coDriverName = prefRepository.getCoDriverName().ifBlank { "No Co-driver" }

        binding.shippingNumber.text = getString(shipping_number).plus(shippingText)
        binding.trailerNumber.text = getString(trailer_number).plus(trailerText)
        binding.coDriver.text = "Co-Driver : $coDriverName"
    }

    private fun setupCodriverPanel() {
        val panel = binding.cvCodriverPanel

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch other driver data from server (no codriverId = server does lookup)
                val resp = homeViewModel.getCodriverHos()
                val body = if (resp.isSuccessful) resp.body() else null
                val data = if (body?.status == true) body.codriver else null

                withContext(Dispatchers.Main) {
                    // VIN mismatch warning
                    if (body?.vinMismatch == true && !body.expectedVin.isNullOrBlank() && !body.codriverVin.isNullOrBlank()) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Wrong Truck Connected")
                            .setMessage("Your co-driver is connected to a different truck (VIN: ${body.codriverVin}).\n\nPlease ask them to reconnect to your truck (VIN: ${body.expectedVin}).")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    if (data == null || data.id == null || data.id <= 0 || data.id == prefRepository.getDriverId()) {
                        panel.visibility = View.GONE
                        // Relationship removed on another device — clear local state
                        if (prefRepository.isCodriverLoggedIn()) {
                            prefRepository.clearCodriver()
                            activity?.let { (it as? com.eagleye.eld.fragment.Dashboard)?.updateCodriverNavHeader() }
                        }
                        return@withContext
                    }

                    // Persist locally if not already set
                    if (!prefRepository.isCodriverLoggedIn()) {
                        val name = data.name?.ifEmpty { data.username } ?: data.username ?: ""
                        prefRepository.setCoDriverId(data.id)
                        prefRepository.setCoDriverName(name)
                        prefRepository.setCodriverUsername(data.username ?: "")
                        prefRepository.setIsCodriverLoggedIn(true)
                    }

                    panel.visibility = View.VISIBLE

                    val tvLabel  = panel.findViewById<android.widget.TextView>(R.id.tv_codriver_label)
                    val tvBadge  = panel.findViewById<android.widget.TextView>(R.id.tv_codriver_status_badge)
                    val tvDrive  = panel.findViewById<android.widget.TextView>(R.id.tv_cd_drive)
                    val tvShift  = panel.findViewById<android.widget.TextView>(R.id.tv_cd_shift)
                    val tvBreak  = panel.findViewById<android.widget.TextView>(R.id.tv_cd_break)
                    val tvCycle  = panel.findViewById<android.widget.TextView>(R.id.tv_cd_cycle)
                    val btnRemove = panel.findViewById<android.widget.Button>(R.id.btn_remove_codriver)

                    val displayName = data.name?.ifEmpty { data.username } ?: data.username ?: "Co-Driver"
                    tvLabel?.text = "Co-Driver: $displayName"

                    val status = data.currentStatus ?: ""
                    tvBadge?.text = when (status) { "d" -> "DR"; "on" -> "ON"; "sb" -> "SB"; else -> "OFF" }
                    val badgeColor = when (status) {
                        "d"  -> android.graphics.Color.parseColor("#3B82F6")
                        "on" -> android.graphics.Color.parseColor("#F97316")
                        "sb" -> android.graphics.Color.parseColor("#8B5CF6")
                        else -> android.graphics.Color.parseColor("#9CA3AF")
                    }
                    tvBadge?.backgroundTintList = android.content.res.ColorStateList.valueOf(badgeColor)

                    fun fmt(secs: Int?) = String.format("%02d:%02d", (secs ?: 0) / 3600, ((secs ?: 0) % 3600) / 60)
                    val c = data.conditions
                    tvDrive?.text = fmt(c?.drive)
                    tvShift?.text = fmt(c?.shift)
                    tvBreak?.text = fmt(c?.drivebreak)
                    tvCycle?.text = fmt(c?.cycle)
                    if (c?.driveViolation == true) tvDrive?.setTextColor(android.graphics.Color.RED)
                    if (c?.shiftViolation == true) tvShift?.setTextColor(android.graphics.Color.RED)
                    if (c?.driveBreakViolation == true) tvBreak?.setTextColor(android.graphics.Color.RED)
                    if (c?.cycleViolation == true) tvCycle?.setTextColor(android.graphics.Color.RED)

                    btnRemove?.setOnClickListener {
                        panel.visibility = View.GONE
                        prefRepository.clearCodriver()
                        (activity as? com.eagleye.eld.fragment.Dashboard)?.updateCodriverNavHeader()
                        viewLifecycleOwner.lifecycleScope.launch {
                            try { homeViewModel.setMyCodriver(null); homeViewModel.codriverLogout() } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load codriver panel: ${e.message}")
            }
        }
    }

    private fun loadShipmentContext() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val codriverResponse = homeViewModel.getMyCodrivers()
                if (codriverResponse.isSuccessful && codriverResponse.body()?.status == true) {
                    codrivers = codriverResponse.body()?.codrivers ?: emptyList()
                } else {
                    codrivers = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load codrivers: ${e.message}")
                codrivers = emptyList()
            }

            try {
                val shipmentResponse = homeViewModel.getActiveDriverShipment()
                if (shipmentResponse.isSuccessful && shipmentResponse.body()?.status == true) {
                    val shipment = shipmentResponse.body()?.data
                    prefRepository.setShippingNumber(shipment?.shippingNumber.orEmpty())
                    prefRepository.setTrailerNumber(shipment?.trailerNumber.orEmpty())
                    val coDriverId = shipment?.codriverId ?: 0
                    if (coDriverId > 0) {
                        prefRepository.setCoDriverId(coDriverId)
                        val name = codrivers.firstOrNull { it.id == coDriverId }?.username ?: "ID $coDriverId"
                        prefRepository.setCoDriverName(name)
                    } else {
                        prefRepository.clearCoDriverId()
                        prefRepository.setCoDriverName("")
                    }
                } else if (shipmentResponse.code() == 404) {
                    prefRepository.setShippingNumber("")
                    prefRepository.setTrailerNumber("")
                    prefRepository.clearCoDriverId()
                    prefRepository.setCoDriverName("")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load active shipment: ${e.message}")
            } finally {
                updateShipmentInfoUI()
            }
        }
    }

    private fun showShipmentDialog() {
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_update_shipment, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.ModernDialogStyle).setView(dialogView).create()

        val shippingField: EditText = dialogView.findViewById(R.id.input_shipping_number)
        val trailerField: EditText = dialogView.findViewById(R.id.input_trailer_number)
        val codriverSpinner: AppCompatSpinner = dialogView.findViewById(R.id.spinner_codriver)
        val cancelButton: Button = dialogView.findViewById(R.id.btn_cancel_shipment)
        val updateButton: Button = dialogView.findViewById(R.id.btn_update_shipment)

        shippingField.setText(prefRepository.getShippingNumber())
        trailerField.setText(prefRepository.getTrailerNumber())

        val spinnerOptions = mutableListOf<Pair<Int?, String>>()
        spinnerOptions.add(null to "No Co-driver")
        codrivers.forEach { c ->
            spinnerOptions.add(c.id to (c.username ?: "Driver #${c.id}"))
        }

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_modern,
            spinnerOptions.map { it.second }
        )
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_modern)
        codriverSpinner.adapter = spinnerAdapter

        val preselectedId = prefRepository.getCoDriverId().takeIf { it > 0 }
        val selectedIndex = spinnerOptions.indexOfFirst { it.first == preselectedId }.takeIf { it >= 0 } ?: 0
        codriverSpinner.setSelection(selectedIndex)

        cancelButton.setOnClickListener { dialog.dismiss() }
        updateButton.setOnClickListener {
            val shippingText = shippingField.text.toString().trim()
            val trailerText = trailerField.text.toString().trim()
            val selectedCodriverId = spinnerOptions.getOrNull(codriverSpinner.selectedItemPosition)?.first

            val shippingNumber = shippingText.toIntOrNull()
            if (shippingText.isEmpty() || shippingNumber == null || shippingNumber < 0) {
                makeText(requireContext(), "Enter a valid shipping number", LENGTH_LONG).show()
                return@setOnClickListener
            }

            val trailerNumber = if (trailerText.isBlank()) null else trailerText.toIntOrNull()
            if (trailerText.isNotBlank() && (trailerNumber == null || trailerNumber <= 0)) {
                makeText(requireContext(), "Enter a valid trailer number", LENGTH_LONG).show()
                return@setOnClickListener
            }

            updateButton.isEnabled = false
            updateButton.alpha = 0.5f
            updateButton.text = "Saving..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = homeViewModel.upsertDriverShipment(
                        DriverShipmentRequest(
                            shippingNumber = shippingNumber,
                            trailerNumber = trailerNumber,
                            codriverId = selectedCodriverId
                        )
                    )

                    if (response.isSuccessful && response.body()?.status == true) {
                        prefRepository.setShippingNumber(shippingNumber.toString())
                        prefRepository.setTrailerNumber(trailerNumber?.toString().orEmpty())
                        if (selectedCodriverId != null) {
                            prefRepository.setCoDriverId(selectedCodriverId)
                            val selectedName = codrivers.firstOrNull { it.id == selectedCodriverId }?.username ?: "ID $selectedCodriverId"
                            prefRepository.setCoDriverName(selectedName)
                        } else {
                            prefRepository.clearCoDriverId()
                            prefRepository.setCoDriverName("")
                        }
                        updateShipmentInfoUI()
                        makeText(requireContext(), response.body()?.message ?: "Shipment updated", LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        val errorMessage = try {
                            val raw = response.errorBody()?.string().orEmpty()
                            if (raw.isNotEmpty()) JSONObject(raw).optString("message", "Failed to save shipment details.")
                            else "Failed to save shipment details."
                        } catch (e: Exception) {
                            "Failed to save shipment details."
                        }
                        makeText(requireContext(), errorMessage, LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    makeText(requireContext(), "Network error: ${e.message}", LENGTH_LONG).show()
                } finally {
                    updateButton.isEnabled = true
                    updateButton.alpha = 1.0f
                    updateButton.text = "Update"
                }
            }
        }

        dialog.show()

        // Staggered Entrance Animations for Premium Look
        val delay = 100L
        YoYo.with(Techniques.FadeInDown).duration(500).playOn(dialogView.findViewById(R.id.llDialogHeader))
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            YoYo.with(Techniques.FadeInDown).duration(500).playOn(dialogView.findViewById(R.id.llShippingSection))
        }, delay)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            YoYo.with(Techniques.FadeInDown).duration(500).playOn(dialogView.findViewById(R.id.llCodriverSection))
        }, delay * 2)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            YoYo.with(Techniques.FadeInDown).duration(500).playOn(dialogView.findViewById(R.id.llTrailerSection))
        }, delay * 3)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            YoYo.with(Techniques.FadeInUp).duration(500).playOn(dialogView.findViewById(R.id.llDialogActions))
        }, delay * 4)

        // Premium Click Feedback for Dialog Buttons
        cancelButton.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    YoYo.with(Techniques.Pulse).duration(100).playOn(v)
                }
            }
            false
        }
        updateButton.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    YoYo.with(Techniques.Pulse).duration(100).playOn(v)
                }
            }
            false
        }
    }

    private fun updateViolationTimeCard(conditions: HomeDataModel.Conditions?) {
        conditions?.let { cond ->
            val closestViolation = findClosestViolation(cond)
            
            // Update the top alert card (if exists)
            if (closestViolation != null) {
                binding.closestViolationCard.visibility = View.VISIBLE
                binding.closestViolationType.text = closestViolation.type
                binding.closestViolationTime.text = closestViolation.remainingTime
                binding.closestViolationProgress.progress = closestViolation.progress
                binding.closestViolationProgress.setIndicatorColor(getViolationColor(closestViolation.progress, closestViolation.isViolation))
                
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

    private fun getModeShorthand(type: String): String {
        return when (type.lowercase()) {
            "drive time" -> "DR"
            "drive break" -> "BR"
            "shift time" -> "SH"
            "cycle time" -> "CY"
            "sleeper berth" -> "SB"
            else -> "ON"
        }
    }

    private fun calculateAvailableDriveTime(conditions: HomeDataModel.Conditions): String {
        // Minimum of Drive, Shift, and Cycle
        val times = mutableListOf<Int>()
        conditions.drive?.let { times.add(it) }
        conditions.shift?.let { times.add(it) }
        conditions.cycle?.let { times.add(it) }
        
        val minTimeSec = times.minOrNull() ?: 0
        val shorthand = when (minTimeSec) {
            conditions.drive -> "DR"
            conditions.shift -> "SH"
            conditions.cycle -> "CY"
            else -> "DR"
        }
        return "$shorthand ${Utils.formatTimeFromSeconds(minTimeSec)}"
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
        if (remainingSeconds <= 0) return 0
        return (remainingSeconds.toFloat() / totalSeconds * 100).toInt().coerceIn(0, 100)
    }

    private fun getViolationColor(progress: Int, isViolation: Boolean): Int {
        // progress is now remaining% (100 = full, 0 = empty/violation)
        return if (isViolation || progress <= 0) {
            ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
        } else when {
            progress > 50 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_green)
            progress > 25 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_orange)
            else          -> ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
        }
    }

    private fun startEntranceAnimations() {
        if (_binding == null) return
        
        val duration = 700L
        val stagger = 80L

        val views = listOf(
            binding.tvConsoleTitle,
            binding.liveClock,
            binding.tvStatusSubtitle,
            binding.llQuickPills,
            binding.cvProfile,
            binding.tvDriveModesTitle,
            binding.glModes,
            binding.tvDutyLimitsTitle,
            binding.glGauges,
            binding.tvShipmentTitle,
            binding.cvShipment
        )

        views.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                kotlinx.coroutines.delay(index * stagger)
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    val technique = if (index <= 3) Techniques.FadeInDown else Techniques.FadeInUp
                    YoYo.with(technique).duration(duration).playOn(view)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showEldAttentionDialog(showMalfunction: Boolean) {
        val att = latestEldAttention
        val diagLabels = mapOf(
            1 to "Power data diagnostic",
            2 to "Engine synchronization data diagnostic",
            3 to "Missing required data elements diagnostic",
            4 to "Data transfer data diagnostic",
            5 to "Unidentified driving records diagnostic",
            6 to "Other ELD detected diagnostic"
        )
        val malfLabels = mapOf(
            1 to "P: Power compliance malfunction",
            2 to "E: Engine synchronization compliance malfunction",
            3 to "T: Timing compliance malfunction",
            4 to "L: Positioning compliance malfunction",
            5 to "R: Data recording compliance malfunction",
            6 to "S: Data transfer compliance malfunction",
            7 to "O: Other detected malfunction"
        )
        val activeColor = if (showMalfunction) android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#F97316")
        val clearedColor = android.graphics.Color.parseColor("#9CA3AF")

        val ctx = requireContext()
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        fun addRow(label: String, color: Int, cleared: Boolean) {
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
            }
            val dot = android.widget.TextView(ctx).apply {
                text = "●"
                textSize = 10f
                setTextColor(color)
                setPadding(0, 4, 16, 0)
            }
            val label = android.widget.TextView(ctx).apply {
                text = label
                textSize = 14f
                setTextColor(color)
                if (cleared) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            }
            row.addView(dot)
            row.addView(label)
            container.addView(row)
        }

        var hasAny = false
        if (showMalfunction) {
            att?.malfunctionCodesActive?.forEach { code ->
                addRow(malfLabels[code] ?: "Malfunction code $code", activeColor, false); hasAny = true
            }
            att?.malfunctionCodesCleared?.forEach { code ->
                addRow(malfLabels[code] ?: "Malfunction code $code", clearedColor, true); hasAny = true
            }
        } else {
            att?.diagnosticCodesActive?.forEach { code ->
                addRow("D$code: ${diagLabels[code] ?: "Data diagnostic $code"}", activeColor, false); hasAny = true
            }
            att?.diagnosticCodesCleared?.forEach { code ->
                addRow("D$code: ${diagLabels[code] ?: "Data diagnostic $code"}", clearedColor, true); hasAny = true
            }
            if (att?.dutyStatusDataDiagnosticActive == true && !hasAny) {
                addRow("Data diagnostic on latest duty-status log", activeColor, false); hasAny = true
            }
        }
        if (!hasAny) {
            addRow(if (showMalfunction) "No active malfunctions" else "No active diagnostics", clearedColor, false)
        }

        val title = if (showMalfunction) "M — Malfunction" else "D — Data Diagnostic"
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDriverProfileDialog() {
        val dialog = Dialog(requireContext(), R.style.ModernDialogStyle)
        val view = layoutInflater.inflate(R.layout.dialog_driver_profile, null)
        dialog.setContentView(view)

        val profileInitial = view.findViewById<TextView>(R.id.dialog_profile_initial)
        val driverNameView = view.findViewById<TextView>(R.id.dialog_driver_name)
        val licenseNumberView = view.findViewById<TextView>(R.id.dialog_license_number)
        val carrierNameView = view.findViewById<TextView>(R.id.dialog_carrier_name)
        val dotNumberView = view.findViewById<TextView>(R.id.dialog_dot_number)
        val addressView = view.findViewById<TextView>(R.id.dialog_carrier_address)
        val phoneView = view.findViewById<TextView>(R.id.dialog_carrier_phone)
        val btnClose = view.findViewById<View>(R.id.btn_close_dialog)
        val dialogRoot = view.findViewById<View>(R.id.dialog_root)

        // Set initials from local prefs first
        val name = prefRepository.getName()
        if (name.isNotEmpty()) {
            driverNameView.text = name
            profileInitial.text = name.first().uppercase()
        }

        // Setup observer for driver review data
        homeViewModel.driverReviewLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val driver = result.data?.data?.driver
                    val company = result.data?.data?.company
                    
                    driver?.let {
                        driverNameView.text = it.name ?: name
                        licenseNumberView.text = it.licenseNumber ?: "N/A"
                        if (it.name?.isNotEmpty() == true) {
                            profileInitial.text = it.name.first().uppercase()
                        }
                    }
                    company?.let {
                        carrierNameView.text = it.companyName ?: "N/A"
                    }
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Error fetching driver review: ${result.message}")
                }
                else -> {}
            }
        }

        // Observer for carrier data (reusing same logic as Log Fragment)
        homeViewModel.company.observe(viewLifecycleOwner) { result ->
            if (result is NetworkResult.Success && result.data?.results != null) {
                val company = result.data.results
                carrierNameView.text = company.company_name ?: "N/A"
                dotNumberView.text = "DOT: ${company.dot_no ?: "N/A"}"
                
                val addressParts = mutableListOf<String>()
                if (!company.address.isNullOrBlank()) addressParts.add(company.address)
                if (!company.city.isNullOrBlank()) addressParts.add(company.city)
                if (!company.state.isNullOrBlank()) addressParts.add(company.state)
                if (!company.zip.isNullOrBlank()) addressParts.add(company.zip)
                if (!company.country.isNullOrBlank()) addressParts.add(company.country)
                
                addressView.text = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "N/A"
                phoneView.text = "Phone: ${company.phone_no ?: "N/A"}"
            }
        }

        // Fetch both driver and company data
        context?.let { 
            homeViewModel.getDriverReview(it)
            homeViewModel.getCompanyName(it)
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Premium Animation
        YoYo.with(Techniques.BounceInUp)
            .duration(800)
            .playOn(dialogRoot)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun fetchLocationAndSendLoginLog() {
        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireContext())
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            sendLoginLog(0.0, 0.0)
            return
        }
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                sendLoginLog(loc.latitude, loc.longitude)
            } else {
                val cts = com.google.android.gms.tasks.CancellationTokenSource()
                fused.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { l -> sendLoginLog(l?.latitude ?: 0.0, l?.longitude ?: 0.0) }
                    .addOnFailureListener { sendLoginLog(0.0, 0.0) }
            }
        }.addOnFailureListener { sendLoginLog(0.0, 0.0) }
    }

    private fun sendLoginLog(lat: Double, lng: Double) {
        val logRequest = AddLogRequest(
            modename = "login",
            odometerreading = "0",
            lat = lat,
            long = lng,
            location = lat != 0.0 || lng != 0.0,
            eng_hours = "0",
            vin = "",
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1,
            connection_status = "disconnected"
        )
        context?.let { homeViewModel.logUser(logRequest, it) }
    }
}
