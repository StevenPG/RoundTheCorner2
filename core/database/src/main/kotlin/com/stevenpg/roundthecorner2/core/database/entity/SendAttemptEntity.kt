package com.stevenpg.roundthecorner2.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerId
import java.time.Instant

/**
 * `send_attempts` table - delivery history. Unlike places/recipients, history
 * belongs to its trigger, so it is deleted along with it (CASCADE).
 */
@Entity(
    tableName = "send_attempts",
    foreignKeys = [
        ForeignKey(
            entity = TriggerEntity::class,
            parentColumns = ["id"],
            childColumns = ["trigger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trigger_id"), Index("timestamp")],
)
data class SendAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trigger_id") val triggerId: Long,
    val timestamp: Instant,
    val channel: MessageChannel,
    @ColumnInfo(name = "rendered_message") val renderedMessage: String,
    val status: SendStatus,
    val error: String?,
)

fun SendAttemptEntity.asExternalModel() = SendAttempt(
    id = id,
    triggerId = TriggerId(triggerId),
    timestamp = timestamp,
    channel = channel,
    renderedMessage = renderedMessage,
    status = status,
    error = error,
)
