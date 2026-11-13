package com.stevenpg.roundthecorner2.core.model

/** One address-search result, ready to display and to turn into a [Place]. */
data class GeocodedPlace(
    /** Primary line, e.g. "1600 Pennsylvania Avenue" or "Denver International Airport". */
    val title: String,
    /** Secondary line, e.g. "Bremerton, Washington, United States". May be empty. */
    val subtitle: String,
    /** Full single-line address, stored as the place's address. */
    val fullAddress: String,
    val coordinates: Coordinates,
)
