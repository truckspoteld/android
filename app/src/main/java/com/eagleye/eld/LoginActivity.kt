package com.eagleye.eld
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.eagleye.eld.databinding.ActivityLoginBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.eagleye.eld.fragment.Dashboard
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.pt.devicemanager.TrackerManagerActivity
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.LoginRequest
import com.eagleye.eld.utils.ExceptionHelper
import com.eagleye.eld.utils.Helper
import com.eagleye.eld.utils.NetworkResult
import com.eagleye.eld.utils.PrefRepository
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.eagleye.eld.viewmodel.LoginViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel by viewModels<LoginViewModel>()
    private val homeViewModel by viewModels<HomeViewModel>()
    private lateinit var prefRepository: PrefRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var connection: Boolean = false
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // ⚙️ Runtime permission launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // We proceed to tracker manager regardless of grant status for now, 
            // but location-aware features will fallback gracefully if denied.
            goToTrackerManager()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        prefRepository = PrefRepository(this)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("MyAppData", MODE_PRIVATE)
        binding.etDriver.setText(prefRepository.getUserName())
        binding.etPass.setText(prefRepository.getPassword())
        connection = getData("saveConnection", false)

        println("check connection is ---- $connection")

        if (ExceptionHelper.checkForCrash(prefRepository, this)) {
            // optional
        }

        // ✅ Already logged in
        if (prefRepository.getRememberMe() && prefRepository.getLoggedIn() && prefRepository.getToken().isNotEmpty()) {
            if (connection) {
                prefRepository.setJustLoggedIn(true)
                goToDashboard()
            } else {
                saveData("saveConnection", true)
                checkAllPermissionsAndProceed()
            }
            return
        }

        binding.rememberme.isChecked = prefRepository.getLoggedIn()

        binding.forgotpassword.setOnClickListener {
            playClickAnimation(it)
            startActivity(Intent(this, ForgotpasswordActivity::class.java))
        }

        binding.ivPassToggle.setOnClickListener {
            playClickAnimation(it)
            val isPassword = binding.etPass.transformationMethod is android.text.method.PasswordTransformationMethod
            if (isPassword) {
                binding.etPass.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                binding.etPass.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            binding.etPass.text?.let { text -> binding.etPass.setSelection(text.length) }
        }

        binding.submit.setOnClickListener {
            playClickAnimation(it)
            if (binding.etDriver.text.isNullOrEmpty()) {
                Toast.makeText(this, "Driver ID cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (binding.etPass.text.isNullOrEmpty()) {
                binding.etPass.error = "Password cannot be empty"
            } else {
                Helper.hideKeyboard(it)
                binding.txtError.visibility = View.GONE
                val validationResult = validateUserInput()
                if (validationResult.first) {
                    binding.progressBar.isVisible = true
                    val loginRequest = LoginRequest(
                        binding.etDriver.text.toString(),
                        binding.etPass.text.toString()
                    )
                    loginViewModel.loginUser(loginRequest)
                } else {
                    showValidationErrors(validationResult.second)
                }
            }
        }

        bindObservers()
        startEntranceAnimations()
    }

    private fun saveData(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun getData(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun checkAllPermissionsAndProceed() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            goToTrackerManager()
        }
    }

    private fun goToTrackerManager() {
        val int = Intent(this, TrackerManagerActivity::class.java)
        startActivity(int)
        finish()
    }

    private fun goToDashboard() {
        // Clear stale home data so HomeFragment doesn't flash old conditions on login
        homeViewModel.resetHomeData()
        val int = Intent(this, Dashboard::class.java)
        startActivity(int)
        finish()
    }

    var token: String? = ""

    @RequiresApi(Build.VERSION_CODES.O)
    private fun bindObservers() {
        loginViewModel.loginResponseLiveData.observe(this, Observer {
            binding.progressBar.isVisible = false
            when (it) {
                is NetworkResult.Success<*> -> {
                    if (it.data!!.status && it.data.results != null) {
                        val results = it.data.results
                        val newDriverId = results.id
                        val isRoleSwap = prefRepository.isCodriverLoggedIn() &&
                            prefRepository.getCoDriverId() == newDriverId

                        if (isRoleSwap) {
                            // Co-driver logging in as main — swap roles using driver1 snapshot
                            val prevId       = prefRepository.getDriver1Id()
                            val prevToken    = prefRepository.getDriver1Token()
                            val prevName     = prefRepository.getDriver1Name()
                            val prevUsername = prefRepository.getDriver1Username()

                            // Set new main driver
                            prefRepository.setLoggedIn(true)
                            token = results.token
                            prefRepository.setToken(token!!)
                            prefRepository.setName(results.username)
                            prefRepository.setDriverId(newDriverId)
                            prefRepository.setCompanyId(results.companyid)

                            // Old main driver becomes co-driver
                            prefRepository.setCoDriverId(prevId)
                            prefRepository.setCoDriverName(prevName.ifEmpty { prevUsername })
                            prefRepository.setCodriverUsername(prevUsername)
                            prefRepository.setCodriverToken(prevToken)
                            prefRepository.setIsCodriverLoggedIn(true)

                            // Refresh driver1 snapshot
                            prefRepository.setDriver1Token(results.token)
                            prefRepository.setDriver1Id(newDriverId)
                            prefRepository.setDriver1Name(results.username)
                            prefRepository.setDriver1Username(binding.etDriver.text.toString())
                        } else {
                            // Fresh login — clear any leftover co-driver relationship and stale HOS cache
                            prefRepository.clearCodriver()
                            // Clear cached mode on every fresh login so HOS clocks don't count down
                            // based on the previous session's state before server data arrives
                            prefRepository.setMode("")
                            prefRepository.setLoggedIn(true)
                            token = results.token
                            prefRepository.setName(results.username)
                            prefRepository.setDriverId(newDriverId)
                            prefRepository.setCompanyId(results.companyid)
                            prefRepository.setToken(token!!)
                        }

                        results.company_timezone?.let { tz ->
                            if (tz.isNotBlank()) prefRepository.setTimeZone(tz)
                        }

                        fetchLocationAndSendLoginLog()

                        if (binding.rememberme.isChecked) {
                            prefRepository.setRememberMe(true)
                            prefRepository.setUserName(binding.etDriver.text.toString())
                            prefRepository.setPassword(binding.etPass.text.toString())
                        } else {
                            prefRepository.setRememberMe(false)
                            prefRepository.setUserName("")
                            prefRepository.setPassword("")
                        }

                        if (connection) {
                            prefRepository.setJustLoggedIn(true)
                            goToDashboard()
                        } else {
                            saveData("saveConnection", true)
                            checkAllPermissionsAndProceed()
                        }
                    } else {
                        showValidationErrors(it.data.message ?: "Invalid username or password. Please try again.")
                    }
                }

                is NetworkResult.Error<*> -> {
                    if (it.message == "ALREADY_LOGGED_IN") {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Already Logged In")
                            .setMessage("This account is already logged in on another device. Do you want to log out that device and continue here?")
                            .setPositiveButton("Force Login") { _, _ ->
                                val username = binding.etDriver.text.toString().trim()
                                val password = binding.etPass.text.toString().trim()
                                loginViewModel.forceLoginUser(LoginRequest(username, password))
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        showValidationErrors(it.message.toString())
                    }
                }

                is NetworkResult.Loading<*> -> {
                    binding.progressBar.isVisible = true
                }
            }
        })
    }

    private fun validateUserInput(): Pair<Boolean, String> {
        val driverId = binding.etDriver.text.toString()
        val password = binding.etPass.text.toString()
        return loginViewModel.validateCredentials(driverId, "", password, true)
    }

    @SuppressLint("StringFormatMatches")
    private fun showValidationErrors(error: String) {
        binding.txtError.visibility = View.VISIBLE
        binding.txtError.text =
            String.format(resources.getString(R.string.txt_error_message, error))
    }

    private fun startEntranceAnimations() {
        val duration = 700L
        val stagger = 80L

        val views = listOf(
            binding.logoRow,
            binding.welcometext,
            binding.logintext,
            binding.chipsScroll,
            binding.cardView
        )
        
        views.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay(index * stagger)
                view.visibility = View.VISIBLE
                val technique = if (index <= 2) Techniques.FadeInDown else if (index == 3) Techniques.FadeInRight else Techniques.FadeInUp
                YoYo.with(technique).duration(duration).playOn(view)
            }
        }
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
    @SuppressLint("MissingPermission")
    private fun fetchLocationAndSendLoginLog() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }

        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w("LoginActivity", "Location permissions not granted, sending login log with 0,0")
            sendLog(0.0, 0.0, false)
            return
        }

        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("LoginActivity", "Login Location (last): ${location.latitude}, ${location.longitude}")
                sendLog(location.latitude, location.longitude, true)
            } else {
                Log.d("LoginActivity", "lastLocation null, trying getCurrentLocation")
                val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
                fusedLocationClient?.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )?.addOnSuccessListener { loc ->
                    if (loc != null) {
                        Log.d("LoginActivity", "Login Location (fresh): ${loc.latitude}, ${loc.longitude}")
                        sendLog(loc.latitude, loc.longitude, true)
                    } else {
                        Log.w("LoginActivity", "Location still null, sending 0,0")
                        sendLog(0.0, 0.0, false)
                    }
                }?.addOnFailureListener {
                    Log.e("LoginActivity", "getCurrentLocation failed: ${it.message}")
                    sendLog(0.0, 0.0, false)
                }
            }
        }?.addOnFailureListener {
            Log.e("LoginActivity", "lastLocation failed: ${it.message}")
            sendLog(0.0, 0.0, false)
        }
    }

    private fun sendLog(lat: Double, lng: Double, hasLocation: Boolean) {
        val logRequest = AddLogRequest(
            modename = "login",
            odometerreading = "0",
            lat = lat,
            long = lng,
            location = hasLocation,
            eng_hours = "0",
            vin = "",
            is_active = 1,
            is_autoinsert = 1,
            eventcode = 1,
            eventtype = 1,
            connection_status = "disconnected"
        )
        homeViewModel.logUser(logRequest, this)
    }
}
