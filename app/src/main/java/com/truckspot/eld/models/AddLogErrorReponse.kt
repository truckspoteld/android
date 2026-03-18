package com.truckspot.eld.models

data class AddLogErrorReponse(
    val error: String,
    val lockOtherModes: Boolean,
    val minutesLeft: Int
)