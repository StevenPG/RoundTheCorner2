package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimAddressDto
import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimClient
import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimPlaceDto
import javax.inject.Inject

/** [GeocodingRepository] backed by OpenStreetMap Nominatim. */
internal class NominatimGeocodingRepository @Inject constructor(
    private val client: NominatimClient,
) : GeocodingRepository {

    override suspend fun search(query: String): List<GeocodedPlace> =
        client.search(query.trim()).mapNotNull { it.toGeocodedPlace() }
}

/**
 * Turns Nominatim's verbose result into a two-line display:
 * - title: the POI name, else "house number + road", else the first address part;
 * - subtitle: locality, state/region, country.
 */
internal fun NominatimPlaceDto.toGeocodedPlace(): GeocodedPlace? {
    val coordinates = Coordinates.orNull(
        lat.toDoubleOrNull() ?: return null,
        lon.toDoubleOrNull() ?: return null,
    ) ?: return null

    val street = address?.let { listOfNotNull(it.houseNumber, it.road).joinToString(" ") }.orEmpty()
    val title = name?.takeIf { it.isNotBlank() }
        ?: street.takeIf { it.isNotBlank() }
        ?: displayName.substringBefore(',').trim()

    val subtitle = address?.subtitleParts()
        ?.filter { it != title }
        ?.joinToString(", ")
        ?: displayName.substringAfter(',', "").trim()

    return GeocodedPlace(title = title, subtitle = subtitle, fullAddress = displayName, coordinates = coordinates)
}

private fun NominatimAddressDto.subtitleParts(): List<String> = listOfNotNull(
    city ?: town ?: village ?: hamlet ?: suburb ?: county,
    state,
    country,
).distinct()
