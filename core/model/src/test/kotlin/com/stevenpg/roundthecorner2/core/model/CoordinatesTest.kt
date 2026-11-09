package com.stevenpg.roundthecorner2.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatesTest {

    @Test(expected = IllegalArgumentException::class)
    fun `rejects latitude out of range`() {
        Coordinates(90.0001, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects NaN`() {
        Coordinates(Double.NaN, 0.0)
    }

    @Test
    fun `orNull returns null instead of throwing`() {
        assertNull(Coordinates.orNull(0.0, 180.5))
    }

    @Test
    fun `toString is locale independent with six decimals`() {
        assertEquals("40.712800, -74.006000", Coordinates(40.7128, -74.006).toString())
    }

    @Test
    fun `distance unit conversions`() {
        assertEquals(1_609.344, 1.miles.meters, 1e-9)
        assertEquals(5_280.0, 1.miles.feet, 1e-9)
        assertEquals(2.5, 2_500.meters.kilometers, 1e-9)
    }
}
