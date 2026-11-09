package com.stevenpg.roundthecorner2.core.domain.proximity

import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.meters

/**
 * Tuning knobs for [ProximityEvaluator]. Defaults are chosen for typical phone
 * GPS (5-20 m outdoors) and radii from ~100 m up to tens of kilometres.
 */
data class ProximityConfig(
    /** Dead-band width as a fraction of the radius... */
    val hysteresisFraction: Double = 0.10,
    /** ...but never narrower than this (GPS noise floor)... */
    val minHysteresis: Distance = 15.meters,
    /** ...nor wider than this (so huge radii don't need a huge detour). */
    val maxHysteresis: Distance = 200.meters,
    /** Max allowed fix inaccuracy as a fraction of the radius... */
    val accuracyFraction: Double = 0.5,
    /** ...but always accept fixes at least this good... */
    val minAccuracyLimit: Distance = 50.meters,
    /** ...and never accept fixes worse than this. */
    val maxAccuracyLimit: Distance = 500.meters,
    /** Consecutive agreeing fixes needed to confirm a zone change. */
    val confirmationsRequired: Int = 2,
) {
    init {
        require(confirmationsRequired >= 1) { "confirmationsRequired must be >= 1" }
        require(hysteresisFraction >= 0 && accuracyFraction > 0) { "fractions must be positive" }
    }

    /**
     * Width of the dead band for a radius. Also capped at half the radius so a
     * tiny LEAVE radius still has a usable "inside" region.
     */
    fun hysteresisFor(radius: Distance): Distance {
        val scaled = (radius.meters * hysteresisFraction).coerceIn(minHysteresis.meters, maxHysteresis.meters)
        return Distance(minOf(scaled, radius.meters / 2))
    }

    /** Worst fix accuracy still trusted for a radius. */
    fun maxAccuracyFor(radius: Distance): Distance =
        Distance((radius.meters * accuracyFraction).coerceIn(minAccuracyLimit.meters, maxAccuracyLimit.meters))
}
