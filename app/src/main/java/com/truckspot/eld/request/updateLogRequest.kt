package com.truckspot.eld.request

import java.util.Date

data class updateLogRequest(
    val id: Int,
    val hours: Double=0.0,
    val end_datetime: String?= null,

    val modename: String,
    val odometerreading:String,
    val eng_hours:String,
    val time:String,
    val event_status:Int
)

data class updateCertified(
    val date: String,
    val status: Boolean
)

data class updateLogRequestDriver(
val shipping_number:Int,
val trailer_number:Int
)
data class updateLogRequestUser(
    val id: Int,
    val hours: Double=0.0,
    val end_DateTime: String? = null,
    val modenameval :String,
    val location:String,
    val odometer:String,
    val enghours:String,
    val time:String
)