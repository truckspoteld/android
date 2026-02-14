package com.truckspot.repository

import android.annotation.SuppressLint
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.truckspot.api.TruckSpotAPI
import com.truckspot.models.LoginResponse
import com.truckspot.pt.devicemanager.AppModel
import com.truckspot.request.AddLogRequest
import com.truckspot.request.LoginRequest
import com.truckspot.utils.Constants.TAG
import com.truckspot.utils.NetworkResult
import com.truckspot.utils.PrefRepository
import retrofit2.Response
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import javax.inject.Inject

var miles: BigDecimal? = null
var mOdometer: String? = null
var mEngineHours: String? = null
var speed: Int = 0
var location: String? = null

class LoginRespository @Inject constructor(private val truckSpotAPI: TruckSpotAPI) {
    lateinit var prefRepository: PrefRepository
    var mActivity: FragmentActivity? = null
    private val _loginResponseLiveData = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginResponseLiveData: LiveData<NetworkResult<LoginResponse>>
        get() = _loginResponseLiveData

    @SuppressLint("LongLogTag")
    suspend fun loginUser(loginRequest: LoginRequest) {
        _loginResponseLiveData.postValue(NetworkResult.Loading())
        try {
            val response = truckSpotAPI.login(loginRequest = loginRequest)
            Log.d("Login response will come here", "LOGINRESPONSE:$response")
            handleResponse(response)
            if (response.isSuccessful && response.body() != null) {
            _loginResponseLiveData.postValue(NetworkResult.Success(response.body()!!))
            val userLogsResponse = truckSpotAPI.getAllLog()
            if (userLogsResponse.isSuccessful) {
                val userLogs = userLogsResponse.body()?.response?.results?.userLogs
                if (userLogs != null && userLogs.isNotEmpty()) {
                    val reversedLogs = userLogs.reversed()
                    val mostRecentLog = reversedLogs.first()
                    val modename = mostRecentLog.modename
                    val te = AppModel.getInstance().mLastEvent
                    val defaultOdometer = "1"
                    val defaultEngineHours = "1"
                    val defaultLatitude = 0.0
                    val defaultLongitude = 0.0
                    var vin = "1C6RREHT5NN451094"

                    if (AppModel.getInstance().mVehicleInfo != null && AppModel.getInstance().mVehicleInfo.VIN != null) {
                        vin = AppModel.getInstance().mVehicleInfo.VIN
                    }

                    val offSet = truckSpotAPI.getOffset(vin)
//                    val logRequest = AddLogRequest(
//                        modename,
//                        (te?.mOdometer?.toInt()
//                            ?.minus(offSet.body()?.results?.unidentifiedRecords?.get(0)?.odometer?.toInt()!!)).toString()
//                            ?: "1",
//                        (te?.mEngineHours?.toInt()
//                            ?.minus(offSet.body()?.results?.unidentifiedRecords?.get(0)?.engHour?.toInt()!!)).toString()
//                            ?: "1",
//                        2,
//                        "0.0,0.0",
//                        5,
//                        1,
//                        1,
//                        "1C6RREHT5NN451094",
//                        ""
//                    )
                    try {
//                        val addLogResponse = truckSpotAPI.addLog(logRequest)
//                        if (addLogResponse.isSuccessful && addLogResponse.body() != null) {
//                            Log.d(
//                                "check type here",
//                                "Add Log API Response: ${addLogResponse.body()?.toString()}"
//                            )
//                        } else {
//                            Log.e(
//                                TAG,
//                                "Add Log API Error: ${addLogResponse.code()} - ${addLogResponse.message()}"
//                            )
//                        }
                    } catch (e: Exception) {
                        Log.e("ssss", "Error calling Add Log API: ${e.message}")
                    }
                    Log.d(TAG, "Most recent modename: $modename")
                } else {
                    Log.e(TAG, "No logs found")
                }
            } else {
                Log.e(TAG, "API call failed: ${userLogsResponse.code()}")
            }
        } else if (response.errorBody() != null) {
            _loginResponseLiveData.postValue(NetworkResult.Error("Something Went wrong at API END"))
        } else {
            _loginResponseLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
        }
        } catch (e: ConnectException) {
            Log.e(TAG, "Login connection failed: ${e.message}")
            _loginResponseLiveData.postValue(NetworkResult.Error("Cannot reach server. Check that the server is running and your network (e.g. same Wi‑Fi as server)."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Login timeout: ${e.message}")
            _loginResponseLiveData.postValue(NetworkResult.Error("Server did not respond in time. Check your connection."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Login unknown host: ${e.message}")
            _loginResponseLiveData.postValue(NetworkResult.Error("Cannot reach server. Check the server address and your network."))
        } catch (e: IOException) {
            Log.e(TAG, "Login network error: ${e.message}")
            _loginResponseLiveData.postValue(NetworkResult.Error("Network error: ${e.message ?: "Check your connection."}"))
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            _loginResponseLiveData.postValue(NetworkResult.Error(e.message ?: "Something went wrong."))
        }
    }

    private suspend fun handleResponse(response: Response<LoginResponse>) {
        if (response.isSuccessful && response.body() != null) {
            _loginResponseLiveData.postValue(NetworkResult.Success(response.body()!!))
            Log.d("response here", "${response.body()}")

            // adding user logs record here & state information as well


        } else if (response.errorBody() != null) {
            //val errorObj = JSONObject(response.errorBody()?.charStream()?.readText())
            _loginResponseLiveData.postValue(NetworkResult.Error("Something Went wrong at API END"))
        } else {
            _loginResponseLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
        }
    }
}

