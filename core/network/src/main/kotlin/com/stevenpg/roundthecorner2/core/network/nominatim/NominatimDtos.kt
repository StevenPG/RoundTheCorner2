package com.stevenpg.roundthecorner2.core.network.nominatim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry of Nominatim's `/search?format=jsonv2&addressdetails=1` response.
 * Only the fields we use are declared; the JSON parser ignores the rest.
 * Nominatim returns coordinates as *strings*.
 */
@Serializable
data class NominatimPlaceDto(
    val lat: String,
    val lon: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String,
    val address: NominatimAddressDto? = null,
)

@Serializable
data class NominatimAddressDto(
    @SerialName("house_number") val houseNumber: String? = null,
    val road: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val hamlet: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
)
