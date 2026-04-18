package com.eagleye.eld.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.eagleye.eld.utils.Utils.getDouble
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject


class PrefRepository @Inject constructor(@ApplicationContext val context: Context) {

    private val pref: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    private val editor = pref.edit()
    private val gson = Gson()

    private fun String.put(long: Long) {
        editor.putLong(this, long)
        editor.commit()
    }


    private fun String.put(int: Int) {
        editor.putInt(this, int)
        editor.commit()
    }


    private fun String.put(string: String) {
        editor.putString(this, string)
        editor.commit()
    }


    private fun String.put(boolean: Boolean) {
        editor.putBoolean(this, boolean)
        editor.commit()
    }

    private fun String.getLong() = pref.getLong(this, 0)

    private fun String.getInt() = pref.getInt(this, 0)

    private fun String.getString() = pref.getString(this, "")!!

    private fun String.getBoolean(defaultValue: Boolean = false) =
        pref.getBoolean(this, defaultValue)


    fun setShowUnidentifiedDialog(shouldShow: Boolean) {
        SHOW_UNIDENTIFIED_DIALOG.put(shouldShow)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        PREF_LOGGED_IN.put(isLoggedIn)
    }

    fun setRememberMe(isRememberMe: Boolean) {
        PREF_REMEMBER_ME.put(isRememberMe)
    }

    fun setMode(mode: String) {
        PREF_CURRENT_MODE.put(mode)
    }

    fun setLastEngineState(state: String) {
        PREF_LAST_ENGINE_STATE.put(state)
    }

    fun setRemoveableIndex(index: Int) {
        PREF_REMOVEDINDEX.put(index)
    }

    fun getRemoveableIndex(): Int {
        val REMOVEDINDEX = PREF_REMOVEDINDEX.getInt()

        return REMOVEDINDEX
    }

    fun setDifferenceinOdo(diff: String) {
        PREF_DIFFINODO.put(diff)
    }

    fun setDifferenceinEnghours(diff: String) {
        PREF_DIFFINENG.put(diff)
    }

    fun getDiffinOdo(): String {
        if (PREF_DIFFINODO.getString().equals("null") || PREF_DIFFINENG.getString().isEmpty()) {
            return "0"
        }
        val Difinodo = PREF_DIFFINODO.getString()
        return Difinodo
    }

    fun getDiffinEng(): String {
        if (PREF_DIFFINENG.getString().equals("null") || PREF_DIFFINENG.getString().isEmpty()) {
            return "0"
        }
        val Diffeng = PREF_DIFFINENG.getString()
        return Diffeng
    }

    fun setUnIdentifiedDrivingHours(diff: String) {
        PREF_TIME.put(diff)
    }

    fun getUnIdentifiedDrivingHours(): Double {
        // Retrieve the time value from SharedPreferences as a String
        val timeString = PREF_TIME.getString()

        // Parse the retrieved string to a double value or return a default value if empty
        return getDouble(timeString)
    }


    fun setUnIdentifiedOnHours(diff: String) {
        PREF_TIME_ON.put(diff)
    }

    fun getUnIdentifiedOnHours(): Double {
        val timeString = PREF_TIME_ON.getString()
        return getDouble(timeString)
    }


    fun setSbcondition(sbCondition: Boolean) {
        PREF_SBCONDITION.put(sbCondition)
    }

    fun getSbcondition(): Boolean {
        val sbcond = PREF_SBCONDITION.getBoolean()
        if (sbcond == null) return false
        return sbcond
    }

    fun setUnauthorized(sbCondition: Boolean) {
        PREF_UNAUTHORIZED.put(sbCondition)
    }

    fun getUnauthorized(): Boolean {
        val unauth = PREF_UNAUTHORIZED.getBoolean()
        return unauth
    }

    fun getMode(): String {
        val mode = PREF_CURRENT_MODE.getString()
        if (mode == null || mode.isEmpty()) return "inital"
        return mode
    }

    fun getLastEngineState(): String {
        return PREF_LAST_ENGINE_STATE.getString()
    }
//    fun setLogs(userLogs: List<UserLog>) {
//        val gson = Gson()
//        val json = gson.toJson(userLogs)
//        PREF_LOGS.put("cc", json).apply()
//    }

    fun shouldShowUnidentifiedDialog() = SHOW_UNIDENTIFIED_DIALOG.getBoolean(defaultValue = true)

    fun getLoggedIn() = PREF_LOGGED_IN.getBoolean()
    fun getRememberMe() = PREF_REMEMBER_ME.getBoolean()

    fun setUserName(username: String) {
        PREF_USERNAME.put(username)
    }

    fun getUserName() = PREF_USERNAME.getString()
    fun getSuccessConnection() = PREF_USERNAME.getString()

