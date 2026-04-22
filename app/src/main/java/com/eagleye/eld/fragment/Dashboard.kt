package com.eagleye.eld.fragment

import com.eagleye.eld.PdfViewerActivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import com.eagleye.eld.pt.devicemanager.TrackerManagerActivity
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.utils.PrefRepository
import com.eagleye.eld.utils.NetworkResult
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import androidx.annotation.RequiresPermission
import com.eagleye.eld.UploadDocumentsActivity
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

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1001

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
        
        val homeViewModel: HomeViewModel by viewModels()
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
            Toast.makeText(this, "Co-Driver Login", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_shipping).setOnClickListener {
            binding.drawerLayout.close()
            showShippingDialog()
        }
        headerView.findViewById<View>(R.id.nav_request).setOnClickListener {
            binding.drawerLayout.close()
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_home, false)
                .build()
            navController.navigate(R.id.nav_reports, null, navOptions)
        }
        headerView.findViewById<View>(R.id.nav_fmcsa).setOnClickListener {
            binding.drawerLayout.close()
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
            
            if (selectedId != selectedBottomItemId) {
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
    private fun showEldReconnectDialog() {
        // We show the dialog to allow scanning even if no previous device is found
        // but we can still log info about saved devices.
        val savedName = prefRepository.getLastEldDeviceName()
        val savedAddress = prefRepository.getLastEldDeviceAddress()
        
        Log.d("Dashboard", "Showing ELD dialog. Last saved: $savedName ($savedAddress)")

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_eld_reconnect)

        // Transparent background so our rounded drawable shows
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // ── Wire up views ──────────────────────────────────────────────
        val rvDevices       = dialog.findViewById<RecyclerView>(R.id.rv_bluetooth_devices)
        val llScanning      = dialog.findViewById<LinearLayout>(R.id.ll_scanning_indicator)
        val ivBluetooth     = dialog.findViewById<ImageView>(R.id.iv_bluetooth_icon)
        val glowOuter       = dialog.findViewById<View>(R.id.iv_bt_glow_outer)
        val glowInner       = dialog.findViewById<View>(R.id.iv_bt_glow_inner)
        val btnScanNew      = dialog.findViewById<Button>(R.id.btn_eld_scan_new)
        val tvSkip          = dialog.findViewById<TextView>(R.id.tv_eld_skip)
        val btnClose        = dialog.findViewById<ImageView>(R.id.btn_dialog_close)
        val dialogRoot      = dialog.findViewById<LinearLayout>(R.id.eld_dialog_root)

        // ── Setup List & Adapter ───────────────────────────────────────
        val deviceList = mutableListOf<ScanResult>()
        var scanCallback: ScanCallback? = null

        val adapter = EldDeviceAdapter(deviceList) { selectedResult ->
            // Stop scan and connect
            scanCallback?.let { 
                BluetoothLeScannerCompat.getScanner().stopScan(it) 
            }
            dialog.dismiss()
            val intent = Intent(this, TrackerManagerActivity::class.java).apply {
                putExtra("auto_connect_address", selectedResult.device.address)
                putExtra("auto_connect_name", selectedResult.scanRecord?.deviceName ?: selectedResult.device.name)
            }
            startActivity(intent)
        }
        rvDevices.adapter = adapter

        // ── Bluetooth Scanning Logic ──────────────────────────────────
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (deviceList.none { it.device.address == result.device.address }) {
                    deviceList.add(result)
                    adapter.notifyItemInserted(deviceList.size - 1)
                    llScanning.visibility = View.GONE
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    if (deviceList.none { it.device.address == result.device.address }) {
                        deviceList.add(result)
                    }
                }
                adapter.notifyDataSetChanged()
                if (deviceList.isNotEmpty()) llScanning.visibility = View.GONE
            }
        }

        // Start scanning with filter for ELD service (UART RX)
        val scanner = BluetoothLeScannerCompat.getScanner()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(1000)
            .build()
        
        try {
            scanCallback?.let { scanner.startScan(null, settings, it) }
        } catch (e: Exception) {
            Log.e("Dashboard", "Scan failed: ${e.message}")
        }

        // Auto-stop scan after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            scanCallback?.let { scanner.stopScan(it) }
            llScanning.visibility = View.GONE
        }, 10000L)

        // ── Entrance animation ─────────────────────────────────────────
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_dialog)
        dialogRoot.startAnimation(slideUp)

        // ── Pulse animations on icon and glow rings ────────────────────
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_bluetooth)
        ivBluetooth.startAnimation(pulseAnim)

        val glowOuterAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_bluetooth).apply {
            startOffset = 150L
        }
        glowOuter.startAnimation(glowOuterAnim)

        val glowInnerAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_bluetooth).apply {
            startOffset = 75L
        }
        glowInner.startAnimation(glowInnerAnim)

        // ── Button click: Scan new device ──────────────────────────────
        btnScanNew.setOnClickListener {
            scanCallback?.let { scanner.stopScan(it) }
            dialog.dismiss()
            startActivity(Intent(this, TrackerManagerActivity::class.java))
        }

        // ── Dismiss actions ────────────────────────────────────────────
        tvSkip.setOnClickListener   { 
            scanCallback?.let { scanner.stopScan(it) }
            dialog.dismiss() 
        }
        btnClose.setOnClickListener { 
            scanCallback?.let { scanner.stopScan(it) }
            dialog.dismiss() 
        }

        dialog.show()
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
