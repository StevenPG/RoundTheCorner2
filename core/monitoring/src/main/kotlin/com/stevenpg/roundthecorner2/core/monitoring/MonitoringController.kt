package com.stevenpg.roundthecorner2.core.monitoring

import android.content.Context
import androidx.core.content.ContextCompat
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.monitoring.service.MonitoringIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the monitoring service whenever there is something to watch. The
 * service stops itself when nothing is armed, so this only ever starts it.
 */
/** Lets screens nudge monitoring to start (e.g. right after a permission is granted). */
interface MonitoringStarter {
    fun refresh()
}

@Singleton
class MonitoringController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val triggerRepository: TriggerRepository,
    private val permissions: MonitoringPermissions,
) : MonitoringStarter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Called once from Application.onCreate: start the service whenever triggers become armed. */
    fun observe() {
        scope.launch {
            triggerRepository.observeArmedTriggers()
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { anyArmed -> if (anyArmed) tryStart() }
        }
    }

    /** Call when circumstances change (e.g. a permission was just granted). */
    override fun refresh() {
        scope.launch { if (triggerRepository.observeArmedTriggers().first().isNotEmpty()) tryStart() }
    }

    /**
     * @return false if it couldn't start: no location permission, or Android
     *   refused a background start (the next foreground refresh retries).
     */
    fun tryStart(): Boolean {
        if (!permissions.hasPreciseLocation()) return false
        return try {
            ContextCompat.startForegroundService(context, MonitoringIntents.start(context))
            true
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (Android 12+) extends this.
            false
        }
    }
}
