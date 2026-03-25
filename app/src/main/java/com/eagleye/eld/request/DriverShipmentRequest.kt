package com.eagleye.eld.request

import com.google.gson.annotations.SerializedName

data class DriverShipmentRequest(
    @SerializedName("shipping_number") val shippingNumber: Int,
    @SerializedName("trailer_number") val trailerNumber: Int? = null,
    @SerializedName("codriverid") val codriverId: Int? = null
)
