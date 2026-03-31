package com.example.dale

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Constants for app lock configuration
 */
object DALELockConstants {
    val KNOWN_RECENTS_CLASSES = setOf(
        "com.android.systemui.recents.RecentsActivity",
        "com.android.quickstep.RecentsActivity",
        "com.android.systemui.recents.RecentsView",
        "com.android.systemui.recents.RecentsPanelView",
    )

    val EXCLUDED_APPS = setOf(
        "com.android.systemui",
        "com.android.intentresolver",
        "com.google.android.permissioncontroller",
        "android.uid.system:1000",
        "com.google.android.googlequicksearchbox",
        "android",
        "com.google.android.gms",
        "com.google.android.webview"
    )

    val ACCESSIBILITY_SETTINGS_CLASSES = setOf(
        "com.android.settings.accessibility.AccessibilitySettings",
        "com.android.settings.accessibility.AccessibilityMenuActivity",
        "com.android.settings.accessibility.AccessibilityShortcutActivity",
        "com.android.settings.Settings\$AccessibilitySettingsActivity"
    )

    const val MAX_RESTART_ATTEMPTS = 3
    const val RESTART_COOLDOWN_MS = 30000L
    const val RESTART_INTERVAL_MS = 5000L
}

/**
 * Extension function to check if device is locked
 */
fun Context.isDeviceLocked(): Boolean {
    val keyguardManager = getSystemService(KeyguardManager::class.java)
    return keyguardManager?.isKeyguardLocked ?: false
}

/**
 * Extension function to check if a service is running
 */
@Suppress("DEPRECATION")
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(ActivityManager::class.java) ?: return false
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { serviceClass.name == it.service.className }
}

/**
 * Centralized app lock manager for state management
 * Tracks unlock state, grace periods, and lock screen visibility
 */
object DALEAppLockManager {
    private const val TAG = "DALEAppLockManager"

    // State tracking
    var temporarilyUnlockedApp: String = ""
    val appUnlockTimes = ConcurrentHashMap<String, Long>()
    val isLockScreenShown = AtomicBoolean(false)
    var currentBiometricState: Any? = null

    // Grace period tracking (300ms)
    private var recentlyLeftApp: String = ""
    private var recentlyLeftTime: Long = 0L
    private const val GRACE_PERIOD_MS = 300L

    enum class BiometricState {
        IDLE, AUTH_STARTED
    }

    /**
     * Mark app as recently left for grace period restoration
     */
    fun setRecentlyLeftApp(packageName: String) {
        recentlyLeftApp = packageName
        recentlyLeftTime = System.currentTimeMillis()
        Log.d(TAG, "Left app $packageName at $recentlyLeftTime")
    }

    /**
     * Check if app can be restored within grace period (300ms)
     */
    fun checkAndRestoreRecentlyLeftApp(packageName: String): Boolean {
        if (packageName == recentlyLeftApp && packageName.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - recentlyLeftTime
            if (elapsed <= GRACE_PERIOD_MS) {
                Log.d(TAG, "Restoring unlock state for $packageName (elapsed: ${elapsed}ms)")
                temporarilyUnlockedApp = packageName
                recentlyLeftApp = ""
                recentlyLeftTime = 0L
                return true
            } else {
                Log.d(TAG, "Grace period expired for $packageName (elapsed: ${elapsed}ms)")
                recentlyLeftApp = ""
            }
        }
        return false
    }

    /**
     * Unlock an app and record the timestamp
     */
    fun unlockApp(packageName: String) {
        temporarilyUnlockedApp = packageName
        appUnlockTimes[packageName] = System.currentTimeMillis()
        Log.d(
            TAG,
            "App $packageName unlocked at timestamp: ${appUnlockTimes[packageName]}, current time: ${System.currentTimeMillis()}"
        )
    }

    /**
     * Check if app is currently unlocked
     */
    fun isAppTemporarilyUnlocked(packageName: String): Boolean =
        temporarilyUnlockedApp == packageName

    /**
     * Clear the temporarily unlocked app state
     */
    fun clearTemporarilyUnlockedApp() {
        temporarilyUnlockedApp = ""
    }

    /**
     * Clear all state (called on screen OFF or device LOCK)
     */
    fun clearAllState() {
        isLockScreenShown.set(false)
        clearTemporarilyUnlockedApp()
        appUnlockTimes.clear()
        recentlyLeftApp = ""
        recentlyLeftTime = 0L
    }

    /**
     * Service restart tracking
     */
    private val serviceRestartAttempts = ConcurrentHashMap<String, Int>()
    private val lastRestartTime = ConcurrentHashMap<String, Long>()

    fun resetRestartAttempts(serviceName: String) {
        serviceRestartAttempts.remove(serviceName)
        lastRestartTime.remove(serviceName)
        Log.d(TAG, "Reset restart attempts for $serviceName")
    }

    fun shouldAttemptRestart(serviceName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val attempts = serviceRestartAttempts[serviceName] ?: 0
        val lastRestart = lastRestartTime[serviceName] ?: 0L

        if (currentTime - lastRestart < DALELockConstants.RESTART_INTERVAL_MS) {
            Log.d(TAG, "Service $serviceName restart too recent, skipping")
            return false
        }

        if (attempts >= DALELockConstants.MAX_RESTART_ATTEMPTS) {
            if (currentTime - lastRestart > DALELockConstants.RESTART_COOLDOWN_MS) {
                Log.d(TAG, "Cooldown expired for $serviceName, resetting attempts")
                serviceRestartAttempts[serviceName] = 0
                return true
            }
            Log.d(TAG, "Max restart attempts reached for $serviceName, in cooldown")
            return false
        }

        return true
    }

    private fun recordRestartAttempt(serviceName: String) {
        val currentTime = System.currentTimeMillis()
        serviceRestartAttempts.compute(serviceName) { _, attempts -> (attempts ?: 0) + 1 }
        lastRestartTime[serviceName] = currentTime
        Log.d(TAG, "Recorded restart attempt ${serviceRestartAttempts[serviceName]} for $serviceName")
    }
}

