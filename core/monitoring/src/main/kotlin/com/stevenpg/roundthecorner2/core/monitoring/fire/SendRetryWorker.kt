package com.stevenpg.roundthecorner2.core.monitoring.fire

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stevenpg.roundthecorner2.core.data.repository.SendHistoryRepository
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.messaging.MessageDispatcher
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Retries a send that failed for a transient reason (no network, 429, 5xx,
 * no cell service). WorkManager persists the job, so it survives the app
 * being killed or the phone rebooting, and applies exponential backoff.
 *
 * The *already rendered* message is retried, so "{time}" still says when the
 * trigger actually fired.
 */
@HiltWorker
class SendRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val triggerRepository: TriggerRepository,
    private val dispatcher: MessageDispatcher,
    private val history: SendHistoryRepository,
    private val notifier: TriggerEventNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val triggerId = TriggerId(inputData.getLong(KEY_TRIGGER_ID, -1))
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()
        // Deleted since? Nothing to do.
        val details = triggerRepository.observeTrigger(triggerId).first() ?: return Result.success()
        val channel = details.recipient.destination.channel

        return when (val result = dispatcher.send(details.recipient.destination, message)) {
            SendResult.Sent -> {
                history.record(triggerId, channel, message, SendStatus.SENT)
                notifier.sent(details, message)
                Result.success()
            }
            SendResult.AwaitingUser -> {
                history.record(triggerId, channel, message, SendStatus.AWAITING_USER)
                Result.success()
            }
            is SendResult.Failed -> {
                history.record(triggerId, channel, message, SendStatus.FAILED, result.detail)
                if (result.retryable && runAttemptCount + 1 < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    notifier.failed(details, result.detail)
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val KEY_TRIGGER_ID = "trigger_id"
        const val KEY_MESSAGE = "message"
        const val MAX_ATTEMPTS = 5
    }
}

/** [RetryScheduler] backed by WorkManager: one unique job per trigger. */
class WorkManagerRetryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : RetryScheduler {
    override fun schedule(triggerId: TriggerId, message: String, channel: MessageChannel) {
        val constraints = Constraints.Builder()
            // Webhooks need the internet; SMS only needs the radio, which WorkManager can't express.
            .setRequiredNetworkType(if (channel == MessageChannel.SMS) NetworkType.NOT_REQUIRED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SendRetryWorker>()
            .setInputData(workDataOf(SendRetryWorker.KEY_TRIGGER_ID to triggerId.value, SendRetryWorker.KEY_MESSAGE to message))
            .setConstraints(constraints)
            .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("send-retry-${triggerId.value}", ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val INITIAL_DELAY_SECONDS = 30L
    }
}
