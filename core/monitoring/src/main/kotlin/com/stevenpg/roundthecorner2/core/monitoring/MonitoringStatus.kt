package com.stevenpg.roundthecorner2.core.monitoring

import com.stevenpg.roundthecorner2.core.domain.monitoring.TriggerProgress
import com.stevenpg.roundthecorner2.core.model.TriggerId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** What the service is doing right now, for the UI (live distances in the trigger list). */
data class MonitoringSnapshot(
    val running: Boolean = false,
    val waitingForAccuracy: Boolean = false,
    val progress: Map<TriggerId, TriggerProgress> = emptyMap(),
)

/**
 * In-process bridge from the service to screens. The service writes; ViewModels
 * read. (Same process, so a singleton StateFlow is all that's needed.)
 */
@Singleton
class MonitoringStatus @Inject constructor() {
    private val _snapshot = MutableStateFlow(MonitoringSnapshot())
    val snapshot: StateFlow<MonitoringSnapshot> = _snapshot.asStateFlow()

    internal fun update(transform: (MonitoringSnapshot) -> MonitoringSnapshot) = _snapshot.update(transform)
}
