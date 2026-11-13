package com.stevenpg.roundthecorner2.core.model

/**
 * A WGS84 position in decimal degrees.
 *
 * Construction validates the ranges, so an instance is always a real point on
 * Earth - code receiving `Coordinates` never needs to re-check. Use
 * [CoordinateParser] to turn user input into coordinates with friendly errors.
 */
data class Coordinates(
    /** Degrees north (+) / south (-), in [-90, 90]. */
    val latitude: Double,
    /** Degrees east (+) / west (-), in [-180, 180]. */
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in LATITUDE_RANGE) { "Latitude out of range: $latitude" }
        require(longitude.isFinite() && longitude in LONGITUDE_RANGE) { "Longitude out of range: $longitude" }
    }

    /** "40.712800, -74.006000" - 6 decimals is ~11 cm, more than GPS can resolve. */
    override fun toString(): String = "%.6f, %.6f".format(java.util.Locale.ROOT, latitude, longitude)

    companion object {
        val LATITUDE_RANGE = -90.0..90.0
        val LONGITUDE_RANGE = -180.0..180.0

        /** Non-throwing factory; returns null when either value is out of range. */
        fun orNull(latitude: Double, longitude: Double): Coordinates? =
            if (latitude.isFinite() && longitude.isFinite() &&
                latitude in LATITUDE_RANGE && longitude in LONGITUDE_RANGE
            ) {
                Coordinates(latitude, longitude)
            } else {
                null
            }
    }
}
