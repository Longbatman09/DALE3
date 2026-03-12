package com.example.dale

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dale.utils.MonitorStartupHelper
import com.example.dale.utils.SharedPreferencesManager

class MonitorRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val sharedPrefs = SharedPreferencesManager.getInstance(context)
                if (!sharedPrefs.isSetupCompleted()) return
                if (!sharedPrefs.isProtectionEnabled()) {
                    MonitorStartupHelper.stopMonitoringService(context)
                    return
                }
                MonitorStartupHelper.startMonitoringIfPossible(context)
            }
        }
    }
}
