package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.model.GeocodedPlace

/** Address -> coordinates lookup. Optional: places can always be made from coordinates alone. */
interface GeocodingRepository {
    /**
     * @return matches, best first (possibly empty).
     * @throws java.io.IOException when the service can't be reached.
     */
    suspend fun search(query: String): List<GeocodedPlace>
}
