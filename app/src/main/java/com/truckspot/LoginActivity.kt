package com.truckspot
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
import com.truckspot.databinding.ActivityLoginBinding
import com.truckspot.fragment.Dashboard
import com.truckspot.fragment.ui.home.HomeViewModel
import com.truckspot.pt.devicemanager.TrackerManagerActivity
import com.truckspot.request.AddLogRequest
import com.truckspot.request.LoginRequest
import com.truckspot.utils.ExceptionHelper
import com.truckspot.utils.Helper
import com.truckspot.utils.NetworkResult
import com.truckspot.utils.PrefRepository
import com.truckspot.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel by viewModels<LoginViewModel>()
    private val homeViewModel by viewModels<HomeViewModel>()
    private lateinit var prefRepository: PrefRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var connection: Boolean = false

    // ⚙️ Runtime permission launcher (Android 12+)
    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            if (granted) {
                goToTrackerManager()
            } else {
                Toast.makeText(this, "Bluetooth permission is required!", Toast.LENGTH_LONG).show()
            }
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
                goToDashboard()
            } else {
                saveData("saveConnection", true)
                checkBluetoothPermissionsAndProceed()
            }
            return
        }

        binding.rememberme.isChecked = prefRepository.getLoggedIn()

        binding.forgotpassword.setOnClickListener {
            startActivity(Intent(this, ForgotpasswordActivity::class.java))
        }

        binding.submit.setOnClickListener {
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
    }

    private fun saveData(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun getData(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun checkBluetoothPermissionsAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            if (hasConnect != PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
                return
            }
        }
        goToTrackerManager()
    }

    private fun goToTrackerManager() {
        val int = Intent(this, TrackerManagerActivity::class.java)
        startActivity(int)
        finish()
    }

    private fun goToDashboard() {
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
                    if (it.data!!.status) {
                        prefRepository.setLoggedIn(true)
                        token = it.data.results.token
                        prefRepository.setName(it.data.results.username)
                        prefRepository.setDriverId(it.data.results.id)
                        prefRepository.setCompanyId(it.data.results.companyid)
                        prefRepository.setToken(token!!)

                        val logRequest = AddLogRequest(
                            modename = "login",
                            odometerreading = "0.0",
                            lat = 0.0,
                            long = 0.0,
                            location = true,
                            eng_hours = "10",
                            vin = "1111",
                            is_active = 1,
                            is_autoinsert = 1,
                            eventcode = 1,
                            eventtype = 1
                        )
                        homeViewModel.logUser(logRequest, this)

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
                            goToDashboard()
                        } else {
                            saveData("saveConnection", true)
                            checkBluetoothPermissionsAndProceed()
                        }
                    } else {
                        showValidationErrors(it.data.message)
                    }
                }

                is NetworkResult.Error<*> -> {
                    showValidationErrors(it.message.toString())
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
}
