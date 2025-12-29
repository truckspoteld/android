//package com.truckspot.fragment.ui.home
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.app.Dialog
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.Color
//import android.location.Location
//import android.media.MediaPlayer
//import android.os.Build
//import android.os.Bundle
//import android.text.format.DateFormat
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.View.*
//import android.view.ViewGroup
//import android.view.WindowManager
//import android.view.animation.AnimationUtils
//import android.widget.*
//import android.widget.Toast.LENGTH_LONG
//import android.widget.Toast.makeText
//import androidx.appcompat.widget.AppCompatSpinner
//import androidx.appcompat.widget.PopupMenu
//import androidx.core.content.res.ResourcesCompat.getColor
//import androidx.core.content.res.ResourcesCompat.getDrawable
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentActivity
//import androidx.fragment.app.activityViewModels
//import androidx.fragment.app.viewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance
//import com.daimajia.androidanimations.library.Techniques
//import com.daimajia.androidanimations.library.YoYo
//import com.truckspot.R
//import com.truckspot.R.drawable.home_bg_deign
//import com.truckspot.R.drawable.home_bg_design_selected
//import com.truckspot.R.layout.*
//import com.truckspot.R.string.shipping_number
//import com.truckspot.R.string.trailer_number
//import com.truckspot.databinding.FragmentHomeBinding
//import com.truckspot.fragment.Dashboard
//import com.truckspot.fragment.ui.viewmodels.DashboardViewModel
//import com.truckspot.LoginActivity
//import com.truckspot.models.DRIVE_MODE.*
//import com.truckspot.models.UserLog
//import com.truckspot.pt.devicemanager.AppModel
//import com.truckspot.request.AddLogRequest
//import com.truckspot.request.updateLogRequest
//import com.truckspot.utils.*
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_DRIVING
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_OFF
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_ON
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_PERSONAL
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_SLEEPING
//import com.truckspot.utils.Utils.dialogInterface
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import org.joda.time.DateTime
//import java.math.BigDecimal
//import java.math.BigDecimal.valueOf
//import java.math.RoundingMode.FLOOR
//import java.text.SimpleDateFormat
//import java.util.*
//import androidx.annotation.RequiresApi
//import com.google.android.material.progressindicator.CircularProgressIndicator
//import com.google.gson.Gson
//import com.truckspot.models.HomeDataModel
//import com.truckspot.request.AddOffsetRequest
//import com.truckspot.utils.PrefConstants.TRUCK_MODE_YARD
//import com.truckspot.utils.Utils.getDouble
//import com.truckspot.utils.Utils.toHoursMinutesFormate
//import com.whizpool.supportsystem.SLog
//import java.time.LocalDateTime
//import java.time.ZoneId
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class HomeFragment : Fragment(), OnClickListener {
//    companion object {
//        private const val TAG = "HomeFragment_VIN"
//        val isTesting = true
//    }
//
//    private var _binding: FragmentHomeBinding? = null
//
//    @Inject
//    lateinit var prefRepository: PrefRepository
//    var mActivity: FragmentActivity? = null
//    private val binding
//        get() = _binding ?: run {
//            val inflater = LayoutInflater.from(context)
//            FragmentHomeBinding.inflate(inflater).also { _binding = it }
//        }
//    private val homeViewModel by viewModels<HomeViewModel>()
//    private val dashboardViewModel by activityViewModels<DashboardViewModel>()
//
//    private var clockJob: Job? = null
//    private var locationJob: Job? = null
//    private var timeZone: String = ""
//    private var lastLogTime: String = ""
//    private var lastLogMode: String = ""
//    private var lastEngineApiCallTime: Long = 0
//    private val ENGINE_API_DEBOUNCE_MS = 10000L // 10 seconds debounce
//    private var lastSpeedCheckTime: Long = 0
//    private val SPEED_CHECK_THROTTLE_MS = 2000L // 2 seconds throttle
//    private var lastPushedEngineState: String = ""
//    private var engineStateStableCount: Int = 0
//    private val ENGINE_STATE_STABLE_THRESHOLD = 3
//
//    private lateinit var mediaPlayer: MediaPlayer
//
//    // Bluetooth connection state tracking
//    private var isBluetoothConnecting = false
//    private var bluetoothConnectionJob: Job? = null
//
//    // API call debouncing
//    private var lastApiCallTime: Long = 0
//    private val API_CALL_DEBOUNCE_MS = 5000L // 5 seconds debounce for API calls
//    private var isFetchingLogs = false // Flag to prevent concurrent API calls
//
//    var hrs_MODE_OFF = 0.0
//    var hrs_MODE_ON = 0.0
//    var hrs_MODE_D = 0.0
//    var hrs_MODE_SB = 0.0
//
//    var miles: BigDecimal? = null
//    var mOdometer: String? = null
//    var mEngineHours: String? = null
//    var location: String? = null
//
//    var isEmptyList = false
//    lateinit var yoYo: YoYo.AnimationComposer
//
//    val svcIf = IntentFilter()
//    val tmIf = IntentFilter()
//
//    // Variables to prevent rapid mode changes
//    private var lastModeChangeTime: Long = 0
//    private var isModeChangeInProgress = false
//    private var lastModeChangeTimeout: Job? = null
//    private val MODE_CHANGE_DEBOUNCE_MS = 3000L // 3 seconds debounce
//    private val MODE_CHANGE_TIMEOUT_MS = 30000L // 30 seconds timeout for API call
//
//    var lastEngineLogName: String = ""
//    var tmRefresh: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            Log.d(TAG, "tmRefresh broadcast")
//            if (!isBluetoothConnecting) {
//                updateTelemetryInfo()
//            }
//        }
//    }
//    var svcRefresh: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            Log.d(TAG, "svcRefresh broadcast")
//            if (!isBluetoothConnecting) {
//                updateTelemetryInfo()
//            }
//        }
//    }
//    var viRefresh: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            updateVehicleInfo()
//        }
//    }
//
//    // Speed monitoring broadcast receiver
//    var speedRefresh: BroadcastReceiver = object : BroadcastReceiver() {
//        @RequiresApi(Build.VERSION_CODES.O)
//        override fun onReceive(context: Context, intent: Intent) {
//            checkAndPrintSpeed()
//        }
//    }
//    private lateinit var progressBar: CircularProgressIndicator
//    private lateinit var progressBarShift: CircularProgressIndicator
//    private lateinit var cycleRemaining: CircularProgressIndicator
//    private lateinit var untilBreak: CircularProgressIndicator
//    private var selectedLog: String = TRUCK_MODE_OFF
//    private var observersSet = false
//
//    @SuppressLint("SuspiciousIndentation")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // Cancel jobs when fragment is not in the foreground
//        clockJob?.cancel()
//        bluetoothConnectionJob?.cancel()
//        lastModeChangeTimeout?.cancel()
//    }
//
//    @SuppressLint("SuspiciousIndentation", "ResourceAsColor")
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
//        val speedIf = IntentFilter()
//        speedIf.addAction("REFRESH")
//
//        svcIf.addAction("SVC-BOUND-REFRESH")
//        tmIf.addAction("REFRESH")
//        tmIf.addAction("TRACKER-REFRESH")
//
//        getInstance(requireContext()).registerReceiver(tmRefresh, tmIf)
//        getInstance(requireContext()).registerReceiver(svcRefresh, svcIf)
//        getInstance(requireContext()).registerReceiver(speedRefresh, speedIf)
//
//        progressBar = rootView.findViewById(R.id.progressBarMain)
//        progressBarShift = rootView.findViewById(R.id.progressBarShift)
//        cycleRemaining = rootView.findViewById(R.id.progressBarCycle)
//        untilBreak = rootView.findViewById(R.id.progressBarBreak)
//        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.clicksoud)
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        mActivity = activity
//        yoYo = YoYo.with(Techniques.Tada)
//        prefRepository = PrefRepository(mActivity!!)
//        binding.btnOff.setOnClickListener(this)
//        binding.btnSleep.setOnClickListener(this)
//        binding.btnDrive.setOnClickListener(this)
//        binding.btnOn.setOnClickListener(this)
//        binding.tvPerosnaluse.setOnClickListener(this)
//        binding.tvYarduse.setOnClickListener(this)
//
//        binding.settingsIcon.setOnClickListener {
//            showSettingsPopupMenu(it)
//        }
//
//        val defaultVin = "---------"
//        val vehicleInfo = AppModel.getInstance().mVehicleInfo
//        val vin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: defaultVin
//        binding.vinNumber.text = vin
//        Log.d("VIN_DEBUG", "Initial VIN set to: $vin")
//        homeViewModel.getOffSet(prefRepository, vin, requireContext())
//        homeViewModel.getCompanyName(requireContext())
//        binding.name.text = prefRepository.getName()
//
//        binding.drivingSpeed.text = "Driving Speed: 0 km/h"
//        binding.companyName.text = "Company Name: Loading..."
//
//        val userName = prefRepository.getName()
//        if (userName.isNotEmpty()) {
//            val firstLetter = userName.first().uppercase()
//            binding.profileInitial.text = firstLetter
//        } else {
//            binding.profileInitial.text = "U"
//        }
//        homeViewModel.connectSocket(prefRepository.getDriverId())
//
//        if (!observersSet) {
//            setupObservers()
//            observersSet = true
//        }
//
//        binding.editShipping.setOnClickListener { editShippingNumberPopup() }
//        binding.editCoDriver.setOnClickListener { editCoDriverPopup() }
//        binding.editTrailerNo.setOnClickListener { editTrailerNumberPopup() }
//        updateUI()
//        return _binding!!.root
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun setupObservers() {
//        homeViewModel.company.observe(viewLifecycleOwner) {
//            if (it.data?.results == null) return@observe
//            timeZone = it.data.results.company_timezone ?: ""
//            prefRepository.setTimeZone(timeZone)
//            binding.ManufactureName.text = it.data.results.company_name
//            binding.companyName.text = "Company Name: ${it.data.results.company_name}"
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                updateClockDisplay()
//            }
//        }
//
//        homeViewModel.homeLiveData.observe(viewLifecycleOwner) {
//            when (it) {
//                is NetworkResult.Success<*> -> {
//                    binding.progressBar.visibility = View.GONE
//                    isFetchingLogs = false
//
//                    if (it.data?.logs == null || it.data.logs!!.isEmpty()) {
//                        Toast.makeText(context, "Logs are Empty", Toast.LENGTH_SHORT).show()
//                        return@observe
//                    }
//
//                    updateGauges(it.data)
//                    updateViolationTimeCard(it.data?.conditions)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        updateClockDisplay()
//                    }
//                    updateUIBasedOnLogs(it.data)
//                }
//                is NetworkResult.Error<*> -> {
//                    binding.progressBar.visibility = View.GONE
//                    isFetchingLogs = false
//                    showValidationErrors(it.message.toString())
//                }
//                is NetworkResult.Loading<*> -> {
//                    // Only show if not already showing
//                    if (binding.progressBar.visibility != View.VISIBLE) {
//                        binding.progressBar.visibility = View.VISIBLE
//                    }
//                }
//            }
//        }
//
//        homeViewModel.addLogReponse.observe(viewLifecycleOwner) {
//            when (it) {
//                is NetworkResult.Success<*> -> {
//                    Log.d(TAG, "Log added successfully")
//                    // Reset API in progress flag
//                    isModeChangeApiInProgress = false
//
//                    // Update UI
//                    updateUIAfterModeChange(selectedLog.ifEmpty { TRUCK_MODE_OFF })
//
//                    // Fetch updated logs after successful mode change
//                    if (!isFetchingLogs) {
//                        isFetchingLogs = true
//                        homeViewModel.getHome(requireContext())
//                    }
//                }
//                is NetworkResult.Error<*> -> {
//                    Log.e(TAG, "Error adding log: ${it.message}")
//                    // Reset flags
//                    isModeChangeApiInProgress = false
//                    isModeChangeInProgress = false
//                    binding.progressBar.visibility = View.GONE
//                    showValidationErrors(it.message.toString())
//                }
//                is NetworkResult.Loading<*> -> {
//                    Log.d(TAG, "Adding log...")
//                }
//            }
//        }
//    }
//
//    private fun updateGauges(data: HomeDataModel?) {
//        val driveText = view?.findViewById<TextView>(R.id.timeText1)
//        val shiftText = view?.findViewById<TextView>(R.id.timeText2)
//        val cycleText = view?.findViewById<TextView>(R.id.timeText3)
//        val breakText = view?.findViewById<TextView>(R.id.timeTextBreak)
//
//        val driveBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarMain)
//        val shiftBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarShift)
//        val cycleBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarCycle)
//        val breakBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarBreak)
//
//        val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
//
//        if (data?.conditions != null) {
//            if (data.conditions!!.driveViolation!!) {
//                driveText!!.text = ("Voilation")
//                driveText.setTextColor(Color.RED)
//                driveBar?.startAnimation(blinkAnimation)
//                driveBar?.setIndicatorColor(Color.RED)
//                driveBar?.progress = 0
//            } else {
//                val totalMinutes = 11 * 60
//                val remaining = totalMinutes - (data.conditions?.drive ?: 0)
//                val safeSpent = remaining.coerceIn(0, totalMinutes)
//                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
//                driveBar?.clearAnimation()
//                driveText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
//                driveBar?.max = 100
//                driveBar?.progress = progressPercent
//                val color = when (progressPercent) {
//                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
//                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
//                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
//                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
//                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
//                }
//                driveBar?.setIndicatorColor(color)
//                driveText?.text = data.conditions?.drive?.toHoursMinutesFormate()
//            }
//
//            if (data.conditions!!.cycleViolation ?: false) {
//                cycleText!!.text = ("Voilation")
//                cycleText.setTextColor(Color.RED)
//                cycleBar?.startAnimation(blinkAnimation)
//                cycleBar?.setIndicatorColor(Color.RED)
//                cycleBar?.progress = 0
//            } else {
//                val totalMinutes = 70 * 60
//                val remaining = totalMinutes - (data.conditions?.cycle ?: 0)
//                val safeSpent = remaining.coerceIn(0, totalMinutes)
//                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
//                cycleBar?.clearAnimation()
//                cycleText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
//                cycleBar?.max = 100
//                cycleBar?.progress = progressPercent
//                val color = when (progressPercent) {
//                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
//                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
//                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
//                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
//                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
//                }
//                cycleBar?.setIndicatorColor(color)
//                cycleText?.text = data.conditions?.cycle?.toHoursMinutesFormate()
//            }
//
//            if (data.conditions!!.shiftViolation ?: false) {
//                shiftText!!.text = ("Voilation")
//                shiftText.setTextColor(Color.RED)
//                shiftBar?.startAnimation(blinkAnimation)
//                shiftBar?.setIndicatorColor(Color.RED)
//                shiftBar?.progress = 0
//            } else {
//                val totalMinutes = 14 * 60
//                val remaining = totalMinutes - (data.conditions?.shift ?: 0)
//                val safeSpent = remaining.coerceIn(0, totalMinutes)
//                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
//                shiftBar?.clearAnimation()
//                shiftText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
//                shiftBar?.max = 100
//                shiftBar?.progress = progressPercent
//                val color = when (progressPercent) {
//                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
//                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
//                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
//                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
//                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
//                }
//                shiftBar?.setIndicatorColor(color)
//                shiftText?.text = data.conditions?.shift?.toHoursMinutesFormate()
//            }
//
//            if (data.conditions!!.driveBreakViolation ?: false) {
//                breakText!!.text = ("Voilation")
//                breakText.setTextColor(Color.RED)
//                breakBar?.startAnimation(blinkAnimation)
//                breakBar?.setIndicatorColor(Color.RED)
//                breakBar?.progress = 0
//            } else {
//                val totalMinutes = 8 * 60
//                val remaining = totalMinutes - (data.conditions?.drivebreak ?: 0)
//                val safeSpent = remaining.coerceIn(0, totalMinutes)
//                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
//                breakBar?.clearAnimation()
//                breakText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
//                breakBar?.max = 100
//                breakBar?.progress = progressPercent
//                val color = when (progressPercent) {
//                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
//                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
//                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
//                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
//                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
//                }
//                breakBar?.setIndicatorColor(color)
//                breakText?.text = data.conditions?.drivebreak?.toHoursMinutesFormate()
//            }
//        }
//    }
//    private fun forceUnlockUI() {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//            isModeChangeInProgress = false
//            isModeChangeApiInProgress = false
//            isFetchingLogs = false
//            binding.progressBar.visibility = View.GONE
//            lastModeChangeTimeout?.cancel()
//            Log.d(TAG, "Force unlocked UI")
//        }
//    }
//    private fun updateUIAfterModeChange(mode: String) {
//        when (mode) {
//            TRUCK_MODE_OFF -> {
//                homeViewModel.trackingMode.set(MODE_OFF)
//                updateUI(binding.btnOff)
//            }
//            TRUCK_MODE_ON -> {
//                homeViewModel.trackingMode.set(MODE_ON)
//                updateUI(binding.btnOn)
//            }
//            TRUCK_MODE_SLEEPING -> {
//                homeViewModel.trackingMode.set(MODE_SB)
//                updateUI(binding.btnSleep)
//            }
//            TRUCK_MODE_DRIVING -> {
//                homeViewModel.trackingMode.set(MODE_D)
//                updateUI(binding.btnDrive)
//            }
//            TRUCK_MODE_YARD -> {
//                updateUI(binding.tvYarduse)
//            }
//            TRUCK_MODE_PERSONAL -> {
//                updateUI(binding.tvPerosnaluse)
//            }
//        }
//    }
//
//    private fun updateUIBasedOnLogs(data: HomeDataModel?) {
//        var logList: MutableList<ELDGraphData>? = mutableListOf()
//        logList?.clear()
//        data?.previousDayLog?.let {
//            logList?.add(
//                ELDGraphData(
//                    0.0.toFloat(),
//                    it.modename,
//                    0.0.toLong()
//                )
//            )
//        }
//        data?.logs?.forEach {
//            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
//            logList?.add(
//                ELDGraphData(
//                    time,
//                    it.modename,
//                    time.toLong()
//                )
//            )
//        }
//        data?.latestUpdatedLog?.let {
//            val time: Float = AlertCalculationUtils.refinedTimeStringToFloat(it.time ?: "0")
//            logList?.add(
//                ELDGraphData(
//                    time,
//                    it.modename,
//                    time.toLong()
//                )
//            )
//        }
//        lastEngineLogName = data?.logs?.lastOrNull { it.modename == "ENG_ON" || it.modename == "ENG_OFF" }?.modename ?: ""
//
//        val filterForSelection = logList?.filter { it.status != "login" && it.status != "logout" }
//
//        if (filterForSelection != null && filterForSelection.isNotEmpty()) {
//            when (filterForSelection.last()?.status) {
//                TRUCK_MODE_OFF -> updateUI(binding.btnOff)
//                TRUCK_MODE_ON -> updateUI(binding.btnOn)
//                TRUCK_MODE_SLEEPING -> updateUI(binding.btnSleep)
//                TRUCK_MODE_DRIVING -> updateUI(binding.btnDrive)
//                TRUCK_MODE_YARD -> updateUI(binding.tvYarduse)
//                TRUCK_MODE_PERSONAL -> updateUI(binding.tvPerosnaluse)
//            }
//        }
//    }
//
//    private fun showConnectToDeviceDialog(
//        actualOdo: Double,
//        actualEngHours: Double,
//        mostRecentLogWithValidOdometer: UserLog,
//    ) {
//        val alertDialogBuilder = AlertDialog.Builder(requireContext())
//        alertDialogBuilder.setTitle("Unidentified Logs Detected")
//        val additionalMessage = "Do you want to authorize these unidentified logs?\n\n"
//        val readingsMessage =
//            "Unidentified Odometer Reading: $actualOdo\nUnidentified Engine Hours Reading: $actualEngHours"
//        val finalMessage = "$additionalMessage$readingsMessage"
//        alertDialogBuilder.setMessage(finalMessage)
//
//        alertDialogBuilder.setPositiveButton("Yes") { dialog, _ ->
//            prefRepository.setShowUnidentifiedDialog(false)
//            prefRepository.setUnauthorized(true)
//            val unIdentifiedTime = actualOdo / 45.0
//            Log.d(TAG, "45MINUTESTIME:${unIdentifiedTime}")
//            var unIdentifiedOnHours = 0.0
//            val unIdentifiedDrivingHours: Double
//            if (unIdentifiedTime < actualEngHours) {
//                unIdentifiedOnHours = actualEngHours - unIdentifiedTime
//                unIdentifiedDrivingHours = unIdentifiedTime
//            } else
//                unIdentifiedDrivingHours = actualEngHours
//            val updateLogRequest = updateLogRequest(
//                mostRecentLogWithValidOdometer.id,
//                unIdentifiedDrivingHours,
//                mostRecentLogWithValidOdometer.end_DateTime,
//                mostRecentLogWithValidOdometer.modename,
//                mostRecentLogWithValidOdometer.odometerreading,
//                mostRecentLogWithValidOdometer.eng_hours,
//                mostRecentLogWithValidOdometer.time,
//                0
//            )
//            SLog.detailLogs(
//                "UPDATING LOG FROM DIALOG",
//                Gson().toJson(updateLogRequest) + "\n",
//                true
//            )
//            homeViewModel.updateLog(updateLogRequest, shouldHandleResponse = true, requireContext())
//            dialog.dismiss()
//        }
//
//        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
//            prefRepository.setShowUnidentifiedDialog(false)
//            var vin = ""
//            if (AppModel.getInstance().mVehicleInfo != null && AppModel.getInstance().mVehicleInfo.VIN != null) {
//                vin = AppModel.getInstance().mVehicleInfo.VIN
//            }
//            var odo = prefRepository.getDiffinOdo()
//            if (odo.isNullOrEmpty() || odo == "null") {
//                odo = "0"
//            }
//            var eng = prefRepository.getDiffinEng()
//            if (eng.isNullOrEmpty() || eng == "null") {
//                eng = "0"
//            }
//            homeViewModel.addOffset(
//                prefRepository,
//                AddOffsetRequest(
//                    odo.toInt().plus(actualOdo.toInt()),
//                    eng.toInt().plus(actualEngHours.toInt()),
//                    vin
//                ),
//                requireContext()
//            )
//            dialog.dismiss()
//        }
//        val alertDialog = alertDialogBuilder.create()
//        alertDialog.show()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onResume() {
//        super.onResume()
//        homeViewModel.connectSocket(prefRepository.getDriverId())
//        val context = requireContext()
//        val instance = getInstance(context)
//        logVinState()
//
//        instance.registerReceiver(tmRefresh, tmIf)
//        instance.registerReceiver(svcRefresh, svcIf)
//        val speedIf = IntentFilter()
//        speedIf.addAction("REFRESH")
//        instance.registerReceiver(speedRefresh, speedIf)
//
//        // Debounce and prevent concurrent API calls
//        val currentTime = System.currentTimeMillis()
//        if (!isFetchingLogs && currentTime - lastApiCallTime > API_CALL_DEBOUNCE_MS) {
//            isFetchingLogs = true
//            lastApiCallTime = currentTime
//            SLog.detailLogs("GETTING LOGS FROM HOME FRAGMENT ", "onResume\n", true)
//            homeViewModel.getHome(context)
//        }
//
//        updateTelemetryInfo()
//        startClock()
//    }
//
//    private fun logVinState() {
//        val vehicleInfo = AppModel.getInstance().mVehicleInfo
//        Log.d(TAG, "VehicleInfo exists: ${vehicleInfo != null}")
//        Log.d(TAG, "VIN exists: ${vehicleInfo?.VIN != null}")
//        Log.d(TAG, "VIN value: ${vehicleInfo?.VIN ?: "NULL"}")
//        Log.d(TAG, "VIN empty: ${vehicleInfo?.VIN?.isEmpty() ?: true}")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun handleLocationUpdate_1234(speed: Int, rpm: Int, name: String) {
//        Log.d(TAG, "handleLocationUpdate: Speed=$speed, RPM=$rpm, Event=$name")
//        if (isModeChangeInProgress) {
//            Log.d(TAG, "Mode change already in progress, skipping")
//            return
//        }
//
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastSpeedCheckTime < SPEED_CHECK_THROTTLE_MS) {
//            return
//        }
//        lastSpeedCheckTime = currentTime
//
//        val currentMode = getLastRelevantLogMode()
//        Log.d(TAG, "Current Mode: $currentMode")
//
//        if (name == "EV_POWER_ON") {
//            performModeChangeWithTimeout(hrs_MODE_D, "power_on", "")
//            return
//        } else if (name == "EV_POWER_OFF") {
//            performModeChangeWithTimeout(hrs_MODE_D, "power_off", "")
//            return
//        }
//
//        val newEngineState = if (rpm < 1) "ENG_OFF" else "ENG_ON"
//        if (newEngineState == lastEngineLogName) {
//            engineStateStableCount++
//        } else {
//            engineStateStableCount = 1
//            lastEngineLogName = newEngineState
//        }
//
//        if (engineStateStableCount >= ENGINE_STATE_STABLE_THRESHOLD &&
//            currentTime - lastEngineApiCallTime >= ENGINE_API_DEBOUNCE_MS
//        ) {
//            val apiStateToPush = if (newEngineState == "ENG_OFF") "eng_off" else "eng_on"
//            if (apiStateToPush != lastPushedEngineState) {
//                Log.d(TAG, "Engine state changed: $newEngineState -> $apiStateToPush")
//                lastEngineApiCallTime = currentTime
//                lastPushedEngineState = apiStateToPush
//                performModeChangeWithTimeout(hrs_MODE_D, apiStateToPush, "")
//                return
//            }
//        }
//
//        if (speed > 0 && currentMode != "d") {
//            if (currentTime - lastModeChangeTime > MODE_CHANGE_DEBOUNCE_MS) {
//                Log.d(TAG, "Switching to DRIVE mode due to speed > 0")
//                performModeChangeWithTimeout(hrs_MODE_D, "d", "Auto-switched due to speed > 0 km/h")
//            }
//        } else if (speed <= 0 && currentMode == "d") {
//            if (currentTime - lastModeChangeTime > MODE_CHANGE_DEBOUNCE_MS) {
//                Log.d(TAG, "Switching from DRIVE to ON mode due to speed <= 0")
//                performModeChangeWithTimeout(hrs_MODE_ON, "on", "Auto-switched due to speed <= 0 km/h")
//            }
//        }
//    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun handleLocationUpdate(speed: Int, rpm: Int, name: String) {
//        Log.d(TAG, "handleLocationUpdate: Speed=$speed, RPM=$rpm, Event=$name")
//
//        // Skip if any mode change is in progress
//        if (isModeChangeInProgress || isModeChangeApiInProgress) {
//            Log.d(TAG, "Mode change in progress, skipping location update")
//            return
//        }
//
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastSpeedCheckTime < SPEED_CHECK_THROTTLE_MS) {
//            return
//        }
//        lastSpeedCheckTime = currentTime
//
//        val currentMode = getLastRelevantLogMode()
//        Log.d(TAG, "Current Mode: $currentMode")
//
//        if (name == "EV_POWER_ON") {
//            performModeChangeWithTimeout(hrs_MODE_D, "power_on", "")
//            return
//        } else if (name == "EV_POWER_OFF") {
//            performModeChangeWithTimeout(hrs_MODE_D, "power_off", "")
//            return
//        }
//
//        val newEngineState = if (rpm < 1) "ENG_OFF" else "ENG_ON"
//        if (newEngineState == lastEngineLogName) {
//            engineStateStableCount++
//        } else {
//            engineStateStableCount = 1
//            lastEngineLogName = newEngineState
//        }
//
//        if (engineStateStableCount >= ENGINE_STATE_STABLE_THRESHOLD &&
//            currentTime - lastEngineApiCallTime >= ENGINE_API_DEBOUNCE_MS
//        ) {
//            val apiStateToPush = if (newEngineState == "ENG_OFF") "eng_off" else "eng_on"
//            if (apiStateToPush != lastPushedEngineState) {
//                Log.d(TAG, "Engine state changed: $newEngineState -> $apiStateToPush")
//                lastEngineApiCallTime = currentTime
//                lastPushedEngineState = apiStateToPush
//                performModeChangeWithTimeout(hrs_MODE_D, apiStateToPush, "")
//                return
//            }
//        }
//
//        // Auto-switch to driving mode only if currently not in driving
//        if (speed > 0 && currentMode != "d") {
//            if (currentTime - lastModeChangeTime > MODE_CHANGE_DEBOUNCE_MS) {
//                Log.d(TAG, "Switching to DRIVE mode due to speed > 0")
//                performModeChangeWithTimeout(hrs_MODE_D, "d", "Auto-switched due to speed > 0 km/h")
//            }
//        }
//        // Auto-switch from driving to on when speed drops to 0
//        else if (speed <= 0 && currentMode == "d") {
//            if (currentTime - lastModeChangeTime > MODE_CHANGE_DEBOUNCE_MS) {
//                Log.d(TAG, "Switching from DRIVE to ON mode due to speed <= 0")
//                performModeChangeWithTimeout(hrs_MODE_ON, "on", "Auto-switched due to speed <= 0 km/h")
//            }
//        }
//    }
//    private fun getLastRelevantLogMode(): String {
//        val logs = homeViewModel.homeLiveData.value?.data?.logs ?: emptyList()
//        val relevantLogTypes = listOf("d", "off", "sb", "on", "yard", "personal")
//        return logs.filter { relevantLogTypes.contains(it.modename?.lowercase()) }
//            .lastOrNull()?.modename?.lowercase() ?: ""
//    }
//
//    /**
//     * Performs a mode change with a timeout to prevent hanging.
//     * This is the key fix for the 5-minute hang issue.
//     */
//    private var isModeChangeApiInProgress = false
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun performModeChangeWithTimeout(hoursLast: Double, mode: String, selectedOptionText: String) {
//        if (isModeChangeInProgress || isModeChangeApiInProgress) {
//            Log.d(TAG, "Mode change already in progress, skipping new request")
//            return
//        }
//
//        isModeChangeInProgress = true
//        isModeChangeApiInProgress = true
//        lastModeChangeTime = System.currentTimeMillis()
//
//        // Show progress bar on main thread
//        binding.progressBar.post {
//            binding.progressBar.visibility = View.VISIBLE
//        }
//
//        // Cancel any previous timeout job
//        lastModeChangeTimeout?.cancel()
//
//        // Launch coroutine with proper error handling
//        lastModeChangeTimeout = viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                withTimeout(MODE_CHANGE_TIMEOUT_MS) {
//                    // Call the mode change function
//                    updateModeChange(hoursLast, mode, selectedOptionText)
//
//                    // Wait a bit for the API response to be processed
//                    delay(1000)
//                }
//            } catch (e: TimeoutCancellationException) {
//                Log.e(TAG, "Mode change timed out after $MODE_CHANGE_TIMEOUT_MS ms", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Mode change timed out. Please try again.", Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error during mode change", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//            } finally {
//                // ALWAYS reset flags and hide progress bar
//                withContext(Dispatchers.Main) {
//                    isModeChangeInProgress = false
//                    isModeChangeApiInProgress = false
//                    binding.progressBar.visibility = View.GONE
//                    Log.d(TAG, "Mode change completed, UI unlocked")
//                }
//            }
//        }
//    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    @SuppressLint("SuspiciousIndentation", "DefaultLocale")
//    private fun updateModeChange(hoursLast: Double, mode: String, selectedOptionText: String) {
//        try {
//            SLog.detailLogs("UPDATE MODE CHANGE FROM HOME FRAGMENT", mode + "\n", true)
//            if (mode == TRUCK_MODE_DRIVING) {
//                prefRepository.setShowUnidentifiedDialog(false)
//            }
//            Log.d(TAG, "updating mode --> $mode")
//            selectedLog = mode
//            prefRepository.setMode(mode)
//            val vin_no = AppModel.getInstance().mVehicleInfo?.VIN?.toString() ?: "1HGCM82633A004352"
//            val te = AppModel.getInstance().mLastEvent
//            val defaultLatitude = (activity as Dashboard).glat ?: 0.0
//            val defaultLongitude = (activity as Dashboard).glong ?: 0.0
//
//            val odoact = prefRepository.getDiffinOdo().toDoubleOrNull() ?: 0.0
//            val engact = prefRepository.getDiffinEng().toDoubleOrNull() ?: 0.0
//            val teOdometer = te?.mOdometer?.toDoubleOrNull() ?: 1.0
//            val teEngineHours = te?.mEngineHours?.toDoubleOrNull() ?: 1.0
//
//            var odometer_updated = teOdometer
//            var enghour_updated = teEngineHours
//            if (odoact != 0.0 && engact != 0.0) {
//                odometer_updated = teOdometer - odoact
//                enghour_updated = teEngineHours - engact
//            }
//
//            val miles = ((odometer_updated - getDouble(prefRepository.getDiffinOdo())) * 0.621371)
//            val roundedMiles = String.format("%.2f", miles)
//            val logRequest = AddLogRequest(
//                modename = mode,
//                odometerreading = roundedMiles,
//                lat = te?.mGeoloc?.latitude ?: defaultLatitude,
//                long = te?.mGeoloc?.longitude ?: defaultLongitude,
//                location = true,
//                eng_hours = (enghour_updated - getDouble(prefRepository.getDiffinEng())).toString(),
//                vin = vin_no,
//                is_active = 1,
//                is_autoinsert = 1,
//                eventcode = 1,
//                eventtype = 1
//            )
//
//            if (selectedOptionText == "yard") {
//                logRequest.discreption = "yard"
//            } else if (selectedOptionText == "personal") {
//                logRequest.discreption = "personal"
//            }
//
//            if (homeViewModel.getUserLogs().isNotEmpty()) {
//                val toDayDate = DateFormat.format("dd-MM-yyy", Date()).toString()
//                val lastLog = homeViewModel.getUserLogs().last()
//                val updateLogRequest = updateLogRequest(
//                    lastLog.id,
//                    hoursLast,
//                    toDayDate,
//                    lastLog.modename,
//                    lastLog.odometerreading,
//                    lastLog.eng_hours,
//                    lastLog.time,
//                    0
//                )
//                homeViewModel.updateLog(updateLogRequest, false, requireContext())
//            }
//            homeViewModel.logUser(logRequest, requireContext())
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in updateModeChange: ${e.message}", e)
//            // Re-throw to let the withTimeout block catch it
//            throw e
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        try {
//            getInstance(requireContext()).unregisterReceiver(tmRefresh)
//            getInstance(requireContext()).unregisterReceiver(svcRefresh)
//            getInstance(requireContext()).unregisterReceiver(viRefresh)
//            getInstance(requireContext()).unregisterReceiver(speedRefresh)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error unregistering receivers: ${e.message}")
//        }
//        clockJob?.cancel()
//        locationJob?.cancel()
//        bluetoothConnectionJob?.cancel()
//        lastModeChangeTimeout?.cancel()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun startClock() {
//        // Use lifecycleScope to automatically cancel the job when the fragment is destroyed
//        clockJob = viewLifecycleOwner.lifecycleScope.launch {
//            while (isActive) { // Use isActive to allow for graceful cancellation
//                delay(1000)
//                withContext(Dispatchers.Main) {
//                    updateClockDisplay()
//                }
//            }
//        }
//    }
//
//    private fun getCurrentTime(): String {
//        val currentTime = System.currentTimeMillis()
//        val dateTime = DateTime(currentTime)
//        val hour = dateTime.hourOfDay
//        val minute = dateTime.minuteOfHour
//        val second = dateTime.secondOfMinute
//        val amPm = if (hour < 12) "AM" else "PM"
//        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
//        return String.format("%02d:%02d:%02d %s", hour12, minute, second, amPm)
//    }
//
//    private fun getCurrentDate(): String {
//        val currentTime = System.currentTimeMillis()
//        val dateTime = DateTime(currentTime)
//        return dateTime.toString("yyyy-MM-dd")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun updateClockDisplay() {
//        if (timeZone.isEmpty()) {
//            Log.d(TAG, "No timezone set, using fallback clock")
//            binding.liveClock.text = getCurrentTime()
//            return
//        }
//        try {
//            val currentTimezoneTime = if (timeZone.isNotEmpty()) {
//                val timezoneMappings = mapOf(
//                    "PST" to "America/Los_Angeles",
//                    "AKST" to "America/Anchorage",
//                    "MST" to "America/Denver",
//                    "HST" to "Pacific/Honolulu",
//                    "CST" to "America/Chicago",
//                    "EST" to "America/New_York"
//                )
//                val mappedTimezone = timezoneMappings[timeZone] ?: "America/Los_Angeles"
//                val currentDateTime = LocalDateTime.now()
//                val systemZoneId = ZoneId.systemDefault()
//                val companyZoneId = ZoneId.of(mappedTimezone)
//                val zonedDateTime = currentDateTime.atZone(systemZoneId).withZoneSameInstant(companyZoneId)
//                val companyTime = zonedDateTime.toLocalTime()
//                String.format("%02d:%02d:%02d", companyTime.hour, companyTime.minute, companyTime.second)
//            } else {
//                getCurrentTime()
//            }
//            binding.liveClock.text = currentTimezoneTime
//
//            val lastLog = getLastRelevantLog()
//            if (lastLog != null) {
//                lastLogTime = lastLog.time ?: "00:00"
//                lastLogMode = lastLog.modename ?: ""
//                val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
//                binding.timerTv.text = elapsedTime
//                binding.timerLabelTv.text = "Time spent in ${lastLogMode.uppercase()}"
//            } else {
//                if (lastLogTime.isNotEmpty() && lastLogMode.isNotEmpty()) {
//                    val elapsedTime = calculateElapsedTime(currentTimezoneTime, lastLogTime)
//                    binding.timerTv.text = elapsedTime
//                    binding.timerLabelTv.text = "Time spent in ${lastLogMode.uppercase()}"
//                } else {
//                    binding.timerTv.text = currentTimezoneTime
//                    binding.timerLabelTv.text = "Current $timeZone time"
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating clock display: ${e.message}", e)
//            binding.timerTv.text = getCurrentTime()
//            binding.liveClock.text = getCurrentTime()
//        }
//    }
//
//    private fun getLastRelevantLog(): HomeDataModel.Log? {
//        val homeData = homeViewModel.homeLiveData.value?.data
//        val logs = homeData?.logs ?: emptyList()
//        val relevantLogTypes = listOf("d", "off", "sb", "on", "yard", "personal")
//        val filteredLogs = logs.filter { log ->
//            relevantLogTypes.contains(log.modename?.lowercase())
//        }
//        return filteredLogs.lastOrNull()
//    }
//
//    private fun calculateElapsedTime(currentTime: String, logTime: String): String {
//        return try {
//            val currentTimeParts = currentTime.split(":")
//            val logTimeParts = logTime.split(":")
//            if (currentTimeParts.size >= 2 && logTimeParts.size >= 2) {
//                val currentHour = currentTimeParts[0].toInt()
//                val currentMinute = currentTimeParts[1].toInt()
//                val currentSecond = if (currentTimeParts.size > 2) currentTimeParts[2].toInt() else 0
//                val logHour = logTimeParts[0].toInt()
//                val logMinute = logTimeParts[1].toInt()
//                val logSecond = if (logTimeParts.size > 2) logTimeParts[2].toInt() else 0
//                val currentTotalSeconds = currentHour * 3600 + currentMinute * 60 + currentSecond
//                val logTotalSeconds = logHour * 3600 + logMinute * 60 + logSecond
//                var elapsedSeconds = currentTotalSeconds - logTotalSeconds
//                if (elapsedSeconds < 0) {
//                    elapsedSeconds += 24 * 3600
//                }
//                val elapsedHours = elapsedSeconds / 3600
//                val elapsedMinutes = (elapsedSeconds % 3600) / 60
//                val elapsedSecondsRemainder = elapsedSeconds % 60
//                String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSecondsRemainder)
//            } else {
//                "00:00:00"
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error calculating elapsed time: ${e.message}", e)
//            "00:00:00"
//        }
//    }
//
//    var isNeedToconnect = true
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun showSettingsPopupMenu(anchorView: View) {
//        val popupMenu = PopupMenu(requireContext(), anchorView, Gravity.END)
//        popupMenu.menuInflater.inflate(R.menu.home_settings_menu, popupMenu.menu)
//        popupMenu.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.action_logout -> {
//                    performLogout()
//                    true
//                }
//                else -> false
//            }
//        }
//        popupMenu.show()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun performLogout() {
//        val logRequest = AddLogRequest(
//            modename = "logout",
//            odometerreading = 0.0.toString(),
//            lat = 0.0,
//            long = 0.0,
//            location = true,
//            eng_hours = 10.toString(),
//            vin = "1111",
//            is_active = 1,
//            is_autoinsert = 1,
//            eventcode = 1,
//            eventtype = 1
//        )
//        homeViewModel.logUser(logRequest, requireContext())
//        prefRepository.setLoggedIn(false)
//        prefRepository.setDifferenceinOdo("0")
//        prefRepository.setDifferenceinEnghours("0")
//        prefRepository.setToken("")
//        val intent = Intent(requireContext(), LoginActivity::class.java)
//        startActivity(intent)
//        requireActivity().finish()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onClick(v: View) =
//        if (mEngineHours.isNullOrEmpty() && isNeedToconnect && !isTesting) {
//            Utils.dialog(
//                requireContext(),
//                message = "Please connect to a device first",
//                negativeText = "Cancel",
//                callback = object : dialogInterface {
//                    override fun positiveClick() {
//                        (activity as Dashboard).binding.appBarDashboard.fab.performClick()
//                    }
//                    override fun negativeClick() {}
//                })
//        } else {
//            when (v.id) {
//                R.id.btnDrive -> drivingMode()
//                R.id.btnOff -> offMode()
//                R.id.btnOn -> onMode()
//                R.id.btnSleep -> sbMode()
//                R.id.tvYarduse -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    yardMode()
//                } else {
//                    TODO("VERSION.SDK_INT < O")
//                }
//                R.id.tvPerosnaluse -> personalMode()
//                else -> {}
//            }
//        }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun sbMode() {
//        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_SLEEPING) {
//            Utils.dialog(requireContext(), "Error", "Already in sleeping mode")
//            return
//        }
//        if (!isClickable()) {
//            Log.d(TAG, "Vehicle is running, unable to click")
//            return
//        }
//        if (MODE_SB != homeViewModel.trackingMode.get()!! || isEmptyList) {
//            modeChangeLog(TRUCK_MODE_SLEEPING)
//            val sbButton = binding.tvSb
//            mediaPlayer.start()
//            updateModeChange(hrs_MODE_SB, TRUCK_MODE_SLEEPING, "sb from click")
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun drivingMode() {
//        modeChangeLog(TRUCK_MODE_DRIVING)
//        updateUI(binding.btnDrive)
//        updateModeChange(hrs_MODE_D, TRUCK_MODE_DRIVING, "d from click")
//        mediaPlayer.start()
//    }
//
//    private fun modeChangeLog(mode: String) {
//        SLog.detailLogs("UPDATE MODE FROM CLICK", mode + "\n", true)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun onMode() {
//        val dialog = Dialog(requireContext())
//        dialog.setContentView(R.layout.menu_sb_menu)
//        val optionViews = (1..12).map { i ->
//            dialog.findViewById<TextView>(resources.getIdentifier("sb$i", "id", requireContext().packageName))
//        }
//        optionViews.forEach { optionView ->
//            optionView?.setOnClickListener {
//                val selectedOptionText = optionView.text.toString()
//                if (!isClickable() && prefRepository.getMode() == TRUCK_MODE_DRIVING) {
//                    Log.d(TAG, "Vehicle is running unable to click")
//                    Toast.makeText(context, "Vehicle is running unable to click", Toast.LENGTH_SHORT).show()
//                    dialog.dismiss()
//                    return@setOnClickListener
//                }
//                mediaPlayer.start()
//                if (MODE_ON != homeViewModel.trackingMode.get()!! || isEmptyList) {
//                    modeChangeLog(TRUCK_MODE_ON)
//                    updateUI(binding.btnOn)
//                    updateModeChange(hrs_MODE_ON, TRUCK_MODE_ON, selectedOptionText)
//                }
//                dialog.dismiss()
//            }
//        }
//        val location = IntArray(2)
//        binding.tvOn.getLocationOnScreen(location)
//        val x = location[0]
//        val y = location[1] - (dialog.window?.attributes?.height ?: 0)
//        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog.window?.setGravity(Gravity.TOP or Gravity.START)
//        dialog.window?.attributes?.x = x
//        dialog.window?.attributes?.y = y
//        dialog.show()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun offMode() {
//        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_OFF) {
//            Utils.dialog(requireContext(), "Error", "Already in off mode")
//            return
//        }
//        if (!isClickable()) {
//            Log.d(TAG, "Vehicle is running, unable to click")
//            return
//        }
//        modeChangeLog(TRUCK_MODE_OFF)
//        val dialog = Dialog(requireContext())
//        dialog.setContentView(R.layout.menu_of_menu)
//        val optionViews = (1..3).map { i ->
//            dialog.findViewById<TextView>(resources.getIdentifier("option$i", "id", requireContext().packageName))
//        }
//        optionViews.forEach { optionView ->
//            optionView?.setOnClickListener {
//                val selectedOptionText = optionView.text.toString()
//                updateModeChange(hrs_MODE_OFF, TRUCK_MODE_OFF, selectedOptionText)
//                dialog.dismiss()
//            }
//        }
//        val offButton = binding.tvOff
//        val location = IntArray(2)
//        offButton.getLocationOnScreen(location)
//        val x = location[0]
//        val y = location[1] - (dialog.window?.attributes?.height ?: 0)
//        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog.window?.setGravity(Gravity.TOP or Gravity.START)
//        dialog.window?.attributes?.x = x
//        dialog.window?.attributes?.y = y
//        dialog.show()
//        mediaPlayer.start()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun yardMode() {
//        if (MODE_YARD != homeViewModel.trackingMode.get()!! && prefRepository.getMode() == TRUCK_MODE_OFF) {
//            mediaPlayer.start()
//            modeChangeLog(TRUCK_MODE_YARD)
//            updateUI(binding.tvYarduse)
//            updateModeChange(0.0, TRUCK_MODE_YARD, "yard")
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun personalMode() {
//        if (!isClickable() && prefRepository.getMode() == TRUCK_MODE_PERSONAL && prefRepository.getMode() != TRUCK_MODE_ON) {
//            Log.d(TAG, "Vehicle is running unable to click")
//            return
//        }
//        if ((MODE_PERSONAL != homeViewModel.trackingMode.get()!! && prefRepository.getMode() == TRUCK_MODE_ON)) {
//            mediaPlayer.start()
//            modeChangeLog(TRUCK_MODE_PERSONAL)
//            updateUI(binding.tvPerosnaluse)
//            updateModeChange(0.0, TRUCK_MODE_PERSONAL, "personal")
//        }
//    }
//
//    private fun isClickable(): Boolean {
//        val speed = AppModel.getInstance().mLastEvent?.mGeoloc?.speed ?: 0
//        return speed <= 0
//    }
//
//    private fun updateUI(viewSelect: View? = null) {
//        binding.btnOff.background = getDrawable(resources, home_bg_deign, null)
//        binding.btnSleep.background = getDrawable(resources, home_bg_deign, null)
//        binding.btnDrive.background = getDrawable(resources, home_bg_deign, null)
//        binding.btnOn.background = getDrawable(resources, home_bg_deign, null)
//        binding.tvPerosnaluse.background = getDrawable(resources, home_bg_deign, null)
//        binding.tvPerosnaluse.setTextColor(getColor(resources, R.color.white, null))
//        binding.tvYarduse.background = getDrawable(resources, home_bg_deign, null)
//        binding.tvYarduse.setTextColor(getColor(resources, R.color.white, null))
//        viewSelect?.let {
//            it.background = getDrawable(resources, home_bg_design_selected, null)
//        }
//    }
//
//    private fun showValidationErrors(error: String, dialogInterface: dialogInterface? = null) {
//        Utils.dialog(requireContext(), message = error, callback = dialogInterface)
//    }
//
//    private fun updateTelemetryInfo() {
//        if (bluetoothConnectionJob?.isActive == true) {
//            Log.d(TAG, "Telemetry update already in progress")
//            return
//        }
//        try {
//            val dashboard = activity as? Dashboard ?: return
//            val appModel = AppModel.getInstance()
//            val lastEvent = appModel.mLastEvent
//            if (lastEvent != null) {
//                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_disconnect)
//                binding.tvConected.text = "Connected"
//                isNeedToconnect = false
//                bluetoothConnectionJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                    delay(500)
//                    withContext(Dispatchers.Main) {
//                        checkAndPrintSpeed()
//                    }
//                }
//            } else {
//                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_bluetooth)
//                binding.tvConected.text = "Not Connected"
//                isNeedToconnect = true
//                prefRepository.setShowUnidentifiedDialog(true)
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in updateTelemetryInfo: ${e.message}", e)
//        }
//    }
//
//    fun updateVehicleInfo() {
//        if (AppModel.getInstance().mVehicleInfo != null) {
//            val vehicleInfo = AppModel.getInstance().mVehicleInfo
//            val vin = vehicleInfo?.VIN?.takeIf { it.isNotEmpty() } ?: "NOT_AVAILABLE"
//            binding.vinNumber.text = vin
//            Log.d("VIN_DEBUG", "Vehicle info update - VIN: $vin")
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun checkAndPrintSpeed() {
//        try {
//            val currentTime = System.currentTimeMillis()
//            if (currentTime - lastSpeedCheckTime < SPEED_CHECK_THROTTLE_MS) {
//                return
//            }
//            lastSpeedCheckTime = currentTime
//            val appModel = AppModel.getInstance()
//            val lastEvent = appModel.mLastEvent ?: return
//            val speed = lastEvent.mGeoloc.speed
//            val dashboard = appModel.dashboard
//            val dashboardSpeed = dashboard?.engineSpeed ?: 0
//            val rpm = dashboard?.engineRPM ?: 0
//            binding.drivingSpeed.text = "Driving Speed: ${speed} km/h"
//            handleLocationUpdate(dashboardSpeed, rpm, lastEvent.mEvent.name)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in checkAndPrintSpeed: ${e.message}", e)
//        }
//    }
//
//    private fun updateUI() {
//        val numberText = prefRepository.getShippingNumber()
//        binding.shippingNumber.text = getString(shipping_number).plus(numberText)
//        val trailerText = prefRepository.getTrailerNumber()
//        binding.trailerNumber.text = getString(trailer_number).plus(trailerText)
//    }
//
//    private fun editShippingNumberPopup() {
//        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
//        val dialogView: View = layoutInflater.inflate(edit_shiping_number, null)
//        val dialog = builder.create()
//        dialog.setView(dialogView)
//        val shippingNumber: EditText = dialogView.findViewById(R.id.texture_shipping_number)
//        shippingNumber.setText(prefRepository.getShippingNumber())
//        dialogView.findViewById<Button>(R.id.cancel_shipping).setOnClickListener { dialog.dismiss() }
//        dialogView.findViewById<Button>(R.id.update_shipping).setOnClickListener {
//            val number = shippingNumber.text.toString()
//            if (number.isEmpty()) {
//                makeText(activity, "Shipping number required", LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//            dialog.dismiss()
//            prefRepository.setShippingNumber(number)
//            updateUI()
//        }
//        dialog.show()
//    }
//
//    private fun editTrailerNumberPopup() {
//        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
//        val dialogView: View = layoutInflater.inflate(edit_trailer_number, null)
//        val trailerNumber: EditText = dialogView.findViewById(R.id.trailer_number)
//        trailerNumber.setText(prefRepository.getTrailerNumber())
//        val dialog = builder.create()
//        dialogView.findViewById<Button>(R.id.action_cancel_trailer).setOnClickListener { dialog.dismiss() }
//        dialogView.findViewById<Button>(R.id.action_update_trailer).setOnClickListener {
//            val number = trailerNumber.text.toString()
//            if (number.isEmpty()) {
//                makeText(activity, "Trailer number required", LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//            dialog.dismiss()
//            prefRepository.setTrailerNumber(number)
//            updateUI()
//        }
//        dialog.setView(dialogView)
//        dialog.show()
//    }
//
//    private fun editCoDriverPopup() {
//        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
//        val dialogView: View = layoutInflater.inflate(edit_codriver, null)
//        val dialog = builder.create()
//        dialog.setView(dialogView)
//        val items = arrayOf("No Co-Driver")
//        val coDriverSpinner: AppCompatSpinner = dialogView.findViewById(R.id.spinner_co_driver)
//        val adapter = ArrayAdapter(requireActivity().applicationContext, android.R.layout.simple_spinner_item, items)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        coDriverSpinner.adapter = adapter
//        coDriverSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                // Handle selection if needed
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
//        dialogView.findViewById<Button>(R.id.cancel_codriver).setOnClickListener { dialog.dismiss() }
//        dialogView.findViewById<Button>(R.id.action_update_codriver).setOnClickListener { dialog.dismiss() }
//        dialog.show()
//    }
//
//    private fun updateViolationTimeCard(conditions: HomeDataModel.Conditions?) {
//        conditions?.let { cond ->
//            val closestViolation = findClosestViolation(cond)
//            if (closestViolation != null) {
//                binding.closestViolationCard.visibility = View.VISIBLE
//                binding.closestViolationType.text = closestViolation.type
//                binding.closestViolationTime.text = closestViolation.remainingTime
//                binding.closestViolationProgress.progress = closestViolation.progress
//                binding.closestViolationProgress.setIndicatorColor(getViolationColor(closestViolation.progress, closestViolation.isViolation))
//                val hours = closestViolation.remainingMinutes / 60
//                val minutes = closestViolation.remainingMinutes % 60
//                val timeText = when {
//                    hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
//                    minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
//                    else -> "imminent"
//                }
//                binding.closestViolationMessage.text = "${closestViolation.type} violation due in $timeText"
//            } else {
//                binding.closestViolationCard.visibility = View.GONE
//            }
//        } ?: run {
//            binding.closestViolationCard.visibility = View.GONE
//        }
//    }
//
//    private data class ViolationInfo(
//        val type: String,
//        val remainingMinutes: Int,
//        val remainingTime: String,
//        val progress: Int,
//        val isViolation: Boolean
//    )
//
//    private fun findClosestViolation(conditions: HomeDataModel.Conditions): ViolationInfo? {
//        val violations = mutableListOf<ViolationInfo>()
//        val driveRemaining = conditions.drive ?: 0
//        if (driveRemaining > 0) {
//            violations.add(ViolationInfo("Drive Time", driveRemaining, formatTimeFromMinutes(driveRemaining), calculateProgressFromRemaining(driveRemaining, 11 * 60), conditions.driveViolation ?: false))
//        }
//        val driveBreakRemaining = conditions.drivebreak ?: 0
//        if (driveBreakRemaining > 0) {
//            violations.add(ViolationInfo("Drive Break", driveBreakRemaining, formatTimeFromMinutes(driveBreakRemaining), calculateProgressFromRemaining(driveBreakRemaining, 8 * 60), conditions.driveBreakViolation ?: false))
//        }
//        val shiftRemaining = conditions.shift ?: 0
//        if (shiftRemaining > 0) {
//            violations.add(ViolationInfo("Shift Time", shiftRemaining, formatTimeFromMinutes(shiftRemaining), calculateProgressFromRemaining(shiftRemaining, 14 * 60), conditions.shiftViolation ?: false))
//        }
//        val cycleRemaining = conditions.cycle ?: 0
//        if (cycleRemaining > 0) {
//            violations.add(ViolationInfo("Cycle Time", cycleRemaining, formatTimeFromMinutes(cycleRemaining), calculateProgressFromRemaining(cycleRemaining, 70 * 60), conditions.cycleViolation ?: false))
//        }
//        return violations.minByOrNull { it.remainingMinutes }
//    }
//
//    private fun formatTimeFromMinutes(remainingMinutes: Int): String {
//        return if (remainingMinutes <= 0) "00:00" else {
//            val hours = remainingMinutes / 60
//            val minutes = remainingMinutes % 60
//            String.format("%02d:%02d", hours, minutes)
//        }
//    }
//
//    private fun calculateProgressFromRemaining(remainingMinutes: Int, totalMinutes: Int): Int {
//        return if (remainingMinutes <= 0) 100 else {
//            val spentMinutes = totalMinutes - remainingMinutes
//            val progress = (spentMinutes.toFloat() / totalMinutes * 100).toInt()
//            progress.coerceIn(0, 100)
//        }
//    }
//
//    private fun getViolationColor(progress: Int, isViolation: Boolean): Int {
//        return if (isViolation) {
//            ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
//        } else {
//            when (progress) {
//                in 0..34 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_green)
//                in 35..74 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_orange)
//                in 75..99 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
//                100 -> ContextCompat.getColor(requireContext(), R.color.dark_progress_red)
//                else -> ContextCompat.getColor(requireContext(), R.color.dark_progress_green)
//            }
//        }
//    }
//}





package com.truckspot.fragment.ui.home

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
import com.truckspot.R
import com.truckspot.R.drawable.home_bg_deign
import com.truckspot.R.drawable.home_bg_design_selected
import com.truckspot.R.layout.*
import com.truckspot.R.string.shipping_number
import com.truckspot.R.string.trailer_number
import com.truckspot.databinding.FragmentHomeBinding
import com.truckspot.fragment.Dashboard
import com.truckspot.fragment.ui.viewmodels.DashboardViewModel
import com.truckspot.LoginActivity
import com.truckspot.models.DRIVE_MODE.*
import com.truckspot.models.UserLog
import com.truckspot.pt.devicemanager.AppModel
import com.truckspot.request.AddLogRequest
import com.truckspot.request.updateLogRequest
import com.truckspot.utils.*
import com.truckspot.utils.PrefConstants.TRUCK_MODE_DRIVING
import com.truckspot.utils.PrefConstants.TRUCK_MODE_OFF
import com.truckspot.utils.PrefConstants.TRUCK_MODE_ON
import com.truckspot.utils.PrefConstants.TRUCK_MODE_PERSONAL
import com.truckspot.utils.PrefConstants.TRUCK_MODE_SLEEPING
import com.truckspot.utils.Utils.dialogInterface
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
import com.truckspot.models.HomeDataModel
import com.truckspot.request.AddOffsetRequest
import com.truckspot.utils.PrefConstants.TRUCK_MODE_YARD
import com.truckspot.utils.Utils.getDouble
import com.truckspot.utils.Utils.toHoursMinutesFormate
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
    private val binding
        get() = _binding ?: run {
            val inflater = LayoutInflater.from(context)
            FragmentHomeBinding.inflate(inflater).also { _binding = it }
        }
    private val homeViewModel by viewModels<HomeViewModel>()
    private val dashboardViewModel by activityViewModels<DashboardViewModel>()

    private var clockJob: Job? = null
    private var locationJob: Job? = null
    private var timeZone: String = ""
    private var lastLogTime: String = ""
    private var lastLogMode: String = ""
    private var lastEngineApiCallTime: Long = 0
    private val ENGINE_API_DEBOUNCE_MS = 10000L // 10 seconds debounce
    private var lastSpeedCheckTime: Long = 0
    private val SPEED_CHECK_THROTTLE_MS = 2000L // 2 seconds throttle
    private var lastPushedEngineState: String = ""
    private var engineStateStableCount: Int = 0
    private val ENGINE_STATE_STABLE_THRESHOLD = 3

    private lateinit var mediaPlayer: MediaPlayer

    // Bluetooth connection state tracking
    private var isBluetoothConnecting = false
    private var bluetoothConnectionJob: Job? = null

    // API call debouncing
    private var lastApiCallTime: Long = 0
    private val API_CALL_DEBOUNCE_MS = 5000L // 5 seconds debounce for API calls
    private var isFetchingLogs = false // Flag to prevent concurrent API calls

    var hrs_MODE_OFF = 0.0
    var hrs_MODE_ON = 0.0
    var hrs_MODE_D = 0.0
    var hrs_MODE_SB = 0.0

    var miles: BigDecimal? = null
    var mOdometer: String? = null
    var mEngineHours: String? = null
    var location: String? = null

    var isEmptyList = false
    lateinit var yoYo: YoYo.AnimationComposer

    val svcIf = IntentFilter()
    val tmIf = IntentFilter()

    // Variables to prevent rapid mode changes - ENHANCED
    private var lastModeChangeTime: Long = 0
    private var isModeChangeInProgress = false
    private var lastModeChangeTimeout: Job? = null
    private val MODE_CHANGE_DEBOUNCE_MS = 3000L // 3 seconds debounce
    private val MODE_CHANGE_TIMEOUT_MS = 30000L // 30 seconds timeout for API call

    // NEW: Track API call completion
    private var modeChangeCompletionDeferred: CompletableDeferred<Boolean>? = null
    private var isModeChangeApiInProgress = false
    private var failedModeChangeAttempts = 0
    private val MAX_FAILED_ATTEMPTS = 3

    // NEW: Emergency unlock timer
    private var emergencyUnlockJob: Job? = null

    var lastEngineLogName: String = ""
    var tmRefresh: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "tmRefresh broadcast")
            if (!isBluetoothConnecting) {
                updateTelemetryInfo()
            }
        }
    }
    var svcRefresh: BroadcastReceiver = object : BroadcastReceiver() {
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
        // Cancel jobs when fragment is not in the foreground
        clockJob?.cancel()
        bluetoothConnectionJob?.cancel()
        lastModeChangeTimeout?.cancel()
        emergencyUnlockJob?.cancel()
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

        if (!observersSet) {
            setupObservers()
            observersSet = true
        }

        binding.editShipping.setOnClickListener { editShippingNumberPopup() }
        binding.editCoDriver.setOnClickListener { editCoDriverPopup() }
        binding.editTrailerNo.setOnClickListener { editTrailerNumberPopup() }
        updateUI()
        return _binding!!.root
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
                    isFetchingLogs = false
                    if (it.data?.logs == null || it.data.logs!!.isEmpty()) {
                        Toast.makeText(context, "Logs are Empty", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    // Update UI components based on the fetched data
                    updateGauges(it.data)
                    updateViolationTimeCard(it.data?.conditions)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        updateClockDisplay()
                    }
                    updateUIBasedOnLogs(it.data)
                }
                is NetworkResult.Error<*> -> {
                    binding.progressBar.visibility = View.GONE
                    isFetchingLogs = false
                    showValidationErrors(it.message.toString())
                }
                is NetworkResult.Loading<*> -> {
                    if (binding.progressBar.visibility != View.VISIBLE && !isModeChangeInProgress) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }

        // CRITICAL: This observer completes the deferred for mode changes
        homeViewModel.addLogReponse.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success<*> -> {
                    Log.d(TAG, "✅ addLogReponse SUCCESS - Completing deferred")

                    // Complete the deferred with success
                    modeChangeCompletionDeferred?.complete(true)

                    // Update UI after mode change
                    updateUIAfterModeChange(selectedLog.ifEmpty { TRUCK_MODE_OFF })

                    // Fetch updated logs after successful mode change
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(500) // Small delay before fetching
                        if (!isFetchingLogs) {
                            isFetchingLogs = true
                            homeViewModel.getHome(requireContext())
                        }
                    }
                }
                is NetworkResult.Error<*> -> {
                    Log.e(TAG, "❌ addLogReponse ERROR: ${it.message}")

                    // Complete the deferred with failure
                    modeChangeCompletionDeferred?.complete(false)

                    showValidationErrors(it.message.toString())
                }
                is NetworkResult.Loading<*> -> {
                    Log.d(TAG, "⏳ addLogReponse LOADING")
                }
            }
        }
    }

    private fun updateGauges(data: HomeDataModel?) {
        val driveText = view?.findViewById<TextView>(R.id.timeText1)
        val shiftText = view?.findViewById<TextView>(R.id.timeText2)
        val cycleText = view?.findViewById<TextView>(R.id.timeText3)
        val breakText = view?.findViewById<TextView>(R.id.timeTextBreak)

        val driveBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarMain)
        val shiftBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarShift)
        val cycleBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarCycle)
        val breakBar = view?.findViewById<CircularProgressIndicator>(R.id.progressBarBreak)

        val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)

        if (data?.conditions != null) {
            if (data.conditions!!.driveViolation!!) {
                driveText!!.text = ("Voilation")
                driveText.setTextColor(Color.RED)
                driveBar?.startAnimation(blinkAnimation)
                driveBar?.setIndicatorColor(Color.RED)
                driveBar?.progress = 0
            } else {
                val totalMinutes = 11 * 60
                val remaining = totalMinutes - (data.conditions?.drive ?: 0)
                val safeSpent = remaining.coerceIn(0, totalMinutes)
                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
                driveBar?.clearAnimation()
                driveText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                driveBar?.max = 100
                driveBar?.progress = progressPercent
                val color = when (progressPercent) {
                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_drive_color)
                }
                driveBar?.setIndicatorColor(color)
                driveText?.text = data.conditions?.drive?.toHoursMinutesFormate()
            }

            if (data.conditions!!.cycleViolation ?: false) {
                cycleText!!.text = ("Voilation")
                cycleText.setTextColor(Color.RED)
                cycleBar?.startAnimation(blinkAnimation)
                cycleBar?.setIndicatorColor(Color.RED)
                cycleBar?.progress = 0
            } else {
                val totalMinutes = 70 * 60
                val remaining = totalMinutes - (data.conditions?.cycle ?: 0)
                val safeSpent = remaining.coerceIn(0, totalMinutes)
                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
                cycleBar?.clearAnimation()
                cycleText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                cycleBar?.max = 100
                cycleBar?.progress = progressPercent
                val color = when (progressPercent) {
                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                }
                cycleBar?.setIndicatorColor(color)
                cycleText?.text = data.conditions?.cycle?.toHoursMinutesFormate()
            }

            if (data.conditions!!.shiftViolation ?: false) {
                shiftText!!.text = ("Voilation")
                shiftText.setTextColor(Color.RED)
                shiftBar?.startAnimation(blinkAnimation)
                shiftBar?.setIndicatorColor(Color.RED)
                shiftBar?.progress = 0
            } else {
                val totalMinutes = 14 * 60
                val remaining = totalMinutes - (data.conditions?.shift ?: 0)
                val safeSpent = remaining.coerceIn(0, totalMinutes)
                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
                shiftBar?.clearAnimation()
                shiftText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                shiftBar?.max = 100
                shiftBar?.progress = progressPercent
                val color = when (progressPercent) {
                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                }
                shiftBar?.setIndicatorColor(color)
                shiftText?.text = data.conditions?.shift?.toHoursMinutesFormate()
            }

            if (data.conditions!!.driveBreakViolation ?: false) {
                breakText!!.text = ("Voilation")
                breakText.setTextColor(Color.RED)
                breakBar?.startAnimation(blinkAnimation)
                breakBar?.setIndicatorColor(Color.RED)
                breakBar?.progress = 0
            } else {
                val totalMinutes = 8 * 60
                val remaining = totalMinutes - (data.conditions?.drivebreak ?: 0)
                val safeSpent = remaining.coerceIn(0, totalMinutes)
                val progressPercent = (safeSpent.toFloat() / totalMinutes * 100).toInt()
                breakBar?.clearAnimation()
                breakText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                breakBar?.max = 100
                breakBar?.progress = progressPercent
                val color = when (progressPercent) {
                    in 0..34 -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                    in 35..74 -> ContextCompat.getColor(requireContext(), R.color.gauge_shift_color)
                    in 75..99 -> ContextCompat.getColor(requireContext(), R.color.red)
                    100 -> ContextCompat.getColor(requireContext(), R.color.gauge_cycle_color)
                    else -> ContextCompat.getColor(requireContext(), R.color.gauge_break_color)
                }
                breakBar?.setIndicatorColor(color)
                breakText?.text = data.conditions?.drivebreak?.toHoursMinutesFormate()
            }
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
        lastEngineLogName = data?.logs?.lastOrNull { it.modename == "ENG_ON" || it.modename == "ENG_OFF" }?.modename ?: ""

        val filterForSelection = logList?.filter { it.status != "login" && it.status != "logout" }

        if (filterForSelection != null && filterForSelection.isNotEmpty()) {
            when (filterForSelection.last()?.status) {
                TRUCK_MODE_OFF -> updateUI(binding.btnOff)
                TRUCK_MODE_ON -> updateUI(binding.btnOn)
                TRUCK_MODE_SLEEPING -> updateUI(binding.btnSleep)
                TRUCK_MODE_DRIVING -> updateUI(binding.btnDrive)
                TRUCK_MODE_YARD -> updateUI(binding.tvYarduse)
                TRUCK_MODE_PERSONAL -> updateUI(binding.tvPerosnaluse)
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
        homeViewModel.connectSocket(prefRepository.getDriverId())
        val context = requireContext()
        val instance = getInstance(context)
        logVinState()

        instance.registerReceiver(tmRefresh, tmIf)
        instance.registerReceiver(svcRefresh, svcIf)

        // Debounce and prevent concurrent API calls
        val currentTime = System.currentTimeMillis()
        if (!isFetchingLogs && currentTime - lastApiCallTime > API_CALL_DEBOUNCE_MS) {
            isFetchingLogs = true
            lastApiCallTime = currentTime
            homeViewModel.getHome(context)
        }

        updateTelemetryInfo()
        startClock()
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
        Log.d(TAG, "📍 handleLocationUpdate: Speed=$speed, RPM=$rpm, Event=$name")

        // STRICT CHECK: Block ALL auto-switches if mode change in progress
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Log.d(TAG, "🚫 Blocked location update - mode change in progress")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpeedCheckTime < SPEED_CHECK_THROTTLE_MS) {
            return
        }
        lastSpeedCheckTime = currentTime

        val currentMode = getLastRelevantLogMode()
        Log.d(TAG, "Current Mode: $currentMode")

        if (name == "EV_POWER_ON") {
            Log.d(TAG, "🔌 Power ON event")
            performModeChangeWithTimeout(hrs_MODE_D, "power_on", "")
            return
        } else if (name == "EV_POWER_OFF") {
            Log.d(TAG, "🔌 Power OFF event")
            performModeChangeWithTimeout(hrs_MODE_D, "power_off", "")
            return
        }

        val newEngineState = if (rpm < 1) "ENG_OFF" else "ENG_ON"
        if (newEngineState == lastEngineLogName) {
            engineStateStableCount++
        } else {
            engineStateStableCount = 1
            lastEngineLogName = newEngineState
        }

        if (engineStateStableCount >= ENGINE_STATE_STABLE_THRESHOLD &&
            currentTime - lastEngineApiCallTime >= ENGINE_API_DEBOUNCE_MS
        ) {
            val apiStateToPush = if (newEngineState == "ENG_OFF") "eng_off" else "eng_on"
            if (apiStateToPush != lastPushedEngineState) {
                Log.d(TAG, "🔧 Engine state changed: $newEngineState")
                lastEngineApiCallTime = currentTime
                lastPushedEngineState = apiStateToPush
                performModeChangeWithTimeout(hrs_MODE_D, apiStateToPush, "")
                return
            }
        }

        // Only switch if enough time has passed since last mode change
        if (currentTime - lastModeChangeTime <= MODE_CHANGE_DEBOUNCE_MS) {
            Log.d(TAG, "⏸️ Too soon for mode change (debounce)")
            return
        }

        if (speed > 0 && currentMode != "d") {
            Log.d(TAG, "🚗 Auto-switching to DRIVE mode due to speed > 0")
            performModeChangeWithTimeout(hrs_MODE_D, "d", "Auto-switched due to speed > 0 km/h")
        } else if (speed <= 0 && currentMode == "d") {
            Log.d(TAG, "🛑 Auto-switching from DRIVE to ON mode due to speed <= 0")
            performModeChangeWithTimeout(hrs_MODE_ON, "on", "Auto-switched due to speed <= 0 km/h")
        }
    }

    private fun getLastRelevantLogMode(): String {
        val logs = homeViewModel.homeLiveData.value?.data?.logs ?: emptyList()
        val relevantLogTypes = listOf("d", "off", "sb", "on", "yard", "personal")
        return logs.filter { relevantLogTypes.contains(it.modename?.lowercase()) }
            .lastOrNull()?.modename?.lowercase() ?: ""
    }

    /**
     * ENHANCED: Performs a mode change with timeout and completion tracking
     * This prevents the 5-minute hang issue by:
     * 1. Using CompletableDeferred to wait for actual API response
     * 2. Having dual timeout system (30s normal, 40s emergency)
     * 3. Always cleaning up in finally block
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun performModeChangeWithTimeout(hoursLast: Double, mode: String, selectedOptionText: String) {
        // Check if already in progress
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Log.d(TAG, "❌ Mode change blocked - already in progress")
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

        // Check failed attempts
        if (failedModeChangeAttempts >= MAX_FAILED_ATTEMPTS) {
            Log.e(TAG, "❌ Too many failed attempts ($failedModeChangeAttempts), forcing reset")
            forceResetAllFlags()
            Toast.makeText(requireContext(), "Too many failed attempts. Please try again.", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "✅ Starting mode change to: $mode")

        // Set flags
        isModeChangeInProgress = true
        isModeChangeApiInProgress = true
        lastModeChangeTime = System.currentTimeMillis()

        // Create completion deferred for this operation
        modeChangeCompletionDeferred = CompletableDeferred()

        // Show progress bar
        binding.progressBar.post {
            binding.progressBar.visibility = View.VISIBLE
        }

        // Cancel previous jobs
        lastModeChangeTimeout?.cancel()
        emergencyUnlockJob?.cancel()

        // Start emergency unlock timer (20 seconds - absolute max)
        emergencyUnlockJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(20000) // 20 seconds emergency timeout
            if (isModeChangeApiInProgress) {
                Log.e(TAG, "🚨 EMERGENCY TIMEOUT - Force unlocking after 20s")
                forceResetAllFlags()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Operation timed out. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Main operation with timeout
        lastModeChangeTimeout = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Execute mode change
                withTimeout(MODE_CHANGE_TIMEOUT_MS) { // 30 second timeout
                    updateModeChange(hoursLast, mode, selectedOptionText)

                    // CRITICAL: Wait for API response with its own timeout to prevent infinite blocking
                    Log.d(TAG, "⏳ Waiting for API response...")
                    val success = withTimeoutOrNull(15000L) {
                        modeChangeCompletionDeferred?.await()
                    } ?: false

                    if (success) {
                        Log.d(TAG, "✅ Mode change completed successfully")
                        failedModeChangeAttempts = 0 // Reset counter on success
                    } else {
                        Log.e(TAG, "❌ Mode change failed or timed out")
                        failedModeChangeAttempts++
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "⏱️ Mode change timed out after ${MODE_CHANGE_TIMEOUT_MS}ms", e)
                failedModeChangeAttempts++
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Request timed out. Please try again.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error during mode change: ${e.message}", e)
                failedModeChangeAttempts++
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // ALWAYS cleanup - This is the key fix for the hang issue
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "🧹 Cleaning up mode change operation")
                    isModeChangeInProgress = false
                    isModeChangeApiInProgress = false
                    binding.progressBar.visibility = View.GONE
                    modeChangeCompletionDeferred = null
                }
                emergencyUnlockJob?.cancel()
            }
        }
    }

    /**
     * Force reset all flags - Used for emergency recovery
     */
    private fun forceResetAllFlags() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            Log.d(TAG, "🔄 Force resetting all flags")
            isModeChangeInProgress = false
            isModeChangeApiInProgress = false
            isFetchingLogs = false
            binding.progressBar.visibility = View.GONE
            lastModeChangeTimeout?.cancel()
            emergencyUnlockJob?.cancel()
            modeChangeCompletionDeferred?.complete(false)
            modeChangeCompletionDeferred = null

            // Reset failed attempts after 2 seconds
            delay(2000)
            failedModeChangeAttempts = 0
            Log.d(TAG, "✅ All flags reset complete")
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
            val vin_no = AppModel.getInstance().mVehicleInfo?.VIN?.toString() ?: "1HGCM82633A004352"
            val te = AppModel.getInstance().mLastEvent
            val defaultLatitude = (activity as Dashboard).glat ?: 0.0
            val defaultLongitude = (activity as Dashboard).glong ?: 0.0

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
                homeViewModel.updateLog(updateLogRequest, false, requireContext())
            }
            homeViewModel.logUser(logRequest, requireContext())

        } catch (e: Exception) {
            Log.e(TAG, "Error in updateModeChange: ${e.message}", e)
            throw e
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            getInstance(requireContext()).unregisterReceiver(tmRefresh)
            getInstance(requireContext()).unregisterReceiver(svcRefresh)
            // viRefresh is not registered, so don't unregister it
            getInstance(requireContext()).unregisterReceiver(speedRefresh)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers: ${e.message}")
        }
        clockJob?.cancel()
        locationJob?.cancel()
        bluetoothConnectionJob?.cancel()
        lastModeChangeTimeout?.cancel()
        emergencyUnlockJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastModeChangeTimeout?.cancel()
        emergencyUnlockJob?.cancel()
        modeChangeCompletionDeferred?.complete(false)
        // Release MediaPlayer to prevent memory leak
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startClock() {
        clockJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    updateClockDisplay()
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
        if (timeZone.isEmpty()) {
            binding.liveClock.text = getCurrentTime()
            return
        }
        try {
            val mappedTimezone = timezoneMappings[timeZone] ?: "America/Los_Angeles"
            val currentDateTime = LocalDateTime.now()
            val systemZoneId = ZoneId.systemDefault()
            val companyZoneId = ZoneId.of(mappedTimezone)
            val zonedDateTime = currentDateTime.atZone(systemZoneId).withZoneSameInstant(companyZoneId)
            val companyTime = zonedDateTime.toLocalTime()
            val currentTimezoneTime = String.format("%02d:%02d:%02d", companyTime.hour, companyTime.minute, companyTime.second)
            binding.liveClock.text = currentTimezoneTime

            val lastLog = getLastRelevantLog()
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

    private fun getLastRelevantLog(): HomeDataModel.Log? {
        val homeData = homeViewModel.homeLiveData.value?.data
        val logs = homeData?.logs ?: emptyList()
        val relevantLogTypes = listOf("d", "off", "sb", "on", "yard", "personal")
        val filteredLogs = logs.filter { log ->
            relevantLogTypes.contains(log.modename?.lowercase())
        }
        return filteredLogs.lastOrNull()
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
                R.id.tvYarduse -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    yardMode()
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                R.id.tvPerosnaluse -> personalMode()
                else -> {}
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sbMode() {
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_SLEEPING) {
            Utils.dialog(requireContext(), "Error", "Already in sleeping mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        if (MODE_SB != homeViewModel.trackingMode.get()!! || isEmptyList) {
            modeChangeLog(TRUCK_MODE_SLEEPING)
            mediaPlayer.start()
            performModeChangeWithTimeout(hrs_MODE_SB, TRUCK_MODE_SLEEPING, "sb from click")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drivingMode() {
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "🚫 Driving mode blocked - operation in progress")
            return
        }

        Log.d(TAG, "🚗 Manual switch to DRIVING mode")
        modeChangeLog(TRUCK_MODE_DRIVING)
        updateUI(binding.btnDrive)
        mediaPlayer.start()
        performModeChangeWithTimeout(hrs_MODE_D, TRUCK_MODE_DRIVING, "d from click")
    }

    private fun modeChangeLog(mode: String) {
        Log.d(TAG, "UPDATE MODE FROM CLICK: $mode")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMode() {
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

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
                    Toast.makeText(context, "Vehicle is running unable to click", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setOnClickListener
                }
                mediaPlayer.start()
                if (MODE_ON != homeViewModel.trackingMode.get()!! || isEmptyList) {
                    modeChangeLog(TRUCK_MODE_ON)
                    updateUI(binding.btnOn)
                    performModeChangeWithTimeout(hrs_MODE_ON, TRUCK_MODE_ON, selectedOptionText)
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
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (homeViewModel.getLastLogModeName() == TRUCK_MODE_OFF) {
            Utils.dialog(requireContext(), "Error", "Already in off mode")
            return
        }
        if (!isClickable()) {
            Log.d(TAG, "Vehicle is running, unable to click")
            return
        }
        modeChangeLog(TRUCK_MODE_OFF)
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.menu_of_menu)
        val optionViews = (1..3).map { i ->
            dialog.findViewById<TextView>(resources.getIdentifier("option$i", "id", requireContext().packageName))
        }
        optionViews.forEach { optionView ->
            optionView?.setOnClickListener {
                val selectedOptionText = optionView.text.toString()
                performModeChangeWithTimeout(hrs_MODE_OFF, TRUCK_MODE_OFF, selectedOptionText)
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
    private fun yardMode() {
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (MODE_YARD != homeViewModel.trackingMode.get()!! && prefRepository.getMode() == TRUCK_MODE_OFF) {
            mediaPlayer.start()
            modeChangeLog(TRUCK_MODE_YARD)
            updateUI(binding.tvYarduse)
            performModeChangeWithTimeout(0.0, TRUCK_MODE_YARD, "yard")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun personalMode() {
        if (isModeChangeInProgress || isModeChangeApiInProgress) {
            Toast.makeText(requireContext(), "Please wait, processing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isClickable() && prefRepository.getMode() == TRUCK_MODE_PERSONAL && prefRepository.getMode() != TRUCK_MODE_ON) {
            Log.d(TAG, "Vehicle is running unable to click")
            return
        }
        if ((MODE_PERSONAL != homeViewModel.trackingMode.get()!! && prefRepository.getMode() == TRUCK_MODE_ON)) {
            mediaPlayer.start()
            modeChangeLog(TRUCK_MODE_PERSONAL)
            updateUI(binding.tvPerosnaluse)
            performModeChangeWithTimeout(0.0, TRUCK_MODE_PERSONAL, "personal")
        }
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

    private fun updateTelemetryInfo() {
        if (bluetoothConnectionJob?.isActive == true) {
            return
        }
        try {
            val dashboard = activity as? Dashboard ?: return
            val appModel = AppModel.getInstance()
            val lastEvent = appModel.mLastEvent
            if (lastEvent != null) {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_disconnect)
                binding.tvConected.text = "Connected"
                isNeedToconnect = false
                bluetoothConnectionJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    delay(500)
                    withContext(Dispatchers.Main) {
                        checkAndPrintSpeed()
                    }
                }
            } else {
                dashboard.binding.appBarDashboard.fab.setIconResource(R.drawable.ic_action_bluetooth)
                binding.tvConected.text = "Not Connected"
                isNeedToconnect = true
                prefRepository.setShowUnidentifiedDialog(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateTelemetryInfo: ${e.message}", e)
        }
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndPrintSpeed: ${e.message}", e)
        }
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
                val hours = closestViolation.remainingMinutes / 60
                val minutes = closestViolation.remainingMinutes % 60
                val timeText = when {
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
            violations.add(ViolationInfo("Drive Time", driveRemaining, formatTimeFromMinutes(driveRemaining), calculateProgressFromRemaining(driveRemaining, 11 * 60), conditions.driveViolation ?: false))
        }
        val driveBreakRemaining = conditions.drivebreak ?: 0
        if (driveBreakRemaining > 0) {
            violations.add(ViolationInfo("Drive Break", driveBreakRemaining, formatTimeFromMinutes(driveBreakRemaining), calculateProgressFromRemaining(driveBreakRemaining, 8 * 60), conditions.driveBreakViolation ?: false))
        }
        val shiftRemaining = conditions.shift ?: 0
        if (shiftRemaining > 0) {
            violations.add(ViolationInfo("Shift Time", shiftRemaining, formatTimeFromMinutes(shiftRemaining), calculateProgressFromRemaining(shiftRemaining, 14 * 60), conditions.shiftViolation ?: false))
        }
        val cycleRemaining = conditions.cycle ?: 0
        if (cycleRemaining > 0) {
            violations.add(ViolationInfo("Cycle Time", cycleRemaining, formatTimeFromMinutes(cycleRemaining), calculateProgressFromRemaining(cycleRemaining, 70 * 60), conditions.cycleViolation ?: false))
        }
        return violations.minByOrNull { it.remainingMinutes }
    }

    private fun formatTimeFromMinutes(remainingMinutes: Int): String {
        return if (remainingMinutes <= 0) "00:00" else {
            val hours = remainingMinutes / 60
            val minutes = remainingMinutes % 60
            String.format("%02d:%02d", hours, minutes)
        }
    }

    private fun calculateProgressFromRemaining(remainingMinutes: Int, totalMinutes: Int): Int {
        return if (remainingMinutes <= 0) 100 else {
            val spentMinutes = totalMinutes - remainingMinutes
            val progress = (spentMinutes.toFloat() / totalMinutes * 100).toInt()
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