package com.eagleye.eld.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pt.sdk.VirtualDashboard
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.models.FleetDashboardResponse
import com.eagleye.eld.models.TelemetryRequest
import com.eagleye.eld.models.TelemetryResponse
import com.eagleye.eld.models.VehicleHealthResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryRepository(private val api: TruckSpotAPI) {

    private val TAG = "TelemetryRepository"

    private val _vehicleHealth = MutableLiveData<VehicleHealthResponse?>()
    val vehicleHealth: LiveData<VehicleHealthResponse?> = _vehicleHealth

    private val _fleetDashboard = MutableLiveData<FleetDashboardResponse?>()
    val fleetDashboard: LiveData<FleetDashboardResponse?> = _fleetDashboard

    suspend fun sendTelemetry(vehicleId: Int, snapshot: VirtualDashboard.Snapshot): TelemetryResponse? {
        return try {
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val request = TelemetryRequest(
                vehicleId = vehicleId,
                recordedAt = isoDate,
                engineRpm = snapshot.engineRPM?.toDouble(),
                engineSpeed = snapshot.engineSpeed?.toDouble(),
                engineLoad = snapshot.engineLoad,
                engineHours = snapshot.totalEngineHours,
                engineOdometer = snapshot.engineOdometer,
                coolantTemperature = snapshot.coolantTemperature,
                oilTemperature = snapshot.oilTemperature,
                intakeTemperature = snapshot.intakeTemperature,
                ambientTemperature = snapshot.ambientTemperature,
                transmissionOilTemperature = snapshot.transmissionOilTemperature,
                turboOilTemperature = snapshot.turboOilTemperature,
                intercoolerTemperature = snapshot.intercoolerTemperature,
                fuelTankTemperature = snapshot.fuelTankTemperature,
                oilPressure = snapshot.oilPressure?.toDouble(),
                intakePressure = snapshot.intakePressure,
                ambientPressure = snapshot.ambientPressure,
                oilLevelPercent = snapshot.oilLevelPercent,
                coolantLevelPercent = snapshot.coolantLevelPercent,
                fuelLevelPercent = snapshot.fuelLevelPercent,
                fuelLevel2Percent = snapshot.fuelLevel2Percent,
                defLevelPercent = snapshot.defLevelPercent,
                engineFuelRate = snapshot.engineFuelRate,
                engineFuelEconomy = snapshot.engineFuelEconomy,
                totalFuelUsed = snapshot.totalFuelUsed,
                totalEngineIdleFuel = snapshot.totalEngineIdleFuel,
                dtcCount = snapshot.dtcNo?.toInt(),
                gear = snapshot.gear?.toString(),
                seatBelt = snapshot.seatBelt?.toString(),
                brakePedal = snapshot.brakePedal?.toString(),
                retarderPercent = snapshot.retarderPercent,
                totalEngineIdleTime = snapshot.totalEngineIdleTime,
                totalPtoTime = snapshot.totalPtoTime,
            )
            val response = api.saveTelemetry(request)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Log.e(TAG, "sendTelemetry failed: ${e.message}")
            null
        }
    }

    suspend fun fetchVehicleHealth(vehicleId: Int) {
        try {
            val response = api.getVehicleHealth(vehicleId)
            if (response.isSuccessful) {
                _vehicleHealth.postValue(response.body())
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchVehicleHealth failed: ${e.message}")
        }
    }

    suspend fun fetchFleetDashboard() {
        try {
            val response = api.getFleetDashboard()
            if (response.isSuccessful) {
                _fleetDashboard.postValue(response.body())
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchFleetDashboard failed: ${e.message}")
        }
    }
}
