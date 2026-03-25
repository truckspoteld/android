package com.eagleye.eld.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import android.widget.TextView


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
            Toast.makeText(this, "Shipping", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_request).setOnClickListener {
            binding.drawerLayout.close()
            Toast.makeText(this, "Request", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_fmcsa).setOnClickListener {
            binding.drawerLayout.close()
            Toast.makeText(this, "FMCSA", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_upload_documents).setOnClickListener {
            binding.drawerLayout.close()
            Toast.makeText(this, "Upload Documents", Toast.LENGTH_SHORT).show()
        }
        headerView.findViewById<View>(R.id.nav_more_options).setOnClickListener {
            Toast.makeText(this, "More Options", Toast.LENGTH_SHORT).show()
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
