package com.eagleye.eld.request


data class AddLogRequest @JvmOverloads constructor(
    val modename: String,
    val odometerreading: String,
//    val location: String? = "Location Not Available",
    val lat: Number,
    val long: Number?,
    val location:  Boolean?,
    val eng_hours: String,
    val vin: String,
    val is_active: Int = 1,
    val is_autoinsert: Int = 1,
    val eventcode: Int = 1,
    val eventtype: Int = 1,
    val date: String = "",
    val time: String = "",
    val connection_status: String = "",
    var discreption: String = "",
    val datetime: String = "",
    val codriverid: Int? = null
)
//{
//    "error": "You can only log 'on' during the first 15 minutes after reset.",
//    "lockOtherModes": true,
//    "minutesLeft": 15
//}

//{
//    "success": true,
//    "message": "New log created",
//    "data": {
//    "id": 1230,
//    "eventsequenceid": "4ce",
//    "lockOtherModes": true,
//    "minutesLeft": 15,
//    "conditions": {
//        "drive": 660,
//        "shift": 840,
//        "cycle": 4200,
//        "driveBreak": 480,
//        "driveBreakViolation": false
//    }
//}
//}

//data class AddLogRequest(
//    val modename: String,
//    val odometerreading: String,
//    val eng_hours: String,
//    val is_autoinsert: Int,
//    val location: String? = "Location Not Available",
//    val eventtype: Int,
//    val shipping_number: Int,
//    val trailer_number: Int,
//    val vin: String,
//    val discreption: String
//)

data class GetLogResponse(
    val authorization_status: String
)

data class AddLogRequestunauth(


    val modename: String,
    val odometerreading: String,
    val eng_hours: String,
    val is_autoinsert: Int


)
