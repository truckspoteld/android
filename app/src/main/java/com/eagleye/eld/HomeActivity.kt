package com.eagleye.eld
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager

@SuppressLint("CustomSplashScreen")
class HomeActivity : AppCompatActivity() {
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Create a WakeLock with the FULL_WAKE_LOCK flag
         wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyApp::MyWakeLock")

        // Acquire the wake lock to keep the screen on
        wakeLock.acquire()

        // To prevent the app from getting locked while the activity is running
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        showDialog()
        requestLocationPermission()

    }
    private fun showDialog() {
        val alertDialog = AlertDialog.Builder(this)

        // Set the title and message for your dialog
        alertDialog.setTitle("Welcome to HomeActivity")
        alertDialog.setMessage("This is a sample dialog in HomeActivity")

        // Set a positive button and its click listener
        alertDialog.setPositiveButton("OK") { dialog, _ ->
            // Close the dialog when the "OK" button is clicked
            dialog.dismiss()
        }

        // Create and show the dialog
        alertDialog.create().show()
    }
    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val fineLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted || coarseLocationGranted) {
                    Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
        } else {
            // Request permission
            requestPermissionLauncher.launch(permissions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}


