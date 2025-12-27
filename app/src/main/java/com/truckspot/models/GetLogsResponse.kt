package com.truckspot.models

import java.util.Date

data class GetLogsResponse(
    val code: Int,
    val message: String,
    val results: ResultsNew,
    val status: Boolean
)

data class GetAllLogsResponse(
    val response: ResponseData
)

data class ResponseData(
    val code: Int,
    val message: String,
    val status: Boolean,
    val results: ResultsAll
)

data class ResultsAll(
    val userLogs: List<UserLogs>
)

data class UserLogs(
    val modename: String, // Add other properties here if needed
    val date: String = "",
    val time: String = "",
)

data class GetDatesResponses(
    val dates: List<String>
)

data class ResultsNew(
    val totalCount: Int,
    val currentTime: String,
    val queryDate: String,
    val userLogs: List<UserLog>,
    val allLogs: List<UserLog>
)

data class UserLog(
    val id: Int = -1,
    val driverid: Int = -1,
    val created_on: String = "",
    val date: String = "",
    val datetime: String = "",
    val discreption: Any? = Any(),
    val eng_hours: String = "",
    val is_autoinsert: String = "",
    val location: String = "",
    val modename: String = "",
    val hours: Double = 0.0,
    val end_DateTime: String? = null,
    val odometerreading: String = "",
    val time: String = "",
    val timesheet: String = "",
    val vin: Any = "",
    val authorization_status: String = "",
    val company_timezone: String = "",
    val comments: String = ""
)

data class GetReportsResponse(
    val code: Int,
    val message: String,
    val status: Boolean,
    val results: ReportsResults
)

data class ReportsResults(
    val userLogs: List<UserLogsItem>,
    val totalCount: Int,
    val queryDate: String
)

data class UserLogsItem(
    val date: String,
    val logs: List<ReportLogItem>
)

data class ReportLogItem(
    val id: Int = 0,
    val discreption: String? = null,
    val modename: String = "",
    val odometerreading: String = "",
    val location: String = "",
    val eng_hours: String = "",
    val vin: String? = null,
    val date: String = "",
    val is_autoinsert: String = "",
    val datetime: String = "",
    val time: String = "",
    val created_on: String = "",
    // ... other properties
)
