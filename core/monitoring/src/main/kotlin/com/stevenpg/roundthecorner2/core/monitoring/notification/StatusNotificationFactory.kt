package com.stevenpg.roundthecorner2.core.monitoring.notification

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.stevenpg.roundthecorner2.core.domain.format.DistanceFormatter
import com.stevenpg.roundthecorner2.core.domain.monitoring.Tick
import com.stevenpg.roundthecorner2.core.domain.monitoring.TriggerProgress
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.monitoring.R
import com.stevenpg.roundthecorner2.core.monitoring.service.MonitoringIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Builds the ongoing "how far until it sends" notification for the
 * foreground service.
 *
 * - **Android 16+**: `ProgressStyle` with a segmented, tracker-icon bar. From
 *   16 QPR2 (API 36.1) it is also a *Live Update* - promoted to the top of the
 *   shade and lock screen, with the distance left as a status-bar chip
 *   (e.g. "1.8 mi").
 * - **Older versions**: a standard notification with a progress bar.
 *
 * The notification follows the trigger closest to firing; others are counted.
 */
class StatusNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val formatter = DistanceFormatter()

    /** Shown immediately on start (Android requires a notification within seconds). */
    fun starting(): Notification =
        NotificationCompat.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_stat_monitoring)
            .setContentTitle(context.getString(R.string.monitor_starting))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(BRAND_COLOR)
            .setContentIntent(MonitoringIntents.openApp(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun build(tick: Tick?, units: ResolvedUnitSystem): Notification {
        val nearest = tick?.progress?.firstOrNull() ?: return starting()
        val content = content(nearest, tick.progress.size - 1, units)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildLiveUpdate(nearest, content)
        } else {
            buildCompat(nearest, content)
        }
    }

    private data class Content(val title: String, val text: String, val subText: String?, val chip: String?)

    private fun content(p: TriggerProgress, others: Int, units: ResolvedUnitSystem): Content {
        val details = p.details
        val title = context.getString(
            if (details.trigger.direction == TriggerDirection.ARRIVE) R.string.monitor_title_arrive else R.string.monitor_title_leave,
            details.place.name,
        )
        val remaining = p.remaining
        val distance = p.distance
        val text = if (remaining != null && distance != null) {
            context.getString(R.string.monitor_to_go, formatter.format(remaining, units), formatter.format(distance, units))
        } else {
            context.getString(R.string.monitor_waiting_for_gps)
        }
        return Content(
            title = title,
            text = text,
            subText = if (others > 0) context.getString(R.string.monitor_and_more, others) else null,
            chip = remaining?.let { formatter.format(it, units) },
        )
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildLiveUpdate(p: TriggerProgress, c: Content): Notification {
        val style = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(listOf(Notification.ProgressStyle.Segment(PROGRESS_MAX).setColor(BRAND_COLOR)))
            .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_stat_monitoring))
        val progress = p.progress
        if (progress == null) style.setProgressIndeterminate(true) else style.setProgress((progress * PROGRESS_MAX).toInt())

        val builder = Notification.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_stat_monitoring)
            .setContentTitle(c.title)
            .setContentText(c.text)
            .setSubText(c.subText)
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(BRAND_COLOR)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(MonitoringIntents.openApp(context))
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        // Live Update (promoted placement + status-bar chip). The builder methods
        // only exist from Android 16 QPR2 (API 36.1); calling them on 36.0 would
        // crash with NoSuchMethodError, so 36.0 gets just the (ignored) extra.
        if (supportsLiveUpdateApis()) {
            builder.setRequestPromotedOngoing(true)
            c.chip?.let(builder::setShortCriticalText)
        } else {
            builder.addExtras(Bundle().apply { putBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        }
        actions(p).forEach { (title, intent) ->
            builder.addAction(
                Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stat_monitoring), title, intent).build(),
            )
        }
        return builder.build()
    }

    private fun buildCompat(p: TriggerProgress, c: Content): Notification {
        val progress = p.progress
        val builder = NotificationCompat.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_stat_monitoring)
            .setContentTitle(c.title)
            .setContentText(c.text)
            .setSubText(c.subText)
            .setProgress(PROGRESS_MAX, ((progress ?: 0f) * PROGRESS_MAX).toInt(), progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(BRAND_COLOR)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(MonitoringIntents.openApp(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        actions(p).forEach { (title, intent) -> builder.addAction(0, title, intent) }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun supportsLiveUpdateApis(): Boolean =
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1

    /** Send now / Pause act on the nearest trigger; Stop all pauses everything. */
    private fun actions(p: TriggerProgress) = listOf(
        context.getString(R.string.monitor_action_send_now) to
            MonitoringIntents.action(context, MonitoringIntents.ACTION_SEND_NOW, p.details.trigger.id),
        context.getString(R.string.monitor_action_pause) to
            MonitoringIntents.action(context, MonitoringIntents.ACTION_PAUSE, p.details.trigger.id),
        context.getString(R.string.monitor_action_stop_all) to
            MonitoringIntents.action(context, MonitoringIntents.ACTION_STOP_ALL),
    )

    internal companion object {
        const val PROGRESS_MAX = 1_000

        /** Brand teal (#006A6A); notifications can't read the Compose theme. */
        const val BRAND_COLOR = 0xFF006A6A.toInt()
    }
}
