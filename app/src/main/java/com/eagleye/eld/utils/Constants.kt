package com.eagleye.eld.utils

import com.eagleye.eld.BuildConfig

object Constants {
    const val TAG = "Truck Spot"

    // API base URL — Debug builds hit the DEV server; Release builds hit PRODUCTION.
    val BASE_URL = if (BuildConfig.DEBUG) "https://dev.truckspoteld.com/" else "https://api.truckspoteld.com/"
    val SOCKET_URL = if (BuildConfig.DEBUG) "https://dev.truckspoteld.com/" else "https://api.truckspoteld.com/"

    const val ACTION_SESSION_REPLACED = "com.eagleye.eld.SESSION_REPLACED"
    // Fired when the ELD reports a different real VIN than the anchored one — driver must confirm.
    const val ACTION_VIN_CHANGE = "com.eagleye.eld.VIN_CHANGE"
}