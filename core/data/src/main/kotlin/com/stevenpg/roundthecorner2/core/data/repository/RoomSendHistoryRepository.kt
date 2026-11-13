package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.database.dao.SendAttemptDao
import com.stevenpg.roundthecorner2.core.database.entity.SendAttemptEntity
import com.stevenpg.roundthecorner2.core.database.entity.asExternalModel
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class RoomSendHistoryRepository @Inject constructor(
    private val dao: SendAttemptDao,
    private val clock: Clock,
) : SendHistoryRepository {

    override suspend fun record(
        triggerId: TriggerId,
        channel: MessageChannel,
        renderedMessage: String,
        status: SendStatus,
        error: String?,
    ): Long = dao.insert(
        SendAttemptEntity(
            triggerId = triggerId.value,
            timestamp = clock.instant(),
            channel = channel,
            renderedMessage = renderedMessage,
            status = status,
            error = error,
        ),
    )

    override fun observeForTrigger(id: TriggerId): Flow<List<SendAttempt>> =
        dao.observeForTrigger(id.value).map { it.map(SendAttemptEntity::asExternalModel) }

    override fun observeRecent(limit: Int): Flow<List<SendAttempt>> =
        dao.observeRecent(limit).map { it.map(SendAttemptEntity::asExternalModel) }
}
