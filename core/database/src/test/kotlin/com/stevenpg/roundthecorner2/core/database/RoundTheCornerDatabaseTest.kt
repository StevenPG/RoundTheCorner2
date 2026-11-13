package com.stevenpg.roundthecorner2.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.database.entity.PlaceEntity
import com.stevenpg.roundthecorner2.core.database.entity.RecipientEntity
import com.stevenpg.roundthecorner2.core.database.entity.SendAttemptEntity
import com.stevenpg.roundthecorner2.core.database.entity.TriggerEntity
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** DAO behaviour against a real (in-memory) SQLite database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoundTheCornerDatabaseTest {

    private lateinit var db: RoundTheCornerDatabase
    private val now = Instant.parse("2026-11-09T12:00:00Z")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RoundTheCornerDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertPlace(name: String = "Home") = db.placeDao().insert(
        PlaceEntity(name = name, icon = PlaceIcon.HOME, latitude = 40.0, longitude = -105.0, address = null, createdAt = now),
    )

    private suspend fun insertRecipient() = db.recipientDao().insert(
        RecipientEntity(name = "Alex", channel = MessageChannel.SMS, address = "+15555550100", createdAt = now),
    )

    private suspend fun insertTrigger(
        placeId: Long,
        recipientId: Long,
        status: TriggerStatus = TriggerStatus.ARMED,
        createdAt: Instant = now,
    ) = db.triggerDao().insert(
        TriggerEntity(
            placeId = placeId,
            recipientId = recipientId,
            direction = TriggerDirection.ARRIVE,
            radiusMeters = 500.0,
            messageTemplate = "Almost at {place}",
            status = status,
            createdAt = createdAt,
            lastFiredAt = null,
        ),
    )

    @Test
    fun placesAreSortedCaseInsensitively() = runTest {
        insertPlace("work")
        insertPlace("Home")
        insertPlace("gym")
        assertEquals(listOf("gym", "Home", "work"), db.placeDao().observeAll().first().map { it.name })
    }

    @Test
    fun triggerLoadsWithItsPlaceAndRecipient() = runTest {
        val placeId = insertPlace()
        val recipientId = insertRecipient()
        insertTrigger(placeId, recipientId)

        val details = db.triggerDao().observeAllWithRelations().first().single()
        assertEquals("Home", details.place.name)
        assertEquals("Alex", details.recipient.name)
        assertEquals(now, details.trigger.createdAt) // Instant round-trips via converter.
    }

    @Test
    fun armedTriggersSortFirst() = runTest {
        val p = insertPlace()
        val r = insertRecipient()
        insertTrigger(p, r, TriggerStatus.FIRED, createdAt = now.plusSeconds(30))
        insertTrigger(p, r, TriggerStatus.ARMED, createdAt = now)
        insertTrigger(p, r, TriggerStatus.PAUSED, createdAt = now.plusSeconds(60))

        val statuses = db.triggerDao().observeAllWithRelations().first().map { it.trigger.status }
        assertEquals(listOf(TriggerStatus.ARMED, TriggerStatus.PAUSED, TriggerStatus.FIRED), statuses)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun placeInUseCannotBeDeleted() = runTest {
        val placeId = insertPlace()
        insertTrigger(placeId, insertRecipient())
        db.placeDao().deleteById(placeId)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun triggerMustReferenceExistingPlace() = runTest {
        insertTrigger(placeId = 999, recipientId = insertRecipient())
    }

    @Test
    fun deletingTriggerCascadesToHistory() = runTest {
        val triggerId = insertTrigger(insertPlace(), insertRecipient())
        db.sendAttemptDao().insert(
            SendAttemptEntity(
                triggerId = triggerId, timestamp = now, channel = MessageChannel.SMS,
                renderedMessage = "hi", status = SendStatus.SENT, error = null,
            ),
        )
        db.triggerDao().deleteById(triggerId)
        assertTrue(db.sendAttemptDao().observeRecent(10).first().isEmpty())
    }

    @Test
    fun markFiredIsACompareAndSet() = runTest {
        val id = insertTrigger(insertPlace(), insertRecipient())
        assertEquals(1, db.triggerDao().markFiredIfArmed(id, now))
        assertEquals(0, db.triggerDao().markFiredIfArmed(id, now.plusSeconds(1)))

        val trigger = db.triggerDao().getById(id)!!
        assertEquals(TriggerStatus.FIRED, trigger.status)
        assertEquals(now, trigger.lastFiredAt)
    }

    @Test
    fun armedFlowReactsToStatusChanges() = runTest {
        val id = insertTrigger(insertPlace(), insertRecipient())
        db.triggerDao().observeByStatusWithRelations(TriggerStatus.ARMED).test {
            assertEquals(1, awaitItem().size)
            db.triggerDao().updateStatus(id, TriggerStatus.PAUSED)
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
