package com.example.dale

import android.app.ActivityManager
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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.dale.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val pendingCrossUnlocks = mutableMapOf<String, CrossUnlockHandoff>()
    private val crossUnlockSuppressMs = 5000L
    private var lastUsageEventTimestamp = 0L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val destinationPackage = intent?.getStringExtra("UNLOCKED_PACKAGE") ?: return
            val sourcePackage = intent.getStringExtra("SOURCE_PACKAGE")
            val groupId = intent.getStringExtra("GROUP_ID")
            val isCrossUnlock = !sourcePackage.isNullOrBlank() && sourcePackage != destinationPackage
            val now = System.currentTimeMillis()

            when (intent.action) {
                ACTION_APP_UNLOCKING -> {
                    lockInProgress.remove(destinationPackage)
                    backgroundSince.remove(destinationPackage)

                    if (isCrossUnlock && sourcePackage != null) {
                        unlockingApps.add(sourcePackage)
                        unlockingApps.add(destinationPackage)
                        lockInProgress.remove(sourcePackage)
                        backgroundSince.remove(sourcePackage)
                        unlockTimestamps[sourcePackage] = now
                        unlockTimestamps[destinationPackage] = now
                        pendingCrossUnlocks[destinationPackage] = CrossUnlockHandoff(
                            sourcePackage = sourcePackage,
                            destinationPackage = destinationPackage,
                            groupId = groupId,
                            createdAt = now,
                            firstObservedAt = null,
                            stableForegroundPolls = 0
                        )
                        Log.d(TAG, "Cross-unlocking: $sourcePackage -> $destinationPackage")
                    } else {
                        unlockingApps.add(destinationPackage)
                        pendingCrossUnlocks.remove(destinationPackage)
                        Log.d(TAG, "Unlocking: $destinationPackage")
                    }
                }
                ACTION_APP_UNLOCKED -> {
                    if (isCrossUnlock && sourcePackage != null) {
                        lockInProgress.remove(sourcePackage)
                        lockInProgress.remove(destinationPackage)
                        backgroundSince.remove(sourcePackage)
                        backgroundSince.remove(destinationPackage)
                        unlockTimestamps[sourcePackage] = now
                        unlockTimestamps[destinationPackage] = now
                        // Immediately mark destination as an unlocked session so
                        // the monitor never re-shows the lock screen for it.
                        unlockedSessions.add(destinationPackage)
                        unlockingApps.add(destinationPackage)
                        Log.d(TAG, "Cross-unlock broadcast completed: $sourcePackage -> $destinationPackage")
                    } else {
                        unlockingApps.remove(destinationPackage)
                        unlockedSessions.add(destinationPackage)
                        backgroundSince.remove(destinationPackage)
                        lockInProgress.remove(destinationPackage)
                        unlockTimestamps[destinationPackage] = now
                        Log.d(TAG, "Unlocked: $destinationPackage")
                    }
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

        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) {
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val filter = IntentFilter().apply {
            addAction(ACTION_APP_UNLOCKING)
            addAction(ACTION_APP_UNLOCKED)
        }
        ContextCompat.registerReceiver(this, unlockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Start from "now" so stale historical usage events do not generate fake closes.
        lastUsageEventTimestamp = System.currentTimeMillis()

        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
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
        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) {
            stopSelf()
            return
        }

        refreshProtectedPackageMapIfNeeded()

        if (protectedPackageToGroupId.isEmpty()) {
            handleNoProtectedForeground()
            return
        }

        val activeProtectedPackage = detectActiveProtectedPackage(protectedPackageToGroupId.keys)
        if (activeProtectedPackage == null) {
            handleNoProtectedForeground()
            return
        }

        val groupId = protectedPackageToGroupId[activeProtectedPackage] ?: return
        handleAppTransition(activeProtectedPackage, groupId)
    }

    private fun detectActiveProtectedPackage(protectedPackages: Set<String>): String? {
        detectFromUsageEvents(protectedPackages)?.let { return it }

        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null

        // Requested strategy: try top activity from running tasks first.
        detectFromRunningTasks(am, protectedPackages)?.let { return it }

        // Fallback path for devices/ROMs where running tasks is restricted.
        return detectFromRunningProcesses(am, protectedPackages)
    }

    private fun detectFromUsageEvents(protectedPackages: Set<String>): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = (lastUsageEventTimestamp - 250L).coerceAtLeast(endTime - usageWindowMs)
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var latestForegroundPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE
        var maxProcessedTimestamp = lastUsageEventTimestamp

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp <= lastUsageEventTimestamp) continue

            if (event.timeStamp > maxProcessedTimestamp) {
                maxProcessedTimestamp = event.timeStamp
            }

            val eventType = event.eventType
            val pkg = event.packageName ?: continue
            if (pkg == packageName) continue

            val isForegroundEvent = eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                eventType == UsageEvents.Event.ACTIVITY_RESUMED
            val isBackgroundEvent = eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                eventType == UsageEvents.Event.ACTIVITY_STOPPED

            if (isBackgroundEvent && protectedPackages.contains(pkg)) {
                markProtectedAppClosed(pkg, event.timeStamp, "usage_background")
            }

            if (!isForegroundEvent) continue

            if (event.timeStamp >= latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestForegroundPackage = pkg
            }
        }

        if (maxProcessedTimestamp > lastUsageEventTimestamp) {
            lastUsageEventTimestamp = maxProcessedTimestamp
        }

        val latestPkg = latestForegroundPackage ?: return null
        return if (protectedPackages.contains(latestPkg)) {
            Log.d(TAG, "Detector=usageEvents pkg=$latestPkg")
            latestPkg
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun detectFromRunningTasks(
        am: ActivityManager,
        protectedPackages: Set<String>
    ): String? {
        return try {
            val tasks = am.getRunningTasks(1)
            if (tasks.isNullOrEmpty()) return null

            val task = tasks[0]
            val topPkg = task.topActivity?.packageName
            val basePkg = task.baseActivity?.packageName

            when {
                topPkg != null && topPkg != packageName && protectedPackages.contains(topPkg) -> {
                    Log.d(TAG, "Detector=getRunningTasks top=$topPkg")
                    topPkg
                }
                basePkg != null && basePkg != packageName && protectedPackages.contains(basePkg) -> {
                    Log.d(TAG, "Detector=getRunningTasks base=$basePkg")
                    basePkg
                }
                else -> null
            }
        } catch (t: Throwable) {
            Log.d(TAG, "getRunningTasks unavailable, fallback to runningAppProcesses")
            null
        }
    }

    private fun detectFromRunningProcesses(
        am: ActivityManager,
        protectedPackages: Set<String>
    ): String? {
        val running = am.runningAppProcesses ?: return null
        if (running.isEmpty()) return null

        var best: ProcessCandidate? = null
        for (proc in running) {
            val pkg = pickProtectedPackageFromProcess(proc, protectedPackages) ?: continue
            if (pkg == packageName) continue

            val candidate = ProcessCandidate(
                pkg = pkg,
                importance = proc.importance,
                importanceRank = importanceRank(proc.importance)
            )

            if (best == null || candidate.importanceRank < best!!.importanceRank) {
                best = candidate
            }
        }

        val chosen = best ?: return null
        return if (chosen.importanceRank <= ACTIVE_IMPORTANCE_RANK_MAX) {
            Log.d(TAG, "Detector=runningAppProcesses pkg=${chosen.pkg} importance=${chosen.importance}")
            chosen.pkg
        } else {
            null
        }
    }

    private fun pickProtectedPackageFromProcess(
        proc: ActivityManager.RunningAppProcessInfo,
        protectedPackages: Set<String>
    ): String? {
        val processName = proc.processName?.substringBefore(":")
        if (processName != null && protectedPackages.contains(processName)) {
            return processName
        }

        val pkgList = proc.pkgList ?: return null
        return pkgList.firstOrNull { pkg -> pkg != packageName && protectedPackages.contains(pkg) }
    }

    private fun importanceRank(importance: Int): Int {
        return when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> 0
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> 1
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> 2
            else -> 3
        }
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

    private fun handleNoProtectedForeground() {
        cleanupExpiredCrossUnlocks(System.currentTimeMillis())

        val previous = lastForegroundPackage ?: return
        val now = System.currentTimeMillis()

        markProtectedAppClosed(previous, now, "no_protected_foreground")
        lastForegroundPackage = null
    }

    private fun handleAppTransition(currentPackage: String, groupId: String) {
        val now = System.currentTimeMillis()
        val previous = lastForegroundPackage

        cleanupExpiredCrossUnlocks(now)

        if (previous != null && previous != currentPackage && previous != packageName) {
            markProtectedAppClosed(previous, now, "foreground_switch")
        }

        val pendingCrossUnlock = pendingCrossUnlocks[currentPackage]
        if (pendingCrossUnlock != null) {
            val updated = pendingCrossUnlock.copy(
                firstObservedAt = pendingCrossUnlock.firstObservedAt ?: now,
                stableForegroundPolls = pendingCrossUnlock.stableForegroundPolls + 1
            )
            pendingCrossUnlocks[currentPackage] = updated
            lastForegroundPackage = currentPackage
            backgroundSince.remove(currentPackage)
            lockInProgress.remove(currentPackage)

            if (updated.stableForegroundPolls >= 2 || now - updated.createdAt >= 800L) {
                unlockingApps.remove(updated.sourcePackage)
                unlockingApps.remove(updated.destinationPackage)
                unlockedSessions.add(currentPackage)
                backgroundSince.remove(currentPackage)
                lockInProgress.remove(currentPackage)
                unlockTimestamps[currentPackage] = now
                pendingCrossUnlocks.remove(currentPackage)
                Log.d(TAG, "Accepted cross-unlock handoff to: $currentPackage")
            } else {
                Log.d(TAG, "Waiting for stable cross-unlock handoff to: $currentPackage")
            }
            return
        }

        if (pendingCrossUnlocks.values.any { handoff ->
                (handoff.sourcePackage == currentPackage || handoff.destinationPackage == currentPackage)
                    && now - handoff.createdAt < crossUnlockSuppressMs
            }) {
            lastForegroundPackage = currentPackage
            backgroundSince.remove(currentPackage)
            lockInProgress.remove(currentPackage)
            return
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
            val unlockAt = unlockTimestamps[currentPackage]
            val isRecentUnlocking = unlockAt != null && (now - unlockAt) < unlockGracePeriodMs
            if (isRecentUnlocking) {
                backgroundSince.remove(currentPackage)
                lockInProgress.remove(currentPackage)
                return
            }

            // Stale unlocking state can happen if unlock broadcast/order is interrupted.
            clearSessionStateForPackage(currentPackage)
            Log.d(TAG, "Cleared stale unlocking state for $currentPackage")
        }

        if (currentPackage in unlockedSessions) {
            val latestEvent = SharedPreferencesManager.getInstance(this)
                .getLatestActivityEventForPackage(groupId, currentPackage)
            if (latestEvent != "OPENED") {
                // CLOSED/no-history should always force a relock path.
                clearSessionStateForPackage(currentPackage)
                Log.d(TAG, "Forced relock for $currentPackage due to latestEvent=$latestEvent")
            }

            val leftAt = backgroundSince[currentPackage]
            if (leftAt == null) {
                if (currentPackage in unlockedSessions) {
                    return
                }
            }

            if (leftAt != null) {
                val awayDuration = now - leftAt
                if (awayDuration < exitGracePeriodMs) {
                    backgroundSince.remove(currentPackage)
                    return
                }

                unlockedSessions.remove(currentPackage)
                backgroundSince.remove(currentPackage)
            }
        }

        if (currentPackage !in lockInProgress) {
            if (!shouldTriggerLockFromLastActivity(groupId, currentPackage)) {
                lockInProgress.remove(currentPackage)
                return
            }
            lockInProgress.add(currentPackage)
            Log.d(TAG, "Launching lock screen for: $currentPackage")
            showLockScreen(currentPackage, groupId)
        }
    }

    private fun shouldTriggerLockFromLastActivity(groupId: String, packageName: String): Boolean {
        val latestEvent = SharedPreferencesManager.getInstance(this)
            .getLatestActivityEventForPackage(groupId, packageName)
        val shouldTrigger = latestEvent == null || latestEvent == "CLOSED"
        if (!shouldTrigger) {
            Log.d(TAG, "Lock suppressed for $packageName; latest activity event=$latestEvent")
        }
        return shouldTrigger
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

    private fun cleanupExpiredCrossUnlocks(now: Long) {
        val iterator = pendingCrossUnlocks.entries.iterator()
        while (iterator.hasNext()) {
            val handoff = iterator.next().value
            if (now - handoff.createdAt > crossUnlockSuppressMs) {
                unlockingApps.remove(handoff.sourcePackage)
                unlockingApps.remove(handoff.destinationPackage)
                iterator.remove()
            }
        }
    }

    private fun showLockScreen(packageName: String, groupId: String) {
        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) return
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

    private fun saveActivityLog(groupId: String, packageName: String, event: String) {
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
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

    private fun markProtectedAppClosed(pkg: String, closedAtMs: Long, reason: String) {
        if (pkg in unlockingApps) return

        val insertedAt = backgroundSince.putIfAbsent(pkg, closedAtMs)
        if (insertedAt == null) {
            protectedPackageToGroupId[pkg]?.let { groupId ->
                saveActivityLog(groupId, pkg, "CLOSED")
                Log.d(TAG, "Logged CLOSED for $pkg (reason=$reason)")
            }
            unlockedSessions.remove(pkg)
        }
        lockInProgress.remove(pkg)
    }

    private fun clearSessionStateForPackage(pkg: String) {
        unlockingApps.remove(pkg)
        unlockedSessions.remove(pkg)
        backgroundSince.remove(pkg)
        lockInProgress.remove(pkg)
        unlockTimestamps.remove(pkg)
    }

    companion object {
        private const val TAG = "AppMonitorService"
        private const val CHANNEL_ID = "AppMonitorServiceChannel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_APP_UNLOCKED = "com.example.dale.APP_UNLOCKED"
        const val ACTION_APP_UNLOCKING = "com.example.dale.APP_UNLOCKING"
        private const val ACTIVE_IMPORTANCE_RANK_MAX = 2
        private const val usageWindowMs = 5000L
    }

    private data class ProcessCandidate(
        val pkg: String,
        val importance: Int,
        val importanceRank: Int
    )

    private data class CrossUnlockHandoff(
        val sourcePackage: String,
        val destinationPackage: String,
        val groupId: String?,
        val createdAt: Long,
        val firstObservedAt: Long?,
        val stableForegroundPolls: Int
    )
}
