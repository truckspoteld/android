package com.truckspot.eld.models

import com.truckspot.eld.request.AddOffsetRequest

data class offsetResponse(
    val code: Int,
    val message: String,
    val status: Boolean,
    val offsetResponse: AddOffsetRequest
)