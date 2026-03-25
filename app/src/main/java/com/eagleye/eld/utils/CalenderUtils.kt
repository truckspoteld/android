package com.eagleye.eld.utils

import java.util.*
import java.util.concurrent.TimeUnit

fun isNextDay(time: Long): Pair<Boolean, Int> { // isNextDay, timeDiff
    val c1: Calendar = Calendar.getInstance() // today
    c1.add(Calendar.DAY_OF_YEAR, -1) // yesterday
    val c2: Calendar = Calendar.getInstance()
    c2.timeInMillis = time
    val isNextDay = (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
            && c1.get(Calendar.DAY_OF_YEAR) >= c2.get(Calendar.DAY_OF_YEAR))
//    val timeDifference = TimeUnit.MILLISECONDS.toMinutes(c1.timeInMillis - c2.timeInMillis) / 60f

    val dayDiff = c1.get(Calendar.DAY_OF_YEAR) - c2.get(Calendar.DAY_OF_YEAR)

    return Pair(isNextDay, dayDiff)
}


