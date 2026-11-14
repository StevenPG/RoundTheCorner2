package com.stevenpg.roundthecorner2.core.domain.polling

import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.kilometers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Adaptive location polling: check rarely when far away, often when close.
 *
 * The next check is scheduled for half the time it would take to reach the
 * boundary at an assumed (pessimistically high) speed, clamped to
 * [[minInterval], [maxInterval]]. Far away -> battery-friendly; close by ->
 * responsive. With several triggers, the service uses the shortest interval.
 *
 * It also picks the location accuracy: low-power (no GPS) while the nearest
 * trigger is far from firing, precise (GPS) on the approach.
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
    /** Switch to low-power fixes once the nearest trigger is at least this far from firing... */
    val lowPowerBeyond: Distance = 4.kilometers,
    /**
     * ...and back to precise within this distance. At the assumed top speed that's
     * still about 85 s from the line, so several precise checks happen before it.
     * The gap between the two values stops the request flapping at the edge.
     */
    val preciseWithin: Distance = 3.kilometers,
) {
    init {
        require(minInterval.isPositive() && minInterval <= maxInterval) { "Invalid interval bounds" }
        require(preciseWithin <= lowPowerBeyond) { "preciseWithin must not exceed lowPowerBeyond" }
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

    /**
     * @param remaining worst-case distance until the nearest trigger fires, or
     *   null if there is no estimate yet (then GPS is needed to get one).
     * @param current the accuracy in use now; kept while between the two thresholds.
     */
    fun accuracyFor(remaining: Distance?, current: LocationAccuracy): LocationAccuracy = when {
        remaining == null -> LocationAccuracy.PRECISE
        remaining < preciseWithin -> LocationAccuracy.PRECISE
        remaining >= lowPowerBeyond -> LocationAccuracy.LOW_POWER
        else -> current
    }
}
