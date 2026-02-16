package com.eagleye.eld.models

import com.eagleye.eld.models.HomeDataModel.Log

data class GetLogsByDateResponse(
    val code: Int,
    val message: String,
    val results: Results,
    val status: Boolean
) {
    data class Results(
        val conditions: Conditions,
        val totalCount: Int,
        val meta: Meta,
        val userLogs: List<UserLog>,
        val nextDayLog: UserLog?,
        val previousDayLog: UserLog?,
        val latestUpdatedLog: UserLog?,
        ) {
        data class Conditions(
            val createdat: String,
            val cycle: Int,
            val cycleviolation: Boolean,
            val drive: Int,
            val drivebreak: Int,
            val drivebreakviolation: Boolean,
            val driveviolation: Boolean,
            val shift: Int,
            val shiftviolation: Boolean,
            val updatedat: String,
            val userid: Int
        )

        data class UserLog(
            val authorization_status: String,
            val certification_date: Any,
            val certification_status: Any,
            val codriverid: Int,
            val company_timezone: String,
            val created_on: String,
            val datadiagnostic: Int,
            val date: String,
            val datetime: String,
            val discreption: Any,
            val distance: String,
            val driverid: Int,
            /** Duration in seconds. */
            val duration: Int,
            val end_datetime: Any,
            val eng_hours: String,
            val enginemiles: String,
            val event_status: Int,
            val eventcode: Int,
            val eventrecordorigin: Any,
            val eventrecordstatus: Int,
            val eventsequenceid: String,
            val eventtype: Int,
            val exempt_driver: Int,
            val hours: Int,
            val id: Int,
            val is_active: Int,
            val is_autoinsert: Int,
            val lat: String,
            val linedata_checkvalue: Any,
            val location: String,
            val long: String,
            val malfunctioneld: Int,
            val modename: String,
            val odometerreading: String,
            val order_number: String,
            val powerunitnumber: String,
            val shipping_number: Any,
            val time: String,
            val trailer_number: Any,
            val vin: String
        )
        data class Meta(
            var d: Int? = 0,
            var off: Int? = 0,
            var sb: Int? = 0,
            var on: Int? = 0,
            var login: Int? = 0,
            var logout: Int? = 0,
        )
    }
}