package com.truckspot.newclasses

import com.google.gson.annotations.SerializedName

abstract class BaseResponse(
    @field:SerializedName("statusCode")
    val httpResponse : Int? = 0,

    @field:SerializedName("code")
    val code: Int? = null,

    @field:SerializedName("status")
    val status: Int = 0,
    @SerializedName("message")
    var msg: String? = null

)