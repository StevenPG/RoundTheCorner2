package com.stevenpg.roundthecorner2.core.monitoring.notification

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.monitoring.R
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerEventNotifier
import com.stevenpg.roundthecorner2.core.monitoring.service.MonitoringIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Posts per-trigger event notifications. One notification id per trigger and
 * kind, so a retry that later succeeds *replaces* "couldn't send" rather than
 * stacking up.
 */
class AndroidTriggerEventNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : TriggerEventNotifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun sent(details: TriggerDetails, message: String) = post(
        resultId(details),
        builder(R.drawable.ic_stat_done)
            .setContentTitle(context.getString(R.string.monitor_sent_title, details.recipient.name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)),
    )

    override fun willRetry(details: TriggerDetails, reason: String) = post(
        resultId(details),
        builder(R.drawable.ic_stat_monitoring)
            .setContentTitle(context.getString(R.string.monitor_retry_title, details.recipient.name))
            .setContentText(context.getString(R.string.monitor_retry_text, reason))
            .setSilent(true),
    )

    override fun failed(details: TriggerDetails, reason: String) = post(
        resultId(details),
        builder(R.drawable.ic_stat_monitoring)
            .setContentTitle(context.getString(R.string.monitor_failed_title, details.recipient.name))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason)),
    )

    override fun alreadyHere(details: TriggerDetails) {
        val title = context.getString(
            if (details.trigger.direction == TriggerDirection.ARRIVE) {
                R.string.monitor_already_here_arrive_title
            } else {
                R.string.monitor_already_here_leave_title
            },
            details.place.name,
        )
        val text = context.getString(R.string.monitor_already_here_text)
        post(
            alreadyHereId(details),
            builder(R.drawable.ic_stat_monitoring)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .addAction(
                    0,
                    context.getString(R.string.monitor_action_send_now),
                    MonitoringIntents.action(context, MonitoringIntents.ACTION_SEND_NOW, details.trigger.id),
                )
                .addAction(
                    0,
                    context.getString(R.string.monitor_action_keep_waiting),
                    MonitoringIntents.action(context, MonitoringIntents.ACTION_KEEP_WAITING, details.trigger.id),
                ),
        )
    }

    override fun cancelAlreadyHere(details: TriggerDetails) = manager.cancel(alreadyHereId(details))

    private fun builder(icon: Int) = NotificationCompat.Builder(context, NotificationChannels.EVENTS)
        .setSmallIcon(icon)
        .setColor(StatusNotificationFactory.BRAND_COLOR)
        .setAutoCancel(true)
        .setContentIntent(MonitoringIntents.openApp(context))

    @SuppressLint("MissingPermission") // areNotificationsEnabled() covers POST_NOTIFICATIONS.
    private fun post(id: Int, builder: NotificationCompat.Builder) {
        if (!manager.areNotificationsEnabled()) return
        NotificationChannels.ensureCreated(context)
        manager.notify(id, builder.build())
    }

    private fun resultId(details: TriggerDetails) = RESULT_BASE + details.trigger.id.value.toInt()
    private fun alreadyHereId(details: TriggerDetails) = ALREADY_HERE_BASE + details.trigger.id.value.toInt()

    private companion object {
        const val RESULT_BASE = 20_000
        const val ALREADY_HERE_BASE = 30_000
    }
}
