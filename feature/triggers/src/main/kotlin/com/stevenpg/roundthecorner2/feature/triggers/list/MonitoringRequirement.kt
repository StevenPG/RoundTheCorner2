package com.stevenpg.roundthecorner2.feature.triggers.list

import com.stevenpg.roundthecorner2.core.monitoring.PermissionSnapshot

/** The single most important thing missing for reliable monitoring, in priority order. */
enum class MonitoringRequirement(val optional: Boolean) {
    PRECISE_LOCATION(optional = false),
    NOTIFICATIONS(optional = false),
    BACKGROUND_LOCATION(optional = true),
    BATTERY(optional = true),
}

/** Null when nothing is missing (or the user dismissed the optional ones). */
internal fun PermissionSnapshot.topRequirement(dismissed: Set<MonitoringRequirement>): MonitoringRequirement? = when {
    !preciseLocation -> MonitoringRequirement.PRECISE_LOCATION
    !notifications -> MonitoringRequirement.NOTIFICATIONS
    !backgroundLocation && MonitoringRequirement.BACKGROUND_LOCATION !in dismissed -> MonitoringRequirement.BACKGROUND_LOCATION
    !unrestrictedBattery && MonitoringRequirement.BATTERY !in dismissed -> MonitoringRequirement.BATTERY
    else -> null
}
