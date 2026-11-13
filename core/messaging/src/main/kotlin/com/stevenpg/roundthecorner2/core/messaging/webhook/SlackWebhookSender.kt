package com.stevenpg.roundthecorner2.core.messaging.webhook

import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * Slack Incoming Webhook: `POST {"text": "..."}` -> `200 ok`.
 * https://api.slack.com/messaging/webhooks
 */
class SlackWebhookSender @Inject constructor(private val poster: WebhookPoster) : MessageSender {

    override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
        if (destination !is RecipientDestination.SlackWebhook) {
            return SendResult.Failed(FailureReason.UNKNOWN, "Not a Slack destination")
        }
        val body = buildJsonObject {
            put("text", escapeSlack(message))
            // Plain text, no link unfurling previews.
            put("unfurl_links", false)
            put("unfurl_media", false)
        }
        return poster.post(destination.url, body, SERVICE)
    }

    companion object {
        const val SERVICE = "Slack"

        /**
         * Slack treats `&`, `<`, `>` as markup (e.g. `<!channel>` pings
         * everyone). Escaping them makes user text render literally.
         */
        fun escapeSlack(text: String): String =
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
