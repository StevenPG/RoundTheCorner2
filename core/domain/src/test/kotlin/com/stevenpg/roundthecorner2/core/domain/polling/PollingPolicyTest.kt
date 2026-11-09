package com.stevenpg.roundthecorner2.core.domain.polling

import com.stevenpg.roundthecorner2.core.model.kilometers
import com.stevenpg.roundthecorner2.core.model.meters
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class PollingPolicyTest {

    private val policy = PollingPolicy(minInterval = 10.seconds, maxInterval = 60.seconds)

    @Test
    fun `far away polls at the max interval`() {
        assertEquals(60.seconds, policy.nextInterval(50.kilometers))
    }

    @Test
    fun `close by polls at the min interval`() {
        assertEquals(10.seconds, policy.nextInterval(100.meters))
        assertEquals(10.seconds, policy.nextInterval(0.meters))
    }

    @Test
    fun `in between scales with distance`() {
        // 2,100 m / 35 m/s = 60 s to boundary, x0.5 = 30 s.
        assertEquals(30.seconds, policy.nextInterval(2_100.meters))
    }

    @Test
    fun `a fast observed speed shortens the interval`() {
        // 70 m/s * 1.25 = 87.5 m/s -> 2,100 / 87.5 * 0.5 = 12 s.
        assertEquals(12.seconds, policy.nextInterval(2_100.meters, observedSpeedMetersPerSecond = 70.0))
    }

    @Test
    fun `a slow observed speed never lengthens it beyond the pessimistic default`() {
        assertEquals(30.seconds, policy.nextInterval(2_100.meters, observedSpeedMetersPerSecond = 1.0))
    }
}
