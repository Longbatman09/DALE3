package com.example.dale.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.dale.AppGroup
import com.google.gson.Gson

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

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
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

