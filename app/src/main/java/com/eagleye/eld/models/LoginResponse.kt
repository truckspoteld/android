package com.eagleye.eld.models

data class LoginResponse(
    val code: Int,
    val message: String,
    val results: Results,
    val status: Boolean
)
data class Results(
    val email: String,
    val token: String,
    val username: String,
    val id: Int,
    val companyid: Int,
    val company_timezone: String? = null
)

data class AllResults(
    val modename:String
)


