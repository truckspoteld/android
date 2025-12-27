package com.truckspot.models

sealed class SPopupData {
    data class Data(
        val drawable: Int,
        val tittle: String,
        val message: String,
        val maxHours: Double,
        val hoursSpent: Double,
        val hoursLeft: Double,
        val violationHour: Double
    ) : SPopupData()
}