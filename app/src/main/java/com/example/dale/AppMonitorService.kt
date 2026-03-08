package com.example.dale

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.dale.utils.SharedPreferencesManager

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 400L
    private var isPolling = false

    // Session tracking.
    private val unlockedSessions = mutableSetOf<String>()
    private val unlockingApps = mutableSetOf<String>()
    private val backgroundSince = mutableMapOf<String, Long>()
    private val lockInProgress = mutableSetOf<String>()
    private val unlockTimestamps = mutableMapOf<String, Long>()
    private var lastForegroundPackage: String? = null

    private val unlockGracePeriodMs = 5000L
    private val exitGracePeriodMs = 2000L

    // Cached package -> groupId map, refreshed periodically.
    private var protectedPackageToGroupId: Map<String, String> = emptyMap()
    private var lastGroupRefreshMs = 0L
    private val groupRefreshIntervalMs = 2000L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.getStringExtra("UNLOCKED_PACKAGE") ?: return
            when (intent.action) {
                ACTION_APP_UNLOCKING -> {
                    unlockingApps.add(pkg)
                    Log.d(TAG, "Unlocking: $pkg")
                }
                ACTION_APP_UNLOCKED -> {
                    unlockingApps.remove(pkg)
                    unlockedSessions.add(pkg)
                    backgroundSince.remove(pkg)
                    lockInProgress.remove(pkg)
                    unlockTimestamps[pkg] = System.currentTimeMillis()
                    Log.d(TAG, "Unlocked: $pkg")
                }
            }
        }
    }

    private val appCheckRunnable = object : Runnable {
        override fun run() {
            try {
                pollForegroundApp()
            } finally {
                if (isPolling) {
                    handler.postDelayed(this, checkIntervalMs)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val filter = IntentFilter().apply {
            addAction(ACTION_APP_UNLOCKING)
            addAction(ACTION_APP_UNLOCKED)
        }
        ContextCompat.registerReceiver(this, unlockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        stopPolling()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
            // Receiver may already be unregistered.
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        handler.post(appCheckRunnable)
    }

    private fun stopPolling() {
        isPolling = false
        handler.removeCallbacks(appCheckRunnable)
    }

    private fun pollForegroundApp() {
        refreshProtectedPackageMapIfNeeded()

        if (protectedPackageToGroupId.isEmpty()) {
            handleNoProtectedForeground()
            return
        }

        val foregroundProtectedPackage = detectForegroundProtectedPackage(protectedPackageToGroupId.keys)
        if (foregroundProtectedPackage == null) {
            handleNoProtectedForeground()
            return
        }

        val groupId = protectedPackageToGroupId[foregroundProtectedPackage] ?: return
        handleAppTransition(foregroundProtectedPackage, groupId)
    }

    private fun refreshProtectedPackageMapIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastGroupRefreshMs < groupRefreshIntervalMs) return

        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        val groups = sharedPrefs.getAllAppGroups()
        val map = HashMap<String, String>(groups.size * 2)
        for (group in groups) {
            map[group.app1PackageName] = group.id
            map[group.app2PackageName] = group.id
        }
        protectedPackageToGroupId = map
        lastGroupRefreshMs = now
    }

    private fun detectForegroundProtectedPackage(protectedPackages: Set<String>): String? {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val running = am.runningAppProcesses ?: return null
        if (running.isEmpty()) return null

        val candidates = running
            .asSequence()
            .filter {
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
            }
            .sortedBy { it.importance }

        for (proc in candidates) {
            val fromPkgList = proc.pkgList
                ?.firstOrNull { pkg -> pkg != packageName && protectedPackages.contains(pkg) }
            if (fromPkgList != null) return fromPkgList

            val processName = proc.processName ?: continue
            val normalized = processName.substringBefore(":")
            if (normalized != packageName && protectedPackages.contains(normalized)) {
                return normalized
            }
        }
        return null
    }

    private fun handleNoProtectedForeground() {
        val previous = lastForegroundPackage ?: return
        val now = System.currentTimeMillis()

        if (previous !in unlockingApps) {
            backgroundSince.putIfAbsent(previous, now)
        }
        lockInProgress.remove(previous)
        lastForegroundPackage = null
    }

    private fun handleAppTransition(currentPackage: String, groupId: String) {
        val now = System.currentTimeMillis()
        val previous = lastForegroundPackage

        if (previous != null && previous != currentPackage && previous != packageName) {
            if (previous !in unlockingApps) {
                backgroundSince.putIfAbsent(previous, now)
            }
            lockInProgress.remove(previous)
        }

        lastForegroundPackage = currentPackage

        if (currentPackage == packageName) {
            lockInProgress.clear()
            return
        }

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
            backgroundSince.remove(currentPackage)
            return
        }

        if (currentPackage in unlockedSessions) {
            val leftAt = backgroundSince[currentPackage]
            if (leftAt == null) {
                return
            }

            val awayDuration = now - leftAt
            if (awayDuration < exitGracePeriodMs) {
                backgroundSince.remove(currentPackage)
                return
            }

            unlockedSessions.remove(currentPackage)
            backgroundSince.remove(currentPackage)
        }

        if (currentPackage !in lockInProgress) {
            lockInProgress.add(currentPackage)
            showLockScreen(currentPackage, groupId)
        }
    }

    private fun cleanupExpiredUnlockTimestamps(now: Long) {
        val iterator = unlockTimestamps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > unlockGracePeriodMs) {
                iterator.remove()
            }
        }
    }

    private fun showLockScreen(packageName: String, groupId: String) {
        if (packageName == this.packageName) return

        val intent = Intent(this, DrawOverOtherAppsLockScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra("TARGET_PACKAGE", packageName)
            putExtra("GROUP_ID", groupId)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Monitor Service",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Monitors apps for lock protection"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "AppMonitorService"
        private const val CHANNEL_ID = "AppMonitorServiceChannel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_APP_UNLOCKED = "com.example.dale.APP_UNLOCKED"
        const val ACTION_APP_UNLOCKING = "com.example.dale.APP_UNLOCKING"
    }
}
