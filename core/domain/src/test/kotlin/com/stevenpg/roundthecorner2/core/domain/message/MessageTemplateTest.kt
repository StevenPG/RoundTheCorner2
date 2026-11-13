package com.stevenpg.roundthecorner2.core.domain.message

import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate.Placeholder
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTemplateTest {

    private val values = MessageTemplate.Values(place = "Home", distance = "1.8 mi", radius = "2 mi", time = "5:42 PM")

    @Test
    fun `replaces known placeholders case-insensitively`() {
        assertEquals(
            "About 1.8 mi from Home (5:42 PM)",
            MessageTemplate.render("About {distance} from {PLACE} ({Time})", values),
        )
    }

    @Test
    fun `leaves unknown tokens and plain braces alone`() {
        assertEquals("Hi {name} :-{ } Home", MessageTemplate.render("Hi {name} :-{ } {place}", values))
    }

    @Test
    fun `lists placeholders in use`() {
        assertEquals(
            setOf(Placeholder.PLACE, Placeholder.RADIUS),
            MessageTemplate.placeholdersIn("Within {radius} of {place}, {place}!"),
        )
    }
}
