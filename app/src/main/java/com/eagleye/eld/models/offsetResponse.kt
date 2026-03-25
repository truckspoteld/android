package com.eagleye.eld.models

import com.eagleye.eld.request.AddOffsetRequest

data class offsetResponse(
    val code: Int,
    val message: String,
    val status: Boolean,
    val offsetResponse: AddOffsetRequest
)