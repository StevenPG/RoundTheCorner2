package com.stevenpg.roundthecorner2.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import java.time.Instant

/**
 * `recipients` table. The destination is flattened to (channel, address) -
 * a phone number for SMS or a webhook URL for Slack/Discord.
 *
 * `address` holds ciphertext (phone numbers and webhook URLs are encrypted with
 * an Android Keystore key), so there is intentionally no mapper to the domain
 * model here: `core:data` owns the cipher and maps recipients with
 * `toDecryptedModel`, the single place that decrypts.
 */
@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val channel: MessageChannel,
    val address: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
