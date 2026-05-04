package com.eagleye.eld.models

data class EldAttentionSummary(
    val show: Boolean = false,
    val reasons: List<String>? = null,
    val malfunctionCodesActive: List<Int>? = null,
    val malfunctionLettersActive: List<String>? = null,
    val diagnosticCodesActive: List<Int>? = null,
    val engineSyncDiagnosticActive: Boolean = false,
    val dutyStatusDataDiagnosticActive: Boolean = false,
) {
    val hasMalfunction: Boolean get() = !(malfunctionCodesActive.isNullOrEmpty())
    val hasDiagnostic: Boolean get() = !(diagnosticCodesActive.isNullOrEmpty()) || dutyStatusDataDiagnosticActive
}

data class HomeDataModel(
    var conditions: Conditions? = Conditions(),
    var logs: List<Log>? = listOf(),
    var previousDayLog: Log? = Log(),
    var latestUpdatedLog: Log? = Log(),
    var meta: Meta? = Meta(),
    var totalCount: Int? = 0,
    var eldAttention: EldAttentionSummary? = null
) {
    data class Conditions(
        var createdat: String? = "",
        var cycle: Int? = 0,
        var cycleViolation: Boolean? = false,
        var drive: Int? = 0,
        var drivebreak: Int? = 0,
        var driveBreakViolation: Boolean? = false,
        var driveViolation: Boolean? = false,
        var shift: Int? = 0,
        var shiftViolation: Boolean? = false,
        var updatedat: String? = ""
    )

    data class Log(
        var authorization_status: String? = "",
        var certification_date: Any? = Any(),
        var certification_status: Any? = Any(),
        var codriverid: Int? = 0,
        var company_timezone: String? = "",
        var created_on: String? = "",
        var datadiagnostic: Int? = 0,
        var date: String? = "",
        var datetime: String? = "",
        var discreption: Any? = Any(),
        var distance: String? = "",
        var driverid: Int? = 0,
        var duration: Int? = 0,
        /** Duration in seconds from backend; use for accurate display. Exclude login/logout from totals. */
        var duration_seconds: Int? = null,
        var end_datetime: Any? = Any(),
        var eng_hours: String? = "",
        var enginemiles: String? = "",
        var event_status: Int? = 0,
        var eventcode: Int? = 0,
        var eventrecordorigin: Any? = Any(),
        var eventrecordstatus: Int? = 0,
        var eventsequenceid: String? = "",
        var eventtype: Int? = 0,
        var exempt_driver: Int? = 0,
        var hours: Int? = 0,
        var id: Int? = 0,
        var is_autoinsert: Int? = 0,
        var lat: String? = "",
        var linedata_checkvalue: Any? = Any(),
        var location: String? = "",
        var long: String? = "",
        var malfunctioneld: Int? = 0,
        var modename: String? = "",
        var odometerreading: String? = "",
        var order_number: String? = "",
        var powerunitnumber: String? = "",
        var shipping_number: Any? = Any(),
        var time: String? = "",
        var trailer_number: Any? = Any(),
        var vin: String? = ""
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