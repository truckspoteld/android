package com.truckspot.eld.models

data class AddLogSuccessResponse(
    val success: Boolean,
    val message: String,
    val data: Data
) {
    data class Data(
        val id: Int,
        val eventsequenceid: String,
        val lockOtherModes: Boolean,
        val minutesLeft: Int,
        val adjustmentMade: Boolean,
        val adjustmentMinutes: Int,
        val conditions: Conditions?
    ) {
        data class Conditions(
            val drive: Int,
            val shift: Int,
            val cycle: Int,
            val driveBreak: Int,
            val driveBreakViolation: Boolean,
            val driveViolation: Boolean,
            val shiftViolation: Boolean,
            val cycleViolation: Boolean
        )
    }
}
