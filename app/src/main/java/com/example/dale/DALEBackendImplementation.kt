package com.example.dale

/**
 * Enum to represent different backend implementations for app detection
 */
enum class DALEBackendImplementation {
    /**
     * Tier 1: Premium method using Shizuku system API
     * - Requires: Shizuku app installed
     * - Polling: 500ms
     * - Accuracy: Excellent
     */
    SHIZUKU,

    /**
     * Tier 2: Reliable method using Usage Stats events
     * - Requires: PACKAGE_USAGE_STATS permission
     * - Polling: 250ms (FASTEST)
     * - Accuracy: Good
     */
    USAGE_STATS,

    /**
     * Tier 3: Universal fallback using Accessibility Service
     * - Requires: User enable accessibility
     * - Polling: Event-driven (NO polling)
     * - Accuracy: Fair
     * - Availability: 95%+ devices
     */
    ACCESSIBILITY
}

