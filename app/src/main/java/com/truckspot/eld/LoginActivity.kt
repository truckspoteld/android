package com.truckspot.eld
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
import com.truckspot.eld.databinding.ActivityLoginBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.truckspot.eld.fragment.Dashboard
import com.truckspot.eld.fragment.ui.home.HomeViewModel
import com.truckspot.eld.pt.devicemanager.TrackerManagerActivity
import com.truckspot.eld.request.AddLogRequest
import com.truckspot.eld.request.LoginRequest
import com.truckspot.eld.utils.ExceptionHelper
import com.truckspot.eld.utils.Helper
import com.truckspot.eld.utils.NetworkResult
import com.truckspot.eld.utils.PrefRepository
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.truckspot.eld.viewmodel.LoginViewModel
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
                        it.data.results.company_timezone?.let { tz ->
                            if (tz.isNotBlank()) prefRepository.setTimeZone(tz)
                        }

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
}
