package com.truckspot.eld.repository

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.truckspot.eld.api.TruckSpotAPI
import com.truckspot.eld.models.AddLogSuccessResponse
import com.truckspot.eld.models.GetCompanyById
import com.truckspot.eld.models.GetLogsByDateRequest
import com.truckspot.eld.models.GetLogsByDateResponse
import com.truckspot.eld.models.GetLogsResponse
import com.truckspot.eld.models.GetReportsResponse
import com.truckspot.eld.models.HomeDataModel
import com.truckspot.eld.models.LogIdRequest
import com.truckspot.eld.models.LogResponse
import com.truckspot.eld.models.ReportsDataResponse
import com.truckspot.eld.models.ResultsNew
import com.truckspot.eld.models.UnidentifiedResponse
import com.truckspot.eld.models.UserLog
import com.truckspot.eld.request.AddLogRequest
import com.truckspot.eld.request.AddLogRequestunauth
import com.truckspot.eld.request.AddOffsetRequest
import com.truckspot.eld.request.updateLogRequest
import com.truckspot.eld.utils.AlertCalculationUtils
import com.truckspot.eld.utils.Constants.TAG
import com.truckspot.eld.utils.NetworkResult
import com.truckspot.eld.utils.PrefConstants
import com.truckspot.eld.utils.PrefRepository
import com.whizpool.supportsystem.SLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import com.truckspot.eld.utils.Constants
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject


