package com.stevenpg.roundthecorner2.core.domain.proximity

import com.stevenpg.roundthecorner2.core.model.Coordinates
import java.time.Instant

/**
 * One position report from the device. Mirrors the parts of
 * `android.location.Location` the domain needs, without depending on Android.
 */
data class LocationFix(
    val coordinates: Coordinates,
    /** 68% confidence radius in meters, as reported by the OS; null if unknown. */
    val accuracyMeters: Double?,
    val timestamp: Instant,
    /** Ground speed in m/s if the OS reported one. */
    val speedMetersPerSecond: Double? = null,
)
