package com.example.dale.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.dale.AppMonitorService

object MonitorStartupHelper {
    private const val TAG = "MonitorStartupHelper"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/${com.example.dale.DALEAppLockAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(':')
            .any { it.equals(expectedService, ignoreCase = true) }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val batterySettingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startFirstResolvableActivity(context, listOf(batterySettingsIntent, appDetailsIntent))
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to open accessibility settings", t)
        }
    }

    fun canStartMonitoring(context: Context): Boolean {
        val sharedPrefs = SharedPreferencesManager.getInstance(context)
        if (!sharedPrefs.isProtectionEnabled() || !hasOverlayPermission(context)) return false

        // Accessibility is the primary detection method.
        return isAccessibilityServiceEnabled(context) || hasUsageStatsPermission(context)
    }

    fun startMonitoringIfPossible(context: Context): Boolean {
        val sharedPrefs = SharedPreferencesManager.getInstance(context)
        if (!sharedPrefs.isProtectionEnabled()) {
            stopMonitoringService(context)
            Log.d(TAG, "Skipping monitor start: protection toggle is OFF")
            return false
        }

        if (!hasOverlayPermission(context)) {
            Log.d(TAG, "Skipping monitor start: overlay permission missing")
            stopMonitoringService(context)
            return false
        }

        if (isAccessibilityServiceEnabled(context)) {
            // Accessibility backend is active; avoid duplicate lock triggers from monitor polling.
            stopMonitoringService(context)
            Log.d(TAG, "Accessibility backend active; monitor fallback stopped")
            return true
        }

        if (!hasUsageStatsPermission(context)) {
            Log.d(TAG, "Skipping monitor start: usage permission missing and accessibility disabled")
            return false
        }

        startMonitoringService(context)
        return true
    }

    fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, AppMonitorService::class.java)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to start AppMonitorService", t)
        }
    }

    fun stopMonitoringService(context: Context) {
        try {
            context.stopService(Intent(context, AppMonitorService::class.java))
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to stop AppMonitorService", t)
        }
    }

    private fun startFirstResolvableActivity(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            val canResolve = intent.resolveActivity(context.packageManager) != null
            if (!canResolve) continue

            try {
                context.startActivity(intent)
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to launch settings intent: ${intent.action}", t)
            }
        }
    }
}
