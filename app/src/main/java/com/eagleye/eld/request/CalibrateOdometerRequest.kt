package com.eagleye.eld.request

// Driver enters ONLY the dashboard odometer reading; the backend computes the per-vehicle offset
// (dashboard - live ECM) and stores it. The driver never sees or computes the offset.
data class CalibrateOdometerRequest(
    val vin_no: String,
    val odometer: Double,
    // Live ECM odometer (km) read straight off the device at the moment of calibration. Lets the
    // backend compute the offset immediately without waiting for the separate telemetry upload.
    val ecm_km: Double? = null
)
