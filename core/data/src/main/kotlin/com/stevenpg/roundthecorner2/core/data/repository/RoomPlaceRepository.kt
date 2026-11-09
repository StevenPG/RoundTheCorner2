package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.database.dao.PlaceDao
import com.stevenpg.roundthecorner2.core.database.entity.PlaceEntity
import com.stevenpg.roundthecorner2.core.database.entity.asExternalModel
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class RoomPlaceRepository @Inject constructor(
    private val dao: PlaceDao,
    private val clock: Clock,
) : PlaceRepository {

    override fun observePlaces(): Flow<List<Place>> =
        dao.observeAll().map { list -> list.map(PlaceEntity::asExternalModel) }

    override fun observePlace(id: PlaceId): Flow<Place?> =
        dao.observeById(id.value).map { it?.asExternalModel() }

    override suspend fun getPlace(id: PlaceId): Place? = dao.getById(id.value)?.asExternalModel()

    override suspend fun createPlace(draft: PlaceDraft): PlaceId =
        PlaceId(dao.insert(draft.toEntity(id = 0, createdAt = clock.instant())))

    override suspend fun updatePlace(id: PlaceId, draft: PlaceDraft): Boolean {
        val existing = dao.getById(id.value) ?: return false
        return dao.update(draft.toEntity(id = existing.id, createdAt = existing.createdAt)) > 0
    }

    override suspend fun deletePlace(id: PlaceId): DeleteResult {
        val inUse = dao.countTriggersUsing(id.value)
        if (inUse > 0) return DeleteResult.InUse(inUse)
        return if (dao.deleteById(id.value) > 0) DeleteResult.Deleted else DeleteResult.NotFound
    }

    private fun PlaceDraft.toEntity(id: Long, createdAt: java.time.Instant) = PlaceEntity(
        id = id,
        name = name.trim(),
        icon = icon,
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        address = address?.trim()?.takeIf { it.isNotEmpty() },
        createdAt = createdAt,
    )
}
