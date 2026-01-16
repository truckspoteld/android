package com.truckspot.fragment.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.truckspot.api.TruckSpotAPI
import com.truckspot.models.AddLogSuccessResponse
import com.truckspot.models.DRIVE_MODE
import com.truckspot.models.GetCompanyById
import com.truckspot.models.GetLogsResponse
import com.truckspot.models.GetReportsResponse
import com.truckspot.models.HomeDataModel
import com.truckspot.models.LogIdRequest
import com.truckspot.models.LogResponse
import com.truckspot.models.ReportsDataResponse
import com.truckspot.models.UserLog
import com.truckspot.repository.DashboardRepository
import com.truckspot.request.AddLogRequest
import com.truckspot.request.AddLogRequestunauth
import com.truckspot.request.AddOffsetRequest
import com.truckspot.request.updateLogRequest
import com.truckspot.utils.AlertCalculationUtils
import com.truckspot.utils.NetworkResult
import com.truckspot.utils.PrefRepository
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    var dashboardRespository: DashboardRepository,
    private val prefRepository: PrefRepository,
    private val truckSpotAPI: TruckSpotAPI,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val API_TIMEOUT_MS = 15000L // 15 seconds timeout for API calls
        private const val CACHE_VALIDITY_MS = 30000L // 30 seconds cache
    }

    val logResponseLiveData: LiveData<NetworkResult<LogResponse>>
        get() = dashboardRespository.logResponseLiveData
    var trackingMode: ObservableField<DRIVE_MODE> = ObservableField(DRIVE_MODE.MODE_OFF)
    val getLogsLiveData: LiveData<NetworkResult<GetLogsResponse>> get() = dashboardRespository.getLogsLiveData
    val homeLiveData: LiveData<NetworkResult<HomeDataModel>> get() = dashboardRespository.homeResponseLiveData

    val addLogReponse: LiveData<NetworkResult<AddLogSuccessResponse>> get() = dashboardRespository.addlogResponse
    val company: LiveData<NetworkResult<GetCompanyById>> get() = dashboardRespository.getCompanyById

    private val _reportsLiveData = MutableLiveData<NetworkResult<ReportsDataResponse>>()
    val reportsLiveData: LiveData<NetworkResult<ReportsDataResponse>>
        get() = _reportsLiveData

    // Cache management
    private var lastHomeDataFetchTime: Long = 0
    
    // Prevent concurrent API calls - use AtomicBoolean for thread safety
    private val isHomeApiInProgress = AtomicBoolean(false)
    private val isCompanyApiInProgress = AtomicBoolean(false)
    private val isLogUserApiInProgress = AtomicBoolean(false)
    
    // Jobs for cancellation
    private var homeApiJob: Job? = null
    private var companyApiJob: Job? = null
    private var logUserApiJob: Job? = null

    private var refinedUserLogs: MutableList<UserLog>? = null

    fun getDriverId() = prefRepository.getDriverId()

    fun getUserLogs() = refinedUserLogs ?: mutableListOf()

    fun connectSocket(id: Int) {
        dashboardRespository.connectSocket(id)
    }

    fun discconecttSocket() {
        dashboardRespository.disconnectSocket()
    }

    /**
     * Fetch home data with timeout and single call guarantee
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getHome(context: Context, forceRefresh: Boolean = false) {
        // Check cache first (unless force refresh)
        val currentTime = System.currentTimeMillis()
        val hasExistingData = homeLiveData.value is NetworkResult.Success
        val isDataStale = (currentTime - lastHomeDataFetchTime) > CACHE_VALIDITY_MS
        
        if (!forceRefresh && hasExistingData && !isDataStale) {
            Log.d(TAG, "getHome: Using cached data")
            return
        }
        
        // Prevent multiple concurrent calls
        if (!isHomeApiInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "getHome: Already in progress, skipping")
            return
        }
        
        // Cancel any existing job
        homeApiJob?.cancel()
        
        homeApiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                Log.d(TAG, "getHome: Starting API call")
                
                // Use timeout to prevent infinite hang
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.getHome()
                }
                
                lastHomeDataFetchTime = System.currentTimeMillis()
                Log.d(TAG, "getHome: API call completed successfully")
                
            } catch (e: CancellationException) {
                Log.d(TAG, "getHome: Cancelled")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "getHome: Timeout after ${API_TIMEOUT_MS}ms")
                // Silent timeout - don't show toast to user
            } catch (e: Exception) {
                Log.e(TAG, "getHome: Error - ${e.message}", e)
                showToast(context, "Network error: ${e.message}")
            } finally {
                isHomeApiInProgress.set(false)
            }
        }
    }

    /**
     * Force refresh home data
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshHome(context: Context) {
        getHome(context, forceRefresh = true)
    }

    /**
     * Get company name with timeout protection
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCompanyName(context: Context) {
        // Prevent multiple concurrent calls
        if (!isCompanyApiInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "getCompanyName: Already in progress, skipping")
            return
        }
        
        // Cancel any existing job
        companyApiJob?.cancel()
        
        companyApiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                Log.d(TAG, "getCompanyName: Starting API call")
                
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.getCompany()
                }
                
                Log.d(TAG, "getCompanyName: API call completed successfully")
                
            } catch (e: CancellationException) {
                Log.d(TAG, "getCompanyName: Cancelled")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "getCompanyName: Timeout")
                showToast(context, "Request timeout")
            } catch (e: Exception) {
                Log.e(TAG, "getCompanyName: Error - ${e.message}", e)
            } finally {
                isCompanyApiInProgress.set(false)
            }
        }
    }

    /**
     * Log user with timeout protection
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun logUser(logRequest: AddLogRequest, context: Context) {
        // Prevent multiple concurrent calls
        if (!isLogUserApiInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "logUser: Already in progress, skipping")
            return
        }
        
        // Cancel any existing job
        logUserApiJob?.cancel()
        
        logUserApiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                Log.d(TAG, "logUser: Starting API call with request: $logRequest")
                
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.addLog(logRequest)
                }
                
                Log.d(TAG, "logUser: API call completed successfully")
                
            } catch (e: CancellationException) {
                Log.d(TAG, "logUser: Cancelled")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "logUser: Timeout")
                showToast(context, "Request timeout")
            } catch (e: Exception) {
                Log.e(TAG, "logUser: Error - ${e.message}", e)
                showToast(context, "Network error: ${e.message}")
            } finally {
                isLogUserApiInProgress.set(false)
            }
        }
    }

    /**
     * Add offset
     */
    fun addOffset(prefRepository: PrefRepository, offset: AddOffsetRequest, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.addOffSet(prefRepository, offset)
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "addOffset: Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "addOffset: Error - ${e.message}", e)
            }
        }
    }

    /**
     * Get offset
     */
    fun getOffSet(prefRepository: PrefRepository, vin: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                withTimeout(API_TIMEOUT_MS) {
                    val data = dashboardRespository.getOffSet(vin)
                    val item = data.body()?.results?.unidentifiedRecords?.firstOrNull { 
                        it?.vinNo.equals(vin) 
                    }
                    prefRepository.setDifferenceinOdo(item?.odometer.toString())
                    prefRepository.setDifferenceinEnghours(item?.engHour.toString())
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "getOffSet: Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "getOffSet: Error - ${e.message}", e)
            }
        }
    }

    fun logUserunauth(logRequest: AddLogRequestunauth) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.addLogUnauth(logRequest)
                }
            } catch (e: Exception) {
                Log.e(TAG, "logUserunauth: Error - ${e.message}", e)
            }
        }
    }

    /**
     * Update log
     */
    @SuppressLint("LongLogTag")
    fun updateLog(updateLogReq: updateLogRequest, shouldHandleResponse: Boolean = true, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isInternetAvailable(context)) {
                    showToast(context, "No internet connection")
                    return@launch
                }
                
                Log.d(TAG, "updateLog: Starting with request: $updateLogReq")
                
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.updateLog(updateLogReq, shouldHandleResponse)
                }
                
                Log.d(TAG, "updateLog: Completed successfully")
                
            } catch (e: CancellationException) {
                Log.d(TAG, "updateLog: Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "updateLog: Error - ${e.message}", e)
                showToast(context, "Network error: ${e.message}")
            }
        }
    }

    fun getReports(param: (Any) -> Any) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeout(API_TIMEOUT_MS) {
                    val result = dashboardRespository.getReports()
                    _reportsLiveData.postValue(result)
                }
            } catch (e: Exception) {
                _reportsLiveData.postValue(NetworkResult.Error("Exception: ${e.message}"))
            }
        }
    }

    fun getLastLogModeName() =
        getUserLogs().lastOrNull()?.modename ?: getPreviousDayLastLog()?.modename
        ?: AlertCalculationUtils.secondLastDayLastLog?.modename ?: ""

    fun deleteLog(logId: LogIdRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeout(API_TIMEOUT_MS) {
                    dashboardRespository.deleteLog(logId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteLog: Error - ${e.message}", e)
            }
        }
    }

    suspend fun getCustomLogs(): Response<GetReportsResponse> {
        return dashboardRespository.get7DayLogs()
    }

    suspend fun getReportsLogs(): Response<GetReportsResponse> {
        return dashboardRespository.get7DayLogs()
    }

    fun getPreviousDayLastLog() = AlertCalculationUtils.previousDayLastLog
    fun getPreviousDayLogs() = AlertCalculationUtils.previousDayLogs

    fun calculatePreviousTimeForLogs(mode: String): Double {
        return dashboardRespository.calculatePreviousTimeForLogs(mode)
    }

    fun handleNextDayStuff() {
        dashboardRespository.handleNextDayStuff()
    }

    fun refineLogsFirstIndex(userLogs: List<UserLog>): List<UserLog> {
        refinedUserLogs = userLogs.filterNot { it.modename == "yard" || it.modename == "personal" }.toMutableList()

        SLog.detailLogs("REPLACING CURRENT DAY FIRST LOG WITH PREVIOUS DAY LAST LOG ", "\n", true)

        getPreviousDayLastLog()?.let { lastDayLastLog ->
            val newUserLog = UserLog(
                time = "00:00",
                modename = lastDayLastLog.modename,
                is_autoinsert = "1",
                hours = lastDayLastLog.hours
            )
            refinedUserLogs?.add(0, newUserLog)
        }

        SLog.detailLogs(
            "PREVIOUS DAY LAST LOG ",
            Gson().toJson(getPreviousDayLastLog()) + "\n",
            true
        )

        return getUserLogs()
    }

    /**
     * Check internet availability
     */
    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network: Network? = connectivityManager.activeNetwork
                val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)

                networkCapabilities != null && (
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "isInternetAvailable: Error - ${e.message}")
            false
        }
    }

    /**
     * Helper to show toast on main thread
     */
    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cancel all ongoing API calls when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        homeApiJob?.cancel()
        companyApiJob?.cancel()
        logUserApiJob?.cancel()
        Log.d(TAG, "ViewModel cleared, all jobs cancelled")
    }
}

fun validateCredentials(
    emailAddress: String, userName: String, password: String, isLogin: Boolean
): Pair<Boolean, String> {

    var result = Pair(true, "")
    if (TextUtils.isEmpty(emailAddress) || (!isLogin && TextUtils.isEmpty(userName)) || TextUtils.isEmpty(password)) {
        result = Pair(false, "Please provide the credentials")
    } else if (!TextUtils.isEmpty(password) && password.length <= 2) {
        result = Pair(false, "Password length should be greater than 3")
    }
    return result
}