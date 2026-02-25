package com.eagleye.eld.models

data class DvirDriver(
    val id: Int? = null,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null
)

data class DvirVehicle(
    val id: Int? = null,
    val truck_no: String? = null,
    val vin_no: String? = null,
    val plate_no: String? = null
)

data class DvirReport(
    val id: Int? = null,
    val driverid: Int? = null,
    val companyid: Int? = null,
    val vehicleid: Int? = null,
    val vin_no: String? = null,
    val report_type: String? = null,
    val report_date: String? = null,
    val odometer: String? = null,
    val trailer_number: String? = null,
    val location: String? = null,
    val vehicle_condition: String? = null,
    val has_defects: Boolean? = null,
    val defects_description: String? = null,
    val safe_to_operate: Boolean? = null,
    val driver_signature: String? = null,
    val status: String? = null,
    val review_notes: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val driver: DvirDriver? = null,
    val vehicle: DvirVehicle? = null
)

data class DvirListResults(
    val reports: List<DvirReport>? = emptyList(),
    val totalCount: Int? = 0
)

data class DvirListResponse(
    val status: Boolean? = false,
    val message: String? = null,
    val results: DvirListResults? = null
)

data class DvirCreateResponse(
    val status: Boolean? = false,
    val message: String? = null,
    val results: DvirReport? = null
)
