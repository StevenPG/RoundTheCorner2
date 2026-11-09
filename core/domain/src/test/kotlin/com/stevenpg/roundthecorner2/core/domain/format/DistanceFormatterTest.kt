package com.stevenpg.roundthecorner2.core.domain.format

import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem.IMPERIAL
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem.METRIC
import com.stevenpg.roundthecorner2.core.model.feet
import com.stevenpg.roundthecorner2.core.model.kilometers
import com.stevenpg.roundthecorner2.core.model.meters
import com.stevenpg.roundthecorner2.core.model.miles
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {

    private val us = DistanceFormatter(Locale.US)

    @Test
    fun imperial() {
        assertEquals("45 ft", us.format(43.feet, IMPERIAL))
        assertEquals("350 ft", us.format(347.feet, IMPERIAL))
        assertEquals("0.1 mi", us.format(0.1.miles, IMPERIAL))
        assertEquals("2.3 mi", us.format(2.34.miles, IMPERIAL))
        assertEquals("10 mi", us.format(9.97.miles, IMPERIAL))
        assertEquals("1,234 mi", us.format(1_234.4.miles, IMPERIAL))
    }

    @Test
    fun metric() {
        assertEquals("0 m", us.format(1.meters, METRIC))
        assertEquals("85 m", us.format(86.meters, METRIC))
        assertEquals("850 m", us.format(847.meters, METRIC))
        assertEquals("4.2 km", us.format(4.2.kilometers, METRIC))
        assertEquals("12 km", us.format(12.4.kilometers, METRIC))
    }

    @Test
    fun `uses the locale's decimal separator`() {
        assertEquals("4,2 km", DistanceFormatter(Locale.GERMANY).format(4.2.kilometers, METRIC))
    }

    @Test
    fun `negative distances clamp to zero`() {
        assertEquals("0 m", us.format((-5).meters, METRIC))
    }
}
