package com.example.dale

data class ActivityLogEntry(
    val appName: String,
    val packageName: String,
    val event: String,       // "OPENED" or "CLOSED"
    val timestamp: String
)

data class UsageLogEntry(
    val appName: String,
    val packageName: String,
    val totalDurationMs: Long,
    val formattedDuration: String,
    val lastUsedDate: String,
    val usageFraction: Float   // 0.0 – 1.0 relative to all apps in the group
)

