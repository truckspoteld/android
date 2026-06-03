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

    /**
     * Result of [resolveGuardedOdometerForLog]: the odometer string to write on the log,
     * plus the last-good value/VIN to persist for the next event.
     */
    data class GuardedOdometer(
        @JvmField val value: String,
        @JvmField val lastGoodMiles: String?,
        @JvmField val lastGoodVin: String?
    )

    /**
     * Single source of truth for the odometer written to a duty/event log.
     *
     * Rule (per-VIN): once a real reading is captured for a VIN, every later reading for that
     * same VIN must be >= the captured value. A stale/near-zero reading (or any drop) is
     * suppressed and the last-good value is held instead. Only a genuinely higher reading
     * advances the baseline. A different VIN (truck change) starts fresh, so a legitimately
     * lower odometer on another truck is accepted.
     *
     * Pure function: callers pass the current persisted last-good (miles + VIN) and persist
     * the returned [GuardedOdometer.lastGoodMiles]/[GuardedOdometer.lastGoodVin] afterwards.
     */
    @JvmStatic
    fun resolveGuardedOdometerForLog(
        rawOdometerKm: String?,
        diffOffsetKm: String?,
        vin: String?,
        lastGoodMiles: String?,
        lastGoodVin: String?
    ): GuardedOdometer {
        val normalized = normalizeOdometerForLog(rawOdometerKm, diffOffsetKm)
        val miles = normalized.toDoubleOrNull() ?: 0.0
        val cleanVin = vin?.trim().orEmpty()
        val lastGood = lastGoodMiles?.trim()?.toDoubleOrNull() ?: 0.0
        val sameVin = cleanVin.isNotEmpty() && cleanVin == lastGoodVin?.trim()

        // Same VIN with an established baseline: never decrease, never drop to ~0.
        if (sameVin && lastGood > 0.0) {
            // Self-heal a contaminated baseline: if the stored "miles" baseline is really the
            // KILOMETRE-magnitude version of this reading (lastGood ≈ miles × 1.609344), it was
            // seeded with a raw-km value. Re-baseline to the correct miles reading instead of
            // holding the inflated km value forever (the never-decrease rule would otherwise
            // reject every correct lower reading and keep emitting km).
            if (miles >= 1.0 && lastGood > miles &&
                Math.abs(lastGood - miles * 1.609344) <= miles * 0.03) {
                return GuardedOdometer(normalized, normalized, cleanVin) // re-baseline km→miles
            }
            if (miles < 1.0 || miles < lastGood - 0.5) {
                return GuardedOdometer(lastGoodMiles!!, lastGoodMiles, lastGoodVin) // hold last good
            }
            return GuardedOdometer(normalized, normalized, cleanVin) // genuine forward movement
        }

        // No baseline yet (or different VIN): accept the first real reading as the floor.
        if (miles >= 1.0 && cleanVin.isNotEmpty()) {
            return GuardedOdometer(normalized, normalized, cleanVin)
        }

        // Near-zero with no usable baseline: nothing better to show; keep any existing baseline.
        return GuardedOdometer(normalized, lastGoodMiles, lastGoodVin)
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
