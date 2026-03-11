package com.example.dale.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.dale.ActivityLogEntry
import com.example.dale.AppGroup
import com.example.dale.UsageLogEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SharedPreferencesManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun setSetupCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun isSetupCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun saveAppGroup(appGroup: AppGroup) {
        val json = gson.toJson(appGroup)
        sharedPreferences.edit().putString("app_group_${appGroup.id}", json).apply()
    }

    fun getAppGroup(groupId: String): AppGroup? {
        val json = sharedPreferences.getString("app_group_$groupId", null)
        return if (json != null) gson.fromJson(json, AppGroup::class.java) else null
    }

    fun getAllAppGroups(): List<AppGroup> {
        val groups = mutableListOf<AppGroup>()
        val allPrefs = sharedPreferences.all
        for ((key, value) in allPrefs) {
            if (key.startsWith("app_group_") && value is String) {
                val group = gson.fromJson(value, AppGroup::class.java)
                groups.add(group)
            }
        }
        return groups.sortedByDescending { it.createdAt }
    }

    fun deleteAppGroup(groupId: String) {
        sharedPreferences.edit().remove("app_group_$groupId").apply()
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }

    // ── Activity Logs ────────────────────────────────────────────────────────

    fun saveActivityLog(groupId: String, entry: ActivityLogEntry) {
        val key = "activity_logs_$groupId"
        val existing = getActivityLogs(groupId).toMutableList()
        existing.add(0, entry)          // newest first
        if (existing.size > 200) existing.removeAt(existing.lastIndex)   // cap at 200
        val json = gson.toJson(existing)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getActivityLogs(groupId: String): List<ActivityLogEntry> {
        val json = sharedPreferences.getString("activity_logs_$groupId", null) ?: return emptyList()
        val type = object : TypeToken<List<ActivityLogEntry>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun clearActivityLogs(groupId: String) {
        sharedPreferences.edit().remove("activity_logs_$groupId").apply()
    }

    // ── Usage Logs ───────────────────────────────────────────────────────────

    fun saveUsageSession(groupId: String, packageName: String, appName: String, durationMs: Long) {
        val key = "usage_logs_$groupId"
        val existing = getRawUsageLogs(groupId).toMutableList()
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val idx = existing.indexOfFirst { it.packageName == packageName }
        if (idx >= 0) {
            val old = existing[idx]
            existing[idx] = old.copy(
                totalDurationMs = old.totalDurationMs + durationMs,
                lastUsedDate = dateStr
            )
        } else {
            existing.add(
                UsageLogEntry(
                    appName = appName,
                    packageName = packageName,
                    totalDurationMs = durationMs,
                    formattedDuration = "",
                    lastUsedDate = dateStr,
                    usageFraction = 0f
                )
            )
        }
        val json = gson.toJson(existing)
        sharedPreferences.edit().putString(key, json).apply()
    }

    private fun getRawUsageLogs(groupId: String): List<UsageLogEntry> {
        val json = sharedPreferences.getString("usage_logs_$groupId", null) ?: return emptyList()
        val type = object : TypeToken<List<UsageLogEntry>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun getUsageLogs(groupId: String): List<UsageLogEntry> {
        val raw = getRawUsageLogs(groupId)
        val totalMs = raw.sumOf { it.totalDurationMs }.takeIf { it > 0 } ?: 1L
        return raw.sortedByDescending { it.totalDurationMs }.map { entry ->
            entry.copy(
                formattedDuration = formatDuration(entry.totalDurationMs),
                usageFraction = entry.totalDurationMs.toFloat() / totalMs.toFloat()
            )
        }
    }

    fun clearUsageLogs(groupId: String) {
        sharedPreferences.edit().remove("usage_logs_$groupId").apply()
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    companion object {
        private const val PREFS_NAME = "DALE_PREFS"
        private const val KEY_SETUP_COMPLETED = "setup_completed"

        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
