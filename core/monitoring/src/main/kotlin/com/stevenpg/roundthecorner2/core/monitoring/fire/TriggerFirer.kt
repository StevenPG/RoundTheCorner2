package com.stevenpg.roundthecorner2.core.monitoring.fire

import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.repository.SendHistoryRepository
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.messaging.MessageDispatcher
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** What happened when a trigger was asked to fire. */
enum class FireOutcome { SENT, AWAITING_USER, RETRY_SCHEDULED, FAILED, ALREADY_HANDLED }

/**
 * Sends a trigger's message exactly once.
 *
 * Order matters: the trigger is first flipped ARMED -> FIRED with an atomic
 * compare-and-set. Only the caller that wins sends, so racing location
 * updates, a "Send now" tap, or a restart can never produce a duplicate. The
 * trade-off (a crash between the flip and the send loses that one message) is
 * the safer failure for "text my partner".
 */
@Singleton
class TriggerFirer @Inject constructor(
    private val triggerRepository: TriggerRepository,
    private val dispatcher: MessageDispatcher,
    private val history: SendHistoryRepository,
    private val unitSystemProvider: UnitSystemProvider,
    private val notifier: TriggerEventNotifier,
    private val retryScheduler: RetryScheduler,
    clock: Clock,
) {
    private val renderer = MessageRenderer(clock)

    suspend fun fire(details: TriggerDetails, distance: Distance?): FireOutcome {
        val trigger = details.trigger
        if (!triggerRepository.markFired(trigger.id)) return FireOutcome.ALREADY_HANDLED
        notifier.cancelAlreadyHere(details)

        val message = renderer.render(details, distance, unitSystemProvider.unitSystem.first())
        val channel = details.recipient.destination.channel
        return when (val result = dispatcher.send(details.recipient.destination, message)) {
            SendResult.Sent -> {
                history.record(trigger.id, channel, message, SendStatus.SENT)
                notifier.sent(details, message)
                FireOutcome.SENT
            }
            SendResult.AwaitingUser -> {
                // The SMS hand-off notification is the user-facing signal here.
                history.record(trigger.id, channel, message, SendStatus.AWAITING_USER)
                FireOutcome.AWAITING_USER
            }
            is SendResult.Failed -> {
                history.record(trigger.id, channel, message, SendStatus.FAILED, result.detail)
                if (result.retryable) {
                    retryScheduler.schedule(trigger.id, message, channel)
                    notifier.willRetry(details, result.detail)
                    FireOutcome.RETRY_SCHEDULED
                } else {
                    notifier.failed(details, result.detail)
                    FireOutcome.FAILED
                }
            }
        }
    }
}
