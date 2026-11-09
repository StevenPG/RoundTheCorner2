package com.stevenpg.roundthecorner2.core.monitoring.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stevenpg.roundthecorner2.core.model.TriggerId

/**
 * Intents/PendingIntents that drive [MonitoringService]. Notification actions
 * target the service directly (getForegroundService): Android allows starting
 * a foreground service from a notification tap even when the app is in the
 * background.
 */
object MonitoringIntents {
    internal const val ACTION_START = "com.stevenpg.roundthecorner2.monitoring.START"
    internal const val ACTION_SEND_NOW = "com.stevenpg.roundthecorner2.monitoring.SEND_NOW"
    internal const val ACTION_PAUSE = "com.stevenpg.roundthecorner2.monitoring.PAUSE"
    internal const val ACTION_STOP_ALL = "com.stevenpg.roundthecorner2.monitoring.STOP_ALL"
    internal const val ACTION_KEEP_WAITING = "com.stevenpg.roundthecorner2.monitoring.KEEP_WAITING"
    internal const val EXTRA_TRIGGER_ID = "trigger_id"

    fun start(context: Context): Intent = Intent(context, MonitoringService::class.java).setAction(ACTION_START)

    internal fun action(context: Context, action: String, triggerId: TriggerId? = null): PendingIntent {
        val intent = Intent(context, MonitoringService::class.java).setAction(action)
        triggerId?.let { intent.putExtra(EXTRA_TRIGGER_ID, it.value) }
        // Request code must differ per (action, trigger) or extras would be overwritten.
        val requestCode = 31 * action.hashCode() + (triggerId?.value?.toInt() ?: 0)
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Opens the app (its launcher activity) when a notification body is tapped. */
    internal fun openApp(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
