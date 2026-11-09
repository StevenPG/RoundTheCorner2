package com.stevenpg.roundthecorner2.core.messaging.sms

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.MessageSender
import com.stevenpg.roundthecorner2.core.messaging.R
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Play-safe SMS: never sends by itself. It opens the user's messaging app with
 * the recipient and text pre-filled, so sending takes one tap. Needs no
 * `SEND_SMS` permission, which Google Play reserves for default-SMS apps.
 *
 * - [interactive] (user is in the app): opens the composer immediately.
 * - background (a trigger fired): posts a high-priority notification; tapping
 *   it opens the composer. Android blocks starting activities from the
 *   background, so a notification is the only legitimate route.
 */
class ComposeSmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
) : MessageSender {

    override suspend fun send(destination: RecipientDestination, message: String, interactive: Boolean): SendResult {
        if (destination !is RecipientDestination.Sms) {
            return SendResult.Failed(FailureReason.UNKNOWN, "Not an SMS destination")
        }
        val composeIntent = composeIntent(destination.phoneNumber, message)
        return if (interactive) openComposer(composeIntent) else postHandoffNotification(destination, message, composeIntent)
    }

    private fun openComposer(intent: Intent): SendResult = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        SendResult.AwaitingUser
    } catch (e: ActivityNotFoundException) {
        SendResult.Failed(FailureReason.UNKNOWN, "No messaging app installed")
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() covers POST_NOTIFICATIONS on API 33+.
    private fun postHandoffNotification(destination: RecipientDestination.Sms, message: String, intent: Intent): SendResult {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) {
            return SendResult.Failed(FailureReason.NOTIFICATIONS_BLOCKED, "Notifications are off, so the text can't be handed to you")
        }
        ensureChannel()

        // Distinct request codes so several pending texts don't overwrite each other.
        val requestCode = (destination.phoneNumber + message).hashCode()
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(context.getString(R.string.messaging_sms_handoff_title, destination.phoneNumber))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        notifications.notify(requestCode, notification)
        return SendResult.AwaitingUser
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.messaging_sms_channel_name),
                NotificationManager.IMPORTANCE_HIGH, // Heads-up: the user needs to act now.
            ).apply { description = context.getString(R.string.messaging_sms_channel_description) },
        )
    }

    companion object {
        const val CHANNEL_ID = "sms_handoff"

        /** `smsto:` + `sms_body` is understood by every mainstream messaging app. */
        fun composeIntent(phoneNumber: String, message: String): Intent =
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phoneNumber, null))
                .putExtra("sms_body", message)
    }
}
