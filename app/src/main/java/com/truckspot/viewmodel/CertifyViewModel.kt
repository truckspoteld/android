package com.truckspot.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckspot.models.CertifyModelItem
import com.truckspot.repository.CertifyRepository
import com.truckspot.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CertifyViewModel(private val certifyRepository: CertifyRepository) : ViewModel() {

    // Observe LiveData for get dates operation
    val certifyData: LiveData<NetworkResult<List<CertifyModelItem>>>
        get() = certifyRepository.certifyData

    // Observe LiveData for update certified operation
    val updateCertifyData: LiveData<NetworkResult<CertifyModelItem>>
        get() = certifyRepository.updateCertifyData

    init {
        // loadCertifyData() // Remove auto-loading since we need context
    }

    fun loadCertifyData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isInternetAvailable(context)) {
                certifyRepository.getCertifyData()
            } else {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateCertified(certifyModelItem: CertifyModelItem, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isInternetAvailable(context)) {
                certifyRepository.updateCertified(certifyModelItem)
            } else {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network: Network? = connectivityManager.activeNetwork
        val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && (
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }
}
