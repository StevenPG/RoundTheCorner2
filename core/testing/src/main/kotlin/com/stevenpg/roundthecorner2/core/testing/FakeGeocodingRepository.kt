package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.repository.GeocodingRepository
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import java.io.IOException

/** Scriptable [GeocodingRepository]: set [results] or [failWith], then inspect [queries]. */
class FakeGeocodingRepository(
    var results: List<GeocodedPlace> = emptyList(),
    var failWith: IOException? = null,
) : GeocodingRepository {

    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<GeocodedPlace> {
        queries += query
        failWith?.let { throw it }
        return results
    }
}
