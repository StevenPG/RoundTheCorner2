package com.stevenpg.roundthecorner2.core.model

import java.time.Instant

/**
 * A saved location that triggers can reference. Saving it once means the user
 * only enters an address or coordinates once; editing a place's pin moves
 * every trigger that uses it.
 */
data class Place(
    val id: PlaceId,
    val name: String,
    val icon: PlaceIcon,
    val coordinates: Coordinates,
    /** Human-readable address if the place came from a search; null for coordinate-only places. */
    val address: String?,
    val createdAt: Instant,
)

@JvmInline
value class PlaceId(val value: Long)

/** Icon shown next to a place. Persisted by name, so only ever append new entries. */
enum class PlaceIcon {
    PIN,
    HOME,
    WORK,
    SCHOOL,
    FAMILY,
    FRIEND,
    GYM,
    SHOP,
    RESTAURANT,
    TRAVEL,
}
