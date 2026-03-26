package com.eagleye.eld.models

data class FmcsaWebServiceTransferRequest(
    val transferCode: String,
    val driverId: Int
)

data class FmcsaWebServiceTransferResponse(
    val status: Boolean,
    val message: String,
    val data: FmcsaWebServiceTransferData? = null
)

data class FmcsaWebServiceTransferData(
    val status: String?,
    val errorCount: Int? = null,
    val submissionId: String? = null,
    val broadcast: String? = null,
    val errors: List<FmcsaWebServiceTransferError>? = null
)

data class FmcsaWebServiceTransferError(
    val errorType: String? = null,
    val message: String? = null,
    val detail: String? = null
)

