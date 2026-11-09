package com.stevenpg.roundthecorner2.core.model

import java.time.Instant

/**
 * Someone (or some channel) a trigger can message, saved for reuse - e.g.
 * "Alex (SMS)" or "#family Slack".
 */
data class Recipient(
    val id: RecipientId,
    /** User-facing label, e.g. "Alex" or "#family". */
    val name: String,
    val destination: RecipientDestination,
    val createdAt: Instant,
)

@JvmInline
value class RecipientId(val value: Long)

/** Delivery channel. Persisted by name, so only ever append new entries. */
enum class MessageChannel { SMS, SLACK, DISCORD }

/**
 * Where a message goes. A sealed hierarchy means every `when` over it is
 * checked by the compiler, so adding a channel (Telegram, ntfy, ...) flags
 * every place that must handle it.
 */
sealed interface RecipientDestination {
    val channel: MessageChannel

    /** The single string that identifies the destination (phone number or URL). */
    val address: String

    data class Sms(val phoneNumber: String) : RecipientDestination {
        override val channel get() = MessageChannel.SMS
        override val address get() = phoneNumber
    }

    /** Slack "Incoming Webhook" URL (https://hooks.slack.com/services/...). Secret. */
    data class SlackWebhook(val url: String) : RecipientDestination {
        override val channel get() = MessageChannel.SLACK
        override val address get() = url
        override fun toString() = "SlackWebhook(url=<redacted>)" // Never log secrets.
    }

    /** Discord channel webhook URL (https://discord.com/api/webhooks/...). Secret. */
    data class DiscordWebhook(val url: String) : RecipientDestination {
        override val channel get() = MessageChannel.DISCORD
        override val address get() = url
        override fun toString() = "DiscordWebhook(url=<redacted>)"
    }

    companion object {
        /** Rebuilds a destination from its persisted (channel, address) pair. */
        fun of(channel: MessageChannel, address: String): RecipientDestination = when (channel) {
            MessageChannel.SMS -> Sms(address)
            MessageChannel.SLACK -> SlackWebhook(address)
            MessageChannel.DISCORD -> DiscordWebhook(address)
        }
    }
}
