package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.repository.SendHistoryRepository
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeSendHistoryRepository(initial: List<SendAttempt> = emptyList()) : SendHistoryRepository {
    private val attempts = MutableStateFlow(initial)

    override suspend fun record(
        triggerId: TriggerId,
        channel: MessageChannel,
        renderedMessage: String,
        status: SendStatus,
        error: String?,
    ): Long {
        val id = attempts.value.size + 1L
        attempts.update { it + SendAttempt(id, triggerId, Instant.EPOCH, channel, renderedMessage, status, error) }
        return id
    }

    override fun observeForTrigger(id: TriggerId): Flow<List<SendAttempt>> =
        attempts.map { list -> list.filter { it.triggerId == id }.sortedByDescending { it.timestamp } }

    override fun observeRecent(limit: Int): Flow<List<SendAttempt>> =
        attempts.map { it.sortedByDescending { a -> a.timestamp }.take(limit) }
}
