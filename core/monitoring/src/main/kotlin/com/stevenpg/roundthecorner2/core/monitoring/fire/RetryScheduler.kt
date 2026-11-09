package com.stevenpg.roundthecorner2.core.monitoring.fire

import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.TriggerId

/** Schedules a durable retry of a send that failed for a transient reason. */
fun interface RetryScheduler {
    fun schedule(triggerId: TriggerId, message: String, channel: MessageChannel)
}
