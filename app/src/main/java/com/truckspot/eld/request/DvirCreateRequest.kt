package com.truckspot.eld.request

import com.google.gson.annotations.SerializedName

data class DvirCreateRequest(
    @SerializedName("report_type")
    val reportType: String,
    @SerializedName("report_date")
    val reportDate: String,
    val odometer: String?,
    @SerializedName("trailer_number")
    val trailerNumber: String?,
    val location: String?,
    @SerializedName("vehicle_condition")
    val vehicleCondition: String,
    @SerializedName("has_defects")
    val hasDefects: Boolean,
    @SerializedName("defects_description")
    val defectsDescription: String?,
    val checklist: Map<String, Boolean>,
    @SerializedName("safe_to_operate")
    val safeToOperate: Boolean,
    @SerializedName("driver_signature")
    val driverSignature: String
)
