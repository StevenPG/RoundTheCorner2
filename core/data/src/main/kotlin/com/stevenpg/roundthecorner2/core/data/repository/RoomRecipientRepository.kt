package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.database.dao.RecipientDao
import com.stevenpg.roundthecorner2.core.database.entity.RecipientEntity
import com.stevenpg.roundthecorner2.core.database.entity.asExternalModel
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

internal class RoomRecipientRepository @Inject constructor(
    private val dao: RecipientDao,
    private val clock: Clock,
) : RecipientRepository {

    override fun observeRecipients(): Flow<List<Recipient>> =
        dao.observeAll().map { list -> list.map(RecipientEntity::asExternalModel) }

    override suspend fun getRecipient(id: RecipientId): Recipient? = dao.getById(id.value)?.asExternalModel()

    override suspend fun createRecipient(draft: RecipientDraft): RecipientId =
        RecipientId(dao.insert(draft.toEntity(id = 0, createdAt = clock.instant())))

    override suspend fun updateRecipient(id: RecipientId, draft: RecipientDraft): Boolean {
        val existing = dao.getById(id.value) ?: return false
        return dao.update(draft.toEntity(id = existing.id, createdAt = existing.createdAt)) > 0
    }

    override suspend fun deleteRecipient(id: RecipientId): DeleteResult {
        val inUse = dao.countTriggersUsing(id.value)
        if (inUse > 0) return DeleteResult.InUse(inUse)
        return if (dao.deleteById(id.value) > 0) DeleteResult.Deleted else DeleteResult.NotFound
    }

    private fun RecipientDraft.toEntity(id: Long, createdAt: Instant) = RecipientEntity(
        id = id,
        name = name.trim(),
        channel = destination.channel,
        address = destination.address.trim(),
        createdAt = createdAt,
    )
}
