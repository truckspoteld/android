package com.truckspot.eld.models

import java.util.Date

data class TrackingModel(
    var userId:String="",
    var currentDateTime:Date= Date(),
    var listModeTime:ArrayList<EngineMode> = arrayListOf(),
    var totalHoursOnDuety:Long=0L,
    var isEngineOnViolate:Boolean=false,
    var isTruckDrivingViolate:Boolean=false,
    var isBreakTimeLimitVoilate:Boolean=false,
    var isContinousDrivingVoilate:Boolean=false,
    var weekWiseDrivingVoilate:Boolean=false,
    var driveMode:DRIVE_MODE=DRIVE_MODE.MODE_OFF

)
enum class DRIVE_MODE{
    MODE_OFF,
    MODE_SB,
    MODE_D,
    MODE_ON,
    MODE_YARD,
    MODE_PERSONAL
}
data class EngineMode(
    var engineOnTime:Date=Date(),
    var engineOffTime:Date=Date(),
    var isEngineOff:Boolean=false,
    var hoursOnEngine:Double=0.0,
    var totalHoursDrive:Double=0.0,
    var totalHoursBreak:Double=0.0,
    var listDriveTime:ArrayList<DrivingTime> = arrayListOf(),
    var listBreakTime:ArrayList<BreakTime> = arrayListOf()
)
data class DrivingTime(
    var drivingStartingTime:Date=Date(),
    var drivingStopTime:Date=Date(),
    var hoursDrive:Double=0.0
)
data class BreakTime(
    var breakStartingTime:Date=Date(),
    var breakStopTime:Date=Date(),
    var hoursBreak:Double=0.0
)
