package com.stevenpg.roundthecorner2.core.location

import android.location.Location
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import com.stevenpg.roundthecorner2.core.model.Coordinates
import java.time.Instant

/** android.location.Location -> domain [LocationFix]; null for impossible coordinates. */
internal fun Location.toFix(): LocationFix? {
    val coordinates = Coordinates.orNull(latitude, longitude) ?: return null
    return LocationFix(
        coordinates = coordinates,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
        timestamp = Instant.ofEpochMilli(time),
        speedMetersPerSecond = if (hasSpeed()) speed.toDouble() else null,
    )
}
