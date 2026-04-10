package com.example.dale

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit

class DeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DALEDeviceAdmin"
        private const val PREFS_NAME = "com.example.dale.admin_prefs"
        private const val KEY_ANTI_UNINSTALL_ENABLED = "anti_uninstall_enabled"

        fun enableAntiUninstall(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(context, DeviceAdminReceiver::class.java)

                // Set uninstall blocked for DALE itself
                dpm.setUninstallBlocked(component, context.packageName, true)

                // Store state
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                    putBoolean(KEY_ANTI_UNINSTALL_ENABLED, true)
                }

                Log.d(TAG, "Anti-uninstall enabled for DALE")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable anti-uninstall", e)
                false
            }
        }

        fun disableAntiUninstall(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(context, DeviceAdminReceiver::class.java)

                // Allow uninstall for DALE
                dpm.setUninstallBlocked(component, context.packageName, false)

                // Store state
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                    putBoolean(KEY_ANTI_UNINSTALL_ENABLED, false)
                }

                Log.d(TAG, "Anti-uninstall disabled")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable anti-uninstall", e)
                false
            }
        }

        fun isAntiUninstallEnabled(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(context, DeviceAdminReceiver::class.java)
                dpm.isAdminActive(component)
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        // Block DALE's own uninstallation
        enableAntiUninstall(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device admin disabled")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ANTI_UNINSTALL_ENABLED, false)
        }
    }
}

