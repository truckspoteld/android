package com.eagleye.eld.fragment

import com.eagleye.eld.PdfViewerActivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.eagleye.eld.BuildConfig.VERSION_NAME
import com.eagleye.eld.LoginActivity
import com.eagleye.eld.R
import com.eagleye.eld.databinding.ActivityDashboardBinding
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.fragment.ui.viewmodels.DashboardViewModel
import com.eagleye.eld.pt.devicemanager.BleProfileService
import com.eagleye.eld.pt.devicemanager.TrackerManagerActivity
import com.eagleye.eld.pt.devicemanager.TrackerService
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.CodriverLoginRequest
import com.eagleye.eld.request.LoginRequest
import com.eagleye.eld.utils.PrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eagleye.eld.utils.NetworkResult
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import androidx.annotation.RequiresPermission
import com.eagleye.eld.UploadDocumentsActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.eagleye.eld.utils.Constants.ACTION_SESSION_REPLACED
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import com.pt.sdk.Uart
import java.io.File
import java.io.FileOutputStream



@AndroidEntryPoint
class Dashboard : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var bottomConfiguration: AppBarConfiguration
    lateinit var binding: ActivityDashboardBinding
    lateinit var prefRepository: PrefRepository
    private var fusedClient: FusedLocationProviderClient? = null
    private var mRequest: com.google.android.gms.location.LocationRequest? = null
    private var mCallback: LocationCallback? = null
    private lateinit var navController: NavController
    private var selectedBottomItemId: Int = R.id.home
    var isReviewMode: Boolean = false

    private val homeViewModel: HomeViewModel by viewModels()
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectDialog: Dialog? = null

    private val sessionReplacedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            prefRepository.setLoggedIn(false)
            prefRepository.setToken("")
            prefRepository.clearCodriver()
            Toast.makeText(this@Dashboard, "You have been logged in on another device", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@Dashboard, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

//        SLog.detailLogs("dashboard", "dashboard open", true)
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.home_bg_light)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        prefRepository = PrefRepository(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            sessionReplacedReceiver, IntentFilter(ACTION_SESSION_REPLACED)
        )
        checkAndRequestBluetoothPermission()

        // Show ELD reconnect dialog after login
        if (prefRepository.getJustLoggedIn()) {
            prefRepository.setJustLoggedIn(false)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    showEldReconnectDialog()
                }
            }, 800L)
        }

        binding.appBarDashboard.menuicon.setOnClickListener {
            binding.drawerLayout.open()
        }
        
        homeViewModel.getDriverReview(this)

        val headerView = binding.navView.getHeaderView(0)
        val tvDriverName = headerView.findViewById<TextView>(R.id.tv_nav_driver_name)
        val tvCompanyName = headerView.findViewById<TextView>(R.id.tv_nav_company_name)
        val tvDriverInitial = headerView.findViewById<TextView>(R.id.tv_driver_initial)

        // Carrier details card views
        val tvNavCarrierName = headerView.findViewById<TextView>(R.id.tv_nav_carrier_name)
        val tvNavDot = headerView.findViewById<TextView>(R.id.tv_nav_dot)
        val tvNavCompanyAddress = headerView.findViewById<TextView>(R.id.tv_nav_company_address)
        val tvNavCompanyContact = headerView.findViewById<TextView>(R.id.tv_nav_company_contact)
        val tvNavTimezone = headerView.findViewById<TextView>(R.id.tv_nav_timezone)
        
        val tvNavDriverNameDetail = headerView.findViewById<TextView>(R.id.tv_nav_driver_name_detail)
        val tvNavDriverContact = headerView.findViewById<TextView>(R.id.tv_nav_driver_contact)
        val tvNavDriverEmail = headerView.findViewById<TextView>(R.id.tv_nav_driver_email)
        val tvNavLicence = headerView.findViewById<TextView>(R.id.tv_nav_licence)
        val tvNavLicenceDate = headerView.findViewById<TextView>(R.id.tv_nav_licence_date)
        val tvNavCycle = headerView.findViewById<TextView>(R.id.tv_nav_cycle)
        
        val rlNavCarrierHeader = headerView.findViewById<View>(R.id.rl_nav_carrier_header)
        val llNavExpandedContent = headerView.findViewById<View>(R.id.ll_nav_expanded_content)
        val ivNavExpandIcon = headerView.findViewById<View>(R.id.iv_nav_expand_icon)

        rlNavCarrierHeader.setOnClickListener {
            val isExpanded = llNavExpandedContent.visibility == View.VISIBLE
            if (isExpanded) {
                llNavExpandedContent.visibility = View.GONE
                ivNavExpandIcon.animate().rotation(0f).setDuration(200).start()
            } else {
                llNavExpandedContent.visibility = View.VISIBLE
                ivNavExpandIcon.animate().rotation(180f).setDuration(200).start()
            }
        }

        homeViewModel.driverReviewLiveData.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { response ->
                        val driver = response.data?.driver
                        val company = response.data?.company

                        val driverFullName = "${driver?.firstName ?: ""} ${driver?.lastName ?: ""}".trim()
                        tvDriverName.text = driverFullName.ifEmpty { "Driver Name" }
                        tvDriverInitial.text = if (driverFullName.isNotEmpty()) driverFullName.first().uppercase() else "D"
                        tvCompanyName.text = company?.companyName ?: "Company Name"

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
                        
                        val licenseText = if (!driver?.licenseNumber.isNullOrBlank()) {
                            "${driver!!.licenseNumber} (${driver.licenseState ?: ""})"
                        } else {
                            "--"
                        }
                        val licenseDate = driver?.licensedate?.substringBefore("T") ?: "--"
                        val cycle = company?.multidaybasis ?: "--"

                        tvNavCarrierName.text = carrierName
                        tvNavCompanyAddress.text = addressStr.ifEmpty { "--" }
                        tvNavCompanyContact.text = companyContact.ifEmpty { "--" }
                        tvNavTimezone.text = timezone
                        tvNavDot.text = dotNumber
                        
                        tvNavDriverNameDetail.text = driverName
                        tvNavDriverContact.text = driverContact.ifEmpty { "--" }
                        tvNavDriverEmail.text = driver?.email ?: "--"
                        tvNavLicence.text = licenseText
                        tvNavLicenceDate.text = licenseDate
                        tvNavCycle.text = cycle
                    }
                }
                is NetworkResult.Error -> {
                    Log.e("Dashboard", "Error fetching driver review data: ${result.message}")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Custom Drawer Header Actions
        headerView.findViewById<View>(R.id.btn_close_drawer).setOnClickListener {
            binding.drawerLayout.close()
        }
        headerView.findViewById<View>(R.id.nav_codriver_login).setOnClickListener {
            binding.drawerLayout.close()
            if (prefRepository.isCodriverLoggedIn()) {
                // Log out current driver so the co-driver can log in via LoginActivity
                prefRepository.setLoggedIn(false)
                prefRepository.setToken("")
                startActivity(android.content.Intent(this, com.eagleye.eld.LoginActivity::class.java))
                finish()
            } else {
                showCodriverPickerDialog()
            }
        }
        headerView.findViewById<View>(R.id.nav_shipping).setOnClickListener {
            binding.drawerLayout.close()
            showShippingDialog()
        }
        headerView.findViewById<View>(R.id.nav_fmcsa).setOnClickListener {
            binding.drawerLayout.close()
            isReviewMode = true
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_home, false)
                .build()
            navController.navigate(R.id.nav_reports, null, navOptions)
        }
        headerView.findViewById<View>(R.id.nav_upload_documents).setOnClickListener {
            binding.drawerLayout.close()
            startActivity(Intent(this, UploadDocumentsActivity::class.java))
        }
        headerView.findViewById<View>(R.id.nav_more_options).setOnClickListener {
            Toast.makeText(this, "More Options", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_user_manual).setOnClickListener {
            binding.drawerLayout.close()
            downloadUserManual()
        }

        headerView.findViewById<View>(R.id.btnLogout).setOnClickListener {
            binding.drawerLayout.close()
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
            performLogout()
        }
        
        navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        binding.appBarDashboard.dashboard.setText("Dashboard $VERSION_NAME")
        
        // Entrance animation for bottom nav
        YoYo.with(Techniques.BounceInUp).duration(1000).delay(500).playOn(binding.appBarDashboard.contantDashboard.bottomNav)
        binding.appBarDashboard.contantDashboard.bottomNav.onItemSelected = { selectedIndex ->
            // Map the selectedIndex to the corresponding itemId defined in the menu
            val selectedId = when (selectedIndex) {
                0 -> R.id.home
                1 -> R.id.logs
                2 -> R.id.report
                3 -> R.id.certify
                4 -> R.id.dvir
                else -> R.id.home
            }
            
            if (isReviewMode && selectedId != R.id.report && selectedId != R.id.logs) {
                showExitInspectionDialog(pendingNavId = selectedId)
            } else if (selectedId != selectedBottomItemId) {
                selectedBottomItemId = selectedId
                
                // Use NavOptions to prevent duplicate fragments and back stack issues
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)  // Prevent duplicate instances
                    .setPopUpTo(R.id.nav_home, false)  // Clear intermediate fragments
                    .build()
                
                when (selectedId) {
                    R.id.home -> {
                        val homeOptions = androidx.navigation.NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_home, true)
                            .build()
                        navController.navigate(R.id.nav_home, null, homeOptions)
                    }
                    R.id.logs -> {
                        navController.navigate(R.id.nav_gallery, null, navOptions)
                    }
                    R.id.report -> {
                        navController.navigate(R.id.nav_reports, null, navOptions)
                    }
                    R.id.certify -> {
                        navController.navigate(R.id.fragment_certify, null, navOptions)
                    }
                    R.id.dvir -> {
                        navController.navigate(R.id.nav_dvir, null, navOptions)
                    }
                    else -> {
                        navController.navigate(R.id.nav_home, null, navOptions)
                    }
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val mappedId = when (destination.id) {
                R.id.nav_home -> R.id.home
                R.id.nav_gallery -> R.id.logs
                R.id.nav_reports -> R.id.report
                R.id.fragment_certify -> R.id.certify
                R.id.nav_dvir -> R.id.dvir
                else -> null
            }
            if (mappedId != null && mappedId != selectedBottomItemId) {
                selectedBottomItemId = mappedId
                // Map the itemId back to its index
                val itemIndex = when (mappedId) {
                    R.id.home -> 0
                    R.id.logs -> 1
                    R.id.report -> 2
                    R.id.certify -> 3
                    R.id.dvir -> 4
                    else -> 0
                }
                binding.appBarDashboard.contantDashboard.bottomNav.itemActiveIndex = itemIndex
            }
        }
//
        binding.appBarDashboard.fab.setOnClickListener {
                startActivity(Intent(this, TrackerManagerActivity::class.java))
        }

        mCallback = object : LocationCallback() {
            //This callback is where we get "streaming" location updates. We can check things like accuracy to determine whether
            //this latest update should replace our previous estimate.
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "locationResult null")
                    return
                }
                Log.d(TAG, "received " + locationResult.locations.size + " locations")
                for (loc in locationResult.locations) {
                    Log.d(TAG, "locationResult null")
                    Log.e(
                        "TAG",
                        "${loc.provider}:Accu:(${loc.accuracy}). Lat:${loc.latitude},Lon:${loc.longitude},Loc:${loc.speed}"
                    )
                    glat = loc.latitude
                    glong = loc.longitude
                    lastPhoneLocationAtMs = System.currentTimeMillis()
                    gSpeed = loc.speed
                    insertInDrive(loc)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d(TAG, "locationAvailability is " + locationAvailability.isLocationAvailable)
                super.onLocationAvailability(locationAvailability)
            }
        }




        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            //request permission.
            //However check if we need to show an explanatory UI first
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                showRationale()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE
                    ), 2
                )
            }
        } else {
            //we already have the permission. Do any location wizardry now
            locationWizardry()
        }


    }
    // ─────────────────────────────────────────────────────────────────────
    //  ELD Reconnect Dialog
    // ─────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun showEldReconnectDialog() {
        val savedName = prefRepository.getLastEldDeviceName()
        val savedAddress = prefRepository.getLastEldDeviceAddress().trim()
        
        if (savedAddress.isEmpty()) {
            Log.d("Dashboard", "Skipping ELD reconnect overlay: no saved device")
            return
        }

        if (reconnectDialog?.isShowing == true) return

        if (!hasBluetoothScanPermission()) {
            checkAndRequestBluetoothPermission()
            return
        }

        Log.d("Dashboard", "Showing ELD reconnect overlay. Last saved: $savedName ($savedAddress)")

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_eld_reconnect_overlay)

        val scanner = BluetoothLeScannerCompat.getScanner()
        val deviceLabel = savedName.ifBlank { "ELD" }
        val root = dialog.findViewById<View>(R.id.eld_reconnect_overlay_root)
        val title = dialog.findViewById<TextView>(R.id.tv_eld_reconnect_title)
        val status = dialog.findViewById<TextView>(R.id.tv_eld_reconnect_status)
        title.text = "Reconnecting to ELD"
        status.text = "Searching for $deviceLabel..."

        var finished = false
        var isScanning = false
        var isConnecting = false
        var lastTapMs = 0L

        lateinit var scanCallback: ScanCallback
        lateinit var retryRunnable: Runnable
        lateinit var timeoutRunnable: Runnable
        lateinit var reconnectReceiver: BroadcastReceiver

        fun stopScan() {
            if (!isScanning) return
            runCatching { scanner.stopScan(scanCallback) }
            isScanning = false
        }

        fun cleanup() {
            stopScan()
            reconnectHandler.removeCallbacks(retryRunnable)
            reconnectHandler.removeCallbacks(timeoutRunnable)
            runCatching {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(reconnectReceiver)
            }
            reconnectDialog = null
        }

        fun finishReconnect(showTimeoutToast: Boolean = false) {
            if (finished) return
            finished = true
            cleanup()
            if (dialog.isShowing) dialog.dismiss()
            if (showTimeoutToast) {
                Toast.makeText(this, "ELD reconnect timed out", Toast.LENGTH_SHORT).show()
            }
        }

        fun startTrackerConnection(address: String, name: String) {
            if (isConnecting) return
            isConnecting = true
            status.text = "Connecting to ${name.ifBlank { deviceLabel }}..."
            stopScan()

            val service = Intent(this, TrackerService::class.java).apply {
                putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, address)
                putExtra(BleProfileService.EXTRA_DEVICE_NAME, name.ifBlank { deviceLabel })
            }
            startService(service)
        }

        fun startScan() {
            if (finished || isScanning || isConnecting) return
            status.text = "Searching for $deviceLabel..."
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000)
                .build()

            try {
                scanner.startScan(null, settings, scanCallback)
                isScanning = true
            } catch (e: Exception) {
                Log.e("Dashboard", "Reconnect scan failed: ${e.message}")
            }
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.address.equals(savedAddress, ignoreCase = true)) {
                    val name = result.scanRecord?.deviceName ?: result.device.name ?: savedName
                    startTrackerConnection(result.device.address, name)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.firstOrNull { it.device.address.equals(savedAddress, ignoreCase = true) }
                    ?.let { result ->
                        val name = result.scanRecord?.deviceName ?: result.device.name ?: savedName
                        startTrackerConnection(result.device.address, name)
                    }
            }
        }

        reconnectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BleProfileService.BROADCAST_CONNECTION_STATE -> {
                        when (intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED)) {
                            BleProfileService.STATE_CONNECTED -> finishReconnect()
                            BleProfileService.STATE_DISCONNECTED,
                            BleProfileService.STATE_LINK_LOSS -> {
                                isConnecting = false
                                startScan()
                            }
                        }
                    }
                    BleProfileService.BROADCAST_FAILED_TO_CONNECT -> {
                        isConnecting = false
                        startScan()
                    }
                }
            }
        }

        retryRunnable = object : Runnable {
            override fun run() {
                if (!finished) {
                    if (!isConnecting) {
                        stopScan()
                        startScan()
                    }
                    reconnectHandler.postDelayed(this, RECONNECT_SCAN_INTERVAL_MS)
                }
            }
        }

        timeoutRunnable = Runnable {
            finishReconnect(showTimeoutToast = true)
        }

        root.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastTapMs <= DOUBLE_TAP_DISMISS_MS) {
                finishReconnect()
            }
            lastTapMs = now
        }

        dialog.setOnDismissListener {
            cleanup()
        }

        dialog.show()
        reconnectDialog = dialog
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.65f }
        }

        val filter = IntentFilter().apply {
            addAction(BleProfileService.BROADCAST_CONNECTION_STATE)
            addAction(BleProfileService.BROADCAST_FAILED_TO_CONNECT)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(reconnectReceiver, filter)

        startScan()
        reconnectHandler.postDelayed(retryRunnable, RECONNECT_SCAN_INTERVAL_MS)
        reconnectHandler.postDelayed(timeoutRunnable, RECONNECT_TIMEOUT_MS)
    }

    fun dismissEldReconnectDialog() {
        reconnectDialog?.dismiss()
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }

    // ── Device Adapter for RecyclerView ────────────────────────────────
    private inner class EldDeviceAdapter(
        private val devices: List<ScanResult>,
        private val onPairClick: (ScanResult) -> Unit
    ) : RecyclerView.Adapter<EldDeviceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_item_device_name)
            val tvAddress: TextView = view.findViewById(R.id.tv_item_device_address)
            val btnPair: Button = view.findViewById(R.id.btn_item_pair)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_device, parent, false)
            return ViewHolder(view)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val result = devices[position]
            val deviceName = result.scanRecord?.deviceName ?: result.device.name ?: "Unknown ELD"
            holder.tvName.text = deviceName
            holder.tvAddress.text = result.device.address
            holder.btnPair.setOnClickListener { onPairClick(result) }
        }

        override fun getItemCount() = devices.size
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Bluetooth permissions
    // ─────────────────────────────────────────────────────────────────────

    private fun checkAndRequestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            val permissions = mutableListOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
            
            // Add POST_NOTIFICATIONS for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                enableBluetooth()
            }
        } else {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        startActivity(enableBtIntent)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btnLogout -> {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
                performLogout()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.dashboard, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()


    }

    var glat: Double? = null
    var glong: Double? = null
    var gSpeed: Float = 0f
    var lastPhoneLocationAtMs: Long = 0L

    fun getFreshPhoneLocation(maxAgeMs: Long = 2 * 60 * 1000L): Pair<Double, Double>? {
        val lat = glat
        val long = glong
        val isFresh = lastPhoneLocationAtMs > 0L &&
            System.currentTimeMillis() - lastPhoneLocationAtMs <= maxAgeMs

        return if (lat != null && long != null && isFresh) {
            lat to long
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGeoLocation(onLocationResolved: (Pair<Double, Double>?) -> Unit) {
        val hasFine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "fetchCurrentGeoLocation: No location permissions granted")
            onLocationResolved(null)
            return
        }

        if (fusedClient == null) {
            fusedClient = LocationServices.getFusedLocationProviderClient(this)
        }

        fusedClient?.lastLocation
            ?.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "fetchCurrentGeoLocation: lastLocation success lat=${location.latitude}, lng=${location.longitude}")
                    glat = location.latitude
                    glong = location.longitude
                    lastPhoneLocationAtMs = System.currentTimeMillis()
                    gSpeed = location.speed
                    onLocationResolved(location.latitude to location.longitude)
                } else {
                    // lastLocation was null — fire a fresh one-shot request
                    Log.w(TAG, "fetchCurrentGeoLocation: lastLocation null, requesting fresh location...")
                    requestFreshLocation(onLocationResolved)
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "fetchCurrentGeoLocation: lastLocation failed: ${e.message}")
                requestFreshLocation(onLocationResolved)
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(onLocationResolved: (Pair<Double, Double>?) -> Unit) {
        if (fusedClient == null) {
            fusedClient = LocationServices.getFusedLocationProviderClient(this)
        }

        // Try getCurrentLocation first (API 21+)
        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        fusedClient?.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            ?.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "requestFreshLocation: success lat=${location.latitude}, lng=${location.longitude}")
                    glat = location.latitude
                    glong = location.longitude
                    lastPhoneLocationAtMs = System.currentTimeMillis()
                    gSpeed = location.speed
                    onLocationResolved(location.latitude to location.longitude)
                } else {
                    Log.w(TAG, "requestFreshLocation: getCurrentLocation returned null, using cached")
                    onLocationResolved(getFreshPhoneLocation())
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "requestFreshLocation: getCurrentLocation failed: ${e.message}")
                onLocationResolved(getFreshPhoneLocation())
            }
    }

    @SuppressLint("MissingPermission")
    private fun locationWizardry() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        //Initially, get last known location. We can refine this estimate later
        fusedClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val loc =
                    location.provider + ":Accu:(" + location.accuracy + "). Lat:" + location.latitude + ",Lon:" + location.longitude
                glat = location.latitude
                glong = location.longitude
                lastPhoneLocationAtMs = System.currentTimeMillis()
                gSpeed = location.speed
                insertInDrive(location)
            }
        }


        //now for receiving constant location updates:
        createLocRequest()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mRequest!!)

        //This checks whether the GPS mode (high accuracy,battery saving, device only) is set appropriately for "mRequest". If the current settings cannot fulfil
        //mRequest(the Google Fused Location Provider determines these automatically), then we listen for failutes and show a dialog box for the user to easily
        //change these settings.
        val client = LocationServices.getSettingsClient(this@Dashboard)
        val task = client.checkLocationSettings(builder.build())
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@Dashboard, 500)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

        //actually start listening for updates: See on Resume(). It's done there so that conveniently we can stop listening in onPause
    }

    private fun insertInDrive(location: Location) {
        Log.d(TAG, "location updates ---> ${location.speed}")

//        if (gSpeed > 20 && CurrentMode != "d") navController.navigate(R.id.nav_home)

    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        updateCodriverNavHeader()
    }

    override fun onPause() {
        super.onPause()
        fusedClient?.removeLocationUpdates(mCallback!!)
    }

    private fun downloadUserManual() {
        val fileName = "TruckSpot(usermanual-betaversion).pdf"
        try {
            // 1. Copy from assets to internal storage (so we can share it via FileProvider)
            val inputStream = assets.open(fileName)
            val internalFile = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(internalFile)
            
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // 2. Also try to copy to public Downloads folder for the user (optional but good for "download" request)
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val publicFile = File(downloadsDir, fileName)
                    internalFile.copyTo(publicFile, overwrite = true)
                    Log.d("Dashboard", "Manual copied to public downloads: ${publicFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w("Dashboard", "Could not copy to public downloads (likely permission/scoped storage), but internal copy is ready.")
            }

            // 3. Open the PDF using PdfViewerActivity
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_PATH, internalFile.absolutePath)
                putExtra(PdfViewerActivity.EXTRA_PDF_TITLE, "User Manual")
            }
            startActivity(intent)
            
            Toast.makeText(this, "Opening User Manual...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e("Dashboard", "Error handling manual", e)
            Toast.makeText(this, "Failed to load User Manual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExitInspectionDialog(pendingNavId: Int) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter your password"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Exit Inspection Mode")
            .setMessage("Enter your password to exit inspection mode.")
            .setView(input)
            .setPositiveButton("Exit") { _, _ ->
                val entered = input.text.toString()
                val stored = prefRepository.getPassword()
                if (entered == stored) {
                    isReviewMode = false
                    selectedBottomItemId = pendingNavId
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.nav_home, false)
                        .build()
                    when (pendingNavId) {
                        R.id.home -> navController.navigate(R.id.nav_home, null, androidx.navigation.NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.nav_home, true).build())
                        R.id.report -> navController.navigate(R.id.nav_reports, null, navOptions)
                        R.id.certify -> navController.navigate(R.id.fragment_certify, null, navOptions)
                        R.id.dvir -> navController.navigate(R.id.nav_dvir, null, navOptions)
                    }
                } else {
                    Toast.makeText(this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCodriverPickerDialog() {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Loading drivers...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        scope.launch {
            try {
                val response = homeViewModel.getMyCodrivers()
                val drivers = if (response.isSuccessful) {
                    response.body()?.codrivers?.filter { it.id != prefRepository.getDriverId() } ?: emptyList()
                } else emptyList()

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (drivers.isEmpty()) {
                        AlertDialog.Builder(this@Dashboard)
                            .setTitle("No Drivers Found")
                            .setMessage("No other drivers found in your company.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@withContext
                    }
                    val names = drivers.map { it.name?.ifEmpty { it.username } ?: it.username ?: "Driver ${it.id}" }.toTypedArray()
                    AlertDialog.Builder(this@Dashboard)
                        .setTitle("Add a Co-Driver")
                        .setItems(names) { _, index ->
                            val selected = drivers[index]
                            val displayName = selected.name?.ifEmpty { selected.username } ?: selected.username ?: "Driver ${selected.id}"
                            // Save snapshot of current main driver before saving co-driver
                            prefRepository.setDriver1Token(prefRepository.getToken())
                            prefRepository.setDriver1Id(prefRepository.getDriverId())
                            prefRepository.setDriver1Name(prefRepository.getName())
                            prefRepository.setDriver1Username(prefRepository.getUserName())
                            // Save co-driver (no token needed — HOS fetched via main driver's token)
                            prefRepository.setCoDriverId(selected.id)
                            prefRepository.setCoDriverName(displayName)
                            prefRepository.setCodriverUsername(selected.username ?: "")
                            prefRepository.setCodriverToken("")
                            prefRepository.setIsCodriverLoggedIn(true)
                            // Save pairing on server so other devices can restore it
                            scope.launch { try { homeViewModel.setMyCodriver(selected.id) } catch (_: Exception) {} }
                            updateCodriverNavHeader()
                            Toast.makeText(this@Dashboard, "Co-Driver $displayName added", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@Dashboard, "Failed to load drivers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRemoveCodriverConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remove Co-Driver")
            .setMessage("Are you sure you want to remove the co-driver?")
            .setPositiveButton("Remove") { _, _ ->
                scope.launch {
                    try { homeViewModel.setMyCodriver(null) } catch (_: Exception) {}
                    try { homeViewModel.codriverLogout() } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        prefRepository.clearCodriver()
                        updateCodriverNavHeader()
                        Toast.makeText(this@Dashboard, "Co-Driver removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun updateCodriverNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val codriverBtn = headerView.findViewById<android.widget.TextView>(R.id.tv_nav_codriver_label)
        if (prefRepository.isCodriverLoggedIn()) {
            val name = prefRepository.getCoDriverName().ifEmpty { prefRepository.getCodriverUsername() }
            codriverBtn?.text = "Switch to: $name"
        } else {
            codriverBtn?.text = "Add a Co-Driver"
        }
    }

    private fun showShippingDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_shipping)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val etShippingNumber = dialog.findViewById<android.widget.EditText>(R.id.etShippingNumber)
        val etTrailerNumber = dialog.findViewById<android.widget.EditText>(R.id.etTrailerNumber)
        val tvCoDriver = dialog.findViewById<android.widget.TextView>(R.id.tvCoDriver)
        val btnUpdate = dialog.findViewById<Button>(R.id.btnUpdate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        
        // Load existing values
        etShippingNumber.setText(prefRepository.getShippingNumber())
        etTrailerNumber.setText(prefRepository.getTrailerNumber())
        val coDriverName = prefRepository.getCoDriverName()
        tvCoDriver.text = if (coDriverName.isEmpty()) "No Co-driver" else coDriverName
        
        btnUpdate.setOnClickListener {
            val shippingNo = etShippingNumber.text.toString()
            val trailerNo = etTrailerNumber.text.toString()
            
            if (shippingNo.isNotEmpty()) {
                prefRepository.setShippingNumber(shippingNo)
                prefRepository.setTrailerNumber(trailerNo)
                Toast.makeText(this, "Shipment updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Shipping Number is required", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionReplacedReceiver)
        dismissEldReconnectDialog()
        reconnectHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
        job.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedClient?.requestLocationUpdates(mRequest!!, mCallback!!, null)
    }

    private fun createLocRequest() {
        mRequest = LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            8000L // interval in ms
        )
            .setMinUpdateIntervalMillis(8000L) // fastestInterval
            .build()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            2 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thanks bud", Toast.LENGTH_SHORT).show()
                    locationWizardry()
                } else {
                    Toast.makeText(this, "C'mon man we really need this", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {}
        }
    }

    private fun showRationale() {
        val dialog = AlertDialog.Builder(this).setMessage(
            "We need this, Just suck it up and grant us the" +
                    "permission :)"
        ).setPositiveButton("Sure") { dialogInterface: DialogInterface, i: Int ->
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 2
            )
            dialogInterface.dismiss()
        }
            .create()
        dialog.show()
    }

    companion object {
        private const val TAG = "LocationActivity"
        private const val RECONNECT_SCAN_INTERVAL_MS = 10_000L
        private const val RECONNECT_TIMEOUT_MS = 120_000L
        private const val DOUBLE_TAP_DISMISS_MS = 350L
    }

    private fun performLogout() {
        prefRepository.setLoggedIn(false)
        prefRepository.setToken("")
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}
