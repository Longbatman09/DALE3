package com.example.dale

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.dale.utils.SharedPreferencesManager
// import rikka.shizuku.Shizuku  // TODO: Add Shizuku dependency

/**
 * Tier 1: Shizuku Backend Service
 * Premium app detection using direct system API access
 *
 * Features:
 * - 500ms polling interval
 * - Direct IActivityTaskManager access
 * - Package name only tracking
 * - Screen state reset
 * - Automatic fallback to Tier 2 if Shizuku unavailable
 */
class DALEShizukuAppLockService : Service() {
    private val TAG = "ShizukuAppLockService"
    private val NOTIFICATION_ID = 1111
    private val CHANNEL_ID = "ShizukuAppLockServiceChannel"
    
    private var shizukuActivityManager: DALEShizukuActivityManager? = null
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    
    companion object {
        @Volatile
        var isServiceRunning = false
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        DALEAppLockManager.isLockScreenShown.set(false)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started. Running: $isServiceRunning")
        
        if (isServiceRunning) return START_STICKY
        isServiceRunning = true
        
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        
        // Check if Shizuku is available and permission granted
        if (!sharedPrefs.isProtectionEnabled()) {
            Log.e(TAG, "Protection not enabled")
            stopSelf()
            return START_NOT_STICKY
        }
        
        if (!isShizukuAvailable()) {
            Log.e(TAG, "Shizuku not available, falling back to Tier 2")
            isServiceRunning = false
            startFallbackService()
            stopSelf()
            return START_NOT_STICKY
        }
        
        try {
            DALEAppLockManager.resetRestartAttempts(TAG)
            setupShizukuActivityManager()
            
            val shizukuStarted = shizukuActivityManager?.start() == true
            if (!shizukuStarted) {
                Log.e(TAG, "Shizuku failed to start, falling back")
                isServiceRunning = false
                startFallbackService()
                stopSelf()
                return START_NOT_STICKY
            }
            
            startForegroundService()
            Log.d(TAG, "Shizuku service started successfully")
            return START_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
            isServiceRunning = false
            startFallbackService()
            stopSelf()
            return START_NOT_STICKY
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        shizukuActivityManager?.stop()
        
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        if (isServiceRunning && sharedPrefs.isProtectionEnabled()) {
            Log.d(TAG, "Service destroyed unexpectedly, starting fallback")
            startFallbackService()
        }
        
        isServiceRunning = false
        notificationManager?.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun setupShizukuActivityManager() {
        shizukuActivityManager = DALEShizukuActivityManager(
            context = this,
            onForegroundAppChanged = { packageName, className, timeMillis ->
                handleForegroundAppChanged(packageName, className, timeMillis)
            }
        )
    }
    
    private fun handleForegroundAppChanged(packageName: String, className: String, timeMillis: Long) {
        Log.d(TAG, "Foreground app changed: $packageName/$className")
        
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        val lockedApps = sharedPrefs.getAllLockedApps()
        
        // Check if app is in any protected group
        val appGroups = sharedPrefs.getAllAppGroups()
        for (group in appGroups) {
            if (packageName == group.app1PackageName || packageName == group.app2PackageName) {
                // App is in a protected group
                // The main AppMonitorService will handle the lock screen logic
                Log.d(TAG, "Protected app detected: $packageName in group ${group.id}")
                return
            }
        }
    }
    
    private fun isShizukuAvailable(): Boolean {
        // TODO: Implement Shizuku availability check when dependency is added
        // For now, always return false to fallback to Tier 2
        return false
        /* Reference implementation:
        return try {
            Shizuku.pingBinder()
            val hasPermission = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Shizuku available: true, permission: $hasPermission")
            hasPermission
        } catch (e: Exception) {
            Log.d(TAG, "Shizuku not available: ${e.message}")
            false
        }
        */
    }
    
    private fun startFallbackService() {
        try {
            Log.d(TAG, "Starting fallback Tier 2 (Experimental) service")
            startService(Intent(this, DALEExperimentalAppLockService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start fallback service", e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startForegroundService() {
        try {
            createNotificationChannel()
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }
    }
    
    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shizuku App Lock Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors apps with Shizuku backend"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel", e)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DALE App Lock")
            .setContentText("Monitoring (Shizuku)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}



