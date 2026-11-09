package com.stevenpg.roundthecorner2.core.monitoring

import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.model.miles
import com.stevenpg.roundthecorner2.core.monitoring.fire.FireOutcome
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerFirer
import com.stevenpg.roundthecorner2.core.testing.FakeMessageSender
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.FakeSendHistoryRepository
import com.stevenpg.roundthecorner2.core.testing.FakeTriggerRepository
import com.stevenpg.roundthecorner2.core.testing.FakeUserPreferencesRepository
import com.stevenpg.roundthecorner2.core.testing.fakeDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneOffset

class TriggerFirerTest {

    private val places = FakePlaceRepository(listOf(Fixtures.home))
    private val recipients = FakeRecipientRepository(listOf(Fixtures.alex))
    private val triggers = FakeTriggerRepository(places, recipients, listOf(Fixtures.homeArrive))
    private val history = FakeSendHistoryRepository()
    private val sender = FakeMessageSender()
    private val notifier = RecordingNotifier()
    private val retries = RecordingRetryScheduler()

    private val firer = TriggerFirer(
        triggerRepository = triggers,
        dispatcher = fakeDispatcher(sender),
        history = history,
        unitSystemProvider = UnitSystemProvider(FakeUserPreferencesRepository()) { ResolvedUnitSystem.IMPERIAL },
        notifier = notifier,
        retryScheduler = retries,
        clock = Clock.fixed(Fixtures.now, ZoneOffset.UTC),
    )

    @Test
    fun `fires exactly once with live values and records the send`() = runTest {
        assertEquals(FireOutcome.SENT, firer.fire(Fixtures.details, 1.8.miles))
        assertEquals(FireOutcome.ALREADY_HANDLED, firer.fire(Fixtures.details, 1.7.miles))

        val call = sender.calls.single()
        assertEquals("Almost at Home! 1.8 mi away", call.message)
        assertEquals(false, call.interactive)
        assertEquals(TriggerStatus.FIRED, triggers.get(Fixtures.homeArrive.id)!!.status)
        assertEquals(SendStatus.SENT, history.observeRecent().first().single().status)
        assertTrue(notifier.events.contains("sent:Almost at Home! 1.8 mi away"))
        assertTrue("clears any 'already here' prompt", notifier.events.contains("cancelAlreadyHere:1"))
    }

    @Test
    fun `without a distance the radius is used`() = runTest {
        firer.fire(Fixtures.details, distance = null)
        assertEquals("Almost at Home! 2.0 mi away", sender.calls.single().message)
    }

    @Test
    fun `transient failures schedule a durable retry`() = runTest {
        sender.result = SendResult.Failed(FailureReason.NETWORK, "Couldn't reach Slack")
        assertEquals(FireOutcome.RETRY_SCHEDULED, firer.fire(Fixtures.details, 1.miles))
        assertEquals(Fixtures.homeArrive.id, retries.scheduled.single().first)
        assertEquals(SendStatus.FAILED, history.observeRecent().first().single().status)
        assertTrue(notifier.events.any { it.startsWith("retry:") })
    }

    @Test
    fun `permanent failures notify without retrying`() = runTest {
        sender.result = SendResult.Failed(FailureReason.DESTINATION_INVALID, "Webhook deleted")
        assertEquals(FireOutcome.FAILED, firer.fire(Fixtures.details, 1.miles))
        assertTrue(retries.scheduled.isEmpty())
        assertTrue(notifier.events.contains("failed:Webhook deleted"))
    }

    @Test
    fun `tap-to-send is recorded as awaiting the user`() = runTest {
        sender.result = SendResult.AwaitingUser
        assertEquals(FireOutcome.AWAITING_USER, firer.fire(Fixtures.details, 1.miles))
        assertEquals(SendStatus.AWAITING_USER, history.observeRecent().first().single().status)
    }

    @Test
    fun `paused triggers never send`() = runTest {
        triggers.pause(Fixtures.homeArrive.id)
        assertEquals(FireOutcome.ALREADY_HANDLED, firer.fire(Fixtures.details, 1.miles))
        assertTrue(sender.calls.isEmpty())
    }
}
