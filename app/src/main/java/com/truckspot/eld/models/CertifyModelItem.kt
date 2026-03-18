package com.truckspot.eld.models

data class CertifyModelItem(
    val date: String,
    val status: Boolean,
    val driverId: Int
)

data class LogIdRequest(
    val id: Int // or the type of ID your logs use
)