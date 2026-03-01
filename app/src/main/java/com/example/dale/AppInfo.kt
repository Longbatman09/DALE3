package com.example.dale

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystem: Boolean = false,
    val isLauncher: Boolean = false
)
