package com.stevenpg.roundthecorner2.core.domain.monitoring

import com.stevenpg.roundthecorner2.core.domain.polling.PollingPolicy
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import com.stevenpg.roundthecorner2.core.domain.proximity.ProximityConfig
import com.stevenpg.roundthecorner2.core.domain.proximity.ProximityEvaluator
import com.stevenpg.roundthecorner2.core.model.Coordinates
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
import com.stevenpg.roundthecorner2.core.model.meters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class MonitoringEngineTest {

    private val center = Coordinates(40.0, -105.0)
    private val home = Place(PlaceId(1), "Home", PlaceIcon.HOME, center, null, Instant.EPOCH)
    private val far = Place(PlaceId(2), "Far", PlaceIcon.PIN, Coordinates(40.5, -105.0), null, Instant.EPOCH)
    private val alex = Recipient(RecipientId(1), "Alex", RecipientDestination.Sms("555"), Instant.EPOCH)

    private fun trigger(id: Long, place: Place, radius: Double = 500.0, direction: TriggerDirection = TriggerDirection.ARRIVE) =
        TriggerDetails(
            Trigger(TriggerId(id), place.id, alex.id, direction, radius.meters, "m", TriggerStatus.ARMED, Instant.EPOCH, null),
            place,
            alex,
        )

    private var clock = Instant.parse("2026-01-01T00:00:00Z")
    private fun fixAt(metersNorth: Double, accuracy: Double = 10.0): LocationFix {
        clock = clock.plusSeconds(10)
        return LocationFix(Coordinates(center.latitude + metersNorth / 111_034.0, center.longitude), accuracy, clock)
    }

    private val engine = MonitoringEngine(
        ProximityEvaluator(ProximityConfig(confirmationsRequired = 2)),
        PollingPolicy(minInterval = 10.seconds, maxInterval = 60.seconds),
    )

    @Test
    fun `progress climbs from 0 towards 1 and the trigger fires once`() {
        val t = trigger(1, home)
        val first = engine.onFix(listOf(t), fixAt(2_500.0))
        assertEquals(0f, first.progress.single().progress!!, 0.001f)
        assertEquals(2_000.0, first.progress.single().remaining!!.meters, 2.0)

        val half = engine.onFix(listOf(t), fixAt(1_500.0))
        assertEquals(0.5f, half.progress.single().progress!!, 0.01f)

        assertTrue(engine.onFix(listOf(t), fixAt(400.0)).toFire.isEmpty()) // 1st confirmation
        val fired = engine.onFix(listOf(t), fixAt(300.0))
        assertEquals(t, fired.toFire.single().details)
        assertTrue("fired triggers leave the progress list", fired.progress.isEmpty())

        // DB hasn't caught up yet (still reported armed): must not fire again.
        assertTrue(engine.onFix(listOf(t), fixAt(200.0)).toFire.isEmpty())
    }

    @Test
    fun `moving away never makes progress negative`() {
        val t = trigger(1, home)
        engine.onFix(listOf(t), fixAt(1_500.0))
        val away = engine.onFix(listOf(t), fixAt(3_500.0))
        assertEquals(0f, away.progress.single().progress!!, 0.001f)
    }

    @Test
    fun `armed while inside asks the user, once`() {
        val t = trigger(1, home)
        assertEquals(listOf(t), engine.onFix(listOf(t), fixAt(100.0)).alreadySatisfied)
        assertTrue(engine.onFix(listOf(t), fixAt(120.0)).alreadySatisfied.isEmpty())
    }

    @Test
    fun `re-arming or editing starts fresh`() {
        val t = trigger(1, home)
        engine.onFix(listOf(t), fixAt(100.0)) // AlreadySatisfied consumed
        engine.onFix(emptyList(), fixAt(100.0)) // paused -> forgotten
        assertEquals(listOf(t), engine.onFix(listOf(t), fixAt(100.0)).alreadySatisfied)

        val edited = trigger(1, home, radius = 50.0) // Now outside the smaller radius.
        val tick = engine.onFix(listOf(edited), fixAt(100.0))
        assertTrue(tick.alreadySatisfied.isEmpty())
        assertEquals(50.0, tick.progress.single().remaining!!.meters, 2.0)
    }

    @Test
    fun `nearest trigger sorts first and drives the polling interval`() {
        val near = trigger(1, home)
        val distant = trigger(2, far)
        val tick = engine.onFix(listOf(distant, near), fixAt(800.0))
        assertEquals(listOf(near, distant), tick.progress.map { it.details })
        // 300 m to go: 300 / 35 m/s * 0.5 = 4.3 s, clamped up to the 10 s minimum.
        assertEquals(10.seconds, tick.nextInterval)
    }

    @Test
    fun `only inaccurate fixes means waiting for accuracy and polling fast`() {
        val t = trigger(1, home)
        val tick = engine.onFix(listOf(t), fixAt(1_000.0, accuracy = 900.0))
        assertTrue(tick.waitingForAccuracy)
        assertNull(tick.progress.single().remaining)
        assertEquals(10.seconds, tick.nextInterval)
    }
}
