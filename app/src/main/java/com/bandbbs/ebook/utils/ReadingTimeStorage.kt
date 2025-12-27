package com.bandbbs.ebook.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 阅读时间存储工具类
 * 用于记录和管理阅读时间和会话
 */
object ReadingTimeStorage {
    private const val PREFS_NAME = "reading_time_prefs"
    private const val TAG = "ReadingTimeStorage"
    private const val MIN_SESSION_DURATION_SECONDS = 5L

    /**
     * 记录阅读开始
     */
    fun recordReadingStart(context: Context, bookName: String) {
        if (bookName.isBlank()) {
            Log.w(TAG, "recordReadingStart: bookName is blank")
            return
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()


            editor.putLong("${bookName}_current_session_start", System.currentTimeMillis())
            editor.apply()

            Log.d(TAG, "recordReadingStart: $bookName at ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record reading start for $bookName", e)
        }
    }

    /**
     * 记录阅读结束
     */
    fun recordReadingEnd(context: Context, bookName: String) {
        if (bookName.isBlank()) {
            Log.w(TAG, "recordReadingEnd: bookName is blank")
            return
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val startTime = prefs.getLong("${bookName}_current_session_start", 0L)

            if (startTime == 0L) {
                Log.d(TAG, "recordReadingEnd: No active session for $bookName")
                return
            }

            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000


            if (duration < MIN_SESSION_DURATION_SECONDS) {
                Log.d(
                    TAG,
                    "recordReadingEnd: Session too short ($duration seconds) for $bookName, ignoring"
                )
                val editor = prefs.edit()
                editor.remove("${bookName}_current_session_start")
                editor.apply()
                return
            }


            val totalSeconds = prefs.getLong("${bookName}_total_seconds", 0L)
            val sessionsJson = prefs.getString("${bookName}_sessions", null)


            var sessionsArray = if (sessionsJson != null) {
                try {
                    JSONArray(sessionsJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sessions JSON for $bookName", e)
                    JSONArray()
                }
            } else {
                JSONArray()
            }


            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sessionDate = dateFormat.format(Date(startTime))

            val newSession = JSONObject().apply {
                put("startTime", startTime)
                put("endTime", endTime)
                put("duration", duration)
                put("date", sessionDate)
            }


            sessionsArray.put(newSession)


            val newTotalSeconds = totalSeconds + duration


            val firstReadDate = prefs.getString("${bookName}_first_read_date", null)
            if (firstReadDate == null) {
                prefs.edit().putString("${bookName}_first_read_date", sessionDate).apply()
            }
            prefs.edit().putString("${bookName}_last_read_date", sessionDate).apply()


            val editor = prefs.edit()
            editor.putLong("${bookName}_total_seconds", newTotalSeconds)
            editor.putString("${bookName}_sessions", sessionsArray.toString())
            editor.remove("${bookName}_current_session_start")
            editor.apply()

            Log.d(
                TAG,
                "recordReadingEnd: $bookName, duration=$duration seconds, total=$newTotalSeconds seconds, sessions=${sessionsArray.length()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record reading end for $bookName", e)
        }
    }

    /**
     * 获取阅读时间数据
     */
    fun getReadingTime(context: Context, bookName: String): Map<String, Any>? {
        if (bookName.isBlank()) {
            return null
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val totalSeconds = prefs.getLong("${bookName}_total_seconds", 0L)

            if (totalSeconds == 0L) {
                return null
            }

            val sessionsJson = prefs.getString("${bookName}_sessions", null)
            val sessions = if (sessionsJson != null) {
                try {
                    JSONArray(sessionsJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sessions JSON for $bookName", e)
                    null
                }
            } else null

            val lastReadDate = prefs.getString("${bookName}_last_read_date", null) ?: ""
            val firstReadDate = prefs.getString("${bookName}_first_read_date", null) ?: ""

            val sessionList = sessions?.let {
                (0 until it.length()).map { i ->
                    try {
                        it.getJSONObject(i)
                    } catch (e: Exception) {
                        null
                    }
                }.filterNotNull()
            } ?: emptyList<Any>()

            return mapOf(
                "totalSeconds" to totalSeconds,
                "sessions" to sessionList,
                "lastReadDate" to lastReadDate,
                "firstReadDate" to firstReadDate
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reading time for $bookName", e)
            return null
        }
    }
}

