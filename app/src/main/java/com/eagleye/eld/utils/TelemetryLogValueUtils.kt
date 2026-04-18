package com.eagleye.eld.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max

object TelemetryLogValueUtils {
    private const val KM_TO_MILES = 0.621371

    @JvmStatic
    fun normalizeOdometerForLog(rawOdometerKm: String?, diffOffsetKm: String?): String {
        val adjustedKm = max(0.0, parseNonNegative(rawOdometerKm) - parseNonNegative(diffOffsetKm))
        return String.format(Locale.US, "%.2f", adjustedKm * KM_TO_MILES)
    }

    @JvmStatic
    fun normalizeEngineHoursForLog(rawEngineHours: String?, diffOffsetHours: String?): String {
        val adjustedHours = max(0.0, parseNonNegative(rawEngineHours) - parseNonNegative(diffOffsetHours))
        return String.format(Locale.US, "%.2f", adjustedHours)
    }

    @JvmStatic
    fun formatLogValueForDisplay(value: String?): String {
        val numericValue = value?.trim()?.toDoubleOrNull()
            ?: return value?.ifBlank { "0.0" } ?: "0.0"

        val formatter = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).apply {
            minimumFractionDigits = if (numericValue % 1.0 == 0.0) 1 else 0
            maximumFractionDigits = 2
        }
        return formatter.format(numericValue)
    }

    private fun parseNonNegative(value: String?): Double {
        return value?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}
