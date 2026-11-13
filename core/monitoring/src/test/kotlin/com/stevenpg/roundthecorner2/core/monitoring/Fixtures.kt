package com.stevenpg.roundthecorner2.core.monitoring

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.model.Trigger
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.model.miles
import com.stevenpg.roundthecorner2.core.monitoring.fire.RetryScheduler
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerEventNotifier
import java.time.Instant

internal object Fixtures {
    val now: Instant = Instant.parse("2026-11-09T17:42:00Z")
    val home = Place(PlaceId(1), "Home", PlaceIcon.HOME, Coordinates(39.7392, -104.9903), null, Instant.EPOCH)
    val alex = Recipient(RecipientId(1), "Alex", RecipientDestination.Sms("+15555550100"), Instant.EPOCH)
    val homeArrive = Trigger(
        TriggerId(1), home.id, alex.id, TriggerDirection.ARRIVE, 2.miles,
        "Almost at {place}! {distance} away", TriggerStatus.ARMED, Instant.EPOCH, null,
    )
    val details = TriggerDetails(homeArrive, home, alex)
}

/** Records every notifier call as a readable string. */
internal class RecordingNotifier : TriggerEventNotifier {
    val events = mutableListOf<String>()
    override fun sent(details: TriggerDetails, message: String) { events += "sent:$message" }
    override fun willRetry(details: TriggerDetails, reason: String) { events += "retry:$reason" }
    override fun failed(details: TriggerDetails, reason: String) { events += "failed:$reason" }
    override fun alreadyHere(details: TriggerDetails) { events += "alreadyHere:${details.trigger.id.value}" }
    override fun cancelAlreadyHere(details: TriggerDetails) { events += "cancelAlreadyHere:${details.trigger.id.value}" }
}

internal class RecordingRetryScheduler : RetryScheduler {
    val scheduled = mutableListOf<Triple<TriggerId, String, MessageChannel>>()
    override fun schedule(triggerId: TriggerId, message: String, channel: MessageChannel) {
        scheduled += Triple(triggerId, message, channel)
    }
}
