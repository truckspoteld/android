package com.truckspot.models

data class NewLogResponse(
    var logs: List<Log?>? = listOf(),
    var totalCount: Int? = 0
) {
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
        var discreption: String? = "",
        var distance: String? = "",
        var driverid: Int? = 0,
        var duration: Any? = Any(),
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
}