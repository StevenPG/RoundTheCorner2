package com.stevenpg.roundthecorner2.core.monitoring.fire

import com.stevenpg.roundthecorner2.core.domain.format.DistanceFormatter
import com.stevenpg.roundthecorner2.core.domain.format.RadiusInput
import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Fills a trigger's message template with live values at send time. */
class MessageRenderer(
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
) {
    private val distanceFormatter = DistanceFormatter(locale)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    /**
     * @param distance current distance from the place; falls back to the
     *   radius (e.g. "Send now" before any fix).
     */
    fun render(details: TriggerDetails, distance: Distance?, units: ResolvedUnitSystem): String {
        val radius = details.trigger.radius
        return MessageTemplate.render(
            details.trigger.messageTemplate,
            MessageTemplate.Values(
                place = details.place.name,
                distance = distanceFormatter.format(distance ?: radius, units),
                radius = RadiusInput.display(radius, units),
                time = LocalTime.now(clock.withZone(zone)).format(timeFormatter),
            ),
        )
    }
}
