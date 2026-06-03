package com.eagleye.eld.utils

object Constants {
    const val TAG = "Truck Spot"

    // API base URL — PRODUCTION
    const val BASE_URL = "https://api.truckspoteld.com/"
    const val SOCKET_URL = "https://api.truckspoteld.com/"

    const val ACTION_SESSION_REPLACED = "com.eagleye.eld.SESSION_REPLACED"
    // Fired when the ELD reports a different real VIN than the anchored one — driver must confirm.
    const val ACTION_VIN_CHANGE = "com.eagleye.eld.VIN_CHANGE"
}