package com.stevenpg.roundthecorner2.core.messaging

import com.stevenpg.roundthecorner2.core.model.RecipientDestination

/**
 * Delivers text to one kind of destination. Implementations never throw for
 * expected failures (network down, webhook revoked...) - they return
 * [SendResult.Failed] so callers can record and maybe retry.
 */
interface MessageSender {
    /**
     * @param interactive true when the user is looking at the app right now
     *   (e.g. "Send test message"). Senders that need the user's help may then
     *   open UI directly instead of posting a notification.
     */
    suspend fun send(
        destination: RecipientDestination,
        message: String,
        interactive: Boolean = false,
    ): SendResult
}
