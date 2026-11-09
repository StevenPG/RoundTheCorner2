package com.stevenpg.roundthecorner2.core.domain.proximity

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.meters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Drives the evaluator along a straight north-south line through the target so
 * each fix can be expressed as "meters from the center".
 */
class ProximityEvaluatorTest {

    private val center = Coordinates(40.0, -105.0)
    private val evaluator = ProximityEvaluator(ProximityConfig(confirmationsRequired = 2))

    // 500 m radius -> 50 m dead band (10%), 250 m max accuracy (50%).
    private val arrive = ProximityTarget(center, 500.meters, TriggerDirection.ARRIVE)
    private val leave = ProximityTarget(center, 500.meters, TriggerDirection.LEAVE)

    private var clock = Instant.parse("2026-01-01T00:00:00Z")

    /** A fix [metersNorth] of the center (1 deg latitude ~ 111,000 m here). */
    private fun fix(metersNorth: Double, accuracy: Double? = 10.0): LocationFix {
        clock = clock.plusSeconds(10)
        return LocationFix(Coordinates(center.latitude + metersNorth / 111_034.0, center.longitude), accuracy, clock)
    }

    /** Feeds fixes in order, returning every outcome. */
    private fun run(target: ProximityTarget, vararg metersNorth: Double): List<Outcome> {
        var state = ProximityState.INITIAL
        return metersNorth.map { m ->
            evaluator.evaluate(target, state, fix(m)).also { state = it.state }.outcome
        }
    }

    // --- ARRIVE ----------------------------------------------------------------

    @Test
    fun `arrive fires after confirmed crossing from outside to inside`() {
        val outcomes = run(arrive, 2_000.0, 1_000.0, 450.0, 400.0)
        assertEquals(listOf(Outcome.NoChange, Outcome.NoChange, Outcome.NoChange, Outcome.Fire), outcomes)
    }

    @Test
    fun `arrive fires only once while staying inside`() {
        val outcomes = run(arrive, 2_000.0, 400.0, 300.0, 200.0, 100.0)
        assertEquals(1, outcomes.count { it == Outcome.Fire })
    }

    @Test
    fun `a single wild fix inside does not fire`() {
        val outcomes = run(arrive, 2_000.0, 100.0, 2_000.0, 2_000.0)
        assertTrue(Outcome.Fire !in outcomes)
    }

    @Test
    fun `jitter in the dead band around the boundary never fires`() {
        // Confirmed outside, then wobbling between 510 and 540 m (band is 500-550).
        val outcomes = run(arrive, 2_000.0, 510.0, 540.0, 520.0, 530.0, 510.0)
        assertTrue(outcomes.all { it == Outcome.NoChange })
    }

    @Test
    fun `dead band resets confirmation so readings must be consecutive`() {
        // inside, dead-band, inside: only one consecutive inside each time.
        val outcomes = run(arrive, 2_000.0, 400.0, 520.0, 400.0, 520.0)
        assertTrue(Outcome.Fire !in outcomes)
    }

    @Test
    fun `armed while already inside reports AlreadySatisfied and needs a real crossing`() {
        val outcomes = run(arrive, 100.0, 100.0, 2_000.0, 2_000.0, 100.0, 100.0)
        assertEquals(Outcome.AlreadySatisfied, outcomes.first())
        assertEquals(listOf(5), outcomes.indices.filter { outcomes[it] == Outcome.Fire })
    }

    @Test
    fun `can re-fire after leaving and coming back (re-arm is the caller's choice)`() {
        val outcomes = run(arrive, 2_000.0, 400.0, 400.0, 2_000.0, 2_000.0, 400.0, 400.0)
        assertEquals(2, outcomes.count { it == Outcome.Fire })
    }

    // --- LEAVE -----------------------------------------------------------------

    @Test
    fun `leave fires when crossing outward past the exact radius`() {
        val outcomes = run(leave, 0.0, 300.0, 501.0, 520.0)
        assertEquals(listOf(Outcome.NoChange, Outcome.NoChange, Outcome.NoChange, Outcome.Fire), outcomes)
    }

    @Test
    fun `leave dead band is on the inside`() {
        // 460-500 m is the band for LEAVE (radius 500, band 50 -> inside <= 450).
        val outcomes = run(leave, 0.0, 460.0, 501.0, 501.0, 470.0, 501.0, 501.0)
        assertEquals(1, outcomes.count { it == Outcome.Fire })
    }

    @Test
    fun `leave armed while already outside is AlreadySatisfied`() {
        assertEquals(Outcome.AlreadySatisfied, run(leave, 5_000.0).single())
    }

    // --- Fix quality ---------------------------------------------------------

    @Test
    fun `inaccurate fixes are rejected without changing state`() {
        var state = ProximityState.INITIAL
        state = evaluator.evaluate(arrive, state, fix(2_000.0)).state
        val result = evaluator.evaluate(arrive, state, fix(100.0, accuracy = 400.0))
        assertEquals(Outcome.RejectedInaccurate(400.0, 250.0), result.outcome)
        assertEquals(Zone.OUTSIDE, result.state.confirmedZone)
        assertEquals(0, result.state.candidateCount)
    }

    @Test
    fun `fixes without an accuracy are trusted`() {
        val result = evaluator.evaluate(arrive, ProximityState.INITIAL, fix(2_000.0, accuracy = null))
        assertEquals(Zone.OUTSIDE, result.state.confirmedZone)
    }

    @Test
    fun `out of order fixes are rejected`() {
        val newer = fix(2_000.0)
        val state = evaluator.evaluate(arrive, ProximityState.INITIAL, newer).state
        val older = newer.copy(timestamp = newer.timestamp.minusSeconds(30))
        assertEquals(Outcome.RejectedStale, evaluator.evaluate(arrive, state, older).outcome)
    }

    // --- Progress ------------------------------------------------------------

    @Test
    fun `remaining distance counts down to zero`() {
        val result = evaluator.evaluate(arrive, ProximityState.INITIAL, fix(1_500.0))
        assertEquals(1_000.0, result.remaining!!.meters, 1.0)
        assertEquals(1_500.0, result.distance!!.meters, 1.0)

        val inside = evaluator.evaluate(leave, ProximityState.INITIAL, fix(200.0))
        assertEquals(300.0, inside.remaining!!.meters, 1.0)
    }

    @Test
    fun `single confirmation config fires on the first inside fix`() {
        val eager = ProximityEvaluator(ProximityConfig(confirmationsRequired = 1))
        var state = eager.evaluate(arrive, ProximityState.INITIAL, fix(2_000.0)).state
        assertEquals(Outcome.Fire, eager.evaluate(arrive, state, fix(100.0)).outcome)
    }
}
