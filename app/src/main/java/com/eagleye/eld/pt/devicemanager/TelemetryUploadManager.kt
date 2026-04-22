package com.eagleye.eld.pt.devicemanager

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.pt.sdk.VirtualDashboard
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.repository.TelemetryRepository
import com.eagleye.eld.utils.Constants.BASE_URL
import com.eagleye.eld.utils.PrefRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Uploads a PT-40 engine snapshot to the backend at most once every UPLOAD_INTERVAL_MS.
object TelemetryUploadManager {

    private const val TAG = "TelemetryUploadManager"
    private const val UPLOAD_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

    private var lastUploadTime = 0L
    private val scope = CoroutineScope(Dispatchers.IO)
    private var cachedApi: TruckSpotAPI? = null

    private fun buildApi(token: String): TruckSpotAPI {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                )
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(TruckSpotAPI::class.java)
    }

    fun onDashboardUpdated(context: Context, snapshot: VirtualDashboard.Snapshot) {
        val now = System.currentTimeMillis()
        if (now - lastUploadTime < UPLOAD_INTERVAL_MS) return

        // Resolve VIN directly from the PT-40 device — always the correct truck
        val vin = AppModel.getInstance().mPT30Vin?.takeIf { it.isNotBlank() && it != "n/a" }
            ?: AppModel.getInstance().mVehicleInfo?.VIN?.takeIf { !it.isNullOrBlank() }
            ?: return

        val token = PrefRepository(context).getToken()
        if (token.isNullOrBlank()) return

        lastUploadTime = now

        if (cachedApi == null) {
            cachedApi = buildApi(token)
        }

        scope.launch {
            try {
                val result = TelemetryRepository(cachedApi!!).sendTelemetry(vin, snapshot)
                if (result != null) {
                    Log.d(TAG, "Telemetry saved [VIN=$vin] — health: ${result.healthScore} (${result.healthStatus})")
                    if (!result.warnings.isNullOrEmpty()) {
                        Log.w(TAG, "Warnings: ${result.warnings.joinToString()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}")
            }
        }
    }
}
