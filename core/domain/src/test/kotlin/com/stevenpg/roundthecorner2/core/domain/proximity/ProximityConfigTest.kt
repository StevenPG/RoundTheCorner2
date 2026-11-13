package com.stevenpg.roundthecorner2.core.domain.proximity

import com.stevenpg.roundthecorner2.core.model.meters
import com.stevenpg.roundthecorner2.core.model.miles
import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityConfigTest {

    private val config = ProximityConfig()

    @Test
    fun `hysteresis scales with radius within bounds`() {
        assertEquals(50.0, config.hysteresisFor(500.meters).meters, 1e-9) // 10%
        assertEquals(15.0, config.hysteresisFor(100.meters).meters, 1e-9) // floor
        assertEquals(200.0, config.hysteresisFor(10.miles).meters, 1e-9) // ceiling
    }

    @Test
    fun `hysteresis never exceeds half the radius`() {
        assertEquals(10.0, config.hysteresisFor(20.meters).meters, 1e-9)
    }

    @Test
    fun `accuracy limit scales with radius within bounds`() {
        assertEquals(50.0, config.maxAccuracyFor(50.meters).meters, 1e-9)
        assertEquals(250.0, config.maxAccuracyFor(500.meters).meters, 1e-9)
        assertEquals(500.0, config.maxAccuracyFor(5.miles).meters, 1e-9)
    }
}
