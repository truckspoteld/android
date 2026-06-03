package com.eagleye.eld.request

// Driver enters ONLY the dashboard odometer reading; the backend computes the per-vehicle offset
// (dashboard - live ECM) and stores it. The driver never sees or computes the offset.
data class CalibrateOdometerRequest(
    val vin_no: String,
    val odometer: Double
)
