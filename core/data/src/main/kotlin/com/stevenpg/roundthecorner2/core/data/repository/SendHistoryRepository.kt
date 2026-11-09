package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerId
import kotlinx.coroutines.flow.Flow

/** Record of every delivery attempt, shown in the trigger's history. */
interface SendHistoryRepository {
    suspend fun record(
        triggerId: TriggerId,
        channel: MessageChannel,
        renderedMessage: String,
        status: SendStatus,
        error: String? = null,
    ): Long

    fun observeForTrigger(id: TriggerId): Flow<List<SendAttempt>>
    fun observeRecent(limit: Int = 50): Flow<List<SendAttempt>>
}
