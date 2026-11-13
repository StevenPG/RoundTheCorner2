package com.stevenpg.roundthecorner2.core.domain.polling

import com.stevenpg.roundthecorner2.core.model.Distance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Adaptive location polling: check rarely when far away, often when close.
 *
 * The next check is scheduled for half the time it would take to reach the
 * boundary at an assumed (pessimistically high) speed, clamped to
 * [[minInterval], [maxInterval]]. Far away -> battery-friendly; close by ->
 * responsive. With several triggers, the service uses the shortest interval.
 */
class PollingPolicy(
    /** Never poll faster than this (GPS fixes cost battery). */
    val minInterval: Duration = 10.seconds,
    /** Never poll slower than this - the user-configurable base interval. */
    val maxInterval: Duration = 60.seconds,
    /** Assumed top speed when the fix has none: 35 m/s is about 78 mph / 126 km/h. */
    private val assumedSpeedMetersPerSecond: Double = 35.0,
    /** Fraction of the time-to-boundary to wait; 0.5 = check twice before arrival. */
    private val safetyFactor: Double = 0.5,
) {
    init {
        require(minInterval.isPositive() && minInterval <= maxInterval) { "Invalid interval bounds" }
        require(assumedSpeedMetersPerSecond > 0 && safetyFactor in 0.0..1.0) { "Invalid speed/safety factor" }
    }

    /**
     * @param remaining distance until the nearest trigger fires.
     * @param observedSpeedMetersPerSecond speed from the latest fix, if any. A
     *   faster observed speed (e.g. a train) shortens the interval; a slower one
     *   never lengthens it beyond the pessimistic default - you might speed up.
     */
    fun nextInterval(remaining: Distance, observedSpeedMetersPerSecond: Double? = null): Duration {
        val speed = maxOf(assumedSpeedMetersPerSecond, (observedSpeedMetersPerSecond ?: 0.0) * 1.25)
        val secondsToBoundary = remaining.meters.coerceAtLeast(0.0) / speed
        return (secondsToBoundary * safetyFactor).seconds.coerceIn(minInterval, maxInterval)
    }
}
