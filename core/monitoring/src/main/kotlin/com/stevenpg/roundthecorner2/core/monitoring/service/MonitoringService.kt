package com.stevenpg.roundthecorner2.core.monitoring.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.preferences.UserPreferencesRepository
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.domain.monitoring.MonitoringEngine
import com.stevenpg.roundthecorner2.core.domain.polling.LocationAccuracy
import com.stevenpg.roundthecorner2.core.domain.polling.PollingPolicy
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import com.stevenpg.roundthecorner2.core.location.LocationClient
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringPermissions
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringSnapshot
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringStatus
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerEventNotifier
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerFirer
import com.stevenpg.roundthecorner2.core.monitoring.notification.NotificationChannels
import com.stevenpg.roundthecorner2.core.monitoring.notification.StatusNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service (type `location`) that watches every ARMED trigger.
 *
 * Pipeline: armed triggers (Room flow) + adaptive-interval location updates
 * -> [MonitoringEngine] -> fire / ask / update the live notification.
 *
 * Everything runs on the main thread's coroutine dispatcher, so the engine is
 * only ever touched from one thread; sends run as separate coroutines so a
 * slow network never delays the next location fix.
 *
 * It stops itself as soon as nothing is armed, and is `START_STICKY` so the OS
 * restarts it if it's killed for memory.
 */
@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var triggerRepository: TriggerRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var unitSystemProvider: UnitSystemProvider
    @Inject lateinit var locationClient: LocationClient
    @Inject lateinit var firer: TriggerFirer
    @Inject lateinit var eventNotifier: TriggerEventNotifier
    @Inject lateinit var statusFactory: StatusNotificationFactory
    @Inject lateinit var status: MonitoringStatus
    @Inject lateinit var permissions: MonitoringPermissions

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = MonitoringEngine()
    private val armed = MutableStateFlow<List<TriggerDetails>?>(null)
    /** What to ask the OS for next. A StateFlow drops equal values, so it only re-registers on a real change. */
    private val request = MutableStateFlow(LocationRequestSpec(MIN_INTERVAL, LocationAccuracy.PRECISE))
    private var units: StateFlow<ResolvedUnitSystem>? = null
    private var monitoringJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationChannels.ensureCreated(this)
        // Must happen within seconds of startForegroundService(), before any async work.
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        handleAction(intent)
        ensureMonitoring()
        return START_STICKY
    }

    /** False if Android refused (no location permission, or a disallowed background start). */
    private fun enterForeground(): Boolean {
        if (!permissions.hasPreciseLocation()) return false
        return try {
            ServiceCompat.startForeground(
                this,
                NotificationChannels.STATUS_NOTIFICATION_ID,
                statusFactory.build(engine.lastTick, units?.value ?: ResolvedUnitSystem.METRIC),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun handleAction(intent: Intent?) {
        val triggerId = intent?.getLongExtra(MonitoringIntents.EXTRA_TRIGGER_ID, -1L)
            ?.takeIf { it >= 0 }?.let(::TriggerId)
        when (intent?.action) {
            MonitoringIntents.ACTION_SEND_NOW -> triggerId?.let(::sendNow)
            MonitoringIntents.ACTION_PAUSE -> triggerId?.let { scope.launch { triggerRepository.pause(it) } }
            MonitoringIntents.ACTION_STOP_ALL -> scope.launch {
                triggerRepository.observeArmedTriggers().first().forEach { triggerRepository.pause(it.trigger.id) }
            }
            MonitoringIntents.ACTION_KEEP_WAITING -> triggerId?.let { id ->
                scope.launch { triggerRepository.observeTrigger(id).first()?.let(eventNotifier::cancelAlreadyHere) }
            }
        }
    }

    /** "Send now": fire immediately using the latest known distance. */
    private fun sendNow(id: TriggerId) {
        scope.launch {
            val details = triggerRepository.observeTrigger(id).first() ?: return@launch
            val distance = engine.lastTick?.progress?.find { it.details.trigger.id == id }?.distance
            firer.fire(details, distance)
        }
    }

    private fun ensureMonitoring() {
        if (monitoringJob != null) return
        units = unitSystemProvider.unitSystem.stateIn(scope, SharingStarted.Eagerly, ResolvedUnitSystem.METRIC)
        monitoringJob = scope.launch {
            // 1. What to watch. Nothing armed -> shut down.
            launch {
                triggerRepository.observeArmedTriggers().collect { list ->
                    armed.value = list
                    if (list.isEmpty()) shutDown() else refreshNotification()
                }
            }
            // 2. The user's battery trade-off caps the slowest interval.
            launch {
                preferences.preferences.map { it.checkInterval }.distinctUntilChanged().collect { cap ->
                    engine.pollingPolicy = PollingPolicy(maxInterval = cap.seconds.seconds)
                }
            }
            // 3. Location at an adaptive rate and accuracy; re-subscribes only when either changes.
            request
                .flatMapLatest { locationClient.updates(it.interval, it.accuracy) }
                .retryWhen { cause, attempt ->
                    // Transient provider errors: back off and try again. Permission loss: give up.
                    (cause !is SecurityException && attempt < MAX_LOCATION_RETRIES).also { if (it) delay(30.seconds) }
                }
                .catch { onLocationUnavailable() }
                .collect(::onFix)
        }
    }

    private fun onFix(fix: LocationFix) {
        val watching = armed.value ?: return
        if (watching.isEmpty()) return
        val tick = engine.onFix(watching, fix)

        status.update {
            MonitoringSnapshot(
                running = true,
                waitingForAccuracy = tick.waitingForAccuracy,
                progress = tick.progress.associateBy { it.details.trigger.id },
            )
        }
        postStatus()
        tick.toFire.forEach { request -> scope.launch { firer.fire(request.details, request.distance) } }
        tick.alreadySatisfied.forEach(eventNotifier::alreadyHere)
        request.value = LocationRequestSpec(IntervalBuckets.quantize(tick.nextInterval), tick.nextAccuracy)
    }

    /** Armed set changed between fixes (e.g. paused from the app): drop stale rows from the notification. */
    private fun refreshNotification() {
        val ids = armed.value.orEmpty().map { it.trigger.id }.toSet()
        status.update { it.copy(running = true, progress = it.progress.filterKeys(ids::contains)) }
        postStatus()
    }

    @SuppressLint("MissingPermission") // The FGS notification is exempt from POST_NOTIFICATIONS for display in the task manager.
    private fun postStatus() {
        val ids = armed.value.orEmpty().map { it.trigger.id }.toSet()
        val tick = engine.lastTick?.let { t -> t.copy(progress = t.progress.filter { it.details.trigger.id in ids }) }
        val notification = statusFactory.build(tick, units?.value ?: ResolvedUnitSystem.METRIC)
        NotificationManagerCompat.from(this).notify(NotificationChannels.STATUS_NOTIFICATION_ID, notification)
    }

    private fun onLocationUnavailable() {
        shutDown()
    }

    private fun shutDown() {
        engine.reset()
        status.update { MonitoringSnapshot() }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        status.update { MonitoringSnapshot() }
        super.onDestroy()
    }

    private data class LocationRequestSpec(val interval: Duration, val accuracy: LocationAccuracy)

    private companion object {
        val MIN_INTERVAL = 10.seconds
        const val MAX_LOCATION_RETRIES = 5L
    }
}
