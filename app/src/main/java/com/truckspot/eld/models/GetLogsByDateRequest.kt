package com.truckspot.eld.models

data class GetLogsByDateRequest(
    val driverId: Int,
    val fromdate: String,
    val todate: String
)