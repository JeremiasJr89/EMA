package com.ema.musicschool.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LocalDataSource(context: Context) {

    private val PREFS_NAME = "study_logs_prefs"
    private val KEY_CURRENT_DAY_STUDY_TIME = "current_day_study_time"
    private val KEY_PAST_STUDY_LOGS = "past_study_logs"
    private val KEY_LAST_SYNC_DATE = "last_sync_date" // Para controlar quando a última sincronização ocorreu

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCurrentDayStudyTime(totalTimeMillis: Long) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val editor = sharedPreferences.edit()
        editor.putLong("$KEY_CURRENT_DAY_STUDY_TIME-$todayDate", totalTimeMillis)
        editor.apply()
    }

    fun getCurrentDayStudyTime(): Long {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        return sharedPreferences.getLong("$KEY_CURRENT_DAY_STUDY_TIME-$todayDate", 0L)
    }

    fun savePastStudyLogs(logs: List<StudyLog>) {
        val json = gson.toJson(logs)
        sharedPreferences.edit().putString(KEY_PAST_STUDY_LOGS, json).apply()
    }

    fun getPastStudyLogs(): List<StudyLog> {
        val json = sharedPreferences.getString(KEY_PAST_STUDY_LOGS, null)
        return if (json != null) {
            val type = object : TypeToken<List<StudyLog>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveLastSyncDate(date: Date) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_DATE, date.time).apply()
    }

    fun getLastSyncDate(): Date? {
        val timestamp = sharedPreferences.getLong(KEY_LAST_SYNC_DATE, -1L)
        return if (timestamp != -1L) Date(timestamp) else null
    }
}