package com.eagleye.eld.repository

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.models.CertifyModelItem
import com.eagleye.eld.models.GetLogsResponse
import com.eagleye.eld.models.LogResponse
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.AddLogRequestunauth
import com.eagleye.eld.request.updateLogRequest
import com.eagleye.eld.utils.Constants.TAG
import com.eagleye.eld.utils.NetworkResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import retrofit2.Response
import javax.inject.Inject

class CertifyRepository @Inject constructor(private val truckSpotAPI: TruckSpotAPI) {

    private val _certifyData = MutableLiveData<NetworkResult<List<CertifyModelItem>>>()
    val certifyData: LiveData<NetworkResult<List<CertifyModelItem>>>
        get() = _certifyData

    private val _updateCertifyData = MutableLiveData<NetworkResult<CertifyModelItem>>()
    val updateCertifyData: LiveData<NetworkResult<CertifyModelItem>>
        get() = _updateCertifyData

    suspend fun getCertifyData() {
        _certifyData.postValue(NetworkResult.Loading())
        try {
            val result = truckSpotAPI.getDates()
            if (result.isSuccessful) {
                val certifyModelItems = result.body()
                if (certifyModelItems != null) {
                    _certifyData.postValue(NetworkResult.Success(certifyModelItems))
                } else {
                    _certifyData.postValue(NetworkResult.Error("No data received"))
                }
            } else {
                val errorBody = result.errorBody()
                val errorMessage = errorBody?.string() ?: "Unknown error"
                _certifyData.postValue(NetworkResult.Error(errorMessage))
            }
        } catch (e: Exception) {
            _certifyData.postValue(NetworkResult.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun updateCertified(certifyModelItem: CertifyModelItem) {
        _updateCertifyData.postValue(NetworkResult.Loading())
        try {
            val result = truckSpotAPI.updatedCertified(certifyModelItem)
            if (result.isSuccessful) {
                val response = result.body()
                if (response != null) {
                    _updateCertifyData.postValue(NetworkResult.Success(response))
                } else {
                    _updateCertifyData.postValue(NetworkResult.Error("Update successful but no response data"))
                }
            } else {
                val errorBody = result.errorBody()
                val errorMessage = errorBody?.string() ?: "Unknown error"
                _updateCertifyData.postValue(NetworkResult.Error(errorMessage))
            }
        } catch (e: Exception) {
            _updateCertifyData.postValue(NetworkResult.Error(e.message ?: "Unknown error"))
        }
    }
}
