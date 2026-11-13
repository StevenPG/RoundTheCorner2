package com.stevenpg.roundthecorner2.core.domain.validation

import com.stevenpg.roundthecorner2.core.model.RecipientDestination

/**
 * Checks a recipient destination before it is saved. Returns the first
 * problem found, or null if it looks usable. Deliberately lenient on phone
 * formatting - carriers accept many shapes; the SMS sender normalises later.
 */
object RecipientValidation {

    enum class Problem {
        BLANK,
        INVALID_PHONE_NUMBER,
        NOT_HTTPS,
        NOT_A_SLACK_WEBHOOK,
        NOT_A_DISCORD_WEBHOOK,
    }

    /** Digits plus common punctuation; 3 (short codes) to 15 (E.164 max) digits. */
    private val PHONE_CHARS = Regex("""^\+?[0-9 ()\-.]+$""")

    private val SLACK_HOSTS = setOf("hooks.slack.com")
    private val DISCORD_HOSTS = setOf("discord.com", "discordapp.com", "canary.discord.com", "ptb.discord.com")

    fun validate(destination: RecipientDestination): Problem? {
        val value = destination.address.trim()
        if (value.isEmpty()) return Problem.BLANK
        return when (destination) {
            is RecipientDestination.Sms -> validatePhone(value)
            is RecipientDestination.SlackWebhook -> validateWebhook(value, SLACK_HOSTS, "/services/", Problem.NOT_A_SLACK_WEBHOOK)
            is RecipientDestination.DiscordWebhook -> validateWebhook(value, DISCORD_HOSTS, "/api/webhooks/", Problem.NOT_A_DISCORD_WEBHOOK)
        }
    }

    private fun validatePhone(value: String): Problem? {
        val digits = value.count(Char::isDigit)
        return if (PHONE_CHARS.matches(value) && digits in 3..15) null else Problem.INVALID_PHONE_NUMBER
    }

    private fun validateWebhook(value: String, hosts: Set<String>, pathPrefix: String, wrongService: Problem): Problem? {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return wrongService
        if (!uri.scheme.equals("https", ignoreCase = true)) return Problem.NOT_HTTPS
        val hostOk = uri.host?.lowercase() in hosts
        val pathOk = uri.rawPath?.startsWith(pathPrefix) == true && uri.rawPath.length > pathPrefix.length
        return if (hostOk && pathOk) null else wrongService
    }
}
