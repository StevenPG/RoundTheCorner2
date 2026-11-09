package com.stevenpg.roundthecorner2.core.domain.format

import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts between the radius text field and meters. The field is in miles
 * (imperial) or kilometres (metric); decimals may use '.' or ',' so it works
 * whatever the keyboard's locale.
 */
object RadiusInput {

    /** Quick-pick values, in the display unit. The same numbers read naturally in mi and km. */
    val PRESETS = listOf(0.1, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0)

    fun unitLabel(system: ResolvedUnitSystem) = if (system == ResolvedUnitSystem.IMPERIAL) "mi" else "km"

    fun toDistance(value: Double, system: ResolvedUnitSystem): Distance = when (system) {
        ResolvedUnitSystem.IMPERIAL -> Distance.miles(value)
        ResolvedUnitSystem.METRIC -> Distance.kilometers(value)
    }

    /** Parses the field; null if it isn't a positive number. */
    fun parse(text: String, system: ResolvedUnitSystem): Distance? {
        val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0) return null
        return toDistance(value, system)
    }

    /** Renders a distance for the field: up to 2 decimals, no trailing zeros ("0.25", "2", "1.5"). */
    fun format(distance: Distance, system: ResolvedUnitSystem): String {
        val value = if (system == ResolvedUnitSystem.IMPERIAL) distance.miles else distance.kilometers
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    /**
     * A configured radius for display ("0.25 mi", "2 km") - echoes exactly what
     * the user entered, unlike [DistanceFormatter] which rounds live distances.
     */
    fun display(distance: Distance, system: ResolvedUnitSystem): String = "${format(distance, system)} ${unitLabel(system)}"
}
