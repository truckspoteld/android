package com.eagleye.eld.models

import com.google.gson.annotations.SerializedName

/**
 * Company vehicle list + add-new-vehicle models. Used by the truck-selection sheet shown when the
 * driver taps the connect pill (pick an existing company truck by VIN, or add a new one which lands
 * on the portal as pending approval — is_added=0).
 */
data class VehicleItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("vin_no") val vinNo: String? = null,
    @SerializedName("truck_no") val truckNo: String? = null,
    @SerializedName("plate_no") val plateNo: String? = null,
    @SerializedName("make") val make: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("powerunitnumber") val powerUnitNumber: String? = null,
    @SerializedName("is_added") val isAdded: Int? = null
)

data class GetVehiclesResponse(
    @SerializedName("vehicles") val vehicles: List<VehicleItem> = emptyList(),
    @SerializedName("totalCount") val totalCount: Int = 0
)

data class AddVehicleRequest(
    @SerializedName("vin_no") val vinNo: String,
    @SerializedName("truck_no") val truckNo: String? = null,
    @SerializedName("make") val make: String? = null,
    @SerializedName("powerunitnumber") val powerUnitNumber: String? = null
)
