package com.eagleye.eld.request

import com.google.gson.annotations.SerializedName

data class DriverBehaviorEventRequest(
    @SerializedName("driver_id")       val driverId: Int,
    @SerializedName("vehicle_id")      val vehicleId: Int? = null,
    @SerializedName("event_type")      val eventType: String? = null,
    @SerializedName("engine_speed")    val engineSpeed: Double? = null,
    @SerializedName("idle_minutes")    val idleMinutes: Double = 0.0,
    @SerializedName("driving_minutes") val drivingMinutes: Double = 0.0,
    @SerializedName("distance_km")     val distanceKm: Double = 0.0,
    @SerializedName("max_speed_kmh")   val maxSpeedKmh: Double? = null,
)
