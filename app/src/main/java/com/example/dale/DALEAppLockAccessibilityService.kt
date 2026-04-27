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
import com.example.dale.ActivityLogEntry
import com.example.dale.utils.SharedPreferencesManager
import com.example.dale.utils.AppActivityLogger
import com.example.dale.utils.DetectionMethod
import com.example.dale.utils.DetectionMethodManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

                     // ✅ Clear last opened app when screen turns off (session ends)
                     context?.let {
                         val sharedPrefs = SharedPreferencesManager.getInstance(it)
                         sharedPrefs.clearLastOpenedApp()
                         Log.d("AppDetection", "🔒 Cleared last opened app on screen off")
                     }
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
             Log.d("AppDetection", "✅ ACCESSIBILITY_SERVICE_CREATED - Ready to monitor app events")
             Log.d("AppDetection", "🎯 Watching for app opens/closes...")
             autoOpenTrackedAppAfterServiceCreated()
         } catch (e: Exception) {
             Log.e(TAG, "Error in onCreate", e)
             Log.e("AppDetection", "❌ ERROR_IN_ACCESSIBILITY_SERVICE_CREATION: ${e.message}")
         }
     }

    private fun autoOpenTrackedAppAfterServiceCreated() {
        try {
            val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
            val packageToOpen = sharedPrefs.getLastOpenedAppPackage()

            // If there is no tracked app, open DALE home screen.
            if (packageToOpen.isNullOrBlank()) {
                val daleIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(daleIntent)
                Log.d("AppDetection", "🚀 AUTO_OPEN_TRIGGERED_AFTER_SERVICE_CREATED: DALE_HOME")
                return
            }

            // If the tracked package is DALE, explicitly open DALE home screen.
            if (packageToOpen == packageName) {
                val daleIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(daleIntent)
                Log.d("AppDetection", "🚀 AUTO_OPEN_TRIGGERED_AFTER_SERVICE_CREATED: DALE_HOME")
                return
            }

            val launchIntent = packageManager.getLaunchIntentForPackage(packageToOpen)
            if (launchIntent == null) {
                Log.d("AppDetection", "ℹ️ AUTO_OPEN_SKIPPED - No launch intent for $packageToOpen")
                return
            }

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launchIntent)
            Log.d("AppDetection", "🚀 AUTO_OPEN_TRIGGERED_AFTER_SERVICE_CREATED: $packageToOpen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-open app after service creation", e)
            Log.e("AppDetection", "❌ AUTO_OPEN_ERROR_AFTER_SERVICE_CREATED: ${e.message}")
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
             Log.d("AppDetection", "✅ ACCESSIBILITY_SERVICE_CONNECTED - Now listening to app events")
             DALEAppLockManager.resetRestartAttempts(TAG)
         } catch (e: Exception) {
             Log.e(TAG, "Error in onServiceConnected", e)
             Log.e("AppDetection", "❌ ERROR_CONNECTING_ACCESSIBILITY_SERVICE: ${e.message}")
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

         // Extract and log package name for debugging
         val packageName = event.packageName?.toString() ?: return
         val eventType = when(event.eventType) {
             AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
             AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
             AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
             else -> "OTHER (${event.eventType})"
         }
         Log.v("AppDetection", "📡 Event received - Package: $packageName, Type: $eventType")

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

         // Skip if app is excluded or device locked
         if (!isValidPackageForLocking(packageName)) {
             Log.v("AppDetection", "⏭️  Skipping package: $packageName (excluded or invalid)")
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
         val isSamsungHomeScreen = isSamsungHomeScreenOpened(event)

          when {
              isSamsungHomeScreen -> {
                  Log.d(TAG, "Home launcher detected: ${event.packageName}")
                  Log.d("AppDetection", "🏠 HOME_LAUNCHER_OPENED (${event.packageName})")

                  // ✅ STEP 2 & 3: Use last opened app from storage instead of local variable
                  val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
                  val lastOpenedPackage = sharedPrefs.getLastOpenedAppPackage()
                  val lastOpenedGroupId = sharedPrefs.getLastOpenedAppGroupId()
                  val lastOpenedGroupName = sharedPrefs.getLastOpenedAppGroupName()
                  val lastOpenedAppName = sharedPrefs.getLastOpenedAppName()

                  if (!lastOpenedPackage.isNullOrEmpty() && !lastOpenedGroupId.isNullOrEmpty()) {
                      Log.d("AppDetection", "📤 Logging app closed: $lastOpenedPackage (group: $lastOpenedGroupName)")

                      // ✅ STEP 3: Check if last log is OPENED before logging CLOSED
                      logAppClosedIfProtected(
                          packageName = lastOpenedPackage,
                          groupId = lastOpenedGroupId,
                          groupName = lastOpenedGroupName ?: lastOpenedPackage,
                          appName = lastOpenedAppName ?: lastOpenedPackage,
                          sharedPrefs = sharedPrefs
                      )

                      // Clear after logging
                      sharedPrefs.clearLastOpenedApp()
                  }

                  // Now clear the unlocked state
                  recentsOpen = false
                  clearTemporarilyUnlockedAppIfNeeded()
                  lastForegroundPackage = ""
              }

             isRecentlyOpened -> {
                 Log.d(TAG, "Entering recents")
                 Log.d("AppDetection", "📋 RECENTS_OPENED - User viewing recent apps")
                 recentsOpen = true
             }

              isHomeScreen -> {
                  Log.d(TAG, "On home screen")
                  Log.d("AppDetection", "🏠 HOME_SCREEN_OPENED - User on home screen")
                  recentsOpen = false
                  clearTemporarilyUnlockedAppIfNeeded()

                  // ✅ Also use last opened app tracking here
                  val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
                  val lastOpenedPackage = sharedPrefs.getLastOpenedAppPackage()
                  val lastOpenedGroupId = sharedPrefs.getLastOpenedAppGroupId()
                  val lastOpenedGroupName = sharedPrefs.getLastOpenedAppGroupName()
                  val lastOpenedAppName = sharedPrefs.getLastOpenedAppName()

                  if (!lastOpenedPackage.isNullOrEmpty() && !lastOpenedGroupId.isNullOrEmpty()) {
                      logAppClosedIfProtected(
                          packageName = lastOpenedPackage,
                          groupId = lastOpenedGroupId,
                          groupName = lastOpenedGroupName ?: lastOpenedPackage,
                          appName = lastOpenedAppName ?: lastOpenedPackage,
                          sharedPrefs = sharedPrefs
                      )
                      sharedPrefs.clearLastOpenedApp()
                  }

                  lastForegroundPackage = ""
              }

             isAppSwitchedFromRecents(event) -> {
                 Log.d(TAG, "App switched from recents")
                 Log.d("AppDetection", "🔄 APP_SWITCHED_FROM_RECENTS - App: ${event.packageName}")
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

     private fun isSamsungHomeScreenOpened(event: AccessibilityEvent): Boolean {
         val packageName = event.packageName?.toString() ?: return false
         // Detect any launcher that contains com.android.launcher (Samsung, OneUI, Stock Android, etc.)
         return packageName.contains("com.android.launcher") || packageName == "com.sec.android.app.launcher"
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
        if (packageName == this.packageName ||
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

         // Log app closed if previous app was in a protected group
         if (triggeringPackage.isNotEmpty()) {
             // ✅ Fixed: Find group info before logging closed
             try {
                 val appGroups = sharedPrefs.getAllAppGroups()
                 for (group in appGroups) {
                     if (triggeringPackage == group.app1PackageName) {
                         logAppClosedIfProtected(
                             packageName = triggeringPackage,
                             groupId = group.id,
                             groupName = group.groupName,
                             appName = group.app1Name,
                             sharedPrefs = sharedPrefs
                         )
                         break
                     } else if (triggeringPackage == group.app2PackageName) {
                         logAppClosedIfProtected(
                             packageName = triggeringPackage,
                             groupId = group.id,
                             groupName = group.groupName,
                             appName = group.app2Name,
                             sharedPrefs = sharedPrefs
                         )
                         break
                     }
                 }
             } catch (e: Exception) {
                 Log.e(TAG, "Error logging app closed for $triggeringPackage", e)
             }
         }

         // Log app opened if current app is in a protected group
         logAppOpenedIfProtected(currentForegroundPackage, sharedPrefs)

         checkAndLockApp(currentForegroundPackage, triggeringPackage, System.currentTimeMillis())
     }

     private fun logAppOpenedIfProtected(packageName: String, sharedPrefs: SharedPreferencesManager) {
         try {
             val appGroups = sharedPrefs.getAllAppGroups()
             for (group in appGroups) {
                 if (packageName == group.app1PackageName) {
                     val message = "📱 APP_OPENED: ${group.app1Name} ($packageName) from group '${group.groupName}' [Accessibility Service]"
                     Log.d("AppDetection", message)
                     AppActivityLogger.logAppOpened(
                         packageName,
                         group.app1Name,
                         group.groupName,
                         "Accessibility Service"
                     )
                     return
                 }
                 if (packageName == group.app2PackageName) {
                     val message = "📱 APP_OPENED: ${group.app2Name} ($packageName) from group '${group.groupName}' [Accessibility Service]"
                     Log.d("AppDetection", message)
                     AppActivityLogger.logAppOpened(
                         packageName,
                         group.app2Name,
                         group.groupName,
                         "Accessibility Service"
                     )
                     return
                 }
             }
         } catch (e: Exception) {
             Log.e(TAG, "Error logging app opened: $packageName", e)
         }
     }

      private fun logAppClosedIfProtected(
          packageName: String,
          groupId: String,
          groupName: String,
          appName: String,
          sharedPrefs: SharedPreferencesManager
      ) {
          try {
              Log.d("AppDetection", "🔍 Checking if $packageName should be logged as closed...")

              // ✅ STEP 3: Check if last log entry for this package is "OPENED"
              // If it's already "CLOSED", skip logging to prevent duplicates
              val lastEvent = sharedPrefs.getLatestActivityEventForPackage(groupId, packageName)

              if (lastEvent?.uppercase(Locale.ROOT) == "CLOSED") {
                  Log.d("AppDetection", "⏭️ Skipped logging CLOSED - last event was already CLOSED for $packageName")
                  return
              }

              if (lastEvent == null) {
                  Log.d("AppDetection", "⚠️ No previous log found for $packageName - will log CLOSED anyway")
              } else {
                  Log.d("AppDetection", "✅ Last event was $lastEvent - safe to log CLOSED")
              }

              // ✅ Now log as CLOSED to activity logs (database)
              val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                  .format(Date())

              sharedPrefs.saveActivityLog(
                  groupId = groupId,
                  entry = ActivityLogEntry(
                      appName = appName,
                      packageName = packageName,
                      event = "CLOSED",
                      timestamp = timestamp
                  )
              )

              // Also log to file (security log)
              AppActivityLogger.logAppClosed(
                  packageName,
                  appName,
                  groupName,
                  "Home Launcher Detection (Accessibility Service)"
              )

              Log.d("AppDetection", "✅ App closed successfully logged: $appName ($packageName) in group '$groupName'")

          } catch (e: Exception) {
              Log.e(TAG, "Error logging app closed: $packageName", e)
              Log.e("AppDetection", "❌ Exception in logAppClosedIfProtected: ${e.message}")
              e.printStackTrace()
          }
      }

     private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
          try {
              val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)

              // Check if app is in any protected group
              val appGroups = sharedPrefs.getAllAppGroups()
              for (group in appGroups) {
                  if (packageName == group.app1PackageName || packageName == group.app2PackageName) {
                      val appName = if (packageName == group.app1PackageName) group.app1Name else group.app2Name

                      // ✅ NEW ALGORITHM: Check last log entry for this package FIRST
                      // Do this BEFORE checking isLockScreenShown, because state should be based on actual logs
                      Log.d("AppDetection", "🔍 Checking last log entry for $packageName...")
                      val lastEvent = sharedPrefs.getLatestActivityEventForPackage(group.id, packageName)

                      when {
                          lastEvent?.uppercase(Locale.ROOT) == "OPENED" -> {
                              // ✅ Last log is OPENED - User is currently using the app
                              Log.d("AppDetection", "✅ LAST LOG IS OPENED: $appName ($packageName) - User already unlocked, skipping lock screen")
                              return
                          }
                          lastEvent?.uppercase(Locale.ROOT) == "CLOSED" -> {
                              // ✅ Last log is CLOSED - App was closed, need lock screen
                              Log.d("AppDetection", "🔒 LAST LOG IS CLOSED: $appName ($packageName) - Triggering lock screen")

                              // ✅ Reset lock screen state if needed (app was closed, so it's a new session)
                              if (DALEAppLockManager.isLockScreenShown.get()) {
                                  Log.d("AppDetection", "⚠️ Resetting lock screen state for new session")
                                  DALEAppLockManager.isLockScreenShown.set(false)
                              }

                              showLockScreen(packageName, group.id)
                              return
                          }
                          lastEvent == null -> {
                              // ✅ No previous logs - First time opening, trigger lock screen
                              Log.d("AppDetection", "⚠️ NO PREVIOUS LOGS: $appName ($packageName) - First time opening, triggering lock screen")

                              // ✅ Reset lock screen state if needed
                              if (DALEAppLockManager.isLockScreenShown.get()) {
                                  Log.d("AppDetection", "⚠️ Resetting lock screen state for first-time opening")
                                  DALEAppLockManager.isLockScreenShown.set(false)
                              }

                              showLockScreen(packageName, group.id)
                              return
                          }
                          else -> {
                              // Unknown event type, trigger lock screen for safety
                              Log.d("AppDetection", "❓ UNKNOWN EVENT TYPE: $appName ($packageName) - Last event: $lastEvent - Triggering lock screen")

                              // ✅ Reset lock screen state if needed
                              if (DALEAppLockManager.isLockScreenShown.get()) {
                                  Log.d("AppDetection", "⚠️ Resetting lock screen state for unknown event")
                                  DALEAppLockManager.isLockScreenShown.set(false)
                              }

                              showLockScreen(packageName, group.id)
                              return
                          }
                      }
                  }
              }

              // App is not protected, just log it
              Log.d("AppDetection", "✅ UNPROTECTED_APP_OPENED: $packageName (not in any group)")

          } catch (e: Exception) {
              Log.e(TAG, "Error checking app lock", e)
              Log.e("AppDetection", "❌ ERROR_IN_CHECK_AND_LOCK: $packageName - ${e.message}")
          }
      }

      private fun showLockScreen(packageName: String, groupId: String) {
          try {
              DALEAppLockManager.isLockScreenShown.set(true)
              Log.d(TAG, "Showing lock screen for: $packageName")

              // Log the lock screen trigger
              val sharedPrefs = SharedPreferencesManager.getInstance(applicationContext)
              val group = sharedPrefs.getAppGroup(groupId)
              if (group != null) {
                  val appName = if (packageName == group.app1PackageName) group.app1Name else group.app2Name

                  Log.d("AppDetection", "🔐 LOCK_SCREEN_TRIGGERED ========================================")
                  Log.d("AppDetection", "   App: $appName")
                  Log.d("AppDetection", "   Package: $packageName")
                  Log.d("AppDetection", "   Group: ${group.groupName}")
                  Log.d("AppDetection", "   Method: Accessibility Service")
                  Log.d("AppDetection", "   Status: User must enter credentials to unlock")
                  Log.d("AppDetection", "🔐 ========================================")

                  AppActivityLogger.logLockScreenTriggered(
                      packageName,
                      appName,
                      group.groupName,
                      "Accessibility Service"
                  )
              }

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
              Log.e("AppDetection", "❌ ERROR_SHOWING_LOCK_SCREEN: $packageName - ${e.message}")
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

