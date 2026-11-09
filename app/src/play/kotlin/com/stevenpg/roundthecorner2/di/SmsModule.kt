package com.stevenpg.roundthecorner2.di

import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.messaging.di.MessageChannelKey
import com.stevenpg.roundthecorner2.core.messaging.sms.ComposeSmsSender
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * Play build: SMS opens the user's messaging app pre-filled (tap to send), because Play restricts SEND_SMS.
 *
 * The other flavor has a file with the same name and package; Gradle compiles
 * exactly one of them, so the rest of the app never checks the flavor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SmsModule {

    @Binds
    @IntoMap
    @MessageChannelKey(MessageChannel.SMS)
    abstract fun bindSmsSender(sender: ComposeSmsSender): MessageSender

    companion object {
        @Provides
        fun provideSmsMode(): SmsMode = SmsMode.TAP_TO_SEND
    }
}
