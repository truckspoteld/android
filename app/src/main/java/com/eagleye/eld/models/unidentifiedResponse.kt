package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

data class UnidentifiedResponse(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("results")
	val results: ResultsUnidentifiedResponse? = null,

	@field:SerializedName("status")
	val status: Boolean? = null
)

data class ResultsUnidentifiedResponse(

	@field:SerializedName("totalCount")
	val totalCount: Int? = null,

	@field:SerializedName("unidentifiedRecords")
	val unidentifiedRecords: List<UnidentifiedRecordsItem?>? = null
)

data class RejectUnidentifiedRequest(
	@field:SerializedName("minutes") val minutes: Double,
	@field:SerializedName("vin") val vin: String?,
	@field:SerializedName("odometer") val odometer: Double?,
	@field:SerializedName("eng_hours") val eng_hours: Double?,
	@field:SerializedName("start_datetime") val start_datetime: String?,
	@field:SerializedName("end_datetime") val end_datetime: String?,
)

data class UnidentifiedRecordsItem(

	@field:SerializedName("vin_no")
	val vinNo: String? = null,

	@field:SerializedName("odometer")
	val odometer: String? = null,

	@field:SerializedName("eng_hour")
	val engHour: String? = null,

	@field:SerializedName("id")
	val id: Any? = null
)
