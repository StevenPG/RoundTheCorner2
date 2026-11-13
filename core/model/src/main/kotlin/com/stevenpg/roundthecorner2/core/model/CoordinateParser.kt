package com.stevenpg.roundthecorner2.core.model

/**
 * Parses coordinates typed or pasted by the user, so that a place can be
 * created from coordinates alone (no address lookup required).
 *
 * Accepted formats (case-insensitive, flexible whitespace):
 * - Signed decimal:   `40.7128, -74.0060`   `40.7128 -74.0060`   `40.7128;-74.0060`
 * - Hemisphere:       `40.7128° N, 74.0060° W`   `N40.7128 W74.0060`
 * - Degrees/min/sec:  `40°42'46.1"N 74°00'21.6"W`  (as copied from Google Maps)
 * - Degrees/decimal minutes: `40°42.768'N 74°0.36'W`
 *
 * Order is always latitude first, longitude second (the convention used by
 * every major map app). A hemisphere letter overrides the sign.
 */
object CoordinateParser {

    sealed interface Result {
        data class Success(val coordinates: Coordinates) : Result
        data class Failure(val error: Error) : Result
    }

    enum class Error {
        EMPTY,
        UNRECOGNIZED_FORMAT,
        LATITUDE_OUT_OF_RANGE,
        LONGITUDE_OUT_OF_RANGE,
    }

    /**
     * One coordinate component: optional leading hemisphere, signed degrees,
     * optional minutes and seconds, optional trailing hemisphere.
     * Unicode primes (′ ″) and typographic quotes are normalised beforehand.
     */
    private val COMPONENT = Regex(
        """([NSEW])?\s*([+-]?\d+(?:\.\d+)?)\s*°?\s*""" +
            """(?:(\d+(?:\.\d+)?)\s*'\s*)?""" +
            """(?:(\d+(?:\.\d+)?)\s*"\s*)?""" +
            """([NSEW])?""",
    )

    /** Two components separated by a comma, semicolon or whitespace. */
    private val PAIR = Regex("""^\s*(${COMPONENT.pattern})\s*[,;\s]\s*(${COMPONENT.pattern})\s*$""")

    fun parse(input: String): Result {
        val text = normalise(input)
        if (text.isEmpty()) return Result.Failure(Error.EMPTY)

        val match = PAIR.matchEntire(text) ?: return Result.Failure(Error.UNRECOGNIZED_FORMAT)
        // Group layout: 1 = whole first component, 2..6 its parts,
        //               7 = whole second component, 8..12 its parts.
        val g = match.groupValues
        val lat = component(g[2], g[3], g[4], g[5], g[6], isLatitude = true)
            ?: return Result.Failure(Error.UNRECOGNIZED_FORMAT)
        val lng = component(g[8], g[9], g[10], g[11], g[12], isLatitude = false)
            ?: return Result.Failure(Error.UNRECOGNIZED_FORMAT)

        if (lat !in Coordinates.LATITUDE_RANGE) return Result.Failure(Error.LATITUDE_OUT_OF_RANGE)
        if (lng !in Coordinates.LONGITUDE_RANGE) return Result.Failure(Error.LONGITUDE_OUT_OF_RANGE)
        return Result.Success(Coordinates(lat, lng))
    }

    /** Convenience for callers that only care about success. */
    fun parseOrNull(input: String): Coordinates? = (parse(input) as? Result.Success)?.coordinates

    private fun normalise(input: String): String = input.trim()
        .uppercase()
        .replace('′', '\'').replace('’', '\'').replace('‘', '\'')
        .replace('″', '"').replace('”', '"').replace('“', '"')
        .replace("''", "\"") // Two single primes typed as seconds.
        .replace('º', '°') // Masculine ordinal, a common degree look-alike.

    /**
     * Converts one parsed component to signed decimal degrees, or null when it
     * is internally inconsistent (e.g. two hemisphere letters, a latitude with
     * E/W, minutes >= 60, or a sign combined with a hemisphere letter).
     */
    private fun component(
        leadingHemisphere: String,
        degreesText: String,
        minutesText: String,
        secondsText: String,
        trailingHemisphere: String,
        isLatitude: Boolean,
    ): Double? {
        if (leadingHemisphere.isNotEmpty() && trailingHemisphere.isNotEmpty()) return null
        val hemisphere = leadingHemisphere.ifEmpty { trailingHemisphere }

        val degrees = degreesText.toDoubleOrNull() ?: return null
        val minutes = minutesText.takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
        val seconds = secondsText.takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
        if (minutes >= 60 || seconds >= 60) return null
        // Seconds without minutes, or fractional degrees with minutes, is ambiguous.
        if (secondsText.isNotEmpty() && minutesText.isEmpty()) return null
        if (minutesText.isNotEmpty() && degreesText.contains('.')) return null

        val hasSign = degreesText.startsWith("-") || degreesText.startsWith("+")
        if (hasSign && hemisphere.isNotEmpty()) return null

        val magnitude = kotlin.math.abs(degrees) + minutes / 60.0 + seconds / 3600.0
        val negative = when (hemisphere) {
            "" -> degreesText.startsWith("-")
            "N", "E" -> false
            "S", "W" -> true
            else -> return null
        }
        // A latitude can't be E/W and a longitude can't be N/S.
        if (isLatitude && hemisphere in setOf("E", "W")) return null
        if (!isLatitude && hemisphere in setOf("N", "S")) return null

        return if (negative) -magnitude else magnitude
    }
}
