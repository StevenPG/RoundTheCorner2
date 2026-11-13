package com.stevenpg.roundthecorner2.core.messaging.webhook

import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Real HTTP round-trips against a local mock server. */
class WebhookSendersTest {

    private val server = MockWebServer()
    private val poster = WebhookPoster(OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
    private val slack = SlackWebhookSender(poster)
    private val discord = DiscordWebhookSender(poster)

    @Before fun setUp() = server.start()

    @After fun tearDown() = server.close()

    private fun slackUrl() = RecipientDestination.SlackWebhook(server.url("/services/T/B/X").toString())
    private fun discordUrl() = RecipientDestination.DiscordWebhook(server.url("/api/webhooks/1/abc").toString())
    private fun respond(code: Int, body: String = "") = server.enqueue(MockResponse.Builder().code(code).body(body).build())
    private fun lastJson() = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    @Test
    fun `slack posts text and escapes markup so it can't ping the channel`() = runBlocking {
        respond(200, "ok")
        val result = slack.send(slackUrl(), "Almost home <!channel> & <@U123>")
        assertEquals(SendResult.Sent, result)
        val json = lastJson()
        assertEquals("Almost home &lt;!channel&gt; &amp; &lt;@U123&gt;", json["text"]!!.jsonPrimitive.content)
        assertEquals("false", json["unfurl_links"]!!.jsonPrimitive.content)
    }

    @Test
    fun `discord posts content with mentions disabled`() = runBlocking {
        respond(204)
        assertEquals(SendResult.Sent, discord.send(discordUrl(), "Leaving work @everyone"))
        val json = lastJson()
        assertEquals("Leaving work @everyone", json["content"]!!.jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), json["allowed_mentions"]!!.jsonObject["parse"]!!.jsonArray)
    }

    @Test
    fun `discord truncates to its 2000 character limit`() = runBlocking {
        respond(204)
        discord.send(discordUrl(), "x".repeat(2_500))
        val content = lastJson()["content"]!!.jsonPrimitive.content
        assertEquals(2_000, content.length)
        assertTrue(content.endsWith("…"))
    }

    @Test
    fun `revoked webhook is a permanent destination error with the service's reason`() = runBlocking {
        respond(404, "no_service")
        val result = slack.send(slackUrl(), "hi") as SendResult.Failed
        assertEquals(FailureReason.DESTINATION_INVALID, result.reason)
        assertFalse(result.retryable)
        assertTrue(result.detail.contains("no_service"))
    }

    @Test
    fun `rate limits and server errors are retryable`() = runBlocking {
        respond(429)
        respond(503)
        assertEquals(FailureReason.RATE_LIMITED, (discord.send(discordUrl(), "a") as SendResult.Failed).reason)
        val second = discord.send(discordUrl(), "b") as SendResult.Failed
        assertEquals(FailureReason.SERVER_ERROR, second.reason)
        assertTrue(second.retryable)
    }

    @Test
    fun `other 4xx means the message itself was rejected`() = runBlocking {
        respond(400, "invalid_payload")
        assertEquals(FailureReason.REJECTED, (slack.send(slackUrl(), "a") as SendResult.Failed).reason)
    }

    @Test
    fun `unreachable host is a retryable network failure that never leaks the URL`() = runBlocking {
        val url = server.url("/services/T/B/SECRET").toString()
        server.close()
        val result = slack.send(RecipientDestination.SlackWebhook(url), "a") as SendResult.Failed
        assertEquals(FailureReason.NETWORK, result.reason)
        assertTrue(result.retryable)
        assertFalse(result.detail.contains("SECRET"))
    }

    @Test
    fun `malformed URL fails cleanly`() = runBlocking {
        val result = slack.send(RecipientDestination.SlackWebhook("not a url"), "a") as SendResult.Failed
        assertEquals(FailureReason.DESTINATION_INVALID, result.reason)
    }
}
