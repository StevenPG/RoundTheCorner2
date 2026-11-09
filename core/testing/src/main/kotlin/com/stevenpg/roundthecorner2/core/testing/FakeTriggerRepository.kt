package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.repository.TriggerDraft
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.model.Trigger
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * In-memory [TriggerRepository] that joins against fake place/recipient
 * repositories, like the real Room query does.
 */
class FakeTriggerRepository(
    private val places: FakePlaceRepository,
    private val recipients: FakeRecipientRepository,
    initial: List<Trigger> = emptyList(),
    private val now: () -> Instant = { Instant.parse("2026-11-09T17:42:00Z") },
) : TriggerRepository {

    private val triggers = MutableStateFlow(initial.associateBy { it.id })
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0) + 1

    private val details: Flow<List<TriggerDetails>> =
        combine(triggers, places.observePlaces(), recipients.observeRecipients()) { ts, ps, rs ->
            val placeById = ps.associateBy { it.id }
            val recipientById = rs.associateBy { it.id }
            ts.values.mapNotNull { t ->
                val p = placeById[t.placeId] ?: return@mapNotNull null
                val r = recipientById[t.recipientId] ?: return@mapNotNull null
                TriggerDetails(t, p, r)
            }.sortedWith(compareBy<TriggerDetails> { it.trigger.status.ordinal }.thenByDescending { it.trigger.createdAt })
        }

    override fun observeTriggers(): Flow<List<TriggerDetails>> = details

    override fun observeArmedTriggers(): Flow<List<TriggerDetails>> =
        details.map { list -> list.filter { it.trigger.status == TriggerStatus.ARMED } }

    override fun observeTrigger(id: TriggerId): Flow<TriggerDetails?> = details.map { list -> list.find { it.trigger.id == id } }

    suspend fun get(id: TriggerId): Trigger? = triggers.value[id]

    override suspend fun createTrigger(draft: TriggerDraft, armed: Boolean): TriggerId {
        val id = TriggerId(nextId++)
        val trigger = Trigger(
            id, draft.placeId, draft.recipientId, draft.direction, draft.radius, draft.messageTemplate.trim(),
            if (armed) TriggerStatus.ARMED else TriggerStatus.PAUSED, now(), null,
        )
        triggers.update { it + (id to trigger) }
        return id
    }

    override suspend fun updateTrigger(id: TriggerId, draft: TriggerDraft): Boolean {
        val t = triggers.value[id] ?: return false
        triggers.update {
            it + (
                id to t.copy(
                    placeId = draft.placeId, recipientId = draft.recipientId, direction = draft.direction,
                    radius = draft.radius, messageTemplate = draft.messageTemplate.trim(),
                )
                )
        }
        return true
    }

    private fun setStatus(id: TriggerId, status: TriggerStatus): Boolean {
        val t = triggers.value[id] ?: return false
        triggers.update { it + (id to t.copy(status = status)) }
        return true
    }

    override suspend fun arm(id: TriggerId) = setStatus(id, TriggerStatus.ARMED)

    override suspend fun pause(id: TriggerId) = setStatus(id, TriggerStatus.PAUSED)

    override suspend fun markFired(id: TriggerId): Boolean {
        val t = triggers.value[id]?.takeIf { it.status == TriggerStatus.ARMED } ?: return false
        triggers.update { it + (id to t.copy(status = TriggerStatus.FIRED, lastFiredAt = now())) }
        return true
    }

    override suspend fun deleteTrigger(id: TriggerId): Boolean {
        if (triggers.value[id] == null) return false
        triggers.update { it - id }
        return true
    }
}
