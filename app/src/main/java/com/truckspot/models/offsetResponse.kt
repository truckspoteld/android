package com.truckspot.models

import com.truckspot.request.AddOffsetRequest

data class offsetResponse(
    val code: Int,
    val message: String,
    val status: Boolean,
    val offsetResponse: AddOffsetRequest
)