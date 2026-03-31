package com.example.dale

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.example.dale.utils.SharedPreferencesManager
// TODO: Add Shizuku imports when dependency is available
// import android.app.IActivityTaskManager
// import rikka.shizuku.Shizuku
// import rikka.shizuku.ShizukuBinderWrapper
// import rikka.shizuku.SystemServiceHelper

/**
 * Shizuku-based activity manager for Tier 1 premium app detection
 * Uses direct system API access via Shizuku for maximum accuracy
 *
 * Features:
 * - 500ms polling interval
 * - Direct system API (IActivityTaskManager)
 * - Package name only tracking (ignores internal activity changes)
 * - Screen state monitoring
 * - Device lock detection
 */
class DALEShizukuActivityManager(
    private val context: Context,
    private val onForegroundAppChanged: (String, String, Long) -> Unit
) {
    private val TAG = "ShizukuActivityManager"
    private var lastForegroundApp = ""
    private var shouldLockAppsOnReturn = false
    
    // Screen state receiver
    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen off detected. Resetting app lock state.")
                    DALEAppLockManager.isLockScreenShown.set(false)
                    DALEAppLockManager.clearTemporarilyUnlockedApp()
                    DALEAppLockManager.appUnlockTimes.clear()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Device unlocked")
                    shouldLockAppsOnReturn = true
                }
            }
        }
    }
    
    // Polling runnable (500ms interval)
    private val checkForegroundRunnable = object : Runnable {
        override fun run() {
            try {
                checkForegroundApp()
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkForegroundApp", e)
            }
            // Schedule next check
            context.getMainLooper().let {
                android.os.Handler(it).postDelayed(this, 500)  // 500ms interval
            }
        }
    }
    
    /**
     * Start the Shizuku monitoring
     */
    fun start(): Boolean {
        try {
            // TODO: Implement Shizuku permission check when dependency is available
            // For now, always return false
            Log.d(TAG, "Shizuku monitoring not available (dependency not installed)")
            return false
            
            /* Reference implementation:
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "Shizuku permission granted")
            } else {
                Log.e(TAG, "Shizuku not available")
                return false
            }
            */
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Shizuku monitoring", e)
            return false
        }
    }
    
    /**
     * Stop the Shizuku monitoring
     */
    fun stop() {
        try {
            context.unregisterReceiver(screenStateReceiver)
            Log.d(TAG, "Shizuku monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Shizuku monitoring", e)
        }
    }
    
    private fun startForegroundAppMonitoring() {
        val handler = android.os.Handler(context.getMainLooper())
        handler.post(checkForegroundRunnable)
        Log.d(TAG, "Foreground app monitoring started (500ms interval)")
    }
    
    private fun checkForegroundApp() {
        // Check if protection enabled
        val sharedPrefs = SharedPreferencesManager.getInstance(context)
        if (!sharedPrefs.isProtectionEnabled()) {
            return
        }
        
        // Check if device is locked
        if (context.isDeviceLocked()) {
            return
        }
        
        // Get top activity using system API
        val activity = try {
            topActivity
        } catch (e: Exception) {
            Log.e(TAG, "Error getting top activity", e)
            return
        } ?: return
        
        val packageName = activity.packageName
        val className = activity.className
        
        // Skip DALE itself
        if (packageName == context.packageName) {
            return
        }
        
        // Skip if app is temporarily unlocked
        if (packageName == DALEAppLockManager.temporarilyUnlockedApp) {
            return
        }
        
        // KEY: Only trigger on package change, NOT on class name change!
        // This prevents triggering lock screen on internal activities (camera, dialog, etc.)
        if (packageName != lastForegroundApp) {
            lastForegroundApp = packageName
            
            // Check trigger exclusions
            val triggerExclusions = sharedPrefs.getTriggerExcludedApps()
            if (packageName in triggerExclusions) {
                Log.d(TAG, "Package $packageName is in trigger exclusions")
                return
            }
            
            // Notify about foreground app change
            val timeMillis = System.currentTimeMillis()
            Log.d(TAG, "Foreground app changed to: $packageName, class: $className")
            onForegroundAppChanged(packageName, className, timeMillis)
        }
    }
    
    /**
     * Get the top activity from system
     * TODO: Implement with Shizuku when dependency is available
     */
    private val topActivity: ComponentName?
        get() = null  // TODO: Implement with Shizuku IActivityTaskManager
    
    /**
     * Get activity task manager
     * TODO: Implement with Shizuku when dependency is available
     */
    private val activityTaskManager: Any?
        get() = null  // TODO: Implement with Shizuku SystemServiceHelper
    
    /**
     * Get running tasks
     * TODO: Implement with Shizuku when dependency is available
     */
    private fun getTasksWrapper(): List<ActivityManager.RunningTaskInfo> {
        // TODO: Implement with Shizuku IActivityTaskManager.getTasks()
        return emptyList()
    }
}




