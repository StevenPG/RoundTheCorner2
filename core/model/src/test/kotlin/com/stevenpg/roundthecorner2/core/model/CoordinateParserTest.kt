package com.stevenpg.roundthecorner2.core.model

import com.stevenpg.roundthecorner2.core.model.CoordinateParser.Error
import com.stevenpg.roundthecorner2.core.model.CoordinateParser.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateParserTest {

    private fun ok(input: String, lat: Double, lng: Double) {
        val result = CoordinateParser.parse(input)
        assertTrue("Expected success for '$input' but got $result", result is Result.Success)
        val c = (result as Result.Success).coordinates
        assertEquals("lat of '$input'", lat, c.latitude, 1e-6)
        assertEquals("lng of '$input'", lng, c.longitude, 1e-6)
    }

    private fun fails(input: String, error: Error) =
        assertEquals("for '$input'", Result.Failure(error), CoordinateParser.parse(input))

    @Test
    fun `signed decimal with various separators`() {
        ok("40.7128, -74.0060", 40.7128, -74.006)
        ok("40.7128,-74.0060", 40.7128, -74.006)
        ok("  40.7128   -74.0060 ", 40.7128, -74.006)
        ok("40.7128; -74.0060", 40.7128, -74.006)
        ok("+40.7128, +74", 40.7128, 74.0)
        ok("-33.8688, 151.2093", -33.8688, 151.2093)
    }

    @Test
    fun `hemisphere letters before or after`() {
        ok("40.7128° N, 74.0060° W", 40.7128, -74.006)
        ok("40.7128N 74.0060W", 40.7128, -74.006)
        ok("n40.7128 w74.0060", 40.7128, -74.006)
        ok("33.8688 S, 151.2093 E", -33.8688, 151.2093)
    }

    @Test
    fun `degrees minutes seconds as copied from map apps`() {
        ok("""40°42'46.1"N 74°00'21.6"W""", 40.712805556, -74.006)
        ok("40°42′46.1″N 74°00′21.6″W", 40.712805556, -74.006) // Unicode primes
        ok("40°42'46.1''N 74°00'21.6''W", 40.712805556, -74.006) // Two single quotes
    }

    @Test
    fun `degrees and decimal minutes`() {
        ok("40°42.768'N 74°0.36'W", 40.7128, -74.006)
    }

    @Test
    fun errors() {
        fails("", Error.EMPTY)
        fails("   ", Error.EMPTY)
        fails("123 Main St", Error.UNRECOGNIZED_FORMAT)
        fails("40.7128", Error.UNRECOGNIZED_FORMAT)
        fails("91, 0", Error.LATITUDE_OUT_OF_RANGE)
        fails("0, 181", Error.LONGITUDE_OUT_OF_RANGE)
        fails("40.7 E, 74.0 N", Error.UNRECOGNIZED_FORMAT) // Swapped axes
        fails("-40.7 N, 74.0", Error.UNRECOGNIZED_FORMAT) // Sign + hemisphere
        fails("""40°61'0"N 74°0'0"W""", Error.UNRECOGNIZED_FORMAT) // 61 minutes
    }
}
