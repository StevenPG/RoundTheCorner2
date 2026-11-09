package com.stevenpg.roundthecorner2.core.monitoring.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Changing the location request means re-registering with the OS, so the
 * adaptive interval is snapped *down* to a few fixed steps: it only changes
 * when it matters, and never checks less often than the policy asked.
 */
object IntervalBuckets {
    private val STEPS = listOf(10, 15, 20, 30, 45, 60, 90, 120, 180, 300).map { it.seconds }

    fun quantize(interval: Duration): Duration = STEPS.lastOrNull { it <= interval } ?: STEPS.first()
}
