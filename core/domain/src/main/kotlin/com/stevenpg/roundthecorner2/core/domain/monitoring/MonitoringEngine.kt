package com.stevenpg.roundthecorner2.core.domain.monitoring

import com.stevenpg.roundthecorner2.core.domain.polling.PollingPolicy
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import com.stevenpg.roundthecorner2.core.domain.proximity.Outcome
import com.stevenpg.roundthecorner2.core.domain.proximity.ProximityEvaluator
import com.stevenpg.roundthecorner2.core.domain.proximity.ProximityState
import com.stevenpg.roundthecorner2.core.domain.proximity.ProximityTarget
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import kotlin.time.Duration

/**
 * Turns a stream of location fixes into decisions for every ARMED trigger:
 * which to fire, which were armed while already satisfied, how far each one
 * has to go (for the notification), and when to check next.
 *
 * Pure Kotlin and single-threaded by contract (the service calls it from one
 * coroutine), so it is fully unit-tested without Android.
 *
 * Per-trigger state is reset whenever a trigger's geometry changes (edited) or
 * it stops being armed (fired/paused), so re-arming always starts fresh.
 */
class MonitoringEngine(
    private val evaluator: ProximityEvaluator = ProximityEvaluator(),
    var pollingPolicy: PollingPolicy = PollingPolicy(),
) {
    /** Everything that identifies a trigger's geometry; a change means "start over". */
    private data class Geometry(val target: ProximityTarget)

    private class Tracked(
        val geometry: Geometry,
        var state: ProximityState = ProximityState.INITIAL,
        /** Largest "remaining" seen since arming: the 0% point of the progress bar. */
        var startRemaining: Distance? = null,
        var fired: Boolean = false,
    )

    private val tracked = mutableMapOf<TriggerId, Tracked>()

    /** Latest snapshot per trigger, so UI can be rebuilt between fixes. */
    var lastTick: Tick? = null
        private set

    fun onFix(armed: List<TriggerDetails>, fix: LocationFix): Tick {
        // Forget triggers that are no longer armed (fired, paused, deleted).
        val armedIds = armed.map { it.trigger.id }.toSet()
        tracked.keys.retainAll(armedIds)

        val statuses = mutableListOf<TriggerProgress>()
        val toFire = mutableListOf<FireRequest>()
        val alreadySatisfied = mutableListOf<TriggerDetails>()
        var inaccurate = false

        for (details in armed) {
            val target = ProximityTarget(details.place.coordinates, details.trigger.radius, details.trigger.direction)
            val geometry = Geometry(target)
            val entry = tracked[details.trigger.id]?.takeIf { it.geometry == geometry }
                ?: Tracked(geometry).also { tracked[details.trigger.id] = it }
            if (entry.fired) continue // Fired; waiting for the DB to report it as no longer armed.

            val evaluation = evaluator.evaluate(target, entry.state, fix)
            entry.state = evaluation.state
            val remaining = evaluation.remaining
            if (remaining != null) {
                entry.startRemaining = maxOf(entry.startRemaining ?: remaining, remaining)
            }

            when (evaluation.outcome) {
                Outcome.Fire -> {
                    entry.fired = true
                    toFire += FireRequest(details, evaluation.distance)
                }
                Outcome.AlreadySatisfied -> alreadySatisfied += details
                is Outcome.RejectedInaccurate -> inaccurate = true
                Outcome.NoChange, Outcome.RejectedStale -> Unit
            }
            if (!entry.fired) {
                statuses += TriggerProgress(
                    details = details,
                    distance = evaluation.distance,
                    remaining = remaining,
                    progress = progress(entry.startRemaining, remaining),
                )
            }
        }

        val nearestRemaining = statuses.mapNotNull { it.remaining }.minOrNull()
        val nextInterval = if (nearestRemaining == null) {
            pollingPolicy.minInterval // No usable fix yet: look again soon.
        } else {
            pollingPolicy.nextInterval(nearestRemaining, fix.speedMetersPerSecond)
        }

        return Tick(
            progress = statuses.sortedWith(compareBy(nullsLast()) { it.remaining }),
            toFire = toFire,
            alreadySatisfied = alreadySatisfied,
            waitingForAccuracy = inaccurate && statuses.all { it.remaining == null },
            nextInterval = nextInterval,
        ).also { lastTick = it }
    }

    /** Forgets everything (monitoring stopped). */
    fun reset() {
        tracked.clear()
        lastTick = null
    }

    private fun progress(start: Distance?, remaining: Distance?): Float? {
        if (start == null || remaining == null) return null
        if (start.meters <= 0.0) return 1f
        return (1.0 - remaining.meters / start.meters).coerceIn(0.0, 1.0).toFloat()
    }
}

/** One fix's worth of decisions. */
data class Tick(
    /** Armed, not-yet-fired triggers, nearest-to-firing first. */
    val progress: List<TriggerProgress>,
    val toFire: List<FireRequest>,
    /** Armed while already on the "fire" side: ask the user what to do. */
    val alreadySatisfied: List<TriggerDetails>,
    /** True when every fix so far was too inaccurate to use. */
    val waitingForAccuracy: Boolean,
    val nextInterval: Duration,
)

data class TriggerProgress(
    val details: TriggerDetails,
    /** From the place's centre; null if this fix was rejected. */
    val distance: Distance?,
    /** Still to travel before it fires. */
    val remaining: Distance?,
    /** 0 at arming, 1 at the boundary; null until known. */
    val progress: Float?,
)

data class FireRequest(val details: TriggerDetails, val distance: Distance?)
