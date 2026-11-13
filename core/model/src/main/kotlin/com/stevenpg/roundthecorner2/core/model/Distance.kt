package com.stevenpg.roundthecorner2.core.model

/**
 * A distance, always stored in meters. Wrapping the Double (zero runtime cost:
 * `@JvmInline value class`) stops meters and miles being mixed up by accident.
 */
@JvmInline
value class Distance(val meters: Double) : Comparable<Distance> {

    init {
        require(!meters.isNaN()) { "Distance cannot be NaN" }
    }

    val kilometers: Double get() = meters / METERS_PER_KILOMETER
    val miles: Double get() = meters / METERS_PER_MILE
    val feet: Double get() = meters / METERS_PER_FOOT

    operator fun plus(other: Distance) = Distance(meters + other.meters)
    operator fun minus(other: Distance) = Distance(meters - other.meters)
    operator fun times(factor: Double) = Distance(meters * factor)
    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

    companion object {
        const val METERS_PER_KILOMETER = 1_000.0
        const val METERS_PER_MILE = 1_609.344 // International mile, exact.
        const val METERS_PER_FOOT = 0.3048 // International foot, exact.

        val ZERO = Distance(0.0)

        fun meters(value: Double) = Distance(value)
        fun kilometers(value: Double) = Distance(value * METERS_PER_KILOMETER)
        fun miles(value: Double) = Distance(value * METERS_PER_MILE)
        fun feet(value: Double) = Distance(value * METERS_PER_FOOT)
    }
}

val Number.meters: Distance get() = Distance(toDouble())
val Number.kilometers: Distance get() = Distance.kilometers(toDouble())
val Number.miles: Distance get() = Distance.miles(toDouble())
val Number.feet: Distance get() = Distance.feet(toDouble())
