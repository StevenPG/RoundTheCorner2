package com.stevenpg.roundthecorner2.core.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.monitoring.fire.SendRetryWorker
import com.stevenpg.roundthecorner2.core.testing.FakeMessageSender
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.FakeSendHistoryRepository
import com.stevenpg.roundthecorner2.core.testing.FakeTriggerRepository
import com.stevenpg.roundthecorner2.core.testing.fakeDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SendRetryWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val places = FakePlaceRepository(listOf(Fixtures.home))
    private val recipients = FakeRecipientRepository(listOf(Fixtures.alex))
    private val triggers = FakeTriggerRepository(places, recipients, listOf(Fixtures.homeArrive))
    private val history = FakeSendHistoryRepository()
    private val sender = FakeMessageSender()
    private val notifier = RecordingNotifier()

    /** Builds the worker the way HiltWorkerFactory would, with our fakes. */
    private fun worker(attempt: Int = 0) = TestListenableWorkerBuilder<SendRetryWorker>(context)
        .setInputData(workDataOf(SendRetryWorker.KEY_TRIGGER_ID to 1L, SendRetryWorker.KEY_MESSAGE to "Almost home"))
        .setRunAttemptCount(attempt)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                SendRetryWorker(appContext, workerParameters, triggers, fakeDispatcher(sender), history, notifier)
        })
        .build()

    @Test
    fun `success records the send and resends the original text`() = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals("Almost home", sender.calls.single().message)
        assertEquals(SendStatus.SENT, history.observeRecent().first().single().status)
        assertTrue(notifier.events.contains("sent:Almost home"))
    }

    @Test
    fun `transient failure asks WorkManager to retry with backoff`() = runBlocking {
        sender.result = SendResult.Failed(FailureReason.SERVER_ERROR, "Slack error (HTTP 503)")
        assertEquals(ListenableWorker.Result.retry(), worker(attempt = 1).doWork())
        assertTrue(notifier.events.isEmpty())
    }

    @Test
    fun `gives up after the last attempt and tells the user`() = runBlocking {
        sender.result = SendResult.Failed(FailureReason.NETWORK, "offline")
        val result = worker(attempt = SendRetryWorker.MAX_ATTEMPTS - 1).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
        assertTrue(notifier.events.contains("failed:offline"))
    }

    @Test
    fun `a deleted trigger ends quietly`() = runBlocking {
        triggers.deleteTrigger(Fixtures.homeArrive.id)
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(sender.calls.isEmpty())
    }
}
