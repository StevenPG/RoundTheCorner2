package com.stevenpg.roundthecorner2.core.domain.format

import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem.IMPERIAL
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem.METRIC
import com.stevenpg.roundthecorner2.core.model.kilometers
import com.stevenpg.roundthecorner2.core.model.miles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RadiusInputTest {

    @Test
    fun `parses either decimal separator in the display unit`() {
        assertEquals(1.5.miles.meters, RadiusInput.parse("1.5", IMPERIAL)!!.meters, 1e-9)
        assertEquals(1.5.kilometers.meters, RadiusInput.parse(" 1,5 ", METRIC)!!.meters, 1e-9)
    }

    @Test
    fun `rejects non-positive and garbage input`() {
        assertNull(RadiusInput.parse("", METRIC))
        assertNull(RadiusInput.parse("0", METRIC))
        assertNull(RadiusInput.parse("-1", METRIC))
        assertNull(RadiusInput.parse("two", METRIC))
    }

    @Test
    fun `formats compactly and round-trips`() {
        assertEquals("0.25", RadiusInput.format(0.25.miles, IMPERIAL))
        assertEquals("2", RadiusInput.format(2.kilometers, METRIC))
        assertEquals("3.22", RadiusInput.format(2.miles, METRIC)) // Unit switch converts.
    }
}