    fun setPassword(password: String) {
        PREF_PASSWORD.put(password)
    }

    fun getPassword() = PREF_PASSWORD.getString()
    fun setName(name: String) {
        PREF_NAME.put(name)
    }

    fun setTimeZone(timeZone: String) {
        PREF_TIMEZONE.put(timeZone)
    }

    fun getShippingNumber() = PREF_SHIPPING_NUMBER.getString()

    fun setShippingNumber(number: String) {
        PREF_SHIPPING_NUMBER.put(number)
    }

    fun setTrailerNumber(number: String) {
        PREF_TRAILER_NO.put(number)
    }

    fun getTrailerNumber() = PREF_TRAILER_NO.getString()

    fun setCoDriverId(id: Int) {
        PREF_CODRIVER_ID.put(id)
    }

    fun getCoDriverId() = PREF_CODRIVER_ID.getInt()

    fun clearCoDriverId() {
        PREF_CODRIVER_ID.put(0)
    }

    fun setCoDriverName(name: String) {
        PREF_CODRIVER_NAME.put(name)
    }

    fun getCoDriverName() = PREF_CODRIVER_NAME.getString()

    fun setLastLogTime() {
        PREF_LAST_LOG_TIME.put(Date().time)
    }

    fun getLastLogTime() = PREF_LAST_LOG_TIME.getLong()

    fun getLogTimeDifference() = Date().time - PREF_LAST_LOG_TIME.getLong()
    fun getName() = PREF_NAME.getString()

    fun getTimeZone() = PREF_TIMEZONE.getString()

    fun setToken(token: String) {
        PREF_TOKEN.put(token)
    }

    fun setDriverId(id: Int) {
        PREF_DRIVER_ID.put(id)
    }
    fun setCompanyId(id: Int) {
        PREF_COMPANY_ID.put(id)
    }

    fun getToken() = PREF_TOKEN.getString()

    fun getDriverId() = PREF_DRIVER_ID.getInt()
    fun getCompanyId() = PREF_COMPANY_ID.getInt()

    // Assume PREF_ONHOURS is your SharedPreferences object

//    fun setOnhours(hours: String) {
//        PREF_ONHOURS.put(hours)
//    }
//
//    fun getOnhours() = PREF_ONHOURS.getString()
//
//
//    fun setDriveHours(hours: String) {
//        PREF_DRIVEHOURS.put(hours)
//
//    }
//
//    fun getDhours() = PREF_DRIVEHOURS.getString()


    fun setTilltime(tilltime: String) {
        PREF_TILLTIME.put(tilltime)
    }

    fun getTilltime() = PREF_TILLTIME.getString()

    fun getBreakViolation() = PREF_CYCLE.getBoolean()
    fun setBreakviolation(viol: Boolean) {
        PREF_CYCLE.put(viol)
    }

    //    fun setLastRefreshTime(date: Date) {
//        PREF_LAST_REFRESH_TIME.put(gson.toJson(date))
//    }
//
//    fun getLastRefreshTime(): Date? {
//        PREF_LAST_REFRESH_TIME.getString().also {
//            return if (it.isNotEmpty())
//                gson.fromJson(PREF_LAST_REFRESH_TIME.getString(), Date::class.java)
//            else
//                null
//        }
//    }
    fun getObject(key: String, classOfT: Class<*>): Any? {
        val json = pref.getString(key, "")
        val value = try {
            Gson().fromJson(json, classOfT) ?: null
        } catch (e: Exception) {
            return null
        }
        return value
    }

    fun putObject(key: String, obj: Any) {
        editor.putString(key, "")
        val gson = Gson()
        editor.putString(key, gson.toJson(obj))
        editor.commit()
    }

    fun setLastEldDevice(name: String, address: String) {
        PREF_LAST_ELD_DEVICE_NAME.put(name)
        PREF_LAST_ELD_DEVICE_ADDRESS.put(address)
    }

    fun getLastEldDeviceName(): String = PREF_LAST_ELD_DEVICE_NAME.getString()
    fun getLastEldDeviceAddress(): String = PREF_LAST_ELD_DEVICE_ADDRESS.getString()

    fun setJustLoggedIn(isJustLoggedIn: Boolean) {
        PREF_JUST_LOGGED_IN.put(isJustLoggedIn)
    }

    fun getJustLoggedIn(): Boolean = PREF_JUST_LOGGED_IN.getBoolean()

    fun clearData() {
        editor.clear()
        editor.commit()
    }

    fun setTimerStartTime(time: Long) {
        editor.putLong("timer_start_time", time)
        editor.apply()
    }

    fun getTimerStartTime(): Long {
//        return editor.ge("timer_start_time", 0L)
        return  "timer_start_time".getLong()

    }
}
