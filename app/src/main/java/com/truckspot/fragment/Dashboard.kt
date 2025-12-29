package com.truckspot.fragment

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
import android.widget.Toast
import androidx.activity.viewModels
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
import com.truckspot.BuildConfig.VERSION_NAME
import com.truckspot.LoginActivity
import com.truckspot.R
import com.truckspot.databinding.ActivityDashboardBinding
import com.truckspot.fragment.ui.home.HomeViewModel
import com.truckspot.fragment.ui.viewmodels.DashboardViewModel
import com.truckspot.pt.devicemanager.TrackerManagerActivity
import com.truckspot.request.AddLogRequest
import com.truckspot.utils.PrefRepository
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


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

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

//        SLog.detailLogs("dashboard", "dashboard open", true)
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefRepository = PrefRepository(this)
//        setSupportActionBar(binding.appBarDashboard.toolbar)

        checkAndRequestBluetoothPermission()
        // Commented out drawer functionality
        // val drawerLayout: DrawerLayout = binding.drawerLayout
        // val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        binding.appBarDashboard.dashboard.setText("Dashboard $VERSION_NAME")
        binding.appBarDashboard.contantDashboard.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navController.navigate(R.id.nav_home)
                    true
                }

                R.id.logs -> {
                    navController.navigate(R.id.nav_gallery)
                    //loadFragment(GalleryFragment())
                    true
                }

                R.id.report -> {
                    navController.navigate(R.id.nav_reports)
                    true
                }

                R.id.certify -> {
                    navController.navigate(R.id.fragment_certify)
                    true
                }

                R.id.btnLogout -> {
                    Toast.makeText(this, "Thanks bud", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> {
                    navController.navigate(R.id.nav_home)
                    true
                }
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
                Toast.makeText(this, "Thanks bud", Toast.LENGTH_SHORT)
                    .show() // Implement a method to show a confirmation dialog
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
    val gnsChannel = Channel<Location>()
    val locationFlow = gnsChannel.receiveAsFlow()

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
        scope.launch { gnsChannel.send(location) }

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


}

