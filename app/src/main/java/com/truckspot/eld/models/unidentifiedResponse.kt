package com.truckspot.eld.models

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
