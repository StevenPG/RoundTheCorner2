package com.stevenpg.roundthecorner2.core.messaging.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sends SMS directly via [SmsManager] - fully automatic, no user interaction.
 *
 * **Only bound in the `full` (sideload) flavor**, which is also the only
 * manifest declaring `SEND_SMS`. Google Play restricts that permission to
 * default-SMS apps; the `play` flavor uses [ComposeSmsSender] instead.
 *
 * Long messages are split into parts; the result waits for the carrier's
 * "sent" confirmation of every part, so failures (airplane mode, no signal)
 * are reported rather than silently lost.
 */
class AutomaticSmsSender internal constructor(
    private val context: Context,
    private val transport: SmsTransport,
    private val confirmationTimeout: Duration,
) : MessageSender {

    @Inject
    constructor(@ApplicationContext context: Context, transport: AndroidSmsTransport) :
        this(context, transport, DEFAULT_TIMEOUT)

    override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
        if (destination !is RecipientDestination.Sms) {
            return SendResult.Failed(FailureReason.UNKNOWN, "Not an SMS destination")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return SendResult.Failed(FailureReason.PERMISSION_DENIED, "SMS permission not granted")
        }
        val number = normalizePhoneNumber(context, destination.phoneNumber)
        if (number.isBlank()) return SendResult.Failed(FailureReason.DESTINATION_INVALID, "Invalid phone number")

        // Devices without telephony (Wi-Fi tablets) have no SmsManager, or it
        // throws on use (API 35+).
        val parts = try {
            transport.divideMessage(message)
        } catch (e: UnsupportedOperationException) {
            return SendResult.Failed(FailureReason.UNSUPPORTED, "This device can't send text messages")
        }

        return withTimeoutOrNull(confirmationTimeout) { sendAndAwaitConfirmation(parts, number) }
            // No confirmation in time: it may still go out, so don't retry (avoid duplicates).
            ?: SendResult.Failed(FailureReason.UNKNOWN, "No confirmation from the carrier; the message may still be delivered")
    }

    private suspend fun sendAndAwaitConfirmation(parts: ArrayList<String>, number: String): SendResult =
        suspendCancellableCoroutine { continuation ->
            // Unique action per send so concurrent sends can't see each other's results.
            val action = "${context.packageName}.SMS_SENT.${UUID.randomUUID()}"
            val remaining = AtomicInteger(parts.size)
            val firstErrorCode = AtomicInteger(Activity.RESULT_OK)
            val registered = AtomicBoolean(true)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (resultCode != Activity.RESULT_OK) firstErrorCode.compareAndSet(Activity.RESULT_OK, resultCode)
                    if (remaining.decrementAndGet() == 0) {
                        unregister(this, registered)
                        if (continuation.isActive) continuation.resume(mapResultCode(firstErrorCode.get()))
                    }
                }
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            continuation.invokeOnCancellation { unregister(receiver, registered) }

            val sentIntents = ArrayList(
                parts.indices.map { index ->
                    PendingIntent.getBroadcast(
                        context,
                        index,
                        Intent(action).setPackage(context.packageName),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
                    )
                },
            )
            try {
                transport.sendMultipart(number, parts, sentIntents)
            } catch (e: IllegalArgumentException) {
                unregister(receiver, registered)
                continuation.resume(SendResult.Failed(FailureReason.DESTINATION_INVALID, "Invalid phone number"))
            } catch (e: UnsupportedOperationException) {
                unregister(receiver, registered)
                continuation.resume(SendResult.Failed(FailureReason.UNSUPPORTED, "This device can't send text messages"))
            }
        }

    private fun unregister(receiver: BroadcastReceiver, registered: AtomicBoolean) {
        if (registered.compareAndSet(true, false)) context.unregisterReceiver(receiver)
    }

    companion object {
        val DEFAULT_TIMEOUT = 60.seconds

        /** Maps SmsManager's "sent" result codes to our failure reasons. */
        internal fun mapResultCode(code: Int): SendResult = when (code) {
            Activity.RESULT_OK -> SendResult.Sent
            SmsManager.RESULT_ERROR_RADIO_OFF, SmsManager.RESULT_ERROR_NO_SERVICE ->
                SendResult.Failed(FailureReason.NO_SERVICE, "No cellular service (code $code)")
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ->
                SendResult.Failed(FailureReason.RATE_LIMITED, "Too many texts sent recently (code $code)")
            SmsManager.RESULT_INVALID_SMS_FORMAT, SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE ->
                SendResult.Failed(FailureReason.DESTINATION_INVALID, "The carrier rejected this number (code $code)")
            else -> SendResult.Failed(FailureReason.UNKNOWN, "SMS failed (code $code)")
        }
    }
}
