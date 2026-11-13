package com.stevenpg.roundthecorner2.feature.triggers

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.Trigger
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.model.miles
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal object TestData {
    val now: Instant = Instant.parse("2026-11-09T17:42:00Z")

    /** The calendar day of [now] in UTC, so screenshots don't depend on the real date. */
    val today: LocalDate = now.atZone(ZoneOffset.UTC).toLocalDate()

    val home = Place(PlaceId(1), "Home", PlaceIcon.HOME, Coordinates(39.7392, -104.9903), "1437 Bannock St, Denver", Instant.EPOCH)
    val office = Place(PlaceId(2), "Office", PlaceIcon.WORK, Coordinates(39.7508, -104.9966), null, Instant.EPOCH)
    val gym = Place(PlaceId(3), "Climbing gym", PlaceIcon.GYM, Coordinates(39.7, -105.0), null, Instant.EPOCH)

    val alex = Recipient(RecipientId(1), "Alex", RecipientDestination.Sms("+15555550100"), Instant.EPOCH)
    val family = Recipient(
        RecipientId(2), "#family", RecipientDestination.SlackWebhook("https://hooks.slack.com/services/T/B/X"), Instant.EPOCH,
    )

    val homeArrive = Trigger(
        TriggerId(1), home.id, alex.id, TriggerDirection.ARRIVE, 2.miles,
        "Almost home! About {distance} away.", TriggerStatus.ARMED, now, null,
    )
    val officeLeave = Trigger(
        TriggerId(2), office.id, family.id, TriggerDirection.LEAVE, 0.5.miles,
        "Leaving work at {time}", TriggerStatus.FIRED, now.minusSeconds(86_400), now.minusSeconds(3_600),
    )
    val gymArrive = Trigger(
        TriggerId(3), gym.id, alex.id, TriggerDirection.ARRIVE, 0.25.miles,
        "At the gym", TriggerStatus.PAUSED, now.minusSeconds(172_800), null,
    )

    val listDetails = listOf(
        TriggerDetails(homeArrive, home, alex),
        TriggerDetails(gymArrive, gym, alex),
        TriggerDetails(officeLeave, office, family),
    )

    val history = listOf(
        SendAttempt(2, officeLeave.id, now.minusSeconds(3_600), MessageChannel.SLACK, "Leaving work at 4:42 PM", SendStatus.SENT, null),
        SendAttempt(
            1, officeLeave.id, now.minusSeconds(90_000), MessageChannel.SLACK, "Leaving work at 4:58 PM",
            SendStatus.FAILED, "Couldn't reach Slack: UnknownHostException",
        ),
    )
}
