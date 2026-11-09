package com.stevenpg.roundthecorner2.core.messaging

import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for sending: picks the [MessageSender] registered for the
 * destination's channel. Senders are contributed with Hilt map multibinding
 * (`@IntoMap @MessageChannelKey(...)`), so adding a channel never edits this
 * class - similar to collecting all beans of a type in Spring.
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val senders: Map<MessageChannel, @JvmSuppressWildcards MessageSender>,
) {
    suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean = false): SendResult {
        val sender = senders[destination.channel]
            ?: return SendResult.Failed(FailureReason.UNKNOWN, "No sender registered for ${destination.channel}")
        if (destination.address.isBlank()) {
            // Happens if stored details could not be decrypted (e.g. after a device restore).
            return SendResult.Failed(FailureReason.DESTINATION_INVALID, "Recipient details are missing; edit the recipient")
        }
        return sender.send(destination, message, interactive)
    }
}
