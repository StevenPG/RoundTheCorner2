package com.stevenpg.roundthecorner2.feature.recipients

import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import java.time.Instant

internal object TestData {
    const val SLACK_URL = "https://hooks.slack.com/services/T0001/B0001/abcdEFGHijkl"
    const val DISCORD_URL = "https://discord.com/api/webhooks/123456/abcdefXYZ9"

    val alex = Recipient(RecipientId(1), "Alex", RecipientDestination.Sms("+1 (555) 555-0100"), Instant.EPOCH)
    val family = Recipient(RecipientId(2), "#family", RecipientDestination.SlackWebhook(SLACK_URL), Instant.EPOCH)
    val raidChat = Recipient(RecipientId(3), "Ride share chat", RecipientDestination.DiscordWebhook(DISCORD_URL), Instant.EPOCH)
    val restored = Recipient(
        RecipientId(4), "Mom", RecipientDestination.Sms(""), Instant.EPOCH, needsReentry = true,
    )
}
