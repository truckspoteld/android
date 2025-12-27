package com.truckspot.models

import java.util.Date

data class TrackingModelNew(
    var startTime:String="",
    var endTime:String= "",
    var hoursinThisMode:Double= 0.0,
    var modeType:DRIVE_MODE=DRIVE_MODE.MODE_OFF
)

