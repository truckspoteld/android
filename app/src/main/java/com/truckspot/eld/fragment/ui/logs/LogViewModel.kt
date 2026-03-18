package com.truckspot.eld.fragment.ui.logs

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckspot.eld.api.TruckSpotAPI
import com.truckspot.eld.models.GetLogsByDateRequest
import com.truckspot.eld.models.GetLogsByDateResponse
import com.truckspot.eld.models.HomeDataModel
import com.truckspot.eld.repository.DashboardRepository
import com.truckspot.eld.utils.NetworkResult
import com.truckspot.eld.utils.PrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
@HiltViewModel
class LogViewModel @Inject constructor(
    var dashboardRespository: DashboardRepository,
    private val prefRepository: PrefRepository,
    private val truckSpotAPI: TruckSpotAPI,
    ) : ViewModel(){

    val logByDateLiveData: LiveData<NetworkResult<GetLogsByDateResponse>> get() = dashboardRespository.logByDate


    @RequiresApi(Build.VERSION_CODES.O)
    fun getLogs( request: GetLogsByDateRequest ,context : Context) {
        viewModelScope.launch {
            if (isInternetAvailable(context)) {
                dashboardRespository.getLogsByDate(request)
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isInternetAvailable(context: Context): Boolean {
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