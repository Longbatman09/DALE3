package com.example.dale

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.dale.utils.SharedPreferencesManager

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 500L // Check every 500ms
    private var lastCheckedPackage: String? = null
    private var lockScreenShown = false

    // Track recently unlocked apps to prevent immediate re-lock
    private val unlockedApps = mutableMapOf<String, Long>()
    private val unlockValidityDuration = 5000L // 5 seconds grace period

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("UNLOCKED_PACKAGE")
            if (packageName != null) {
                unlockedApps[packageName] = System.currentTimeMillis()
                lockScreenShown = false
            }
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Register broadcast receiver for unlock events
        val filter = IntentFilter(ACTION_APP_UNLOCKED)
        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
            // Receiver might not be registered
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val currentPackage = getForegroundApp()

        if (currentPackage == null || currentPackage == packageName) {
            lockScreenShown = false
            lastCheckedPackage = currentPackage
            return
        }

        // Don't show lock screen again if it's already shown for this package
        if (currentPackage == lastCheckedPackage && lockScreenShown) {
            return
        }

        // Clean up expired unlock entries
        val currentTime = System.currentTimeMillis()
        unlockedApps.entries.removeAll { (_, unlockTime) ->
            currentTime - unlockTime > unlockValidityDuration
        }

        // Check if app was recently unlocked
        if (unlockedApps.containsKey(currentPackage)) {
            lockScreenShown = false
            lastCheckedPackage = currentPackage
            return
        }

        // Check if this app is in any group
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        val allGroups = sharedPrefs.getAllAppGroups()

        for (group in allGroups) {
            if (group.app1PackageName == currentPackage || group.app2PackageName == currentPackage) {
                // App is in a group - show lock screen
                showLockScreen(currentPackage, group.id)
                lockScreenShown = true
                lastCheckedPackage = currentPackage
                return
            }
        }

        // App is not locked
        lockScreenShown = false
        lastCheckedPackage = currentPackage
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 2 // Last 2 seconds

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var lastPackage: String? = null

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }

        return lastPackage
    }

    private fun showLockScreen(packageName: String, groupId: String) {
        val intent = Intent(this, DrawOverOtherAppsLockScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        private const val CHANNEL_ID = "AppMonitorServiceChannel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_APP_UNLOCKED = "com.example.dale.APP_UNLOCKED"
    }
}



