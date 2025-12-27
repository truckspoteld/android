package com.truckspot.models

data class GetCompanyById(
    val code: Int,
    val message: String,
    val results: Results,
    val status: Boolean
) {
    data class Results(
        val address: String,
        val admin_email: String,
        val admin_firstname: String,
        val admin_password: String,
        val city: String,
        val company_name: String,
        val company_timezone: String,
        val country: String,
        val dot_no: String,
        val id: Int,
        val multidaybasis: Int,
        val phone_no: String,
        val startingtime: Int,
        val state: String,
        val zip: String
    )
}