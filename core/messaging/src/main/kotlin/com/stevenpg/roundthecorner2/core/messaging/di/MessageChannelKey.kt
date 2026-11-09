package com.stevenpg.roundthecorner2.core.messaging.di

import com.stevenpg.roundthecorner2.core.model.MessageChannel
import dagger.MapKey

/** Map-multibinding key: `@Binds @IntoMap @MessageChannelKey(MessageChannel.SLACK)`. */
@MapKey
annotation class MessageChannelKey(val value: MessageChannel)
