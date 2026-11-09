package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.messaging.MessageDispatcher
import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.RecipientDestination

/** Records every send and returns [result]. */
class FakeMessageSender(var result: SendResult = SendResult.Sent) : MessageSender {
    data class Call(val destination: RecipientDestination, val message: String, val interactive: Boolean)

    val calls = mutableListOf<Call>()

    override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
        calls += Call(destination, message, interactive)
        return result
    }
}

/** A real [MessageDispatcher] whose every channel goes to [sender]. */
fun fakeDispatcher(sender: FakeMessageSender) =
    MessageDispatcher(MessageChannel.entries.associateWith { sender })
