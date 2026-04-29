package com.eagleye.eld.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.eagleye.eld.models.UserLog
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_DRIVING
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_OFF
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_ON
import com.eagleye.eld.utils.PrefConstants.TRUCK_MODE_SLEEPING
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Hours
import org.joda.time.LocalTime
import org.joda.time.Minutes
import org.joda.time.Period
import org.joda.time.Seconds
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap


object AlertCalculationUtils {

    const val MID_NIGHT_TIME = "23:59:59"

    var sbIndex: Int = -1
    private const val SB_CONDITION_KEY = "sbcond"
    private const val SB_CONDITION_PREVIOUS_KEY = "sbcondPrev"
    private const val SB_INDEX_KEY = "sbind"

    private val timezoneMappings = mapOf(
        "PST" to "America/Los_Angeles",
        "AKST" to "America/Anchorage",
        "MST" to "America/Denver",
        "HST" to "Pacific/Honolulu",
        "CST" to "America/Chicago",
        "EST" to "America/New_York"
    )

    private val zoneIdCache = ConcurrentHashMap<String, ZoneId>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXXX")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun resolveZoneId(selectedTimezone: String): ZoneId {
        val zoneKey = timezoneMappings[selectedTimezone] ?: "America/Los_Angeles"
        return zoneIdCache.getOrPut(zoneKey) { ZoneId.of(zoneKey) }
    }
    private const val SHIFT_RESET_INDEX_KEY = "shiftResetIndex"

    private const val SB_INDEX_PREVIOUS_KEY = "sbindPrev"

    private const val OFF_CONDITION_KEY = "off_condition"
    private const val OFF_CONDITION_PREVIOUS_KEY = "off_conditionPrev"
    private const val OFF_INDEX_KEY = "off_index"
    private const val OFF_INDEX_PREVIOUS_KEY = "off_indexPrev"
    private const val SHIFT_RESET = "violationHit"
    private const val SHIFT_RESET_TIME = "violationHitTime"
    private const val SHIFT_OFF_RESET_TIME = "violationOffHitTime"
    private const val SHIFT_RESET_AT_ZERO = "shiftResetAtZero"
    private const val SHIFT_OFF_RESET_AT_ZERO = "shiftResetAtZero"
    private const val APP_OPENING_TIME_MILLIS = "appOpenTimeMillis"


    private const val TAG = "AlertCalculationUtils"


    var previousDayLogs: List<UserLog>? = null

    var previousDayLastLogForUnidentified: UserLog? = null
    var previousDayLastLog: UserLog? = null
        get() = previousDayLastLogForUnidentified ?: previousDayLogs?.lastOrNull()
    var secondLastDayLastLog: UserLog? = null

    var previousDayHoursData: DailyHoursData? = null
    var todayTotalHoursData: DailyHoursData? = null


    fun calculate(userLogs: List<UserLog>): Pair<Double, Double> {
        try {
            val lastOnMode = latestOnMode(userLogs)
            if (lastOnMode != null) Log.d(TAG, "lastOnMode $lastOnMode")

            val nextObjects = userLogs.subList(userLogs.indexOf(lastOnMode), userLogs.size)
            Log.d(TAG, "Next objects count is ${nextObjects.size}")

            val next14Hours = next14Hours(nextObjects)
            Log.d(TAG, "next14Hours $next14Hours")
            return Pair(next14Hours.first, next14Hours.second)
        } catch (e: Exception) {
            return Pair(0.0, 0.0)
        }
    }

    fun calculate(
        context: Context,
        userLogs: List<UserLog>,
        mode: String,
    ): Double {
        return overallHours(context, userLogs, mode)
    }

//    fun setSbCondition(context: Context, sbCondition: Boolean, isPreviousDay: Boolean = false) {
//        val sharedPref = context.getSharedPreferences("sbcondition", Context.MODE_PRIVATE)
//        sharedPref.edit().putBoolean(
//            if (isPreviousDay) SB_CONDITION_PREVIOUS_KEY else SB_CONDITION_KEY,
//            sbCondition
//        ).apply()
//    }

