package com.stevenpg.roundthecorner2.core.location

import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Source of device positions. Callers must hold ACCESS_FINE_LOCATION; without
 * it the flow fails with [SecurityException].
 */
interface LocationClient {
    /**
     * Continuous high-accuracy fixes, roughly every [interval]. Collect with
     * `flatMapLatest` over a changing interval to adapt the rate; cancelling
     * collection stops the GPS.
     */
    fun updates(interval: Duration): Flow<LocationFix>
}
