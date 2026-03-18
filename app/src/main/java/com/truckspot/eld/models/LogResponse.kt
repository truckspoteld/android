package com.truckspot.eld.models
data class LogResponse(
    val code: Int,
    val message: Any,
    val status: Boolean
)

data class ReportsDataResponse(
    val carrier: String,
    val drivername: String,
    val dot_no: String,
    val codriver: String?,
    val officeaddress: String,
    val licensestate: String,
    val licensenumber: String

)

