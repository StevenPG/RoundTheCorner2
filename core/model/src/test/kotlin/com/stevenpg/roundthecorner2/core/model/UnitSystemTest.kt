package com.stevenpg.roundthecorner2.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitSystemTest {

    @Test
    fun `SYSTEM follows the locale default`() {
        assertEquals(ResolvedUnitSystem.IMPERIAL, UnitSystem.SYSTEM.resolve(ResolvedUnitSystem.IMPERIAL))
        assertEquals(ResolvedUnitSystem.METRIC, UnitSystem.SYSTEM.resolve(ResolvedUnitSystem.METRIC))
    }

    @Test
    fun `explicit choices override the locale`() {
        assertEquals(ResolvedUnitSystem.METRIC, UnitSystem.METRIC.resolve(ResolvedUnitSystem.IMPERIAL))
        assertEquals(ResolvedUnitSystem.IMPERIAL, UnitSystem.IMPERIAL.resolve(ResolvedUnitSystem.METRIC))
    }
}
