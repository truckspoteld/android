package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

data class DriverBehaviorResponse(
    @SerializedName("status")         val status: Boolean,
    @SerializedName("date")           val date: String?,
    @SerializedName("safety_score")   val safetyScore: Int?,
    @SerializedName("safety_grade")   val safetyGrade: String?,
    @SerializedName("event_recorded") val eventRecorded: String?,
)
