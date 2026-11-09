package com.stevenpg.roundthecorner2.core.messaging.di

import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.webhook.DiscordWebhookSender
import com.stevenpg.roundthecorner2.core.messaging.webhook.SlackWebhookSender
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * Webhook senders are the same in every build. The SMS sender differs per
 * flavor and is contributed by the app module (`app/src/full` / `app/src/play`).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MessagingModule {

    @Binds
    @IntoMap
    @MessageChannelKey(MessageChannel.SLACK)
    abstract fun bindSlack(sender: SlackWebhookSender): MessageSender

    @Binds
    @IntoMap
    @MessageChannelKey(MessageChannel.DISCORD)
    abstract fun bindDiscord(sender: DiscordWebhookSender): MessageSender
}