//    fun getSbCondition(context: Context, isPreviousDay: Boolean = false): Boolean {
//        val sharedPref = context.getSharedPreferences("sbcondition", Context.MODE_PRIVATE)
//        return sharedPref.getBoolean(
//            if (isPreviousDay) SB_CONDITION_PREVIOUS_KEY else SB_CONDITION_KEY,
//            false
//        )
//    }

    fun setsbIndex(context: Context, sbIndex: Int, isPreviousDay: Boolean = false) {
        val sharedPref = context.getSharedPreferences("sbindex", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putInt(if (isPreviousDay) SB_INDEX_PREVIOUS_KEY else SB_INDEX_KEY, sbIndex).apply()

    }

    fun getsbIndex(context: Context, isPreviousDay: Boolean = false): Int {
        val sharedPref = context.getSharedPreferences("sbindex", Context.MODE_PRIVATE)
        return sharedPref.getInt(if (isPreviousDay) SB_INDEX_PREVIOUS_KEY else SB_INDEX_KEY, 0)
    }

    fun setShiftResetIndex(context: Context, sbIndex: Int) {
        val sharedPref = context.getSharedPreferences("sindex", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putInt(SHIFT_RESET_INDEX_KEY, sbIndex).apply()

    }

    fun getShiftResetIndex(context: Context): Int {
        val sharedPref = context.getSharedPreferences("sindex", Context.MODE_PRIVATE)
        return sharedPref.getInt(SHIFT_RESET_INDEX_KEY, 0)
    }

    fun setOffCondition(context: Context, offCondition: Boolean, isPreviousDay: Boolean = false) {
        val sharedPref = context.getSharedPreferences("offcondition", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(
            if (isPreviousDay) OFF_CONDITION_PREVIOUS_KEY else OFF_CONDITION_KEY,
            offCondition
        ).apply()
    }

    fun getOffCondition(context: Context, isPreviousDay: Boolean = false): Boolean {
        val sharedPref = context.getSharedPreferences("offcondition", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(
            if (isPreviousDay) OFF_CONDITION_PREVIOUS_KEY else OFF_CONDITION_KEY,
            false
        )
    }

    fun setOffIndex(context: Context, offIndex: Int, isPreviousDay: Boolean = false) {
        val sharedPref = context.getSharedPreferences("offindex", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putInt(if (isPreviousDay) OFF_INDEX_PREVIOUS_KEY else OFF_INDEX_KEY, offIndex).apply()
    }

    fun getOffIndex(context: Context, isPreviousDay: Boolean = false): Int {
        val sharedPref = context.getSharedPreferences("offindex", Context.MODE_PRIVATE)
        return sharedPref.getInt(if (isPreviousDay) OFF_INDEX_PREVIOUS_KEY else OFF_INDEX_KEY, 0)
    }

    fun setShiftReset(context: Context, violationHit: Boolean) {
        val sharedPref = context.getSharedPreferences("shiftReset", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean(SHIFT_RESET, violationHit)
            if (violationHit)
                putLong(SHIFT_RESET_TIME, Calendar.getInstance().timeInMillis)
            apply()
        }
    }

    fun isShiftReset(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("shiftReset", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(SHIFT_RESET, false)
    }

    fun getShiftResetTimeFromPrefs(context: Context): Long {
        val sharedPref = context.getSharedPreferences("shiftReset", Context.MODE_PRIVATE)
        return sharedPref.getLong(SHIFT_RESET_TIME, 0)
    }

    fun setOffShiftResetTimeToPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("shiftReset", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putLong(SHIFT_OFF_RESET_TIME, Calendar.getInstance().timeInMillis)
            apply()
        }
    }

    fun getOffShiftResetTimeFromPrefs(context: Context): Long {
        val sharedPref = context.getSharedPreferences("shiftReset", Context.MODE_PRIVATE)
        return sharedPref.getLong(SHIFT_OFF_RESET_TIME, 0)
    }

    fun setShiftResetAtZero(context: Context, shiftResetAtZero: Boolean) {
        val sharedPref = context.getSharedPreferences("shiftResetAtZero", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean(SHIFT_RESET_AT_ZERO, shiftResetAtZero)
            apply()
        }
    }

    fun isShiftResetAtZero(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("shiftResetAtZero", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(SHIFT_RESET_AT_ZERO, false)
    }

    fun setOffShiftResetAtZero(context: Context, shiftResetAtZero: Boolean) {
        val sharedPref = context.getSharedPreferences("shiftResetAtZero", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean(SHIFT_OFF_RESET_AT_ZERO, shiftResetAtZero)
            apply()
        }
    }

    fun isOffShiftResetAtZero(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("shiftResetAtZero", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(SHIFT_OFF_RESET_AT_ZERO, false)
    }
//
//    @SuppressLint("SuspiciousIndentation")
//    private fun overallHours(
//        context: Context,
//        userLogs: List<UserLog>,
//        mode: String,
//    ): Double {
//
//        if (userLogs.isEmpty()) return 0.0
//
//        val maxIndex = if (userLogs.isNotEmpty()) userLogs.size - 1 else 0
//
//        val lastElement = userLogs.last()
//        var totalHours = 0.0
////        var sbCondition = getSbCondition(context)
////        var offCondition = getOffCondition(context)
////        Log.d(TAG, "SBB:$sbCondition")
//
////        Log.d(TAG, "OFF:$offCondition")
//
//        Log.d(TAG, "current mode:$mode")
//
//
////        if (sbCondition) {
////            setSbCondition(context, false)
////        }
////        if (offCondition) {
////            setOffCondition(context, false)
////        }
////        var startIndex = maxOf(sbIndex, offIndex)
//
//
//        var isShiftResetAtZero = false
//        var startIndex = getShiftResetTimeIndex(context) { isNextDay, dayDiff ->
//            Day(isNextDay, dayDiff).today.also { //-> boolean condition for lambda return
//                if (it) // only include shift reset at zero condition for current day when condition reset
//                    isShiftResetAtZero = isShiftResetAtZero(context)
//            }
//        }
//
////        if ((startIndex in 1 until maxIndex) || userLogs.first().id == -1)
//        if ((startIndex in 1 until maxIndex) || isShiftResetAtZero)
//            startIndex += 1
//
////        if (startIndex > maxIndex) {
////            startIndex = 0
////        }
//
//
//        for (index in startIndex until userLogs.size) {
//
//            var previousDayLastSleepingHours = 0.0
//            var previousDayLastOffHours = 0.0
//            if (index == 0)
//                previousDayLastLog?.let { previousDayLastLog ->
//                    val timeDiff = getDifferenceInHours(previousDayLastLog.time, MID_NIGHT_TIME)
//
//                    when (previousDayLastLog.modename) {
//                        TRUCK_MODE_SLEEPING -> previousDayLastSleepingHours = timeDiff
//                        TRUCK_MODE_OFF -> {
//
//                            // handling of case if previous day is completely off it means its time start from 00:00
//                            // and is only one log, which means second last day last log must be off.
//                            // Then include that log time too for checking if 34 hour condition is reset or not
//                            var secondLastDayLastLogTime = 0.0
//                            if (previousDayLastLog.time == "00:00") {
//                                secondLastDayLastLog?.let { secondLastDayLastLog ->
//                                    secondLastDayLastLogTime = getDifferenceInHours(
//                                        secondLastDayLastLog.time,
//                                        MID_NIGHT_TIME
//                                    )
//                                }
//                            }
//                            previousDayLastOffHours = timeDiff + secondLastDayLastLogTime
//                        }
//                    }
//                }
//
//            val currentLog = userLogs[index]
//            Log.d(TAG, "indexeddd:$index")
//            val hasNextLog = hasNext(index, userLogs)
//            val currentMode = currentLog.modename.trim().toLowerCase()
//
//
//            // handling logic for unidentified logs of driving
//            // if found any
//            if (currentLog.hours > 0.0) {
//                when (mode) {
//                    TRUCK_MODE_DRIVING -> {
//                        totalHours += currentLog.hours
//                        continue
//                    }
//
//                    TRUCK_MODE_ON -> {
//                        val diff = getDifferenceInHours(
//                            currentLog.time,
//                            hasNextLog?.time ?: ""
//                        ) - currentLog.hours
//                        totalHours += diff
//                        continue
//                    }
//                }
//            }
//
//
//            if (currentMode == mode.toLowerCase()) {
//
//                if (hasNextLog != null) {
//
//                    val timeDiff = getDifferenceInHours(currentLog.time, hasNextLog.time)
//
//                    totalHours += timeDiff
////                    if (mode == "sb" && timeDiff >= 10) {
//                    if (mode == "sb" && (timeDiff + previousDayLastSleepingHours) >= 10
////                            ?: 0.0)) >= 0.13 // 8 minutes
//                    ) {
////                        Log.d(TAG, "ISNIDE SB MODE TAG : $sbCondition")
////                        setSbCondition(context, true)
//
////                        setsbIndex(context, index)
//
//                        setShiftResetIfZero(context, index)
//
//                        setShiftResetIndex(context, index)
//                        setShiftReset(context, true)
////                        sbIndex = index
//                        Log.d(TAG, "SB CONDITION INSIDE CURRENTMODE : $index")
//                        previousDayHoursData = null
//                        previousDayLastSleepingHours = 0.0
//                        totalHours = 0.0
//
////                        break
////                    } else if (mode == "off" && timeDiff >= 34) {
//                    } else if (mode == "off" && (timeDiff + previousDayLastOffHours) >= 34
//                    ) {
//
//                        setShiftResetIfZero(context, index)
//                        setOffShiftResetAtZero(context, index == 0)
//                        setOffShiftResetTimeToPrefs(context)
//
//                        setCycleHours(
//                            context,
//                            0.0
//                        ) // resetting the cycle hours after off condition met
//
//                        setOffCondition(context, true)
//
//                        setOffIndex(context, index)
//                        setShiftResetIndex(context, index)
//
//                        setShiftReset(context, true)
////                        offIndex = index
//                        Log.d(TAG, "OFF CONDITION INSIDE CURRENTMODE : $index")
//                        previousDayHoursData = null
//                        previousDayLastOffHours = 0.0
//                        totalHours = 0.0
//
////                        break
//                    }
//                } else {
////                    totalHours += getHours(lastElement.timesheet, lastElement.company_timezone)
//                    Log.d(TAG, "last mode found $currentMode")
//                }
//            }
//        }
//
//
////        Log.d(TAG, "SBCONDITIONOUT  : $sbCondition")
////        Log.d(TAG, "OFFCONDITIONOUT  : $offCondition")
//
//        return totalHours
////        return when {
////            sbCondition -> {
////                Log.d(TAG, "sbbcondcheck:$sbCondition offcondcheck:$offCondition")
////                0.0
////            }
////
////            offCondition -> {
////                Log.d(TAG, "sbbcondcheck:$sbCondition offcondcheck:$offCondition")
////                0.001
////            }
////
////            else -> totalHours
////        }
//    }
@SuppressLint("SuspiciousIndentation")
private fun overallHours(
    context: Context,
    userLogs: List<UserLog>,
    mode: String,
): Double {
    if (userLogs.isEmpty()) return 0.0

    val maxIndex = userLogs.size - 1
    var totalHours = 0.0
    var isShiftResetAtZero = false

    val startIndex = getShiftResetTimeIndex(context) { isNextDay, dayDiff ->
        Day(isNextDay, dayDiff).today.also {
            if (it) isShiftResetAtZero = isShiftResetAtZero(context)
        }
    }.let { if (it in 1 until maxIndex || isShiftResetAtZero) it + 1 else it }

    // Check if there is any "ON" or "DRIVING" log after shift reset
    val hasValidLogAfterReset = userLogs.drop(startIndex).any {
        it.modename.trim().lowercase() in listOf(TRUCK_MODE_ON, TRUCK_MODE_DRIVING)
    }

    if (!hasValidLogAfterReset) return 0.0

    for (index in startIndex until userLogs.size) {
        val currentLog = userLogs[index]
        val hasNextLog = hasNext(index, userLogs)
        val currentMode = currentLog.modename.trim().lowercase()
        var previousDayLastSleepingHours = 0.0
        var previousDayLastOffHours = 0.0

        if (index == 0) {
            previousDayLastLog?.let {
                val timeDiff = getDifferenceInHours(it.time, MID_NIGHT_TIME)
                previousDayLastSleepingHours = if (it.modename == TRUCK_MODE_SLEEPING) timeDiff else 0.0
                previousDayLastOffHours = if (it.modename == TRUCK_MODE_OFF) {
                    val secondLastDayLastLogTime =
                        if (it.time == "00:00") secondLastDayLastLog?.let { secondLastDay ->
                            getDifferenceInHours(secondLastDay.time, MID_NIGHT_TIME)
                        } ?: 0.0 else 0.0
                    timeDiff + secondLastDayLastLogTime
                } else 0.0
            }
        }

        if (currentLog.hours > 0.0) {
            when (mode) {
                TRUCK_MODE_DRIVING -> totalHours += currentLog.hours
                TRUCK_MODE_ON -> totalHours += getDifferenceInHours(currentLog.time, hasNextLog?.time ?: "") - currentLog.hours
            }
            continue
        }

        if (currentMode == mode.lowercase()) {
            hasNextLog?.let {
                val timeDiff = getDifferenceInHours(currentLog.time, it.time)
                totalHours += timeDiff

                if ((mode == "sb" && (timeDiff + previousDayLastSleepingHours) >= 10) ||
                    (mode == "off" && (timeDiff + previousDayLastOffHours) >= 34)) {
                    setShiftResetIfZero(context, index)
                    setShiftResetIndex(context, index)
                    setShiftReset(context, true)
                    if (mode == "off") {
                        setOffShiftResetAtZero(context, index == 0)
                        setOffShiftResetTimeToPrefs(context)
                        setCycleHours(context, 0.0)
                        setOffCondition(context, true)
                        setOffIndex(context, index)
                    }
                    totalHours = 0.0
                }
            }
        }
    }
    return totalHours
}


    fun setShiftResetIfZero(context: Context, index: Int) {
        setShiftResetAtZero(context, index == 0)
    }

    fun calculateDayHours(
        context: Context,
        userLogs: List<UserLog>?,
        isCalculateFromIndex: Boolean,
        shouldIncludeMidNightTime: Boolean,
        shouldAssignData: Boolean = false,
    ): DailyHoursData? {

        if (userLogs.isNullOrEmpty())
            return null

//        if (initialStatePreviousData != null)
//            return initialStatePreviousData

        var dHours = 0.0
        var onHours = 0.0
        var offHours = 0.0
        var sbHours = 0.0

        var startIndex = 0

        var isShiftResetAtZero = false
        if (isCalculateFromIndex) {
            startIndex = getShiftResetTimeIndex(context) { isNextDay, dayDiff ->
                if (dayDiff > 0) // reset this shiftResetZero at third day
                    setShiftResetAtZero(context, false)

                Day(isNextDay, dayDiff).yesterday.also { //-> boolean condition for lambda return
                    if (it) // only include shift reset at zero condition for yesterday when condition reset
                        isShiftResetAtZero = isShiftResetAtZero(context)
                }
            }
        }

        if (startIndex > 0 || isShiftResetAtZero)
            startIndex += 1

        val userLogSize = userLogs.size
        for (index in startIndex until userLogSize) {

            val currentLog = userLogs[index]

            val currentLogTime = currentLog.time

            val timeDiff = hasNext(index, userLogs)?.let { hasNextLog ->
//
                getDifferenceInHours(currentLogTime, hasNextLog.time)

            } ?: run {// has no next log so time will be mid night
                if (shouldIncludeMidNightTime) {
                    getDifferenceInHours(// previous day last log to mid night time
                        currentLogTime,
                        MID_NIGHT_TIME
                    )
                } else
                    0.0
            }


            // handle condition if unidentified driving logs detected
            if (currentLog.hours > 0.0) {

                // if hours (unidentified) are less than time diff
                // assigning hours to drive and difference of timeDiff and hours will assign to "on hours"
                if (currentLog.hours < timeDiff) {
                    dHours += currentLog.hours
                    onHours += timeDiff - currentLog.hours

                } else
                // if hours exceed from timeDiff then no on time and hours will assign to drive
                // this case can occur in day last log and shift move to next day
                    dHours += timeDiff


                // if last log is driving and unidentified driving detected
                // after assigning driving time the remaining time will be on time
                // so converting last log to on mode because after ward remaining time will consider on time
                if (index == userLogSize - 1)
                    previousDayLastLogForUnidentified = if (currentLog.hours < timeDiff)
                        previousDayLastLog?.copy(modename = TRUCK_MODE_ON, hours = 0.0)
                    else {
                        val actualDriveTimeForNextDay = currentLog.hours - timeDiff
                        previousDayLastLog?.copy(
                            hours = actualDriveTimeForNextDay
                        )
                    }
            } else
                when (currentLog.modename) {
                    TRUCK_MODE_SLEEPING -> sbHours += timeDiff
                    TRUCK_MODE_ON -> onHours += timeDiff
                    TRUCK_MODE_OFF -> offHours += timeDiff
                    TRUCK_MODE_DRIVING -> dHours += timeDiff
                }

        }


        val totalHours = DailyHoursData(
            onHours = onHours,
            offHours = offHours,
            drivingHours = dHours,
            sleepingHours = sbHours
        )

        if (shouldAssignData)
            if (isCalculateFromIndex)
                previousDayHoursData = totalHours
            else
                todayTotalHoursData = totalHours

        return totalHours
    }


    fun calculateCycleHours(
        context: Context,
        userLogs: List<UserLog>?,
        shouldIncludeMidNightTime: Boolean,
        isForPreviousDay: Boolean
    ): Double {

        if (userLogs.isNullOrEmpty())
            return 0.0

        var totalHours = 0.0

        var startIndex = 0

        val shiftResetTime = getOffShiftResetTimeFromPrefs(context)
        val (isNextDay, dayDiff) = isNextDay(shiftResetTime)

        val day = Day(isNextDay, dayDiff)


        var isOffShiftResetAtZero = false
        if ((!isForPreviousDay && day.today) || (isForPreviousDay && day.yesterday)) { // if today or yesterday calculate from saved off index if 34 hour condition reset
            startIndex = getOffIndex(context)
            isOffShiftResetAtZero = isOffShiftResetAtZero(context)
        }

        if (startIndex > 0 || isOffShiftResetAtZero)
            startIndex += 1

        val userLogSize = userLogs.size
        for (index in startIndex until userLogSize) {

            val currentLog = userLogs[index]

            val currentLogTime = currentLog.time

            val timeDiff = hasNext(index, userLogs)?.let { hasNextLog ->
//
                getDifferenceInHours(currentLogTime, hasNextLog.time)

            } ?: run {// has no next log so time will be mid night
                if (shouldIncludeMidNightTime) {
                    getDifferenceInHours(// previous day last log to mid night time
                        currentLogTime,
                        MID_NIGHT_TIME
                    )
                } else
                    0.0
            }

            when (currentLog.modename) {
                TRUCK_MODE_ON,
                TRUCK_MODE_DRIVING -> totalHours += timeDiff
            }

        }

        return totalHours
    }

    private fun getShiftResetTimeIndex(
        context: Context,
        condition: (Boolean, Int) -> Boolean
    ): Int {
        val shiftResetTime = getShiftResetTimeFromPrefs(context)
        val (isNextDay, dayDiff) = isNextDay(shiftResetTime)
        if (condition(isNextDay, dayDiff)) {
//            val sbIndex = getsbIndex(context)
//            val offIndex = getOffIndex(context)

            val shiftResetIndex = getShiftResetIndex(context)
            Log.d(TAG, "shiftResetIndex:${shiftResetIndex}")
//            return maxOf(sbIndex, offIndex)
            return shiftResetIndex
        }
        return 0
    }


    fun getTotalLogTime(userLogs: List<UserLog>, startIndex: Int): Double {
        var totalTime = 0.0
        var currentIndex = startIndex
        var nextLogIndex = startIndex + 1

        while (nextLogIndex < userLogs.size) {
            val log = userLogs[currentIndex]
            val nextLog = userLogs[nextLogIndex]

            val timeDifference = getDifferenceInMinutes(log.time, nextLog.time)
            totalTime += timeDifference.toDouble()

            currentIndex = nextLogIndex
            nextLogIndex++
        }

        return totalTime
    }

    // Constants for keys
    private const val FOUND_OFF_BREAK_KEY = "found_off_break"
    private const val START_INDEX_KEY = "start_index"

    // Functions to manage foundOffBreak flag
    fun setFoundOffBreak(context: Context, value: Boolean) {
        val sharedPref = sharedPreferences(context)
        with(sharedPref.edit()) {
            putBoolean(FOUND_OFF_BREAK_KEY, value)
            apply()
        }
    }

    fun getFoundOffBreak(context: Context): Boolean {
        val sharedPref = sharedPreferences(context)
        return sharedPref.getBoolean(FOUND_OFF_BREAK_KEY, false)
    }

    // Functions to manage startIndex
    fun setStartIndex(context: Context, value: Int) {
        val sharedPref = sharedPreferences(context)
        with(sharedPref.edit()) {
            putInt(START_INDEX_KEY, value)
            apply()
        }
    }

    fun getStartIndex(context: Context): Int {
        val sharedPref = sharedPreferences(context)
        return sharedPref.getInt(START_INDEX_KEY, 0)
    }

    private fun sharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    }


    // Retrieve integer values

    fun calculateDrivingTimeAfterOffBreak(
        userLogs: List<UserLog>,
        previousDayLastLog: UserLog?
    ): Double {
        if (userLogs.isEmpty()) return 0.0

        val lastElement = userLogs.last()
        var totalHours = 0.0
        var violationIndex = -1

        // adding previous day driving hours to total hours if available
        // will reset to 0 if half hour condition meet below
        var previousDayDrivingHours = previousDayHoursData?.drivingHours ?: 0.0


        for ((index, logs) in userLogs.withIndex()) {
            val hasNextLog = hasNext(index, userLogs)
            val currentMode = logs.modename.trim().toLowerCase()

            var previousDayLastLogTime = 0.0

            // getting previous day last log  on 0 index because shift time is coming from previous day
            if (index == 0) {
                previousDayLastLog?.let { lastLog ->
                    previousDayLastLogTime = getDifferenceInHours(lastLog.time, MID_NIGHT_TIME)
                }
            }


            if (currentMode in listOf(TRUCK_MODE_OFF, TRUCK_MODE_SLEEPING)) {
                val timeDiff = if (hasNextLog != null) {
                    getDifferenceInHours(logs.time, hasNextLog.time)
                } else {
//                    getDifferenceInHours(logs.time, MID_NIGHT_TIME)
                    getHours(lastElement.timesheet, lastElement.company_timezone)
                }
                if ((previousDayLastLogTime + timeDiff) >= 0.5) {
                    violationIndex = index
                    totalHours = 0.0
                    previousDayDrivingHours = 0.0
                }
            } else if (currentMode == TRUCK_MODE_DRIVING && index > violationIndex) {

                val logTime = logs.time

                val timeDiff = if (hasNextLog != null) {
                    if (logs.hours > 0) logs.hours
                    else
                        getDifferenceInHours(logTime, hasNextLog.time)
                } else {
//                    getDifferenceInHours(logTime, MID_NIGHT_TIME)
                    getHours(lastElement.timesheet, lastElement.company_timezone)
                }

                totalHours += timeDiff
            }
        }

        return totalHours + previousDayDrivingHours
    }

    fun continuousDriving(userLogs: List<UserLog>): Double {
        return try {
            val logs = userLogs.last()
            if (logs.modename == TRUCK_MODE_DRIVING) {
                getHours(logs.timesheet, logs.company_timezone)
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun next14Hours(userLogs: List<UserLog>): Pair<Double, Double> {
        if (userLogs.isEmpty()) return Pair(0.0, 0.0)
        val lastElement = userLogs.last()
        var totalHours = 0.0
        var driveHours = 0.0

        for (index in userLogs.indices) {
            val logs = userLogs[index]

            val hasNextLog = hasNext(index, userLogs)

            if (logs.modename == TRUCK_MODE_ON || logs.modename == TRUCK_MODE_DRIVING) {
                if (hasNextLog != null) {
                    totalHours += getDifferenceInHours(logs.time, hasNextLog.time)

                    if (logs.modename == TRUCK_MODE_DRIVING) {
                        driveHours += getDifferenceInHours(logs.time, hasNextLog.time)
                    }
                    Log.d(TAG, "calculated time ---> $totalHours")
                } else {
                    totalHours += getHours(lastElement.timesheet, lastElement.company_timezone)
                    if (logs.modename == TRUCK_MODE_DRIVING) {
                        driveHours += getHours(lastElement.timesheet, lastElement.company_timezone)
                    }

                    Log.d(TAG, "last mode found ${logs.modename}")
                }
            }

        }
        Log.d(TAG, "next14Hours total time spent so far $totalHours")
        Log.d(TAG, "next14Hours total time spent in drive $driveHours")
        return Pair(totalHours, driveHours)
    }

    private fun hasNext(currentIndex: Int, userLogs: List<UserLog>): UserLog? {
        return if (currentIndex < userLogs.size - 1) {
            userLogs[currentIndex + 1]
        } else {
            null
        }
    }

    private fun latestOnMode(userLogs: List<UserLog>): UserLog? {
        for (log in userLogs) if (log.modename == TRUCK_MODE_ON) return log
        return null
    }

    fun getHours(timeSheet: String?, timezone: String?): Double {
        return try {
            if (timeSheet != null) {
                val localTime = DateTime()
                val SelectedTimezone = DateTimeZone.forID(timezone)
                val currentTime = localTime.withZone(SelectedTimezone)
                val givenDateTime = DateTime(timeSheet)
                val timeDifference = Period(givenDateTime.toLocalTime(), currentTime.toLocalTime())
                val hours = timeDifference.hours
                val minutes = timeDifference.minutes
                Log.d(TAG, "hours : $hours minutes : $minutes")
                val decimalFormat = DecimalFormat("#.#")
                return decimalFormat.format(hours + (minutes / 60.0)).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            // Handle any exceptions that occur during the conversion
            0.0
        }
    }

    fun getHoursIntoMinutes(time: Double): String {
        return try {
            val hours = time.toInt()

            val minutesDecimal = time - hours
            val minutes = (minutesDecimal * 60).toInt()
            "$hours:$minutes"
        } catch (e: Exception) {
            decimalHours(time).toString()
        }
    }
    fun getHoursIntoDigitalTimeFormate(time: Double): String {
        return try {
            val hours = time.toInt()
            val minutesDecimal = time - hours
            val minutes = (minutesDecimal * 60).toInt()

            // Format with leading zeros if needed
            String.format("%d:%02d", hours, minutes)
        } catch (e: Exception) {
            decimalHours(time).toString()
        }
    }

    fun getDifferenceInHours(startTimeStr: String, endTimeStr: String): Double {

        var startTime = startTimeStr
        var endTime = endTimeStr

        var midNightMin = 0.0
        if (startTimeStr.contains("24:00"))
            startTime = MID_NIGHT_TIME

        if (endTime.contains("24:00")) {
            midNightMin = 0.016666666666667
            endTime = MID_NIGHT_TIME
        }


//        val totalHours = hours + (minutes / 60.0)
        return try {
            val startTimeNew = LocalTime.parse(startTime)
            val endTimeNew = LocalTime.parse(endTime)

//        val hours = Hours.hoursBetween(startTimeNew, endTimeNew).hours
//        val minutes = (Minutes.minutesBetween(startTimeNew, endTimeNew).minutes % 60) + midNightMin

            val secondsDifference = Seconds.secondsBetween(startTimeNew, endTimeNew).seconds

            // Convert the difference to hours in decimal
            val hoursDifference = secondsDifference / 3600.0
            Log.d(TAG, "difference in hours ---> $hoursDifference")
            hoursDifference
        } catch (e: Exception) {
            Log.d(TAG, "difference in hours ---> Exception occurred", e)
            0.0
        }
    }

    fun getDifferenceInMinutes(startTimeStr: String, endTimeStr: String): Int {
        val startTime = LocalTime.parse(startTimeStr)
        val endTime = LocalTime.parse(endTimeStr)

        val minutes = Minutes.minutesBetween(startTime, endTime).minutes

        return minutes
    }

    fun getDifferenceInHours(previousDate: Date, newDate: Date): Double {
        val diff: Long = newDate.time - previousDate.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        Log.d(TAG, "minutes date ----> $minutes")
        return minutes / 60.0
    }

    fun decimalHours(hours: Double): Double {
        val decimalFormat = DecimalFormat("#.#")
        return decimalFormat.format(hours).toDouble()
    }


    fun handleNextDayStuff(context: Context) {

        if (context.isNextDay().first) {
            val prefs = sharedPreferences(context)

            prefs.edit {
                putLong(APP_OPENING_TIME_MILLIS, System.currentTimeMillis())
            }

            // handle cycle hours
            val savedCycleHours = getCycleHours(context)

            calculateCycleHours(
                context,
                previousDayLogs,
                shouldIncludeMidNightTime = true,
                isForPreviousDay = true
            ).also { previousDayCycleHours ->

                val newCycleHours: Double = previousDayCycleHours + savedCycleHours

                setCycleHours(context, newCycleHours)

            }

            setOffCondition(context, false)
            setShiftReset(context, false)
        }

    }


    fun getCycleHours(context: Context): Double {
        return sharedPreferences(context).getString(CYCLE_HOURS, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun setCycleHours(context: Context, newCycleHours: Double) {
        sharedPreferences(context).edit {
            putString(CYCLE_HOURS, newCycleHours.toString())
        }
    }


    @JvmStatic
    fun Context.isNextDay(): Pair<Boolean, Int> {
        val previousDayTime =
            sharedPreferences(this).getLong(
                APP_OPENING_TIME_MILLIS,
                System.currentTimeMillis() - 86400000 // minus 24 hours, assuming this is next day for saving value first time
            )
        return isNextDay(previousDayTime)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun daysDiff(daysAgo: Int): LocalDate? {
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        return convertToPST(date)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun setDateAndTimeBasedOnTimezone(selectedTimezone: String): Map<String, String> {
        // Removed println to prevent log spam during resume
        val zoneId = resolveZoneId(selectedTimezone)
        val zonedDateTime = ZonedDateTime.now(zoneId)

        val time24HourFormat = zonedDateTime.format(timeFormatter)
        val dateTimeFormat = zonedDateTime.format(dateTimeFormatter)
        val dateFormat = zonedDateTime.format(dateFormatter)

        return mapOf(
            "date" to dateFormat,
            "time" to time24HourFormat,
            "dateandtime" to dateTimeFormat
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertTimeToTimezone(time: String, fromTimezone: String, toTimezone: String): String {
        return try {
            val fromZoneId = resolveZoneId(fromTimezone)
            val toZoneId = resolveZoneId(toTimezone)

            // Parse the time string (assuming HH:mm:ss format)
            val timeParts = time.split(":")
            if (timeParts.size >= 2) {
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val second = if (timeParts.size > 2) timeParts[2].toInt() else 0

                // Create a LocalTime and convert to the target timezone
                val localTime = java.time.LocalTime.of(hour, minute, second)
                val today = java.time.LocalDate.now()
                val fromZonedDateTime = ZonedDateTime.of(today, localTime, fromZoneId)
                val toZonedDateTime = fromZonedDateTime.withZoneSameInstant(toZoneId)

                toZonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            } else {
                time // Return original if parsing fails
            }
        } catch (e: Exception) {
            Log.e("AlertCalculationUtils", "Error converting time: ${e.message}")
            time // Return original if conversion fails
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeInTimezone(timezone: String): String {
        return try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timezone)
            timezoneData["time"] ?: ""
        } catch (e: Exception) {
            Log.e("AlertCalculationUtils", "Error getting current time: ${e.message}")
            ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateInTimezone(timezone: String): String {
        return try {
            val timezoneData = setDateAndTimeBasedOnTimezone(timezone)
            timezoneData["date"] ?: ""
        } catch (e: Exception) {
            Log.e("AlertCalculationUtils", "Error getting current date: ${e.message}")
            ""
        }
    }

    /** Current time in company timezone as hours (0f–24f) for graph x-axis. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeAsFloatInTimezone(timezone: String): Float {
        return try {
            val timeStr = setDateAndTimeBasedOnTimezone(timezone)["time"] ?: "00:00:00"
            val parts = timeStr.split(":")
            when (parts.size) {
                3 -> {
                    val h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    val s = parts[2].toIntOrNull() ?: 0
                    (h + m / 60f + s / 3600f).coerceIn(0f, 24f)
                }
                else -> 0f
            }
        } catch (e: Exception) {
            Log.e("AlertCalculationUtils", "Error getCurrentTimeAsFloatInTimezone: ${e.message}")
            0f
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTimeWithTimezone(time: String, timezone: String): String {
        if (time.isBlank() || time == "00:00") return ""

        return try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // 24-hour format
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())  // 12-hour format with AM/PM

            val date = inputFormat.parse(time)
            if (date == null) {
                Log.e("AlertCalculationUtils", "Failed to parse time: $time")
                return time
            }

            val formattedTime = outputFormat.format(date)
            "$formattedTime ($timezone)"
        } catch (e: Exception) {
            Log.e("AlertCalculationUtils", "Error formatting time: ${e.message}")
            time
        }
    }


    fun convertToPST(localDate: LocalDate?, sourceFormat: String = "yyyy-MM-dd"): LocalDate? {

        if (localDate == null)
            return null

        val zonedDateTimeLocal =
            ZonedDateTime.of(localDate, java.time.LocalTime.now(), ZoneId.systemDefault())
        val currentPST = zonedDateTimeLocal.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))


        return currentPST.toLocalDate()
    }

    //    val previousDay: List<UserLog> = listOf(
//    val currentDay: List<UserLog> = listOf(
//        UserLog(id = 2, created_on = "2024-03-02", date = "2024-04-16", datetime = "2024-03-02 14:45:00", discreption = "Description 2", eng_hours = "7", is_autoinsert = "2", location = "Location 2", modename = "off", time = "21:00", timesheet = "Timesheet 2", vin = "XYZ789", authorization_status = "Pending", company_timezone = "GMT", comments = "Comments 2"),
//        UserLog(id = 3, created_on = "2024-03-03", date = "2024-04-16", datetime = "2024-03-03 10:00:00", discreption = "Description 3", eng_hours = "4", is_autoinsert = "2", location = "Location 3", modename = "d", time = "22:00", timesheet = "Timesheet 3", vin = "JKL456", authorization_status = "Rejected", company_timezone = "PST", comments = "Comments 3", hours = 3.0),
//        UserLog(id = 3, created_on = "2024-03-03", date = "2024-04-17", datetime = "2024-03-03 10:00:00", discreption = "Description 3", eng_hours = "4", is_autoinsert = "2", location = "Location 3", modename = "sb", time = "2:00", timesheet = "Timesheet 3", vin = "JKL456", authorization_status = "Rejected", company_timezone = "PST", comments = "Comments 3"),
//    )


    fun refinedTime(time: String): Float {
        val replacedString = time.replace(":", ".")
        return (replacedString.substring(
            0,
            replacedString.length - 3
        ) + "f").toFloat()
    }

    fun refinedTimeStringToFloat(time: String): Float {
        return try {
            val parts = time.split(":")
            val hours   = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0
            hours + minutes / 60f + seconds / 3600f
        } catch (e: Exception) {
            0f
        }
    }

    class Day(private val isNextDay: Boolean, private val dayDiff: Int) {
        val today get() = !isNextDay && dayDiff < 0
        val yesterday get() = isNextDay && dayDiff == 0
    }
}

data class DailyHoursData(
    val onHours: Double,
    val offHours: Double,
    val drivingHours: Double,
    val sleepingHours: Double,
)
