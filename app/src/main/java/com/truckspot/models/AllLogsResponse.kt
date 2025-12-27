package com.truckspot.models

data class AllLogsResponse(
    val logs: List<UserLog>,
    val totalCount: Int
)