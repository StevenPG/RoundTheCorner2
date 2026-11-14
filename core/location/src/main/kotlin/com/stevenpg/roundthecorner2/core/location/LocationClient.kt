package com.stevenpg.roundthecorner2.core.location

import com.stevenpg.roundthecorner2.core.domain.polling.LocationAccuracy
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Source of device positions. Callers must hold ACCESS_FINE_LOCATION; without
 * it the flow fails with [SecurityException].
 */
interface LocationClient {
    /**
     * Continuous fixes, roughly every [interval], at the given [accuracy]
     * ([LocationAccuracy.LOW_POWER] avoids GPS). Collect with `flatMapLatest`
     * over a changing request to adapt; cancelling collection stops updates.
     */
    fun updates(interval: Duration, accuracy: LocationAccuracy = LocationAccuracy.PRECISE): Flow<LocationFix>
}
