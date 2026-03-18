package com.truckspot.eld.fragment.ui.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.truckspot.eld.models.UserLog
import com.truckspot.eld.utils.ELDGraphData
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(var app: Application): ViewModel() {
   // var trackingModel: ObservableField<TrackingModel> = ObservableField(TrackingModel())
   var userLogsSocket  = mutableListOf<UserLog>()
   var currentDayUserLog  = mutableListOf<UserLog>()
    val list = mutableListOf<ELDGraphData>()
    private val _logsLiveData = MutableLiveData<List<UserLog>>()
    val logsLiveData: LiveData<List<UserLog>> get() = _logsLiveData

}