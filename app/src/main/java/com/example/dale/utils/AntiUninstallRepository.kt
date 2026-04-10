package com.example.dale.utils

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.ComponentName
import com.example.dale.DeviceAdminReceiver

/**
 * Repository for managing anti-uninstall feature
 * Automatically adds all apps in groups to the anti-uninstall protection list
 */
class AntiUninstallRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Add a package to anti-uninstall protection
     */
    fun addProtectedPackage(packageName: String) {
        if (packageName.isBlank()) return
        val current = getProtectedPackages().toMutableSet()
        current.add(packageName)
        saveProtectedPackages(current)
    }

    /**
     * Remove a package from anti-uninstall protection
     */
    fun removeProtectedPackage(packageName: String) {
        val current = getProtectedPackages().toMutableSet()
        current.remove(packageName)
        saveProtectedPackages(current)
    }

    /**
     * Get all protected packages
     */
    fun getProtectedPackages(): Set<String> {
        return sharedPrefs.getStringSet(KEY_PROTECTED_PACKAGES, emptySet()) ?: emptySet()
    }

    /**
     * Check if a package is protected
     */
    fun isProtected(packageName: String): Boolean {
        return getProtectedPackages().contains(packageName)
    }

    /**
     * Clear all protected packages
     */
    fun clearAll() {
        sharedPrefs.edit().putStringSet(KEY_PROTECTED_PACKAGES, emptySet()).apply()
    }

    /**
     * Enable anti-uninstall system-wide
     * Requires device admin to be active
     */
    fun enableAntiUninstall(): Boolean {
        return try {
            DeviceAdminReceiver.enableAntiUninstall(context)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Disable anti-uninstall system-wide
     */
    fun disableAntiUninstall(): Boolean {
        return try {
            DeviceAdminReceiver.disableAntiUninstall(context)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if anti-uninstall is currently active
     */
    fun isAntiUninstallActive(): Boolean {
        return DeviceAdminReceiver.isAntiUninstallEnabled(context)
    }

    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, DeviceAdminReceiver::class.java)
            dpm.isAdminActive(component)
        } catch (e: Exception) {
            false
        }
    }

    private fun saveProtectedPackages(packages: Set<String>) {
        sharedPrefs.edit().putStringSet(KEY_PROTECTED_PACKAGES, packages).apply()
    }

    companion object {
        private const val PREFS_NAME = "com.example.dale.anti_uninstall"
        private const val KEY_PROTECTED_PACKAGES = "protected_packages"

        @Volatile
        private var INSTANCE: AntiUninstallRepository? = null

        fun getInstance(context: Context): AntiUninstallRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AntiUninstallRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

