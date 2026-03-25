package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

data class DriverReviewResponse(
    @SerializedName("status")
    val status: Boolean? = null,
    @SerializedName("data")
    val data: DriverReviewData? = null
)

data class DriverReviewData(
    @SerializedName("driver")
    val driver: DriverDetails? = null,
    @SerializedName("company")
    val company: CompanyDetails? = null
)

data class DriverDetails(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("companyid") val companyid: Int? = null,
    @SerializedName("vin_no") val vinNo: String? = null,
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("licensedate") val licensedate: String? = null,
    @SerializedName("licensestate") val licenseState: String? = null,
    @SerializedName("ordernumber") val ordernumber: String? = null,
    @SerializedName("is_active") val isActive: Int? = null
)

data class CompanyDetails(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("dot_no") val dotNo: String? = null,
    @SerializedName("admin_email") val adminEmail: String? = null,
    @SerializedName("admin_firstname") val adminFirstname: String? = null,
    @SerializedName("admin_lastname") val adminLastname: String? = null,
    @SerializedName("phone_no") val phoneNo: String? = null,
    @SerializedName("company_timezone") val companyTimezone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("zip") val zip: String? = null,
    @SerializedName("multidaybasis") val multidaybasis: String? = null,
    @SerializedName("startingtime") val startingtime: String? = null
)
