package com.truckspot.eld.models

data class AllLogsResponse(
    val logs: List<UserLog>,
    val totalCount: Int
)