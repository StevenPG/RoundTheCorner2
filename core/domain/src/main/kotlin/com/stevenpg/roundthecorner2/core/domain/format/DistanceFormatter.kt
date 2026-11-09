package com.stevenpg.roundthecorner2.core.domain.format

import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Human-friendly distances: "350 ft", "2.3 mi", "12 mi", "850 m", "4.2 km".
 *
 * Precision drops as distance grows - nobody needs "12.37 mi". Decimal
 * separators follow [locale] ("2,3 km" in Germany).
 *
 * | Imperial          | Metric           |
 * |-------------------|------------------|
 * | < 0.1 mi -> feet  | < 1 km -> meters |
 * | < 10 mi -> 0.0 mi | < 10 km -> 0.0 km|
 * | else -> whole mi  | else -> whole km |
 *
 * Short distances round to 5 (under 100) or 10 units, matching GPS precision.
 */
class DistanceFormatter(private val locale: Locale = Locale.getDefault()) {

    fun format(distance: Distance, system: ResolvedUnitSystem): String {
        val meters = distance.meters.coerceAtLeast(0.0)
        return when (system) {
            ResolvedUnitSystem.IMPERIAL -> {
                val miles = Distance(meters).miles
                when {
                    miles < 0.1 -> "${roundShort(Distance(meters).feet)} ft"
                    miles < 9.95 -> "${oneDecimal(miles)} mi" // 9.95+ would print "10.0"
                    else -> "${whole(miles)} mi"
                }
            }
            ResolvedUnitSystem.METRIC -> {
                val km = meters / Distance.METERS_PER_KILOMETER
                when {
                    meters < 1_000 -> "${roundShort(meters)} m"
                    km < 9.95 -> "${oneDecimal(km)} km"
                    else -> "${whole(km)} km"
                }
            }
        }
    }

    private fun roundShort(value: Double): String {
        val step = if (value < 100) 5 else 10
        return whole((value / step).roundToLong() * step.toDouble())
    }

    private fun oneDecimal(value: Double): String = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)

    private fun whole(value: Double): String = NumberFormat.getIntegerInstance(locale).format(value.roundToLong())
}
