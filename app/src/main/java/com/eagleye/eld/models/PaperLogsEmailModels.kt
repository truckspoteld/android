package com.eagleye.eld.models

data class PaperLogsEmailRequest(
    val email: String,
    val driverId: Int,
    val start: String,
    val end: String,
    val fileName: String,
    val pdfBase64: String
)

data class PaperLogsEmailResponse(
    val status: Boolean,
    val message: String
)

