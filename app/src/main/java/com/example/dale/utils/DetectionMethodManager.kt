package com.example.dale.utils

import android.content.Context
import android.util.Log

/**
 * Detection method configuration for DALE
 * Allows switching between different app detection backends
 */
enum class DetectionMethod {
    SHIZUKU,           // Premium - Direct system API
    ACCESSIBILITY,     // Universal - Works on all devices
    USAGE_STATS        // Reliable - High accuracy polling
}

/**
 * Manager for switching between detection methods
 * Used for developer testing and optimization
 */
object DetectionMethodManager {
    private const val TAG = "DetectionMethodManager"
    private const val PREFS_NAME = "detection_method_prefs"
    private const val KEY_METHOD = "detection_method"

    fun setDetectionMethod(context: Context, method: DetectionMethod) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(KEY_METHOD, method.name)
                apply()
            }
            Log.d(TAG, "Detection method set to: ${method.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting detection method", e)
        }
    }

    fun getDetectionMethod(context: Context): DetectionMethod {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val methodName = prefs.getString(KEY_METHOD, DetectionMethod.ACCESSIBILITY.name)
            DetectionMethod.valueOf(methodName ?: DetectionMethod.ACCESSIBILITY.name)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detection method, defaulting to ACCESSIBILITY", e)
            DetectionMethod.ACCESSIBILITY
        }
    }

    fun getMethodName(method: DetectionMethod): String {
        return when (method) {
            DetectionMethod.SHIZUKU -> "Shizuku (Premium API)"
            DetectionMethod.ACCESSIBILITY -> "Accessibility Service"
            DetectionMethod.USAGE_STATS -> "Usage Stats (Polling)"
        }
    }

    fun getMethodDescription(method: DetectionMethod): String {
        return when (method) {
            DetectionMethod.SHIZUKU -> "Direct system API - Most accurate, requires Shizuku"
            DetectionMethod.ACCESSIBILITY -> "Universal - Works on all devices when enabled"
            DetectionMethod.USAGE_STATS -> "Reliable polling - High accuracy with 250ms interval"
        }
    }

    fun getAllMethods(): List<DetectionMethod> {
        return DetectionMethod.values().toList()
    }
}

