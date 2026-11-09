package com.stevenpg.roundthecorner2.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stevenpg.roundthecorner2.core.model.MessageChannel

/*
 * Generic Material icons rather than brand logos: avoids trademark issues and
 * keeps the visual language consistent.
 */
val MessageChannel.imageVector: ImageVector
    get() = when (this) {
        MessageChannel.SMS -> Icons.Outlined.Sms
        MessageChannel.SLACK -> Icons.Outlined.Tag
        MessageChannel.DISCORD -> Icons.Outlined.Forum
    }

@get:StringRes
val MessageChannel.label: Int
    get() = when (this) {
        MessageChannel.SMS -> R.string.channel_sms
        MessageChannel.SLACK -> R.string.channel_slack
        MessageChannel.DISCORD -> R.string.channel_discord
    }

/** Channel icon in a tertiary-tinted circle (places use secondary, so the two read differently). */
@Composable
fun ChannelBadge(channel: MessageChannel, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    IconBadge(
        icon = channel.imageVector,
        modifier = modifier,
        size = size,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}
