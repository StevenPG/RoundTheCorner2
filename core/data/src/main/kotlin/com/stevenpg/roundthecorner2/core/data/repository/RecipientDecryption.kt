package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.data.security.SecretCipher
import com.stevenpg.roundthecorner2.core.data.security.SecretUnreadableException
import com.stevenpg.roundthecorner2.core.database.entity.RecipientEntity
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId

/**
 * The only way to turn a stored [RecipientEntity] into a domain [Recipient].
 *
 * The `address` column holds ciphertext, so every read path - the recipients
 * list *and* triggers, which load their recipient through a Room `@Relation` -
 * must come through here. `core:database` deliberately has no mapper for
 * recipients: an earlier one skipped decryption, and fired triggers sent to the
 * ciphertext instead of the real number or webhook.
 *
 * Unreadable secrets (a backup restored onto a new phone, whose Keystore lacks
 * the key) degrade to an empty address plus [Recipient.needsReentry] instead of
 * crashing, and the dispatcher refuses to send to them.
 */
internal fun RecipientEntity.toDecryptedModel(cipher: SecretCipher): Recipient {
    val plain = try {
        cipher.decrypt(address)
    } catch (e: SecretUnreadableException) {
        null
    }
    return Recipient(
        id = RecipientId(id),
        name = name,
        destination = RecipientDestination.of(channel, plain.orEmpty()),
        createdAt = createdAt,
        needsReentry = plain == null,
    )
}
