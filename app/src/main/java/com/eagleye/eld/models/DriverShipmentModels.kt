package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

data class CodriverItem(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("role") val role: String? = null
)

data class SetCodriverRequest(@SerializedName("codriverId") val codriverId: Int?)
data class SetCodriverResponse(@SerializedName("status") val status: Boolean = false)

data class CodriverRespondRequest(
    @SerializedName("fromDriverId") val fromDriverId: Int,
    @SerializedName("accepted") val accepted: Boolean
)
data class CodriverRespondResponse(@SerializedName("status") val status: Boolean = false)

data class CodriverHosConditions(
    @SerializedName("drive") val drive: Int? = null,
    @SerializedName("shift") val shift: Int? = null,
    @SerializedName("cycle") val cycle: Int? = null,
    @SerializedName("drivebreak") val drivebreak: Int? = null,
    @SerializedName("driveViolation") val driveViolation: Boolean? = null,
    @SerializedName("shiftViolation") val shiftViolation: Boolean? = null,
    @SerializedName("cycleViolation") val cycleViolation: Boolean? = null,
    @SerializedName("driveBreakViolation") val driveBreakViolation: Boolean? = null
)

data class CodriverHosData(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("currentStatus") val currentStatus: String? = null,
    @SerializedName("conditions") val conditions: CodriverHosConditions? = null
)

data class CodriverHosResponse(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("codriver") val codriver: CodriverHosData? = null,
    @SerializedName("vinMismatch") val vinMismatch: Boolean = false,
    @SerializedName("expectedVin") val expectedVin: String? = null,
    @SerializedName("codriverVin") val codriverVin: String? = null
)

data class DriverCodriversResponse(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("codrivers") val codrivers: List<CodriverItem> = emptyList(),
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("message") val message: String? = null
)

data class DriverShipmentData(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("driverid") val driverId: Int? = null,
    @SerializedName("shipping_number") val shippingNumber: String? = null,
    @SerializedName("trailer_number") val trailerNumber: String? = null,
    @SerializedName("codriverid") val codriverId: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("start_datetime") val startDateTime: String? = null,
    @SerializedName("end_datetime") val endDateTime: String? = null
)

data class DriverShipmentResponse(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: DriverShipmentData? = null
)
