package com.stevenpg.roundthecorner2.core.messaging

import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageDispatcherTest {

    private class Recording(private val result: SendResult) : MessageSender {
        val calls = mutableListOf<Triple<RecipientDestination, String, Boolean>>()
        override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
            calls += Triple(destination, message, interactive)
            return result
        }
    }

    private val sms = Recording(SendResult.AwaitingUser)
    private val slack = Recording(SendResult.Sent)
    private val dispatcher = MessageDispatcher(mapOf(MessageChannel.SMS to sms, MessageChannel.SLACK to slack))

    @Test
    fun `routes by channel and passes interactivity through`() = runBlocking {
        assertEquals(SendResult.AwaitingUser, dispatcher.send(RecipientDestination.Sms("555"), "hi", interactive = true))
        assertEquals(SendResult.Sent, dispatcher.send(RecipientDestination.SlackWebhook("https://x"), "yo"))
        assertEquals(1, sms.calls.size)
        assertEquals(true, sms.calls.single().third)
        assertEquals("yo", slack.calls.single().second)
    }

    @Test
    fun `missing sender or blank address fails without throwing`() = runBlocking {
        val noDiscord = dispatcher.send(RecipientDestination.DiscordWebhook("https://x"), "hi") as SendResult.Failed
        assertEquals(FailureReason.UNKNOWN, noDiscord.reason)

        val blank = dispatcher.send(RecipientDestination.Sms(""), "hi") as SendResult.Failed
        assertEquals(FailureReason.DESTINATION_INVALID, blank.reason)
        assertEquals(0, sms.calls.size)
    }
}
