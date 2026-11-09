package com.stevenpg.roundthecorner2.core.domain.validation

import com.stevenpg.roundthecorner2.core.domain.validation.RecipientValidation.Problem
import com.stevenpg.roundthecorner2.core.model.RecipientDestination.DiscordWebhook
import com.stevenpg.roundthecorner2.core.model.RecipientDestination.SlackWebhook
import com.stevenpg.roundthecorner2.core.model.RecipientDestination.Sms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipientValidationTest {

    @Test
    fun `phone numbers`() {
        assertNull(RecipientValidation.validate(Sms("+1 (555) 555-0100")))
        assertNull(RecipientValidation.validate(Sms("555.555.0100")))
        assertNull(RecipientValidation.validate(Sms("12345"))) // Short code
        assertEquals(Problem.BLANK, RecipientValidation.validate(Sms("  ")))
        assertEquals(Problem.INVALID_PHONE_NUMBER, RecipientValidation.validate(Sms("call me")))
        assertEquals(Problem.INVALID_PHONE_NUMBER, RecipientValidation.validate(Sms("+1234567890123456")))
    }

    @Test
    fun `slack webhooks`() {
        assertNull(RecipientValidation.validate(SlackWebhook("https://hooks.slack.com/services/T000/B000/XXXX")))
        assertEquals(Problem.NOT_HTTPS, RecipientValidation.validate(SlackWebhook("http://hooks.slack.com/services/T/B/X")))
        assertEquals(
            Problem.NOT_A_SLACK_WEBHOOK,
            RecipientValidation.validate(SlackWebhook("https://discord.com/api/webhooks/1/abc")),
        )
        assertEquals(Problem.NOT_A_SLACK_WEBHOOK, RecipientValidation.validate(SlackWebhook("not a url")))
    }

    @Test
    fun `discord webhooks`() {
        assertNull(RecipientValidation.validate(DiscordWebhook("https://discord.com/api/webhooks/123/abc")))
        assertNull(RecipientValidation.validate(DiscordWebhook("https://discordapp.com/api/webhooks/123/abc")))
        assertEquals(
            Problem.NOT_A_DISCORD_WEBHOOK,
            RecipientValidation.validate(DiscordWebhook("https://evil.example/api/webhooks/1/a")),
        )
    }
}
