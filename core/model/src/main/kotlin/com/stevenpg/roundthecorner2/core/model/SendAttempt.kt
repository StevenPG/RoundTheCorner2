package com.stevenpg.roundthecorner2.core.model

import java.time.Instant

/** One attempt to deliver a trigger's message - the app's send history. */
data class SendAttempt(
    val id: Long,
    val triggerId: TriggerId,
    val timestamp: Instant,
    val channel: MessageChannel,
    /** The message after placeholders were filled in. */
    val renderedMessage: String,
    val status: SendStatus,
    /** Failure reason for [SendStatus.FAILED]; null otherwise. */
    val error: String?,
)

/** Persisted by name, so only ever append new entries. */
enum class SendStatus {
    SENT,
    FAILED,

    /** Play flavor SMS: a "tap to send" prompt was shown; we can't confirm delivery. */
    AWAITING_USER,
}
