package com.stevenpg.roundthecorner2.feature.places

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.PlaceId
import java.time.Instant

internal object TestData {
    val home = Place(
        id = PlaceId(1),
        name = "Home",
        icon = PlaceIcon.HOME,
        coordinates = Coordinates(39.7392, -104.9903),
        address = "1437 Bannock St, Denver, Colorado, United States",
        createdAt = Instant.EPOCH,
    )
    val office = Place(
        id = PlaceId(2),
        name = "Office",
        icon = PlaceIcon.WORK,
        coordinates = Coordinates(39.7508, -104.9966),
        address = "1801 California St, Denver, Colorado, United States",
        createdAt = Instant.EPOCH,
    )
    val trailhead = Place(
        id = PlaceId(3),
        name = "Mount Falcon trailhead",
        icon = PlaceIcon.TRAVEL,
        coordinates = Coordinates(39.6386, -105.2393),
        address = null, // Coordinate-only place.
        createdAt = Instant.EPOCH,
    )

    val searchResults = listOf(
        GeocodedPlace(
            title = "Denver International Airport",
            subtitle = "Denver, Colorado, United States",
            fullAddress = "Denver International Airport, 8500 Peña Blvd, Denver, Colorado, United States",
            coordinates = Coordinates(39.8561, -104.6737),
        ),
        GeocodedPlace(
            title = "Denver Union Station",
            subtitle = "Denver, Colorado, United States",
            fullAddress = "Denver Union Station, 1701 Wynkoop St, Denver, Colorado, United States",
            coordinates = Coordinates(39.7527, -105.0002),
        ),
    )
}
