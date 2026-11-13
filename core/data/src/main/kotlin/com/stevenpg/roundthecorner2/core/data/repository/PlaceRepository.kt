package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.PlaceId
import kotlinx.coroutines.flow.Flow

/** Saved places. Features depend on this interface, never on Room directly. */
interface PlaceRepository {
    /** All places, alphabetical. */
    fun observePlaces(): Flow<List<Place>>
    fun observePlace(id: PlaceId): Flow<Place?>
    suspend fun getPlace(id: PlaceId): Place?
    suspend fun createPlace(draft: PlaceDraft): PlaceId

    /** @return false if the place no longer exists. */
    suspend fun updatePlace(id: PlaceId, draft: PlaceDraft): Boolean
    suspend fun deletePlace(id: PlaceId): DeleteResult
}

/**
 * User-editable fields of a place. Only [coordinates] are needed to locate
 * it; [address] is optional and purely informational.
 */
data class PlaceDraft(
    val name: String,
    val coordinates: Coordinates,
    val icon: PlaceIcon = PlaceIcon.PIN,
    val address: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Place name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Place name too long" }
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
