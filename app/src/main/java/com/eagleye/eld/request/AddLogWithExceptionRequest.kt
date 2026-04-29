package com.eagleye.eld.request

data class AddLogWithExceptionRequest(
    val date: String,
    val time: String,
    val modename: String,
    val odometerreading: String,
    val remark: String
)
