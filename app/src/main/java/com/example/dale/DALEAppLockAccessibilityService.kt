package com.example.dale

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.example.dale.utils.SharedPreferencesManager

/**
 * Tier 3: Accessibility Service Backend
 * Universal fallback using AccessibilityService for 95%+ device coverage
 *
 * Features:
 * - Event-driven (no polling needed!)
 * - Detects recents and home screen
 * - System UI aware
 * - Works on all devices when enabled
 * - Lowest battery impact
 */
@SuppressLint("AccessibilityPolicy")
class DALEAppLockAccessibilityService : AccessibilityService() {
    private val TAG = "AccessibilityService"
    
    private var recentsOpen = false
    private var lastForegroundPackage = ""
    private var keyboardPackages: List<String> = emptyList()
    
    companion object {
        @Volatile
        var isServiceRunning = false
            private set
    }
    
    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            try {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    Log.d(TAG, "Screen off detected. Resetting app lock state.")
                    DALEAppLockManager.isLockScreenShown.set(false)
                    DALEAppLockManager.clearTemporarilyUnlockedApp()
                    DALEAppLockManager.appUnlockTimes.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in screenStateReceiver", e)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            isServiceRunning = true
            DALEAppLockManager.currentBiometricState = DALEAppLockManager.BiometricState.IDLE
            DALEAppLockManager.isLockScreenShown.set(false)
            
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
            
            // Get keyboard packages
            try {
                keyboardPackages = getSystemService<InputMethodManager>()
                    ?.enabledInputMethodList
                    ?.map { it.packageName }
                    ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting keyboard packages", e)
            }
            
            Log.d(TAG, "Accessibility service created")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                packageNames = null
            }
            
            Log.d(TAG, "Accessibility service connected")
            DALEAppLockManager.resetRestartAttempts(TAG)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onServiceConnected", e)
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            handleAccessibilityEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }
    
    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        // Early return if protection disabled
        if (!applicationContext.let { SharedPreferencesManager.getInstance(it).isProtectionEnabled() } || !isServiceRunning) {
            return
        }
        
        // Handle window state changes (recents, home screen detection)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                handleWindowStateChanged(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling window state change", e)
                return
            }
        }
        
        // Skip processing if recents are open
        if (recentsOpen) {
            Log.d(TAG, "Recents open, ignoring event")
            return
        }
        
        // Extract and validate package name
        val packageName = event.packageName?.toString() ?: return
        
        // Skip if app is excluded or device locked
        if (!isValidPackageForLocking(packageName)) {
            return
        }
        
        try {
            processPackageLocking(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing package locking", e)
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val isRecentlyOpened = isRecentlyOpened(event)
        val isHomeScreen = isHomeScreen(event)
        
        when {
            isRecentlyOpened -> {
                Log.d(TAG, "Entering recents")
                recentsOpen = true
            }
            
            isHomeScreen -> {
                Log.d(TAG, "On home screen")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded()
            }
            
            isAppSwitchedFromRecents(event) -> {
                Log.d(TAG, "App switched from recents")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded(event.packageName?.toString())
            }
        }
    }
    
    @SuppressLint("InlinedApi")
    private fun isRecentlyOpened(event: AccessibilityEvent): Boolean {
        return (event.packageName == getSystemDefaultLauncherPackageName() &&
                event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) ||
                (event.text.toString().lowercase().contains("recent"))
    }
    
    private fun isHomeScreen(event: AccessibilityEvent): Boolean {
        return event.packageName == getSystemDefaultLauncherPackageName() &&
                (event.className?.contains("Launcher") == true ||
                        event.text.toString().lowercase().contains("home screen"))
    }
    
    private fun isAppSwitchedFromRecents(event: AccessibilityEvent): Boolean {
        return event.packageName != getSystemDefaultLauncherPackageName() && recentsOpen
    }
    
    private fun clearTemporarilyUnlockedAppIfNeeded(newPackage: String? = null) {
        val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
        val shouldClear = newPackage == null ||
                (newPackage != DALEAppLockManager.temporarilyUnlockedApp &&
                        newPackage !in sharedPrefs.getTriggerExcludedApps())
        
        if (shouldClear) {
            Log.d(TAG, "Clearing temporarily unlocked app")
            DALEAppLockManager.clearTemporarilyUnlockedApp()
        }
    }
    
    private fun isValidPackageForLocking(packageName: String): Boolean {
        // Check if device is locked
        if (applicationContext.isDeviceLocked()) {
            DALEAppLockManager.appUnlockTimes.clear()
            DALEAppLockManager.clearTemporarilyUnlockedApp()
            return false
        }
        
        // Skip excluded packages
        if (packageName == packageName ||
            packageName in keyboardPackages ||
            packageName in DALELockConstants.EXCLUDED_APPS
        ) {
            return false
        }
        
        return true
    }
    
    private fun processPackageLocking(packageName: String) {
        val currentForegroundPackage = packageName
        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = currentForegroundPackage
        
        val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
        
        // Skip if triggering package is excluded
        if (triggeringPackage in sharedPrefs.getTriggerExcludedApps()) {
            return
        }
        
        // Check if package changed
        if (currentForegroundPackage == triggeringPackage) {
            return
        }
        
        checkAndLockApp(currentForegroundPackage, triggeringPackage, System.currentTimeMillis())
    }
    
    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        try {
            // Return if lock screen already showing
            if (DALEAppLockManager.isLockScreenShown.get()) {
                Log.d(TAG, "Lock screen already shown")
                return
            }
            
            val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
            
            // Check if app is in any protected group
            val appGroups = sharedPrefs.getAllAppGroups()
            for (group in appGroups) {
                if (packageName == group.app1PackageName || packageName == group.app2PackageName) {
                    // Check unlock grace period
                    val unlockTime = DALEAppLockManager.appUnlockTimes[packageName]
                    if (unlockTime != null) {
                        val elapsed = currentTime - unlockTime
                        if (elapsed < 5000) {
                            Log.d(TAG, "App in grace period")
                            return
                        }
                        DALEAppLockManager.appUnlockTimes.remove(packageName)
                    }
                    
                    // Check if temporarily unlocked
                    if (DALEAppLockManager.isAppTemporarilyUnlocked(packageName)) {
                        return
                    }
                    
                    // Show lock screen
                    showLockScreen(packageName, group.id)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app lock", e)
        }
    }
    
    private fun showLockScreen(packageName: String, groupId: String) {
        try {
            DALEAppLockManager.isLockScreenShown.set(true)
            Log.d(TAG, "Showing lock screen for: $packageName")
            
            val intent = Intent(this, DrawOverOtherAppsLockScreen::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra("TARGET_PACKAGE", packageName)
                putExtra("GROUP_ID", groupId)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing lock screen", e)
            DALEAppLockManager.isLockScreenShown.set(false)
        }
    }
    
    private fun getSystemDefaultLauncherPackageName(): String {
        return try {
            val pm = packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            
            val resolveInfoList = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfoList.firstOrNull()?.activityInfo?.packageName ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting launcher package", e)
            ""
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Service unbound")
        isServiceRunning = false
        return super.onUnbind(intent)
    }
    
    override fun onDestroy() {
        try {
            super.onDestroy()
            isServiceRunning = false
            Log.d(TAG, "Accessibility service destroyed")
            
            try {
                unregisterReceiver(screenStateReceiver)
            } catch (_: IllegalArgumentException) {
                Log.w(TAG, "Receiver not registered")
            }
            
            DALEAppLockManager.isLockScreenShown.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}

