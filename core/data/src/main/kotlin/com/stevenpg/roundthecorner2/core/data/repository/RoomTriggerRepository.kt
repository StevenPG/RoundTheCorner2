package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.data.security.SecretCipher
import com.stevenpg.roundthecorner2.core.database.dao.TriggerDao
import com.stevenpg.roundthecorner2.core.database.entity.TriggerEntity
import com.stevenpg.roundthecorner2.core.database.entity.TriggerWithRelations
import com.stevenpg.roundthecorner2.core.database.entity.asExternalModel
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

internal class RoomTriggerRepository @Inject constructor(
    private val dao: TriggerDao,
    private val clock: Clock,
    private val cipher: SecretCipher,
) : TriggerRepository {

    override fun observeTriggers(): Flow<List<TriggerDetails>> =
        dao.observeAllWithRelations().map { it.map { row -> row.toDetails() } }

    // distinctUntilChanged: Room re-emits on *any* write to the table; the
    // monitoring service only cares when the armed set actually changes.
    override fun observeArmedTriggers(): Flow<List<TriggerDetails>> =
        dao.observeByStatusWithRelations(TriggerStatus.ARMED)
            .map { it.map { row -> row.toDetails() } }
            .distinctUntilChanged()

    override fun observeTrigger(id: TriggerId): Flow<TriggerDetails?> =
        dao.observeByIdWithRelations(id.value).map { it?.toDetails() }

    override suspend fun createTrigger(draft: TriggerDraft, armed: Boolean): TriggerId {
        val entity = draft.toEntity(
            id = 0,
            status = if (armed) TriggerStatus.ARMED else TriggerStatus.PAUSED,
            createdAt = clock.instant(),
            lastFiredAt = null,
        )
        return TriggerId(dao.insert(entity))
    }

    override suspend fun updateTrigger(id: TriggerId, draft: TriggerDraft): Boolean {
        val existing = dao.getById(id.value) ?: return false
        return dao.update(draft.toEntity(existing.id, existing.status, existing.createdAt, existing.lastFiredAt)) > 0
    }

    override suspend fun arm(id: TriggerId): Boolean = dao.updateStatus(id.value, TriggerStatus.ARMED) > 0

    override suspend fun pause(id: TriggerId): Boolean = dao.updateStatus(id.value, TriggerStatus.PAUSED) > 0

    override suspend fun markFired(id: TriggerId): Boolean = dao.markFiredIfArmed(id.value, clock.instant()) > 0

    override suspend fun deleteTrigger(id: TriggerId): Boolean = dao.deleteById(id.value) > 0

    // The recipient comes from a Room @Relation, so it must be decrypted here too;
    // see toDecryptedModel for why core:database has no mapper of its own.
    private fun TriggerWithRelations.toDetails() = TriggerDetails(
        trigger = trigger.asExternalModel(),
        place = place.asExternalModel(),
        recipient = recipient.toDecryptedModel(cipher),
    )

    private fun TriggerDraft.toEntity(id: Long, status: TriggerStatus, createdAt: Instant, lastFiredAt: Instant?) =
        TriggerEntity(
            id = id,
            placeId = placeId.value,
            recipientId = recipientId.value,
            direction = direction,
            radiusMeters = radius.meters,
            messageTemplate = messageTemplate.trim(),
            status = status,
            createdAt = createdAt,
            lastFiredAt = lastFiredAt,
        )
}
