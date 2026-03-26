package com.eagleye.eld.models

data class FmcsaEmailTransferRequest(
    val transferCode: String,
    val driverId: Int
)

data class FmcsaEmailTransferResponse(
    val status: Boolean,
    val message: String,
    val data: FmcsaEmailTransferData? = null
)

data class FmcsaEmailTransferData(
    val transferCode: String,
    val driverId: Int,
    val fileName: String,
    val sentTo: String
)
