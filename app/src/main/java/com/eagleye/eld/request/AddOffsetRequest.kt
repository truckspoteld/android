package com.eagleye.eld.request

data class AddOffsetRequest(
    val odometer: Int?,
    val eng_hour: Int?,
    val vin_no: String
)
