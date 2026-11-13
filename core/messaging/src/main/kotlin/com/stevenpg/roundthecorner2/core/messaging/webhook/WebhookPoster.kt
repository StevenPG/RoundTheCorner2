package com.stevenpg.roundthecorner2.core.messaging.webhook

import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

/**
 * POSTs a JSON body to a webhook URL and maps the HTTP outcome to a
 * [SendResult]. Shared by the Slack and Discord senders.
 *
 * Webhook URLs are bearer secrets: they're never included in [SendResult]
 * details or logs.
 */
class WebhookPoster @Inject constructor(private val httpClient: OkHttpClient) {

    suspend fun post(url: String, body: JsonObject, serviceName: String): SendResult = withContext(Dispatchers.IO) {
        val request = try {
            Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(JSON))
                .build()
        } catch (e: IllegalArgumentException) {
            return@withContext SendResult.Failed(FailureReason.DESTINATION_INVALID, "$serviceName webhook URL is malformed")
        }

        try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                when {
                    response.isSuccessful -> SendResult.Sent
                    code == 429 -> SendResult.Failed(FailureReason.RATE_LIMITED, "$serviceName is rate limiting (HTTP 429)")
                    code in 500..599 -> SendResult.Failed(FailureReason.SERVER_ERROR, "$serviceName error (HTTP $code)")
                    // Deleted, revoked or wrong webhook - the user must fix the recipient.
                    code == 401 || code == 403 || code == 404 || code == 410 -> SendResult.Failed(
                        FailureReason.DESTINATION_INVALID,
                        "$serviceName rejected the webhook (HTTP $code${response.shortBody()}). It may have been deleted.",
                    )
                    else -> SendResult.Failed(FailureReason.REJECTED, "$serviceName rejected the message (HTTP $code${response.shortBody()})")
                }
            }
        } catch (e: IOException) {
            SendResult.Failed(FailureReason.NETWORK, "Couldn't reach $serviceName: ${e.javaClass.simpleName}")
        }
    }

    /** Slack puts the error code in the body ("invalid_token", "no_service"); include a short excerpt. */
    private fun okhttp3.Response.shortBody(): String {
        val text = runCatching { peekBody(MAX_ERROR_BODY.toLong()).string().trim() }.getOrDefault("")
        return if (text.isEmpty() || text.startsWith("<")) "" else ": ${text.take(MAX_ERROR_BODY)}"
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val MAX_ERROR_BODY = 120
    }
}
