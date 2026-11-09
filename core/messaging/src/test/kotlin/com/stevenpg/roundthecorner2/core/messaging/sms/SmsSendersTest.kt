package com.stevenpg.roundthecorner2.core.messaging.sms

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.app.PendingIntent
import android.telephony.SmsManager
import androidx.test.core.app.ApplicationProvider
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmsSendersTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alex = RecipientDestination.Sms("+1 555 555 0100")

    /** Records what would have gone to the radio. */
    private class FakeTransport(private val supported: Boolean = true) : SmsTransport {
        var destination: String? = null
        var parts: List<String> = emptyList()
        var sentIntents: List<PendingIntent> = emptyList()

        override fun divideMessage(message: String): ArrayList<String> {
            if (!supported) throw UnsupportedOperationException("Sms is not supported")
            return ArrayList(message.chunked(153)) // GSM-7 multipart segment size.
        }

        override fun sendMultipart(destination: String, parts: ArrayList<String>, sentIntents: ArrayList<PendingIntent>) {
            this.destination = destination
            this.parts = parts
            this.sentIntents = sentIntents
        }
    }

    // --- Automatic (full flavor) ---------------------------------------------

    @Test
    fun `automatic sender refuses without the SMS permission`() = runBlocking {
        val result = AutomaticSmsSender(app, FakeTransport(), 5.seconds).send(alex, "hi") as SendResult.Failed
        assertEquals(FailureReason.PERMISSION_DENIED, result.reason)
    }

    @Test
    fun `automatic sender hands every part to SmsManager and waits for confirmation`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS)
        val transport = FakeTransport()
        val sender = AutomaticSmsSender(app, transport, confirmationTimeout = 5.seconds)
        val longText = "Almost there! ".repeat(20) // > 160 chars -> multipart

        val pending = async { sender.send(alex, longText) }
        // Let the sender register its receiver and call SmsManager.
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }

        assertNotNull("sendMultipart was not called", transport.destination)
        assertEquals("+15555550100", transport.destination)
        assertTrue(transport.parts.size > 1)
        assertEquals(transport.parts.size, transport.sentIntents.size)

        // Simulate the carrier confirming each part.
        transport.sentIntents.forEach { it.send(app, Activity.RESULT_OK, Intent()) }
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }

        assertEquals(SendResult.Sent, pending.await())
    }

    @Test
    fun `no carrier confirmation times out without claiming success`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS)
        val result = AutomaticSmsSender(app, FakeTransport(), confirmationTimeout = 50.milliseconds).send(alex, "hi") as SendResult.Failed
        assertEquals(FailureReason.UNKNOWN, result.reason)
        assertTrue("never auto-retry an SMS that may have gone out", !result.retryable)
    }

    @Test
    fun `devices that can't text report UNSUPPORTED instead of crashing`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS)
        val sender = AutomaticSmsSender(app, FakeTransport(supported = false), 5.seconds)
        val result = sender.send(alex, "hi") as SendResult.Failed
        assertEquals(FailureReason.UNSUPPORTED, result.reason)
    }

    @Test
    fun `one failed part fails the whole send`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS)
        val transport = FakeTransport()
        val pending = async { AutomaticSmsSender(app, transport, 5.seconds).send(alex, "x".repeat(400)) }
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }
        transport.sentIntents.forEachIndexed { i, intent ->
            intent.send(app, if (i == 1) SmsManager.RESULT_ERROR_NO_SERVICE else Activity.RESULT_OK, Intent())
        }
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }
        assertEquals(FailureReason.NO_SERVICE, (pending.await() as SendResult.Failed).reason)
    }

    @Test
    fun `carrier result codes map to useful reasons`() {
        assertEquals(SendResult.Sent, AutomaticSmsSender.mapResultCode(Activity.RESULT_OK))
        assertEquals(
            FailureReason.NO_SERVICE,
            (AutomaticSmsSender.mapResultCode(SmsManager.RESULT_ERROR_RADIO_OFF) as SendResult.Failed).reason,
        )
    }

    // --- Compose (play flavor) -----------------------------------------------

    @Test
    fun `interactive compose opens the messaging app pre-filled`() = runBlocking {
        val result = ComposeSmsSender(app).send(alex, "On my way", interactive = true)
        assertEquals(SendResult.AwaitingUser, result)

        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_SENDTO, started.action)
        assertEquals("smsto", started.data!!.scheme)
        assertEquals("+1 555 555 0100", started.data!!.schemeSpecificPart)
        assertEquals("On my way", started.getStringExtra("sms_body"))
    }

    @Test
    fun `background compose posts a tap-to-send notification`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val result = ComposeSmsSender(app).send(alex, "Leaving now", interactive = false)
        assertEquals(SendResult.AwaitingUser, result)

        val nm = app.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(nm).allNotifications.single()
        assertEquals(ComposeSmsSender.CHANNEL_ID, posted.channelId)
        assertNotNull(posted.contentIntent)
    }

    @Test
    fun `background compose reports blocked notifications`() = runBlocking {
        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        val result = ComposeSmsSender(app).send(alex, "x", interactive = false) as SendResult.Failed
        assertEquals(FailureReason.NOTIFICATIONS_BLOCKED, result.reason)
    }
}
