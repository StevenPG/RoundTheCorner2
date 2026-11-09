package com.stevenpg.roundthecorner2.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.model.Trigger
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import java.time.Instant

/**
 * `triggers` table.
 *
 * Foreign keys use RESTRICT: a place or recipient that is still used by a
 * trigger cannot be deleted. The repository checks first and reports which
 * triggers are in the way, so the user is never surprised by a trigger
 * silently disappearing (as CASCADE would do).
 */
@Entity(
    tableName = "triggers",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RecipientEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipient_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("place_id"), Index("recipient_id"), Index("status")],
)
data class TriggerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "place_id") val placeId: Long,
    @ColumnInfo(name = "recipient_id") val recipientId: Long,
    val direction: TriggerDirection,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Double,
    @ColumnInfo(name = "message_template") val messageTemplate: String,
    val status: TriggerStatus,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "last_fired_at") val lastFiredAt: Instant?,
)

fun TriggerEntity.asExternalModel() = Trigger(
    id = TriggerId(id),
    placeId = PlaceId(placeId),
    recipientId = RecipientId(recipientId),
    direction = direction,
    radius = Distance(radiusMeters),
    messageTemplate = messageTemplate,
    status = status,
    createdAt = createdAt,
    lastFiredAt = lastFiredAt,
)

/**
 * A trigger with its place and recipient loaded in one transaction. The FKs
 * guarantee both exist, hence the non-null properties.
 */
data class TriggerWithRelations(
    @Embedded val trigger: TriggerEntity,
    @Relation(parentColumn = "place_id", entityColumn = "id")
    val place: PlaceEntity,
    @Relation(parentColumn = "recipient_id", entityColumn = "id")
    val recipient: RecipientEntity,
)

fun TriggerWithRelations.asExternalModel() = TriggerDetails(
    trigger = trigger.asExternalModel(),
    place = place.asExternalModel(),
    recipient = recipient.asExternalModel(),
)
