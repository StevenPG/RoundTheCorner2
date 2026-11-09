package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.PlaceDraft
import com.stevenpg.roundthecorner2.core.data.repository.PlaceRepository
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * In-memory [PlaceRepository]. [triggerUsage] lets a test pretend a place is
 * referenced by triggers, to exercise the "in use" path.
 */
class FakePlaceRepository(initial: List<Place> = emptyList()) : PlaceRepository {

    private val places = MutableStateFlow(initial.associateBy { it.id })
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0) + 1
    val triggerUsage = mutableMapOf<PlaceId, Int>()

    override fun observePlaces(): Flow<List<Place>> = places.map { it.values.sortedBy { p -> p.name.lowercase() } }

    override fun observePlace(id: PlaceId): Flow<Place?> = places.map { it[id] }

    override suspend fun getPlace(id: PlaceId): Place? = places.value[id]

    override suspend fun createPlace(draft: PlaceDraft): PlaceId {
        val id = PlaceId(nextId++)
        places.update { it + (id to draft.toPlace(id, Instant.EPOCH)) }
        return id
    }

    override suspend fun updatePlace(id: PlaceId, draft: PlaceDraft): Boolean {
        val existing = places.value[id] ?: return false
        places.update { it + (id to draft.toPlace(id, existing.createdAt)) }
        return true
    }

    override suspend fun deletePlace(id: PlaceId): DeleteResult {
        triggerUsage[id]?.takeIf { it > 0 }?.let { return DeleteResult.InUse(it) }
        if (places.value[id] == null) return DeleteResult.NotFound
        places.update { it - id }
        return DeleteResult.Deleted
    }

    private fun PlaceDraft.toPlace(id: PlaceId, createdAt: Instant) =
        Place(id, name.trim(), icon, coordinates, address?.takeIf { it.isNotBlank() }, createdAt)
}
