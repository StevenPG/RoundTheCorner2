package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimAddressDto
import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimPlaceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodedPlaceMappingTest {

    @Test
    fun `street address uses house number and road as the title`() {
        val place = NominatimPlaceDto(
            lat = "47.5755192",
            lon = "-122.6419508",
            name = "",
            displayName = "1600, Pennsylvania Avenue, Bremerton, Washington, United States",
            address = NominatimAddressDto(
                houseNumber = "1600", road = "Pennsylvania Avenue", city = "Bremerton",
                state = "Washington", country = "United States",
            ),
        ).toGeocodedPlace()!!

        assertEquals("1600 Pennsylvania Avenue", place.title)
        assertEquals("Bremerton, Washington, United States", place.subtitle)
        assertEquals(47.5755192, place.coordinates.latitude, 1e-9)
    }

    @Test
    fun `named POI uses its name`() {
        val place = NominatimPlaceDto(
            lat = "39.85", lon = "-104.67", name = "Denver International Airport",
            displayName = "Denver International Airport, Denver, Colorado, United States",
            address = NominatimAddressDto(city = "Denver", state = "Colorado", country = "United States"),
        ).toGeocodedPlace()!!
        assertEquals("Denver International Airport", place.title)
        assertEquals("Denver, Colorado, United States", place.subtitle)
    }

    @Test
    fun `falls back to display name when there are no address details`() {
        val place = NominatimPlaceDto(lat = "1", lon = "2", displayName = "Somewhere, Region, Country").toGeocodedPlace()!!
        assertEquals("Somewhere", place.title)
        assertEquals("Region, Country", place.subtitle)
    }

    @Test
    fun `invalid coordinates are dropped`() {
        assertNull(NominatimPlaceDto(lat = "999", lon = "0", displayName = "x").toGeocodedPlace())
        assertNull(NominatimPlaceDto(lat = "abc", lon = "0", displayName = "x").toGeocodedPlace())
    }
}
