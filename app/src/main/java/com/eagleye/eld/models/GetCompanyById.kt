package com.eagleye.eld.models

data class GetCompanyById(
    val code: Int,
    val message: String,
    val results: Results,
    val status: Boolean
) {
    data class Results(
        val address: String? = null,
        val admin_email: String? = null,
        val admin_firstname: String? = null,
        val admin_password: String? = null,
        val city: String? = null,
        val company_name: String? = null,
        val company_timezone: String? = null,
        val country: String? = null,
        val dot_no: String? = null,
        val id: Int? = null,
        val multidaybasis: Int? = null,
        val phone_no: String? = null,
        val startingtime: Int? = null,
        val state: String? = null,
        val zip: String? = null
    )
}