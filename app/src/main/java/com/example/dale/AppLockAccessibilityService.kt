package com.example.dale

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.dale.utils.MonitorStartupHelper
import com.example.dale.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppLockAccessibilityService : AccessibilityService() {

    private val sharedPrefs by lazy { SharedPreferencesManager.getInstance(this) }

    private val unlockedSessions = mutableSetOf<String>()
    private val unlockingApps = mutableSetOf<String>()
    private val backgroundSince = mutableMapOf<String, Long>()
    private val lockInProgress = mutableSetOf<String>()
    private val unlockTimestamps = mutableMapOf<String, Long>()

    private var protectedPackageToGroupId: Map<String, String> = emptyMap()
    private var lastGroupRefreshMs = 0L
    private var lastForegroundPackage: String? = null

    private val groupRefreshIntervalMs = 2000L
    private val unlockGracePeriodMs = 5000L
    private val exitGracePeriodMs = 2000L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val destinationPackage = intent?.getStringExtra("UNLOCKED_PACKAGE") ?: return
            val sourcePackage = intent.getStringExtra("SOURCE_PACKAGE")
            val now = System.currentTimeMillis()
            val isCrossUnlock = !sourcePackage.isNullOrBlank() && sourcePackage != destinationPackage

            when (intent.action) {
                AppMonitorService.ACTION_APP_UNLOCKING -> {
                    lockInProgress.remove(destinationPackage)
                    backgroundSince.remove(destinationPackage)
                    unlockingApps.add(destinationPackage)

                    if (isCrossUnlock) {
                        unlockingApps.add(sourcePackage!!)
                        lockInProgress.remove(sourcePackage)
                        backgroundSince.remove(sourcePackage)
                        unlockTimestamps[sourcePackage] = now
                    }

                    unlockTimestamps[destinationPackage] = now
                }

                AppMonitorService.ACTION_APP_UNLOCKED -> {
                    if (isCrossUnlock) {
                        unlockingApps.remove(sourcePackage!!)
                        lockInProgress.remove(sourcePackage)
                        backgroundSince.remove(sourcePackage)
                        unlockedSessions.add(sourcePackage)
                        unlockTimestamps[sourcePackage] = now
                    }

                    unlockingApps.remove(destinationPackage)
                    unlockedSessions.add(destinationPackage)
                    backgroundSince.remove(destinationPackage)
                    lockInProgress.remove(destinationPackage)
                    unlockTimestamps[destinationPackage] = now
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(AppMonitorService.ACTION_APP_UNLOCKING)
            addAction(AppMonitorService.ACTION_APP_UNLOCKED)
        }
        ContextCompat.registerReceiver(this, unlockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = null
            notificationTimeout = 100
        }
        Log.d(TAG, "Accessibility lock service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        if (!sharedPrefs.isProtectionEnabled()) return
        if (!MonitorStartupHelper.hasOverlayPermission(this)) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName.isBlank() || packageName == this.packageName) return

        refreshProtectedPackageMapIfNeeded()
        cleanupExpiredUnlockTimestamps(System.currentTimeMillis())
        val groupId = protectedPackageToGroupId[packageName]

        if (groupId == null) {
            handleNoProtectedForeground(packageName)
            return
        }

        handleProtectedForeground(packageName, groupId)
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun handleNoProtectedForeground(currentPackage: String) {
        val previous = lastForegroundPackage
        if (previous != null && previous != currentPackage && previous !in unlockingApps) {
            markProtectedAppClosed(previous, System.currentTimeMillis(), "no_protected_foreground")
        }
        if (currentPackage != this.packageName) {
            lastForegroundPackage = null
        }
    }

    private fun handleProtectedForeground(currentPackage: String, groupId: String) {
        val now = System.currentTimeMillis()
        val previous = lastForegroundPackage

        if (previous != null && previous != currentPackage && previous != packageName) {
            markProtectedAppClosed(previous, now, "foreground_switch")
        }

        lastForegroundPackage = currentPackage
        cleanupExpiredUnlockTimestamps(now)

        val unlockTime = unlockTimestamps[currentPackage]
        if (unlockTime != null) {
            val elapsed = now - unlockTime
            if (elapsed < unlockGracePeriodMs) {
                backgroundSince.remove(currentPackage)
                lockInProgress.remove(currentPackage)
                return
            }
            unlockTimestamps.remove(currentPackage)
        }

        if (currentPackage in unlockingApps) {
            val unlockAt = unlockTimestamps[currentPackage]
            val isRecentUnlocking = unlockAt != null && (now - unlockAt) < unlockGracePeriodMs
            if (isRecentUnlocking) {
                backgroundSince.remove(currentPackage)
                lockInProgress.remove(currentPackage)
                return
            }
            clearSessionStateForPackage(currentPackage)
        }

        if (currentPackage in unlockedSessions) {
            val leftAt = backgroundSince[currentPackage]
            if (leftAt != null) {
                val awayDuration = now - leftAt
                if (awayDuration < exitGracePeriodMs) {
                    backgroundSince.remove(currentPackage)
                    return
                }

                // Grace period expired - finalize closure and remove session
                finalizeAppClosed(currentPackage, groupId)
            } else {
                return // Still in session and no background recorded
            }
        }

        if (currentPackage !in lockInProgress) {
            if (!shouldTriggerLockFromLastActivity(groupId, currentPackage)) {
                lockInProgress.remove(currentPackage)
                return
            }
            lockInProgress.add(currentPackage)
            showLockScreen(currentPackage, groupId)
        }
    }

    private fun cleanupExpiredUnlockTimestamps(now: Long) {
        val iterator = unlockTimestamps.entries.iterator()
        val expiredPackages = mutableListOf<String>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > unlockGracePeriodMs) {
                expiredPackages.add(entry.key)
                iterator.remove()
            }
        }

        if (expiredPackages.isNotEmpty()) {
            for (pkg in expiredPackages) {
                // Prevent stale cross-unlock state from suppressing future CLOSED logs.
                unlockingApps.remove(pkg)
            }
        }
    }

    private fun refreshProtectedPackageMapIfNeeded() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastGroupRefreshMs < groupRefreshIntervalMs) return

        val groups = sharedPrefs.getAllAppGroups()
        val map = HashMap<String, String>(groups.size * 2)
        for (group in groups) {
            map[group.app1PackageName] = group.id
            map[group.app2PackageName] = group.id
        }

        protectedPackageToGroupId = map
        lastGroupRefreshMs = now
    }

    private fun showLockScreen(targetPackage: String, groupId: String) {
        val intent = Intent(this, DrawOverOtherAppsLockScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra("TARGET_PACKAGE", targetPackage)
            putExtra("GROUP_ID", groupId)
        }

        try {
            startActivity(intent)
        } catch (t: Throwable) {
            lockInProgress.remove(targetPackage)
            Log.e(TAG, "Failed to launch lock screen for $targetPackage", t)
        }
    }

    private fun saveActivityLog(groupId: String, packageName: String, event: String) {
        val group = sharedPrefs.getAppGroup(groupId) ?: return
        val appName = when (packageName) {
            group.app1PackageName -> group.app1Name
            group.app2PackageName -> group.app2Name
            else -> packageName
        }

        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            .format(Date())

        sharedPrefs.saveActivityLog(
            groupId = groupId,
            entry = ActivityLogEntry(
                appName = appName,
                packageName = packageName,
                event = event,
                timestamp = timestamp
            )
        )
    }

    private fun shouldTriggerLockFromLastActivity(groupId: String, packageName: String): Boolean {
        val latestEvent = sharedPrefs.getLatestActivityEventForPackage(groupId, packageName)
        return latestEvent == null || latestEvent == "CLOSED"
    }

    private fun markProtectedAppClosed(pkg: String, closedAtMs: Long, reason: String) {
        if (pkg in unlockingApps) return

        val groupId = resolveGroupIdForPackage(pkg)
        if (groupId != null) {
            val latestEvent = sharedPrefs.getLatestActivityEventForPackage(groupId, pkg)
            if (latestEvent != "CLOSED") {
                saveActivityLog(groupId, pkg, "CLOSED")
                Log.d(TAG, "Logged CLOSED for $pkg (reason=$reason)")
            } else {
                Log.d(TAG, "Skipped duplicate CLOSED for $pkg (reason=$reason)")
            }
        }

        // Always reset runtime state so subsequent sessions are tracked cleanly.
        clearSessionStateForPackage(pkg)
        backgroundSince[pkg] = closedAtMs
        lockInProgress.remove(pkg)
    }

    private fun finalizeAppClosed(pkg: String, groupId: String) {
        saveActivityLog(groupId, pkg, "CLOSED")
        unlockedSessions.remove(pkg)
        backgroundSince.remove(pkg)
        lockInProgress.remove(pkg)
        Log.d(TAG, "Finalized CLOSED for $pkg and removed session")
    }

    private fun clearSessionStateForPackage(pkg: String) {
        unlockingApps.remove(pkg)
        unlockedSessions.remove(pkg)
        backgroundSince.remove(pkg)
        lockInProgress.remove(pkg)
        unlockTimestamps.remove(pkg)
    }

    private fun resolveGroupIdForPackage(packageName: String): String? {
        protectedPackageToGroupId[packageName]?.let { return it }

        refreshProtectedPackageMapIfNeeded()
        return protectedPackageToGroupId[packageName]
            ?: sharedPrefs.getAllAppGroups().firstOrNull {
                it.app1PackageName == packageName || it.app2PackageName == packageName
            }?.id
    }

    companion object {
        private const val TAG = "DALEAccessibilityLock"
    }
}
