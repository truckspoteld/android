package com.eagleye.eld.utils

import com.eagleye.eld.BuildConfig
import com.eagleye.eld.utils.Constants.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls the backend version gate (GET /app/version-check) on launch. Self-contained
 * (raw OkHttp, no DI) and fails OPEN: any error/timeout returns null so startup is
 * never blocked by a network hiccup. Only a clear `update_required:true` blocks the app.
 */
object AppUpdateChecker {

    data class UpdateInfo(val required: Boolean, val message: String?, val storeUrl: String?)

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()

            val url = BASE_URL.trimEnd('/') + "/api/v1/app/version-check"
            val request = Request.Builder()
                .url(url)
                .addHeader("x-app-platform", "android")
                .addHeader("x-app-build", BuildConfig.VERSION_CODE.toString())
                .addHeader("x-app-version", BuildConfig.VERSION_NAME)
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                val required = json.optBoolean("update_required", false)
                if (!required) return@use UpdateInfo(false, null, null)
                UpdateInfo(
                    required = true,
                    message = json.optString("message", null)
                        ?: "Please update the app to continue.",
                    storeUrl = json.optString("store_url", null)
                )
            }
        } catch (e: Exception) {
            null // fail open
        }
    }
}
