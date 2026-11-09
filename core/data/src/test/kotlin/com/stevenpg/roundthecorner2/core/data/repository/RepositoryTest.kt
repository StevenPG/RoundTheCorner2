package com.stevenpg.roundthecorner2.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.stevenpg.roundthecorner2.core.database.RoundTheCornerDatabase
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.model.meters
import com.stevenpg.roundthecorner2.core.model.miles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Repositories wired to a real in-memory Room database and a fixed clock. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryTest {

    private val now = Instant.parse("2026-11-09T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        RoundTheCornerDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val places = RoomPlaceRepository(db.placeDao(), clock)
    private val recipients = RoomRecipientRepository(db.recipientDao(), clock)
    private val triggers = RoomTriggerRepository(db.triggerDao(), clock)
    private val history = RoomSendHistoryRepository(db.sendAttemptDao(), clock)

    @After
    fun tearDown() = db.close()

    private val home = PlaceDraft("  Home ", Coordinates(40.0, -105.0), PlaceIcon.HOME, address = "  ")
    private val alex = RecipientDraft("Alex", RecipientDestination.Sms("+1 555 555 0100"))

    private suspend fun newTrigger(): com.stevenpg.roundthecorner2.core.model.TriggerId {
        val placeId = places.createPlace(home)
        val recipientId = recipients.createRecipient(alex)
        return triggers.createTrigger(
            TriggerDraft(placeId, recipientId, TriggerDirection.ARRIVE, 2.miles, "Almost at {place}"),
        )
    }

    // --- Places --------------------------------------------------------------

    @Test
    fun `coordinate-only place round-trips and input is tidied`() = runTest {
        val id = places.createPlace(home)
        val place = places.getPlace(id)!!
        assertEquals("Home", place.name)
        assertNull("Blank address becomes null", place.address)
        assertEquals(Coordinates(40.0, -105.0), place.coordinates)
        assertEquals(now, place.createdAt)
    }

    @Test
    fun `update keeps the creation time`() = runTest {
        val id = places.createPlace(home)
        val later = RoomPlaceRepository(db.placeDao(), Clock.fixed(now.plusSeconds(3600), ZoneOffset.UTC))
        assertTrue(later.updatePlace(id, home.copy(name = "Casa")))
        val place = places.getPlace(id)!!
        assertEquals("Casa", place.name)
        assertEquals(now, place.createdAt)
    }

    @Test
    fun `place in use reports how many triggers block deletion`() = runTest {
        newTrigger()
        val placeId = places.observePlaces().first().single().id
        assertEquals(DeleteResult.InUse(1), places.deletePlace(placeId))
    }

    @Test
    fun `unused place deletes, missing place is NotFound`() = runTest {
        val id = places.createPlace(home)
        assertEquals(DeleteResult.Deleted, places.deletePlace(id))
        assertEquals(DeleteResult.NotFound, places.deletePlace(id))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank place names are rejected`() {
        PlaceDraft(" ", Coordinates(0.0, 0.0))
    }

    // --- Recipients ----------------------------------------------------------

    @Test
    fun `webhook recipients round-trip as the right destination type`() = runTest {
        val id = recipients.createRecipient(
            RecipientDraft("#family", RecipientDestination.SlackWebhook("https://hooks.slack.com/services/T/B/X")),
        )
        val r = recipients.getRecipient(id)!!
        assertEquals(MessageChannel.SLACK, r.destination.channel)
        assertTrue(r.destination is RecipientDestination.SlackWebhook)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid destinations are rejected`() {
        RecipientDraft("Bad", RecipientDestination.DiscordWebhook("https://example.com/hook"))
    }

    // --- Triggers ------------------------------------------------------------

    @Test
    fun `new triggers are armed and appear in the armed flow`() = runTest {
        val id = newTrigger()
        val armed = triggers.observeArmedTriggers().first().single()
        assertEquals(id, armed.trigger.id)
        assertEquals("Home", armed.place.name)
        assertEquals(TriggerStatus.ARMED, armed.trigger.status)
    }

    @Test
    fun `fire, re-arm, pause lifecycle`() = runTest {
        val id = newTrigger()

        assertTrue("first markFired wins", triggers.markFired(id))
        assertFalse("second markFired is a no-op", triggers.markFired(id))
        val fired = triggers.observeTrigger(id).first()!!.trigger
        assertEquals(TriggerStatus.FIRED, fired.status)
        assertEquals(now, fired.lastFiredAt)
        assertTrue(triggers.observeArmedTriggers().first().isEmpty())

        triggers.arm(id)
        assertEquals(1, triggers.observeArmedTriggers().first().size)
        assertEquals("lastFiredAt survives re-arming", now, triggers.observeTrigger(id).first()!!.trigger.lastFiredAt)

        triggers.pause(id)
        assertFalse("paused triggers can't fire", triggers.markFired(id))
    }

    @Test
    fun `editing a trigger keeps its status`() = runTest {
        val id = newTrigger()
        triggers.pause(id)
        val t = triggers.observeTrigger(id).first()!!.trigger
        triggers.updateTrigger(
            id,
            TriggerDraft(t.placeId, t.recipientId, TriggerDirection.LEAVE, 800.meters, "Left {place}"),
        )
        val updated = triggers.observeTrigger(id).first()!!.trigger
        assertEquals(TriggerStatus.PAUSED, updated.status)
        assertEquals(TriggerDirection.LEAVE, updated.direction)
        assertEquals(800.0, updated.radius.meters, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `radius below the GPS floor is rejected`() = runTest {
        val placeId = places.createPlace(home)
        val recipientId = recipients.createRecipient(alex)
        TriggerDraft(placeId, recipientId, TriggerDirection.ARRIVE, 10.meters, "hi")
    }

    // --- History -------------------------------------------------------------

    @Test
    fun `history is recorded and removed with its trigger`() = runTest {
        val id = newTrigger()
        history.record(id, MessageChannel.SMS, "Almost at Home", SendStatus.SENT)
        assertEquals("Almost at Home", history.observeForTrigger(id).first().single().renderedMessage)

        triggers.deleteTrigger(id)
        assertTrue(history.observeRecent().first().isEmpty())
    }
}
