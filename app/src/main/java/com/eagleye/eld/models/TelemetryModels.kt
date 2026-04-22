package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

data class TelemetryRequest(
    @SerializedName("vehicle_id") val vehicleId: Int? = null,
    @SerializedName("vin_no") val vinNo: String? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null,
    @SerializedName("engine_rpm") val engineRpm: Double? = null,
    @SerializedName("engine_speed") val engineSpeed: Double? = null,
    @SerializedName("engine_load") val engineLoad: Double? = null,
    @SerializedName("engine_hours") val engineHours: Double? = null,
    @SerializedName("engine_odometer") val engineOdometer: Double? = null,
    @SerializedName("coolant_temperature") val coolantTemperature: Double? = null,
    @SerializedName("oil_temperature") val oilTemperature: Double? = null,
    @SerializedName("intake_temperature") val intakeTemperature: Double? = null,
    @SerializedName("ambient_temperature") val ambientTemperature: Double? = null,
    @SerializedName("transmission_oil_temperature") val transmissionOilTemperature: Double? = null,
    @SerializedName("turbo_oil_temperature") val turboOilTemperature: Double? = null,
    @SerializedName("intercooler_temperature") val intercoolerTemperature: Double? = null,
    @SerializedName("fuel_tank_temperature") val fuelTankTemperature: Double? = null,
    @SerializedName("oil_pressure") val oilPressure: Double? = null,
    @SerializedName("intake_pressure") val intakePressure: Double? = null,
    @SerializedName("ambient_pressure") val ambientPressure: Double? = null,
    @SerializedName("oil_level_percent") val oilLevelPercent: Double? = null,
    @SerializedName("coolant_level_percent") val coolantLevelPercent: Double? = null,
    @SerializedName("fuel_level_percent") val fuelLevelPercent: Double? = null,
    @SerializedName("fuel_level2_percent") val fuelLevel2Percent: Double? = null,
    @SerializedName("def_level_percent") val defLevelPercent: Double? = null,
    @SerializedName("engine_fuel_rate") val engineFuelRate: Double? = null,
    @SerializedName("engine_fuel_economy") val engineFuelEconomy: Double? = null,
    @SerializedName("total_fuel_used") val totalFuelUsed: Double? = null,
    @SerializedName("total_engine_idle_fuel") val totalEngineIdleFuel: Double? = null,
    @SerializedName("dtc_count") val dtcCount: Int? = null,
    @SerializedName("dtc_codes") val dtcCodes: List<String>? = null,
    @SerializedName("gear") val gear: String? = null,
    @SerializedName("seat_belt") val seatBelt: String? = null,
    @SerializedName("brake_pedal") val brakePedal: String? = null,
    @SerializedName("retarder_percent") val retarderPercent: Double? = null,
    @SerializedName("total_engine_idle_time") val totalEngineIdleTime: Double? = null,
    @SerializedName("total_pto_time") val totalPtoTime: Double? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
)

data class TelemetryResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("health_score") val healthScore: Int?,
    @SerializedName("health_status") val healthStatus: String?,
    @SerializedName("warnings") val warnings: List<String>?,
    @SerializedName("id") val id: Int?,
)

data class VehicleHealthResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("vehicle_id") val vehicleId: Int,
    @SerializedName("health_score") val healthScore: Int?,
    @SerializedName("health_status") val healthStatus: String?,
    @SerializedName("warnings") val warnings: List<String>?,
    @SerializedName("recorded_at") val recordedAt: String?,
)

data class FleetDashboardResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("fleet") val fleet: List<FleetVehicleHealth>?,
    @SerializedName("total") val total: Int,
)

data class FleetVehicleHealth(
    @SerializedName("vehicle_id") val vehicleId: Int,
    @SerializedName("truck_no") val truckNo: String?,
    @SerializedName("vin_no") val vinNo: String?,
    @SerializedName("make") val make: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("health_score") val healthScore: Int?,
    @SerializedName("health_status") val healthStatus: String?,
    @SerializedName("last_updated") val lastUpdated: String?,
)
