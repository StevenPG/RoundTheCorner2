package com.stevenpg.roundthecorner2.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import java.time.Instant

/**
 * `recipients` table. The destination is flattened to (channel, address) -
 * a phone number for SMS or a webhook URL for Slack/Discord.
 *
 * Note: webhook URLs are bearer secrets. The messaging milestone encrypts the
 * `address` column with an Android Keystore key before it is written here.
 */
@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val channel: MessageChannel,
    val address: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

fun RecipientEntity.asExternalModel() = Recipient(
    id = RecipientId(id),
    name = name,
    destination = RecipientDestination.of(channel, address),
    createdAt = createdAt,
)
