package com.stevenpg.roundthecorner2.feature.triggers

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "5:42 PM" if today, otherwise "Nov 9, 5:42 PM" - localised. */
internal fun formatWhen(instant: Instant, zone: ZoneId, today: LocalDate = LocalDate.now(zone)): String {
    val local = instant.atZone(zone)
    val formatter = if (local.toLocalDate() == today) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    return local.format(formatter)
}
