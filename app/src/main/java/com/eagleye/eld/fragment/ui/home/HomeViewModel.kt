package com.eagleye.eld.fragment.ui.home
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
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.models.AddLogSuccessResponse
import com.eagleye.eld.models.DRIVE_MODE
import com.eagleye.eld.models.GetCompanyById
import com.eagleye.eld.models.GetLogsResponse
import com.eagleye.eld.models.GetReportsResponse
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.models.LogIdRequest
import com.eagleye.eld.models.LogResponse
import com.eagleye.eld.models.ReportsDataResponse
import com.eagleye.eld.models.UserLog
import com.eagleye.eld.repository.DashboardRepository
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.AddLogRequestunauth
import com.eagleye.eld.request.AddOffsetRequest
import com.eagleye.eld.request.updateLogRequest
import com.eagleye.eld.utils.AlertCalculationUtils
import com.eagleye.eld.utils.NetworkResult
import com.eagleye.eld.utils.PrefRepository
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    var dashboardRespository: DashboardRepository,
    private val prefRepository: PrefRepository,
    private val truckSpotAPI: TruckSpotAPI,

    ) : ViewModel() {

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


    private var refinedUserLogs: MutableList<UserLog>? = null

    fun getDriverId() = prefRepository.getDriverId()

    fun getUserLogs() = refinedUserLogs ?: mutableListOf()

    fun connectSocket(id: Int) {
        dashboardRespository.connectSocket(id)
    }

    fun setLogUpdatedCallback(callback: () -> Unit) {
        dashboardRespository.onLogUpdatedCallback = callback
    }

    fun discconecttSocket() {
        dashboardRespository.disconnectSocket()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun logUser(logRequest: AddLogRequest, context: Context ) {
        try {
            Log.d("checkDate", "latest data : ${logRequest}")
            viewModelScope.launch {
                val internetAvailable = isInternetAvailable(context)
                Log.d("HomeViewModel", "Internet available for logUser: $internetAvailable")

                if (internetAvailable) {
                    try {
                        Log.d("HomeViewModel", "Making API call to addLog")
                        dashboardRespository.addLog(logRequest)
                        Log.d("HomeViewModel", "API call to addLog completed")
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Network error in logUser: ${e.message}", e)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Handle no internet connection scenario
                    Log.d("HomeViewModel", "No internet available, showing toast")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Exception in logUser: ${e.message}", e)
//            withContext(kotlinx.coroutines.Dispatchers.Main) {
//                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCompanyName( context: Context ) {
        viewModelScope.launch {
            if (isInternetAvailable(context)) {
                try {
                    dashboardRespository.getCompany()
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Network error in getCompanyName: ${e.message}", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun addOffset(prefRepository: PrefRepository, offset: AddOffsetRequest, context: Context) {
        viewModelScope.launch {
            if (isInternetAvailable(context)) {
                try {
                    dashboardRespository.addOffSet(prefRepository, offset)
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Network error in addOffset: ${e.message}", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Handle no internet connection scenario
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun getOffSet(prefRepository: PrefRepository, vin: String , context: Context) {
        viewModelScope.launch {
            if (isInternetAvailable(context)) {
                try {
                    val data = dashboardRespository.getOffSet(vin)
                    val item =
                        data.body()?.results?.unidentifiedRecords?.firstOrNull { it?.vinNo.equals(vin) }
                    prefRepository.setDifferenceinOdo(item?.odometer.toString())
                    prefRepository.setDifferenceinEnghours(item?.engHour.toString())
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Network error in getOffSet: ${e.message}", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Handle no internet connection scenario
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    fun logUserunauth(logRequest: AddLogRequestunauth) {
        viewModelScope.launch {
            try {
                dashboardRespository.addLogUnauth(logRequest)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Network error in logUserunauth: ${e.message}", e)
            }
        }
    }

    @SuppressLint("LongLogTag")
    fun updateLog(updateLogReq: updateLogRequest, shouldHandleResponse: Boolean = true, context: Context) {
        Log.d("updateapibeingimplemented", updateLogReq.toString())

        viewModelScope.launch {
            if (isInternetAvailable(context)) {
                try {
                    dashboardRespository.updateLog(updateLogReq, shouldHandleResponse)
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Network error in updateLog: ${e.message}", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    // Job to track the getHome coroutine for proper cancellation
    private var getHomeJob: kotlinx.coroutines.Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun getHome(context: Context) {
        // Cancel any existing job to prevent multiple loops
        getHomeJob?.cancel()

        getHomeJob = viewModelScope.launch {
            val internetAvailable = isInternetAvailable(context)
            Log.d("HomeViewModel", "Internet available for getHome: $internetAvailable")

            if (internetAvailable) {
                try {
                    Log.d("HomeViewModel", "Making single API call to getHome")
                    dashboardRespository.getHome()
                    Log.d("HomeViewModel", "API call to getHome completed")
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Network error in getHome: ${e.message}", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("HomeViewModel", "No internet available for getHome, showing toast")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Cancel the getHome job when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        getHomeJob?.cancel()
    }


    fun getReports(param: (Any) -> Any) {
        viewModelScope.launch {
            try {
                val result = dashboardRespository.getReports()
                _reportsLiveData.postValue(result)
            } catch (e: Exception) {
                _reportsLiveData.postValue(NetworkResult.Error("Exception: ${e.message}"))
            }
        }
    }

    fun getLastLogModeName() =
        getUserLogs().lastOrNull()?.modename ?: getPreviousDayLastLog()?.modename
        ?: AlertCalculationUtils.secondLastDayLastLog?.modename ?: ""

    fun deleteLog(logId: LogIdRequest) {
        viewModelScope.launch {
            dashboardRespository.deleteLog(logId)
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
//        refinedUserLogs = userLogs.toMutableList()
        refinedUserLogs  =  userLogs.filterNot { it.modename == "yard" || it.modename == "personal" }.toMutableList()
//        refinedUserLogs = AlertCalculationUtils.currentDay.toMutableList()

        SLog.detailLogs("REPLACING CURRENT DAY FIRST LOG WITH PREVIOUS DAY LAST LOG ", "\n", true)

        getPreviousDayLastLog()
            ?.let { lastDayLastLog ->
                val newUserLog = UserLog(
//                    id = lastDayLastLog.id,
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
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)

            val result = networkCapabilities != null && (
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )

            Log.d("HomeViewModel", "Network check result: $result")
            result
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            val result = networkInfo?.isConnected == true
            Log.d("HomeViewModel", "Network check result (legacy): $result")
            result
        }
    }


}

fun validateCredentials(
    emailAddress: String, userName: String, password: String, isLogin: Boolean
): Pair<Boolean, String> {

    var result = Pair(true, "")
    if (TextUtils.isEmpty(emailAddress) || (!isLogin && TextUtils.isEmpty(userName)) || TextUtils.isEmpty(
            password
        )
    ) {
        result = Pair(false, "Please provide the credentials")
    }
//        else if(!Helper.isValidEmail(emailAddress)){
//            result = Pair(false, "Email is invalid")
//        }
    else if (!TextUtils.isEmpty(password) && password.length <= 2) {
        result = Pair(false, "Password length should be greater than 3")
    }
    return result
}
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.os.Build
//import android.text.TextUtils
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.databinding.ObservableField
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.gson.Gson
//import com.eagleye.eld.api.TruckSpotAPI
//import com.eagleye.eld.models.AddLogSuccessResponse
//import com.eagleye.eld.models.DRIVE_MODE
//import com.eagleye.eld.models.GetCompanyById
//import com.eagleye.eld.models.GetLogsResponse
//import com.eagleye.eld.models.GetReportsResponse
//import com.eagleye.eld.models.HomeDataModel
//import com.eagleye.eld.models.LogIdRequest
//import com.eagleye.eld.models.LogResponse
//import com.eagleye.eld.models.ReportsDataResponse
//import com.eagleye.eld.models.UserLog
//import com.eagleye.eld.repository.DashboardRepository
//import com.eagleye.eld.request.AddLogRequest
//import com.eagleye.eld.request.AddLogRequestunauth
//import com.eagleye.eld.request.AddOffsetRequest
//import com.eagleye.eld.request.updateLogRequest
//import com.eagleye.eld.utils.AlertCalculationUtils
//import com.eagleye.eld.utils.NetworkResult
//import com.eagleye.eld.utils.PrefRepository
//import com.whizpool.supportsystem.SLog
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import retrofit2.Response
//import javax.inject.Inject
//
//@HiltViewModel
//class HomeViewModel @Inject constructor(
//    var dashboardRespository: DashboardRepository,
//    private val prefRepository: PrefRepository,
//    private val truckSpotAPI: TruckSpotAPI,
//) : ViewModel() {
//
//    companion object {
//        private const val TAG = "HomeViewModel"
//    }
//
//    val logResponseLiveData: LiveData<NetworkResult<LogResponse>>
//        get() = dashboardRespository.logResponseLiveData
//    var trackingMode: ObservableField<DRIVE_MODE> = ObservableField(DRIVE_MODE.MODE_OFF)
//    val getLogsLiveData: LiveData<NetworkResult<GetLogsResponse>> get() = dashboardRespository.getLogsLiveData
//    val homeLiveData: LiveData<NetworkResult<HomeDataModel>> get() = dashboardRespository.homeResponseLiveData
//
//    val addLogReponse: LiveData<NetworkResult<AddLogSuccessResponse>> get() = dashboardRespository.addlogResponse
//    val company: LiveData<NetworkResult<GetCompanyById>> get() = dashboardRespository.getCompanyById
//
//    private val _reportsLiveData = MutableLiveData<NetworkResult<ReportsDataResponse>>()
//    val reportsLiveData: LiveData<NetworkResult<ReportsDataResponse>>
//        get() = _reportsLiveData
//
//    private var refinedUserLogs: MutableList<UserLog>? = null
//
//    fun getDriverId() = prefRepository.getDriverId()
//
//    fun getUserLogs() = refinedUserLogs ?: mutableListOf()
//
//    fun connectSocket(id: Int) {
//        dashboardRespository.connectSocket(id)
//    }
//
//    fun discconecttSocket() {
//        dashboardRespository.disconnectSocket()
//    }
//
//    /**
//     * Fetch home data - Simple like EagleEye (no blocking)
//     */
//    // Job to track the getHome coroutine for proper cancellation
//    private var getHomeJob: Job? = null
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getHome(context: Context, forceRefresh: Boolean = false) {
//        // Cancel any existing job to prevent multiple loops
//        getHomeJob?.cancel()
//
//        getHomeJob = viewModelScope.launch {
//            val internetAvailable = isInternetAvailable(context)
//            Log.d(TAG, "Internet available for getHome: $internetAvailable")
//
//            if (internetAvailable) {
//                try {
//                    Log.d(TAG, "Making single API call to getHome")
//                    dashboardRespository.getHome()
//                    Log.d(TAG, "API call to getHome completed")
//                } catch (e: Exception) {
//                    Log.e(TAG, "Network error in getHome: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } else {
//                Log.d(TAG, "No internet available for getHome, showing toast")
//                withContext(Dispatchers.Main) {
//                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    /**
//     * Force refresh home data
//     */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun refreshHome(context: Context) {
//        getHome(context, forceRefresh = true)
//    }
//
//    /**
//     * Get company name - Simple like EagleEye (no blocking)
//     */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCompanyName(context: Context) {
//        viewModelScope.launch {
//            if (isInternetAvailable(context)) {
//                try {
//                    dashboardRespository.getCompany()
//                } catch (e: Exception) {
//                    Log.e(TAG, "Network error in getCompanyName: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    /**
//     * Log user - Simple fire-and-forget like EagleEye (no blocking)
//     */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun logUser(logRequest: AddLogRequest, context: Context) {
//        try {
//            Log.d(TAG, "logUser: Starting with request: $logRequest")
//            viewModelScope.launch {
//                val internetAvailable = isInternetAvailable(context)
//                Log.d(TAG, "Internet available for logUser: $internetAvailable")
//
//                if (internetAvailable) {
//                    try {
//                        Log.d(TAG, "Making API call to addLog")
//                        dashboardRespository.addLog(logRequest)
//                        Log.d(TAG, "API call to addLog completed")
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Network error in logUser: ${e.message}", e)
//                        withContext(Dispatchers.Main) {
//                            android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                } else {
//                    Log.d(TAG, "No internet available, showing toast")
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Exception in logUser: ${e.message}", e)
//        }
//    }
//
//    /**
//     * Add offset - Simple like EagleEye
//     */
//    fun addOffset(prefRepository: PrefRepository, offset: AddOffsetRequest, context: Context) {
//        viewModelScope.launch {
//            if (isInternetAvailable(context)) {
//                try {
//                    dashboardRespository.addOffSet(prefRepository, offset)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Network error in addOffset: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    /**
//     * Get offset - Simple like EagleEye
//     */
//    fun getOffSet(prefRepository: PrefRepository, vin: String, context: Context) {
//        viewModelScope.launch {
//            if (isInternetAvailable(context)) {
//                try {
//                    val data = dashboardRespository.getOffSet(vin)
//                    val item = data.body()?.results?.unidentifiedRecords?.firstOrNull {
//                        it?.vinNo.equals(vin)
//                    }
//                    prefRepository.setDifferenceinOdo(item?.odometer.toString())
//                    prefRepository.setDifferenceinEnghours(item?.engHour.toString())
//                } catch (e: Exception) {
//                    Log.e(TAG, "Network error in getOffSet: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    fun logUserunauth(logRequest: AddLogRequestunauth) {
//        viewModelScope.launch {
//            try {
//                dashboardRespository.addLogUnauth(logRequest)
//            } catch (e: Exception) {
//                Log.e(TAG, "Network error in logUserunauth: ${e.message}", e)
//            }
//        }
//    }
//
//    /**
//     * Update log - Simple like EagleEye
//     */
//    @SuppressLint("LongLogTag")
//    fun updateLog(updateLogReq: updateLogRequest, shouldHandleResponse: Boolean = true, context: Context) {
//        Log.d(TAG, "updateLog: $updateLogReq")
//        viewModelScope.launch {
//            if (isInternetAvailable(context)) {
//                try {
//                    dashboardRespository.updateLog(updateLogReq, shouldHandleResponse)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Network error in updateLog: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        android.widget.Toast.makeText(context, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    fun getReports(param: (Any) -> Any) {
//        viewModelScope.launch {
//            try {
//                val result = dashboardRespository.getReports()
//                _reportsLiveData.postValue(result)
//            } catch (e: Exception) {
//                _reportsLiveData.postValue(NetworkResult.Error("Exception: ${e.message}"))
//            }
//        }
//    }
//
//    fun getLastLogModeName() =
//        getUserLogs().lastOrNull()?.modename ?: getPreviousDayLastLog()?.modename
//        ?: AlertCalculationUtils.secondLastDayLastLog?.modename ?: ""
//
//    fun deleteLog(logId: LogIdRequest) {
//        viewModelScope.launch {
//            dashboardRespository.deleteLog(logId)
//        }
//    }
//
//    suspend fun getCustomLogs(): Response<GetReportsResponse> {
//        return dashboardRespository.get7DayLogs()
//    }
//
//    suspend fun getReportsLogs(): Response<GetReportsResponse> {
//        return dashboardRespository.get7DayLogs()
//    }
//
//    fun getPreviousDayLastLog() = AlertCalculationUtils.previousDayLastLog
//    fun getPreviousDayLogs() = AlertCalculationUtils.previousDayLogs
//
//    fun calculatePreviousTimeForLogs(mode: String): Double {
//        return dashboardRespository.calculatePreviousTimeForLogs(mode)
//    }
//
//    fun handleNextDayStuff() {
//        dashboardRespository.handleNextDayStuff()
//    }
//
//    fun refineLogsFirstIndex(userLogs: List<UserLog>): List<UserLog> {
//        refinedUserLogs = userLogs.filterNot { it.modename == "yard" || it.modename == "personal" }.toMutableList()
//
//        SLog.detailLogs("REPLACING CURRENT DAY FIRST LOG WITH PREVIOUS DAY LAST LOG ", "\n", true)
//
//        getPreviousDayLastLog()?.let { lastDayLastLog ->
//            val newUserLog = UserLog(
//                time = "00:00",
//                modename = lastDayLastLog.modename,
//                is_autoinsert = "1",
//                hours = lastDayLastLog.hours
//            )
//            refinedUserLogs?.add(0, newUserLog)
//        }
//
//        SLog.detailLogs(
//            "PREVIOUS DAY LAST LOG ",
//            Gson().toJson(getPreviousDayLastLog()) + "\n",
//            true
//        )
//
//        return getUserLogs()
//    }
//
//    /**
//     * Check internet availability
//     */
//    fun isInternetAvailable(context: Context): Boolean {
//        return try {
//            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val network: Network? = connectivityManager.activeNetwork
//                val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
//
//                networkCapabilities != null && (
//                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
//                )
//            } else {
//                @Suppress("DEPRECATION")
//                connectivityManager.activeNetworkInfo?.isConnected == true
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "isInternetAvailable: Error - ${e.message}")
//            false
//        }
//    }
//
//    /**
//     * Cancel the getHome job when ViewModel is cleared
//     */
//    override fun onCleared() {
//        super.onCleared()
//        getHomeJob?.cancel()
//    }
//}
//
//fun validateCredentials(
//    emailAddress: String, userName: String, password: String, isLogin: Boolean
//): Pair<Boolean, String> {
//
//    var result = Pair(true, "")
//    if (TextUtils.isEmpty(emailAddress) || (!isLogin && TextUtils.isEmpty(userName)) || TextUtils.isEmpty(password)) {
//        result = Pair(false, "Please provide the credentials")
//    } else if (!TextUtils.isEmpty(password) && password.length <= 2) {
//        result = Pair(false, "Password length should be greater than 3")
//    }
//    return result
//}