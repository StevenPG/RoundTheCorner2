package com.stevenpg.roundthecorner2.core.domain.geo

import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeodesyTest {

    @Test
    fun `matches Vincenty's published Flinders Peak to Buninyong result`() {
        // Classic geodesy test case: 54,972.271 m on WGS84.
        val flindersPeak = CoordinateParser.parseOrNull("""37°57'03.72030"S 144°25'29.52440"E""")!!
        val buninyong = CoordinateParser.parseOrNull("""37°39'10.15610"S 143°55'35.38390"E""")!!
        assertEquals(54_972.271, Geodesy.distance(flindersPeak, buninyong).meters, 0.001)
    }

    @Test
    fun `one degree of longitude on the equator`() {
        // a * pi / 180 on WGS84.
        val d = Geodesy.distance(Coordinates(0.0, 0.0), Coordinates(0.0, 1.0))
        assertEquals(111_319.491, d.meters, 0.001)
    }

    @Test
    fun `identical points are zero apart`() {
        val p = Coordinates(40.7128, -74.006)
        assertEquals(0.0, Geodesy.distance(p, p).meters, 0.0)
    }

    @Test
    fun `is symmetric`() {
        val a = Coordinates(34.0522, -118.2437)
        val b = Coordinates(40.7128, -74.0060)
        assertEquals(Geodesy.distance(a, b).meters, Geodesy.distance(b, a).meters, 1e-6)
    }

    @Test
    fun `nearly antipodal points fall back to haversine instead of failing`() {
        val a = Coordinates(0.0, 0.0)
        val b = Coordinates(0.5, 179.7)
        assertNull("Vincenty should not converge here", Geodesy.vincentyMeters(a, b))
        val d = Geodesy.distance(a, b).meters
        assertTrue(d.isFinite() && d in 19_900_000.0..20_040_000.0)
    }

    @Test
    fun `haversine stays within half a percent of the ellipsoid`() {
        val a = Coordinates(51.5074, -0.1278) // London
        val b = Coordinates(48.8566, 2.3522) // Paris
        val ellipsoid = Geodesy.distance(a, b).meters
        val sphere = Geodesy.haversineMeters(a, b)
        assertEquals(ellipsoid, sphere, ellipsoid * 0.005)
    }
}
