package com.stevenpg.roundthecorner2.core.model

import java.time.Instant

/**
 * "When I [direction] within [radius] of [place], send [messageTemplate] to
 * [recipient]." Triggers are one-shot: once fired they stop monitoring until
 * the user re-arms them.
 */
data class Trigger(
    val id: TriggerId,
    val placeId: PlaceId,
    val recipientId: RecipientId,
    val direction: TriggerDirection,
    val radius: Distance,
    /** Message text; may contain [MessageTemplate] placeholders such as `{place}`. */
    val messageTemplate: String,
    val status: TriggerStatus,
    val createdAt: Instant,
    /** When it last sent (null if never). Kept after re-arming, for history. */
    val lastFiredAt: Instant?,
)

@JvmInline
value class TriggerId(val value: Long)

/** Which boundary crossing sends the message. */
enum class TriggerDirection {
    /** Fires when moving from outside the radius to inside it. */
    ARRIVE,

    /** Fires when moving from inside the radius to outside it. */
    LEAVE,
}

/** Lifecycle of a trigger. Persisted by name, so only ever append new entries. */
enum class TriggerStatus {
    /** Being monitored by the background service. */
    ARMED,

    /** Saved but not monitored (paused by the user). */
    PAUSED,

    /** Already sent; waiting to be re-armed. */
    FIRED,
}

/**
 * A trigger joined with the place and recipient it references - what the list
 * screen and the monitoring service actually need.
 */
data class TriggerDetails(
    val trigger: Trigger,
    val place: Place,
    val recipient: Recipient,
)
