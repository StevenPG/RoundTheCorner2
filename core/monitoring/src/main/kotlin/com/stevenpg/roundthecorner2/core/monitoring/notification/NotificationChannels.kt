package com.stevenpg.roundthecorner2.core.monitoring.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.stevenpg.roundthecorner2.core.monitoring.R

/** Channel ids and one-time registration (idempotent; cheap to call on every start). */
object NotificationChannels {
    /** Ongoing distance/progress: LOW importance so updates never buzz. */
    const val STATUS = "monitoring_status"

    /** Sent / failed / needs-a-decision: DEFAULT importance so they're noticed. */
    const val EVENTS = "trigger_events"

    const val STATUS_NOTIFICATION_ID = 1_001

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(STATUS, context.getString(R.string.monitor_channel_status), NotificationManager.IMPORTANCE_LOW)
                    .apply {
                        description = context.getString(R.string.monitor_channel_status_description)
                        setShowBadge(false)
                    },
                NotificationChannel(EVENTS, context.getString(R.string.monitor_channel_events), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.monitor_channel_events_description) },
            ),
        )
    }
}
