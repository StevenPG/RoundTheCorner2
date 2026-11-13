package com.stevenpg.roundthecorner2.core.messaging.webhook

import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

/**
 * Discord channel webhook: `POST {"content": "..."}` -> `204 No Content`.
 * https://discord.com/developers/docs/resources/webhook#execute-webhook
 */
class DiscordWebhookSender @Inject constructor(private val poster: WebhookPoster) : MessageSender {

    override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
        if (destination !is RecipientDestination.DiscordWebhook) {
            return SendResult.Failed(FailureReason.UNKNOWN, "Not a Discord destination")
        }
        val body = buildJsonObject {
            put("content", truncate(message))
            put("username", "Round The Corner")
            // Never let a message ping @everyone, @here, roles or users.
            putJsonObject("allowed_mentions") { putJsonArray("parse") {} }
        }
        return poster.post(destination.url, body, SERVICE)
    }

    companion object {
        const val SERVICE = "Discord"

        /** Discord rejects content over 2,000 characters. */
        const val MAX_LENGTH = 2_000

        fun truncate(text: String): String =
            if (text.length <= MAX_LENGTH) text else text.take(MAX_LENGTH - 1) + "…"
    }
}