class DashboardRepository @Inject constructor(
    private val truckSpotAPI: TruckSpotAPI,
    private val csvApi: CsvDownloadApi,
    private val prefRepository: PrefRepository,
    @ApplicationContext val context: Context,
    private val okHttpClient: OkHttpClient
    ) {
    private val _logResponseLiveData = MutableLiveData<NetworkResult<LogResponse>>()
    private val _addLog = MutableLiveData<NetworkResult<AddLogSuccessResponse>>()
    private val _homeData = MutableLiveData<NetworkResult<HomeDataModel>>()

    private val _logByDate= MutableLiveData<NetworkResult<GetLogsByDateResponse>>()
    private val _reportsLiveData = MutableLiveData<NetworkResult<ReportsDataResponse>>()

    val homeResponseLiveData: LiveData<NetworkResult<HomeDataModel>>
        get() = _homeData

    val addlogResponse: LiveData<NetworkResult<AddLogSuccessResponse>>
        get() = _addLog

    val logByDate: LiveData<NetworkResult<GetLogsByDateResponse>>
        get() = _logByDate
    private val _ReportsResponseLiveData = MutableLiveData<NetworkResult<LogResponse>>()

    val logResponseLiveData: LiveData<NetworkResult<LogResponse>>
        get() = _logResponseLiveData

    val reportsResponseLiveData: LiveData<NetworkResult<ReportsDataResponse>>
        get() = _reportsLiveData
    private val _GetLogsLiveData = MutableLiveData<NetworkResult<GetLogsResponse>>()
    val getLogsLiveData: LiveData<NetworkResult<GetLogsResponse>>
        get() = _GetLogsLiveData


    private val _getCompanyById = MutableLiveData<NetworkResult<GetCompanyById>>()
    val getCompanyById: LiveData<NetworkResult<GetCompanyById>>
        get() = _getCompanyById

    private val _driverReviewLiveData = MutableLiveData<NetworkResult<com.truckspot.eld.models.DriverReviewResponse>>()
    val driverReviewLiveData: LiveData<NetworkResult<com.truckspot.eld.models.DriverReviewResponse>>
        get() = _driverReviewLiveData

    // Use the injected truckSpotAPI directly
    // Redundant retrofit and csvApi instances removed to save memory and improve initialization speed

//    @RequiresApi(Build.VERSION_CODES.O)suspend fun getLogs(page: Int, pageSize: Int, days: Int) {
//        SLog.detailLogs("GETTING LOGS OF DAY ", days.toString() + "\n", true)
//        _logResponseLiveData.postValue(NetworkResult.Loading())
//
//        /*coroutineScope {
//            // Make asynchronous network requests for previous day and day after
//            val responseOldDayDeferred = async { truckSpotAPI.getLogs(page, pageSize, days + 1) }
//            SLog.detailLogs("GETTING LOGS OF PREVIOUS DAY ", (days + 1).toString() + "\n", true)
//
//            val responseOldDay2Deferred = async { truckSpotAPI.getLogs(page, pageSize, days + 2) }
//            SLog.detailLogs("GETTING LOGS OF 3rd DAY ", (days + 2).toString() + "\n", true)
//
//
//            // Wait for both requests to complete
//            val responseOldDay = responseOldDayDeferred.await()
//            val responseOldDay2 = responseOldDay2Deferred.await()
//
//        }*/
//
//            val allLogResponse = truckSpotAPI.getAllLogs()
//
//            if (allLogResponse.isSuccessful && allLogResponse.body() != null) {
//                val logs = allLogResponse.body()!!.logs.filter {
//                    it.driverid == prefRepository.getDriverId()
//                }.sortedBy {
//                    it.id
//                }
//
////            val logs = AlertCalculationUtils.currentDay
//
//                AlertCalculationUtils.allLogs = logs.takeLast(100)
//
//                val currentDayLogs = filterUserLogsByDaysAgo(logs, days)
//                val responseOldDay = filterUserLogsByDaysAgo(logs, days + 1)
//                val responseOldDay2 = filterUserLogsByDaysAgo(logs, days + 2)
//
////        val response = truckSpotAPI.getLogs(page, pageSize, days)
//
////        Log.d(TAG, response.body().toString())
//
//                SLog.detailLogs("PREVIOUS DAY LOGS", Gson().toJson(responseOldDay) + "\n", true)
//                SLog.detailLogs("PREVIOUS 3rd DAY LOGS", Gson().toJson(responseOldDay2) + "\n", true)
//                SLog.detailLogs("CURRENT DAY LOGS", Gson().toJson(currentDayLogs) + "\n", true)
//
//                handleOldDayResponseLogs(responseOldDay, responseOldDay2)
//
//            // wrapping current day response
//                val getLogsResponse = GetLogsResponse(
//                    200,
//                    "",
//                    ResultsNew(
//                        allLogResponse.body()!!.totalCount,
//                        "",
//                        "",
//                        currentDayLogs,
//                        logs
//                    ),
//                    true
//                )
//
//                _GetLogsLiveData.postValue(NetworkResult.Success(getLogsResponse))
//            } else if (allLogResponse.errorBody() != null) {
//                val errorObj = JSONObject(allLogResponse.errorBody()?.charStream()?.readText())
//                _GetLogsLiveData.postValue(NetworkResult.Error("Something Went wrong at API END"))
//            } else {
//                _GetLogsLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
//            }
////        handleResponseGetLogs(response)
//        }


    init {
        SocketManager.initialize()
    }

    private var isListeningForLogs = false
    var onLogUpdatedCallback: (() -> Unit)? = null

    fun connectSocket(id: Int) {
        SocketManager.connect(id)
        if (!isListeningForLogs) {
            isListeningForLogs = true
            listenNewLogs()
        }
    }

    fun disconnectSocket() {
        SocketManager.disconnect()
        isListeningForLogs = false
        onLogUpdatedCallback = null
    }

    @SuppressLint("SuspiciousIndentation")
    private fun listenNewLogs() {
        SocketManager.listenForLogUpdates {
            onLogUpdatedCallback?.invoke()
        }
        SocketManager.listenForLogs { jsonObject ->
//            val newLogResponse = Gson().fromJson(jsonObject.toString(), UserLog::class.java)
            println(jsonObject.toString())
            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonObject.toString()).asJsonObject
            val logsJsonArray = jsonObject.getAsJsonArray("logs")

            // Convert to List<UserLog>03§
            val listType = object : TypeToken<List<HomeDataModel.Log>>() {}.type
            val logsList: List<HomeDataModel.Log> = gson.fromJson(logsJsonArray, listType)
            // Add the log to the existing logs list
            val currentResult = _homeData.value

            if (currentResult is NetworkResult.Success) {
                val currentHomeData = currentResult.data
                if(currentHomeData != null){
                val updatedLogs = currentHomeData.logs.orEmpty().toMutableList().apply {
                    add(logsList.first())
                }
                val updatedHomeData = currentHomeData.copy(logs = updatedLogs)
//                _homeData.postValue(NetworkResult.Success(updatedHomeData))
            }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun downloadAndEmailCsvViaGmail(context: Context, recipientEmail: String = ""  , startDate: String = "", endDate: String = "") {
        try {
            val id = prefRepository.getDriverId()
            val response = csvApi.downloadCsv(id, startDate, endDate)
            if (!response.isSuccessful) {
                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(context,  response.body()?.message, Toast.LENGTH_SHORT).show()
            val url = response.body()?.downloadUrl;
            if(url == null){
                Toast.makeText(context, "Invalid Url ", Toast.LENGTH_SHORT).show()
                return
            }
            val csv = csvApi.downloadFile("/api/v1/download-csv/combined_data.csv");
            if (csv.code() != 200) {
                Toast.makeText(context, "Download failed : ${csv.code()}", Toast.LENGTH_SHORT).show()
                return
            }
            downloadAndEmailCsv(context, csv, recipientEmail)
//            val fileName = "data_${System.currentTimeMillis()}.csv"
//            val resolver = context.contentResolver
//            val contentValues = ContentValues().apply {
//                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
//                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
//                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//                put(MediaStore.Downloads.IS_PENDING, 1)
//            }
//
//            val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
//            if (fileUri == null) {
//                Toast.makeText(context, "File creation failed", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            csv.body()?.byteStream()?.use { inputStream ->
//                resolver.openOutputStream(fileUri)?.use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            contentValues.clear()
//            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
//            resolver.update(fileUri, contentValues, null, null)
//
//            // Try to launch Gmail directly
//            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
//                `package` = "com.google.android.gm" // restrict to Gmail only
//                type = "text/csv"
//                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
//                putExtra(Intent.EXTRA_SUBJECT, "CSV File")
//                putExtra(Intent.EXTRA_TEXT, "")
//                putExtra(Intent.EXTRA_STREAM, fileUri)
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//
//            // Check if Gmail is installed
//            if (gmailIntent.resolveActivity(context.packageManager) != null) {
//                context.startActivity(gmailIntent)
//            } else {
//                Toast.makeText(context, "Gmail app not found", Toast.LENGTH_SHORT).show()
//            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Log.e("EmailCSV", "Error", e)
        }
    }

    fun downloadAndEmailCsv(context: Context, csv: Response<ResponseBody>, recipientEmail: String) {
        val fileName = "ELD_Report_${System.currentTimeMillis()}.csv"
        var fileUri: Uri? = null

        try {
            // Create a temporary file in the app's cache directory
            val tempFile = File(context.cacheDir, fileName)
            
            // Write the CSV data to the temporary file
            csv.body()?.let { responseBody ->
                try {
                    // Get the raw text content
                    val content = responseBody.string()
                    
                    // Write the content to file
                    tempFile.writeText(content)
                    
                    // Verify file was written
                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        throw IOException("File was not written properly")
                    }
                    
                    Log.d("EmailCSV", "File written successfully. Size: ${tempFile.length()} bytes")
                } catch (e: Exception) {
                    Log.e("EmailCSV", "Error writing file", e)
                    Toast.makeText(context, "Error writing file: ${e.message}", Toast.LENGTH_SHORT).show()
                    return
                }
            } ?: run {
                Log.e("EmailCSV", "No response body received")
                Toast.makeText(context, "No data received", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                // Get content URI using FileProvider with correct authority
                fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider", // Changed from .provider to .fileprovider
                    tempFile
                )
                
                Log.d("EmailCSV", "FileProvider URI: $fileUri")
                
                // Verify the URI can be opened
                context.contentResolver.openInputStream(fileUri)?.close()
                
            } catch (e: Exception) {
                Log.e("EmailCSV", "Error creating file URI", e)
                Toast.makeText(context, "Er ror creating file URI: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            // Create email intent with proper MIME type
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, "ELD Report")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the ELD report containing event logs and related data.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verify Gmail package is available
            val gmailPackage = "com.google.android.gm"
            val gmailAvailable = try {
                context.packageManager.getPackageInfo(gmailPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            try {
                if (gmailAvailable) {
                    // Grant read permission to Gmail with correct authority
                    context.grantUriPermission(
                        gmailPackage,
                        fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    
                    emailIntent.`package` = gmailPackage
                    context.startActivity(emailIntent)
                } else {
                    // If Gmail is not found, try any email app
                    val chooserIntent = Intent.createChooser(emailIntent, "Send ELD report using...")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
            } catch (e: Exception) {
                Log.e("EmailCSV", "Error launching email intent", e)
                Toast.makeText(context, "Error launching email: ${e.message}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("EmailCSV", "Error while handling ELD data", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCompany() {
        _logByDate.postValue(NetworkResult.Loading())
        val companyId = prefRepository.getCompanyId()

        try {
            val response = truckSpotAPI.getCompanyById(companyId)
            if(response.isSuccessful && response.body() != null){
                _getCompanyById.postValue(NetworkResult.Success(response.body()!!))
            } else if (response.errorBody() != null){
                val errorMessage = response.errorBody()?.string() ?: "Network Error"
                _getCompanyById.postValue(NetworkResult.Error(errorMessage))
            } else{
                _getCompanyById.postValue(NetworkResult.Error("Network Error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCompany: ${e.message}", e)
            _getCompanyById.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    suspend fun getOffSet(vin: String): Response<UnidentifiedResponse> {
        return try {
            truckSpotAPI.getOffset(vin)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOffSet: ${e.message}", e)
            throw e
        }
    }

    suspend fun getReports(): NetworkResult<ReportsDataResponse> {
        _reportsLiveData.postValue(NetworkResult.Loading())
        return try {
            val response = truckSpotAPI.getReportsData()
            Log.d(TAG, "check reports response here :${response.body().toString()}")
            handleResponseGetReports(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getReports: ${e.message}", e)
            NetworkResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun getDriverReview() {
        _driverReviewLiveData.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.getDriverReview()
            if (response.isSuccessful && response.body() != null) {
                _driverReviewLiveData.postValue(NetworkResult.Success(response.body()!!))
            } else if (response.errorBody() != null) {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                _driverReviewLiveData.postValue(NetworkResult.Error("API error: $errorMessage"))
            } else {
                _driverReviewLiveData.postValue(NetworkResult.Error("Network Error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getDriverReview: ${e.message}", e)
            _driverReviewLiveData.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    suspend fun deleteLog(logIdRequest: LogIdRequest) {
        try {
            val response = truckSpotAPI.deleteLog(logIdRequest)
            Log.d(TAG, "Delete log response: ${response.body()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteLog: ${e.message}", e)
        }
    }

    suspend fun get7DayLogs(): Response<GetReportsResponse> {
        _logResponseLiveData.postValue(NetworkResult.Loading())
        return try {
            val response = truckSpotAPI.getReportsLogs()
            Log.d(TAG, "8DAYSLOGS:${response}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in get7DayLogs: ${e.message}", e)
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getHome() {
        _homeData.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.getHomeData()
            Log.d("API_RESPONSE_DEBUG", "getHome Raw Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                // Print extensive JSON log for debugging
                val jsonResponse = Gson().toJson(response.body())
                Log.d("API_RESPONSE_DEBUG", "getHome JSON: $jsonResponse")
                SLog.detailLogs("GET_HOME_RESPONSE", jsonResponse, true)

                Log.d("DashboardRepository", "getHome: Success")
                _homeData.postValue(NetworkResult.Success(response.body()!!))
            } else if (response.errorBody() != null) {
                val errorString = response.errorBody()?.string() ?: "Unknown error"
                Log.w(TAG, "getHome error: $errorString")
                _homeData.postValue(NetworkResult.Error("Network error: $errorString"))
            } else {
                _homeData.postValue(NetworkResult.Error("Network Error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getHome: ${e.message}", e)
            _homeData.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getLogsByDate(request: GetLogsByDateRequest) {
        _logByDate.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.getLogByDate(request)
            Log.d("API_RESPONSE_DEBUG", "getLogsByDate Raw Response: $response")
            if (response.isSuccessful && response.body() != null) {
                // Print extensive JSON log for debugging
                val jsonResponse = Gson().toJson(response.body())
                Log.d("API_RESPONSE_DEBUG", "getLogsByDate JSON: $jsonResponse")
                SLog.detailLogs("GET_LOGS_BY_DATE_RESPONSE", jsonResponse, true)

                Log.d(TAG, "handleResponse: ${response.body()}")
                _logByDate.postValue(NetworkResult.Success(response.body()!!))
            } else if (response.errorBody() != null) {
                val errorString = response.errorBody()?.byteStream()?.bufferedReader().use { it?.readText() }
                Log.w(TAG, "handleResponse error: $errorString")
                _logByDate.postValue(NetworkResult.Error("Network error: ${errorString ?: "Unknown error"}"))
            } else {
                _logByDate.postValue(NetworkResult.Error("Network Error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getLogsByDate error: ${e.message}", e)
            _logByDate.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addLog(addLogRequest: AddLogRequest?) {
        _addLog.postValue(NetworkResult.Loading())
        if (addLogRequest == null) {
            _addLog.postValue(NetworkResult.Error("Invalid log request"))
            return
        }
        
        try {
            val response = truckSpotAPI.addLog(addLogRequest)
            if(response.isSuccessful && response.body() != null){
                Log.d("DashboardRepository", "addLog: Success")
                _addLog.postValue(NetworkResult.Success(response.body()!!))
            } else if(response.errorBody() != null){
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJson ?: "{}").optString("error", "Unknown error")
                } catch (e: Exception) {
                    "Network error occurred"
                }
                _addLog.postValue(NetworkResult.Error(errorMessage))
            } else {
                _addLog.postValue(NetworkResult.Error("Network Error"))
            }
            // getHome() // Removed redundant call - Fragment will handle refresh or Socket will update
        } catch (e: Exception) {
            Log.e(TAG, "Error in addLog: ${e.message}", e)
            _addLog.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    suspend fun addLogUnauth(addLogRequest: AddLogRequestunauth) {
        _logResponseLiveData.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.addLogUnauth(addLogRequest)
            Log.d(TAG, response.body().toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error in addLogUnauth: ${e.message}", e)
            _logResponseLiveData.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    @SuppressLint("LongLogTag")
    suspend fun updateLog(updateLog: updateLogRequest, shouldHandleResponse: Boolean) {
        _logResponseLiveData.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.updateLog(updateLog)
            Log.d("check the updated api being", response.body().toString())
            if (shouldHandleResponse) {
                if (response.isSuccessful && response.body() != null) {
                    _logResponseLiveData.postValue(NetworkResult.Success(response.body()!!))
                } else {
                    _logResponseLiveData.postValue(NetworkResult.Error("Update failed"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateLog: ${e.message}", e)
            _logResponseLiveData.postValue(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addLogJava(addLogRequest: AddLogRequest) =
        GlobalScope.future { addLog(addLogRequest) }


//    @RequiresApi(Build.VERSION_CODES.O)
//    fun filterUserLogsByDaysAgo(userLogs: List<UserLog>, daysAgo: Int): List<UserLog> {
//        val targetDate: LocalDate? = AlertCalculationUtils.daysDiff(daysAgo)
//        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//
//        return userLogs.filter {
//            val logDate = LocalDate.parse(it.date, dateFormatter)
//            logDate == targetDate || (daysAgo == 0 && logDate == LocalDate.now())
//        }
//    }


    private fun handleOldDayResponseLogs(
        response: List<UserLog>,
        responseOldDay2: List<UserLog>
    ) {

//        val userLog2ResponseLogs = responseOldDay2.results?.userLogs?.toMutableList()

        var previousDayLogs = response.toMutableList()
        var day3LastLog: UserLog? = null
        responseOldDay2?.let {
//                if (it.lastOrNull()?.time?.contains("00:00") == true)
//                    it.removeLast()

            it.lastOrNull().also {
                day3LastLog = it
                AlertCalculationUtils.secondLastDayLastLog = it

            }
        }

//        val refinedUserLogs = response.results.userLogs.toMutableList()
        day3LastLog
            ?.let { day3LastLog ->
                val newUserLog = UserLog(
//                        id = day3LastLog.id,
                    time = "00:00",
                    is_autoinsert = "1",
                    modename = day3LastLog.modename
                )
                previousDayLogs.add(0, newUserLog)
            }


        //                if (it.lastOrNull()?.time?.contains("00:00") == true)
        //                    it.removeLast()

        AlertCalculationUtils.previousDayLastLogForUnidentified = null
        AlertCalculationUtils.previousDayLogs = previousDayLogs



        SLog.detailLogs(
            "PREVIOUS DAY REFINE LOGS",
            Gson().toJson(AlertCalculationUtils.previousDayLogs) + "\n",
            true
        )
    }

    private fun handleResponseGetLogs(response: Response<GetLogsResponse>) {
        if (response.isSuccessful && response.body() != null) {

            SLog.detailLogs("CURRENT DAY LOGS", Gson().toJson(response.body()) + "\n", true)

            _GetLogsLiveData.postValue(NetworkResult.Success(response.body()!!))
        } else if (response.errorBody() != null) {
            //val errorObj = JSONObject(response.errorBody()?.charStream()?.readText())
            _GetLogsLiveData.postValue(NetworkResult.Error("Something Went wrong at API END"))
        } else {
            _GetLogsLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
        }
    }

    private fun handleResponseGetReports(response: Response<ReportsDataResponse>): NetworkResult<ReportsDataResponse> {
        return if (response.isSuccessful && response.body() != null) {
            NetworkResult.Success(response.body()!!)

        } else if (response.errorBody() != null) {
            NetworkResult.Error("Something went wrong at API END")
        } else {
            NetworkResult.Error("Something went wrong")
        }
    }

    private fun handleResponse(response: Response<HomeDataModel>) {
        if (response.isSuccessful && response.body() != null) {
            Log.d(TAG, "handleResponse: ${response.body()}")
            _homeData.postValue(NetworkResult.Success(response.body()!!))
        } else if (response.errorBody() != null) {
//            val errorObj = JSONObject(response.errorBody()?.charStream()?.readText())

            val errorString =
                response.errorBody()?.byteStream()?.bufferedReader().use { it?.readText() }

            Log.w(TAG, "handleResponse error")
            _homeData.postValue(NetworkResult.Error("Something Went wrong at API END"))
        } else {
            _homeData.postValue(NetworkResult.Error("Something Went Wrong"))
        }
    }


    suspend fun addOffSet(prefRepository: PrefRepository, offset: AddOffsetRequest) {
         var data1  = AddOffsetRequest(23333,33,"Testing Vin")
        val response =    truckSpotAPI.addOffset(offset)
        var offset2= response.body()?.results
        val data = getOffSet(offset.vin_no)
        val item =
            data.body()?.results?.unidentifiedRecords?.lastOrNull { it?.vinNo.equals(offset.vin_no) }
        prefRepository.setDifferenceinOdo(data.body()?.results?.unidentifiedRecords?.get(0)?.odometer.toString())
        prefRepository.setDifferenceinEnghours(data.body()?.results?.unidentifiedRecords?.get(0)?.engHour.toString())
    }

    fun calculatePreviousTimeForLogs(mode: String): Double {
        if (AlertCalculationUtils.isShiftReset(context))
            return 0.0

        return AlertCalculationUtils.previousDayLogs
            ?.let {
                AlertCalculationUtils.calculateDayHours(
                    context,
                    it,
                    isCalculateFromIndex = true,
                    shouldAssignData = true,
                    shouldIncludeMidNightTime = true
                )?.run {
                    when (mode) {
                        PrefConstants.TRUCK_MODE_SLEEPING -> sleepingHours
                        PrefConstants.TRUCK_MODE_ON -> onHours
                        PrefConstants.TRUCK_MODE_OFF -> offHours
                        PrefConstants.TRUCK_MODE_DRIVING -> drivingHours
                        else -> {
                            0.0
                        }
                    }
                }
            } ?: 0.0
    }


    fun handleNextDayStuff() {
        AlertCalculationUtils.handleNextDayStuff(context)
    }

    /*suspend fun getOffset(vin: String): Response<UnidentifiedResponse>
    {
        return truckSpotAPI.getOffset(vin)
    }*/


//    private fun getDayStartELDGraphData(str: String): ELDGraphData {
//        val dateUtils: DateUtils = DateUtils.INSTANCE
//        val of: LocalDateTime = LocalDateTime.m333of(
//            LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd")),
//            LocalTime.MIDNIGHT
//        )
//        Intrinsics.checkNotNullExpressionValue(
//            of,
//            "of(\n            LocalDat…alTime.MIDNIGHT\n        )"
//        )
//        val previousDaysLastDutyLogFromMilli: List<EventLog> =
//            this.eventLogDao.getPreviousDaysLastDutyLogFromMilli(dateUtils.toEpochMilli(of))
//        return if (previousDaysLastDutyLogFromMilli.isEmpty()) {
//            ELDGraphData(
//                DateUtils.INSTANCE.getHour("00:00"),
//                DutyStatus.OFF_DUTY,
//                0,
//                4,
//                null as DefaultConstructorMarker?
//            )
//        } else ELDGraphData(
//            DateUtils.INSTANCE.getHour("00:00"),
//            previousDaysLastDutyLogFromMilli[0].getEventName(),
//            0,
//            4,
//            null as DefaultConstructorMarker?
//        )
//    }
//
//    private fun currentDayELDGraphDataList(str: String): List<ELDGraphData> {
//        val str2: String
//        val f: Float
//        val dateUtils: DateUtils = DateUtils.INSTANCE
//        val charSequence: CharSequence = str
//        val of: LocalDateTime = LocalDateTime.m333of(
//            LocalDate.parse(
//                charSequence,
//                DateTimeFormatter.ofPattern("yyyy-MM-dd")
//            ), LocalTime.MIDNIGHT
//        )
//        Intrinsics.checkNotNullExpressionValue(
//            of,
//            "of(\n            LocalDat…alTime.MIDNIGHT\n        )"
//        )
//        val epochMilli: Long = dateUtils.toEpochMilli(of)
//        val arrayList: MutableCollection<*> = ArrayList<Any?>()
//        for (next in this.eventLogDao.getParticularDayEventLogs(
//            epochMilli,
//            86400000L + epochMilli
//        )) {
//            val eventLog = next as EventLog
//            if (Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.ON_DUTY as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.OFF_DUTY as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    "DRIVE" as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.INT as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.SLEEP as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.PERSONAL_USE as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.YARD_MOVE as Any
//                ) || Intrinsics.areEqual(
//                    eventLog.getEventName() as Any?,
//                    DutyStatus.INTERMEDIATE as Any
//                )
//            ) {
//                arrayList.add(next)
//            }
//        }
//        val list: List<EventLog> = arrayList as List<*>
//        val arrayList2: MutableList<ELDGraphData> = ArrayList()
//        for (eventLog2 in list) {
//            val hour: Float =
//                DateUtils.INSTANCE.getHour(DateUtils.INSTANCE.milliToTime(eventLog2.getEventUTCTimestamp()))
//            val eventName: String = eventLog2.getEventName()
//            val id: Long = eventLog2.getId()
//            Intrinsics.checkNotNull(id)
//            arrayList2.add(ELDGraphData(hour, eventName, id))
//        }
//        if (list.isEmpty()) {
//            str2 = getDayStartELDGraphData(str).status
//        } else {
//            str2 = (CollectionsKt.last(list) as EventLog).getEventName()
//        }
//        f = if (Intrinsics.areEqual(
//                LocalDate.parse(charSequence, DateTimeFormatter.ofPattern("yyyy-MM-dd")) as Any,
//                LocalDate.now(
//                    ZoneId.m349of(UserSession.INSTANCE.getTimeZone())
//                ) as Any?
//            )
//        ) {
//            DateUtils.INSTANCE.getHour()
//        } else {
//            DateUtils.INSTANCE.getHour("23:59")
//        }
//        arrayList2.add(ELDGraphData(f, str2, 0, 4, null as DefaultConstructorMarker?))
//        return arrayList2
//    }
//
//    fun getLogDayList(str: String, list: List<String>): List<LogData> {
//        var str2: String
//        var str3: String
//        val str4: String
//        val str5: String
//        Intrinsics.checkNotNullParameter(str, "dateString")
//        Intrinsics.checkNotNullParameter(list, "logDisplaySettings")
//        val dateUtils: DateUtils = DateUtils.INSTANCE
//        val of: LocalDateTime = LocalDateTime.m333of(
//            LocalDate.parse(
//                str,
//                DateTimeFormatter.ofPattern("yyyy-MM-dd")
//            ), LocalTime.MIDNIGHT
//        )
//        Intrinsics.checkNotNullExpressionValue(
//            of,
//            "of(\n            LocalDat…alTime.MIDNIGHT\n        )"
//        )
//        val epochMilli: Long = dateUtils.toEpochMilli(of)
//        val arrayList: MutableList<LogData> = ArrayList<LogData>()
//        val logDayEventLogs: List<EventLog> =
//            this.eventLogDao.getLogDayEventLogs(epochMilli, 86400000L + epochMilli)
//        val lastDutyLogFromMilli: List<EventLog> =
//            this.eventLogDao.getLastDutyLogFromMilli(epochMilli)
//        val arrayList2: MutableCollection<*> = ArrayList<Any?>()
//        for (next in LOG_DISPLAY_SETTING_LIST) {
//            if (!list.contains(next as String)) {
//                arrayList2.add(next)
//            }
//        }
//        val dutyStatusNameFromLogDisplaySettings: List<String> =
//            getDutyStatusNameFromLogDisplaySettings(arrayList2 as List<*>)
//        val arrayList3: MutableCollection<*> = ArrayList<Any?>()
//        for (next2 in lastDutyLogFromMilli) {
//            if (!dutyStatusNameFromLogDisplaySettings.contains(next2.getEventName())) {
//                arrayList3.add(next2)
//            }
//        }
//        val list3 = arrayList3 as List<*>
//        val arrayList4: MutableCollection<*> = ArrayList<Any?>()
//        for (next3 in logDayEventLogs) {
//            if (!dutyStatusNameFromLogDisplaySettings.contains(next3.getEventName())) {
//                arrayList4.add(next3)
//            }
//        }
//        val list4: List<EventLog> = arrayList4 as List<*>
//        if (!list3.isEmpty()) {
//            val id: Long = (CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getId()
//            val longValue = id ?: -1
//            val serverId: String =
//                (CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getServerId()
//            str4 = serverId ?: ""
//            val trimEventStatus: String =
//                trimEventStatus((CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getEventName())
//            if (Intrinsics.areEqual(
//                    (CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getLocationDescription() as Any?,
//                    "E" as Any
//                )
//            ) {
//                str5 = "Location Not Available"
//            } else {
//                str5 =
//                    (CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getLocationDescription()
//            }
//            arrayList.add(
//                LogData(
//                    longValue,
//                    str4,
//                    "00:00",
//                    trimEventStatus,
//                    str5,
//                    java.lang.String.valueOf((CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getEngineMiles()),
//                    java.lang.String.valueOf((CollectionsKt.first(lastDutyLogFromMilli) as EventLog).getEngineHours()),
//                    if ((CollectionsKt.first(lastDutyLogFromMilli) as EventLog).isAutoGenerated()) "Auto" else "Manual",
//                    false,
//                    256,
//                    null as DefaultConstructorMarker?
//                )
//            )
//        }
//        for (eventLog in list4) {
//            val id2: Long = eventLog.getId()
//            val longValue2 = id2 ?: -1
//            val serverId2: String = eventLog.getServerId()
//            str2 = serverId2 ?: ""
//            val milliToDate: String =
//                DateUtils.INSTANCE.milliToDate(eventLog.getEventUTCTimestamp())
//            val trimEventStatus2: String = trimEventStatus(eventLog.getEventName())
//            if (Intrinsics.areEqual(eventLog.getLocationDescription() as Any?, "E" as Any)) {
//                str3 = "Location Not Available"
//            } else {
//                str3 = eventLog.getLocationDescription()
//            }
//            arrayList.add(
//                LogData(
//                    longValue2,
//                    str2,
//                    milliToDate,
//                    trimEventStatus2,
//                    str3,
//                    java.lang.String.valueOf(eventLog.getEngineMiles()),
//                    java.lang.String.valueOf(eventLog.getEngineHours()),
//                    if (eventLog.isAutoGenerated()) "Auto" else "Manual",
//                    false,
//                    256,
//                    null as DefaultConstructorMarker?
//                )
//            )
//        }
//        return arrayList
//    }

}


interface CsvDownloadApi {
    @GET("/api/v1/output-file/{id}") // Replace with your actual endpoint
    suspend fun downloadCsv(
        @Path("id") id: Int,
        @Query("start") startDate: String,
        @Query("end") endDate: String
    ): Response<CsvDownloadResponse>

    @GET
    suspend fun downloadFile(@Url dynamicEndpoint: String): Response<ResponseBody>

}

data class CsvDownloadResponse(
    val status: Int,
    val message: String,
    val data: String,
    val downloadUrl: String
)