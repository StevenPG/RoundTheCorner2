package com.stevenpg.roundthecorner2.core.domain.proximity

import com.stevenpg.roundthecorner2.core.domain.geo.Geodesy
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.meters
import java.time.Instant

/** Which side of a trigger's boundary the device is on. */
enum class Zone { INSIDE, OUTSIDE }

/**
 * What the evaluator remembers about one trigger between fixes. Lives in
 * memory in the monitoring service; starts as [ProximityState.INITIAL].
 */
data class ProximityState(
    /** Last zone confirmed by enough consecutive fixes; null until the first fix. */
    val confirmedZone: Zone?,
    /** A zone that recent fixes point to but that isn't confirmed yet. */
    val candidateZone: Zone?,
    val candidateCount: Int,
    val lastFixTime: Instant?,
) {
    companion object {
        val INITIAL = ProximityState(null, null, 0, null)
    }
}

/** The geometry of one trigger, as seen by the evaluator. */
data class ProximityTarget(
    val center: Coordinates,
    val radius: Distance,
    val direction: TriggerDirection,
)

/** Result of feeding one fix to the evaluator. */
data class ProximityEvaluation(
    /** State to pass to the next [ProximityEvaluator.evaluate] call. */
    val state: ProximityState,
    val outcome: Outcome,
    /** Distance from the fix to the place's center (null if the fix was rejected). */
    val distance: Distance?,
    /** How much further until the trigger fires; zero once it would fire (null if rejected). */
    val remaining: Distance?,
)

sealed interface Outcome {
    /** The boundary was crossed in the trigger's direction: send the message. */
    data object Fire : Outcome

    /** Nothing happened worth acting on. */
    data object NoChange : Outcome

    /**
     * The first confirmed position is already on the "fire" side (e.g. an
     * ARRIVE trigger armed while already at the place). It does **not** fire;
     * the device must cross the boundary for real. The UI can explain this.
     */
    data object AlreadySatisfied : Outcome

    /** The fix was too inaccurate to trust; state is unchanged. */
    data class RejectedInaccurate(val accuracyMeters: Double, val maxAllowedMeters: Double) : Outcome

    /** The fix is older than one already processed (fixes can arrive out of order). */
    data object RejectedStale : Outcome
}

/**
 * The core "should we send now?" state machine. Pure and deterministic: same
 * inputs, same outputs, so every edge case is unit-tested.
 *
 * ### How it avoids false sends
 * 1. **Accuracy gate** - fixes whose reported accuracy is worse than
 *    [ProximityConfig.maxAccuracyFor] the radius are ignored.
 * 2. **Hysteresis (dead band)** - the trigger fires exactly at the configured
 *    radius, but to count as having *started* on the other side you must be
 *    clearly past it by [ProximityConfig.hysteresisFor]. GPS wobble around the
 *    boundary therefore can't fire, un-fire and re-fire it.
 *    - ARRIVE: OUTSIDE = beyond radius + band, INSIDE = within radius.
 *    - LEAVE:  INSIDE = within radius - band,  OUTSIDE = beyond radius.
 * 3. **Confirmation** - a zone change needs [ProximityConfig.confirmationsRequired]
 *    consecutive fixes agreeing, so one wild fix (urban-canyon multipath) can't fire.
 * 4. **Edge-triggered** - it fires only on a confirmed transition *into* the
 *    target zone *from* the opposite zone; never on the very first reading.
 */
class ProximityEvaluator(
    private val config: ProximityConfig = ProximityConfig(),
) {

    fun evaluate(target: ProximityTarget, state: ProximityState, fix: LocationFix): ProximityEvaluation {
        // Out-of-order delivery: never let an older fix overwrite newer knowledge.
        if (state.lastFixTime != null && fix.timestamp.isBefore(state.lastFixTime)) {
            return ProximityEvaluation(state, Outcome.RejectedStale, distance = null, remaining = null)
        }

        val maxAccuracy = config.maxAccuracyFor(target.radius)
        val accuracy = fix.accuracyMeters
        if (accuracy != null && accuracy > maxAccuracy.meters) {
            return ProximityEvaluation(
                state = state.copy(lastFixTime = fix.timestamp),
                outcome = Outcome.RejectedInaccurate(accuracy, maxAccuracy.meters),
                distance = null,
                remaining = null,
            )
        }

        val distance = Geodesy.distance(fix.coordinates, target.center)
        val remaining = remainingUntilFire(target, distance)
        val observed = classify(target, distance)
        val (newState, outcome) = advance(target.direction, state, observed)

        return ProximityEvaluation(
            state = newState.copy(lastFixTime = fix.timestamp),
            outcome = outcome,
            distance = distance,
            remaining = remaining,
        )
    }

    /** Distance still to travel before the trigger would fire. */
    fun remainingUntilFire(target: ProximityTarget, distance: Distance): Distance = when (target.direction) {
        TriggerDirection.ARRIVE -> (distance - target.radius).coerceAtLeastZero()
        TriggerDirection.LEAVE -> (target.radius - distance).coerceAtLeastZero()
    }

    /** Maps a distance to a zone, or null when it falls inside the dead band. */
    private fun classify(target: ProximityTarget, distance: Distance): Zone? {
        val band = config.hysteresisFor(target.radius)
        return when (target.direction) {
            TriggerDirection.ARRIVE -> when {
                distance <= target.radius -> Zone.INSIDE
                distance > target.radius + band -> Zone.OUTSIDE
                else -> null
            }
            TriggerDirection.LEAVE -> when {
                distance > target.radius -> Zone.OUTSIDE
                distance <= target.radius - band -> Zone.INSIDE
                else -> null
            }
        }
    }

    private fun advance(
        direction: TriggerDirection,
        state: ProximityState,
        observed: Zone?,
    ): Pair<ProximityState, Outcome> {
        val fireZone = if (direction == TriggerDirection.ARRIVE) Zone.INSIDE else Zone.OUTSIDE

        // Dead band: no evidence either way. Drop any pending candidate so that
        // confirmations must be *consecutive* clear readings.
        if (observed == null) {
            return state.copy(candidateZone = null, candidateCount = 0) to Outcome.NoChange
        }

        // First clear reading establishes the baseline immediately - it can never
        // fire, so there's nothing to gain by waiting for confirmations.
        if (state.confirmedZone == null) {
            val outcome = if (observed == fireZone) Outcome.AlreadySatisfied else Outcome.NoChange
            return ProximityState(observed, null, 0, state.lastFixTime) to outcome
        }

        // Still where we were: clear any pending candidate.
        if (observed == state.confirmedZone) {
            return state.copy(candidateZone = null, candidateCount = 0) to Outcome.NoChange
        }

        // Evidence of a change: count consecutive agreeing fixes.
        val count = if (state.candidateZone == observed) state.candidateCount + 1 else 1
        if (count < config.confirmationsRequired) {
            return state.copy(candidateZone = observed, candidateCount = count) to Outcome.NoChange
        }

        // Confirmed transition.
        val newState = ProximityState(observed, null, 0, state.lastFixTime)
        val outcome = if (observed == fireZone) Outcome.Fire else Outcome.NoChange
        return newState to outcome
    }

    private fun Distance.coerceAtLeastZero() = if (meters < 0) 0.meters else this
}
