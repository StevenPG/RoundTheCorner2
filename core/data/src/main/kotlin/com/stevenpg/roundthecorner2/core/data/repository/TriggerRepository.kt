package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.meters
import kotlinx.coroutines.flow.Flow

/** Triggers and their lifecycle (ARMED -> FIRED -> re-armed, or PAUSED). */
interface TriggerRepository {
    /** All triggers with place + recipient; armed first, then newest. */
    fun observeTriggers(): Flow<List<TriggerDetails>>

    /** Only ARMED triggers - the monitoring service's work list. */
    fun observeArmedTriggers(): Flow<List<TriggerDetails>>

    fun observeTrigger(id: TriggerId): Flow<TriggerDetails?>

    suspend fun createTrigger(draft: TriggerDraft, armed: Boolean = true): TriggerId

    /** Edits the trigger's settings; its status is left unchanged. */
    suspend fun updateTrigger(id: TriggerId, draft: TriggerDraft): Boolean

    /** Start (or restart, after firing) monitoring. */
    suspend fun arm(id: TriggerId): Boolean

    /** Stop monitoring without deleting. */
    suspend fun pause(id: TriggerId): Boolean

    /**
     * Atomically ARMED -> FIRED. Returns true only for the single caller that
     * won, so a message is never sent twice even if location updates race.
     */
    suspend fun markFired(id: TriggerId): Boolean

    suspend fun deleteTrigger(id: TriggerId): Boolean
}

/** User-editable fields of a trigger. */
data class TriggerDraft(
    val placeId: PlaceId,
    val recipientId: RecipientId,
    val direction: TriggerDirection,
    val radius: Distance,
    val messageTemplate: String,
) {
    init {
        require(radius in MIN_RADIUS..MAX_RADIUS) { "Radius must be within $MIN_RADIUS..$MAX_RADIUS" }
        require(messageTemplate.isNotBlank()) { "Message must not be blank" }
        require(messageTemplate.length <= MAX_MESSAGE_LENGTH) { "Message too long" }
    }

    companion object {
        /** Below ~25 m, consumer GPS noise makes the trigger unreliable. */
        val MIN_RADIUS = 25.meters

        /** 1,000 km - far beyond any sensible use, but stops typos like 5,000,000. */
        val MAX_RADIUS = 1_000_000.meters

        const val MAX_MESSAGE_LENGTH = 1_000
    }
}
