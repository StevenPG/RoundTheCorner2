package com.stevenpg.roundthecorner2.feature.triggers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.domain.monitoring.TriggerProgress
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringSnapshot
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringStarter
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringStatus
import com.stevenpg.roundthecorner2.core.monitoring.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TriggersUiState {
    data object Loading : TriggersUiState
    data class Ready(
        val triggers: List<TriggerDetails>,
        val unitSystem: ResolvedUnitSystem,
        /** Live distance per armed trigger, while the service is running. */
        val live: Map<TriggerId, TriggerProgress> = emptyMap(),
        val waitingForAccuracy: Boolean = false,
        /** Shown as a banner when something armed can't be watched reliably. */
        val requirement: MonitoringRequirement? = null,
    ) : TriggersUiState
}

@HiltViewModel
class TriggersViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository,
    unitSystemProvider: UnitSystemProvider,
    monitoringStatus: MonitoringStatus,
    private val permissionChecker: PermissionChecker,
    private val monitoringStarter: MonitoringStarter,
) : ViewModel() {

    /** Permissions aren't observable: re-read on every resume (the user may have changed Settings). */
    private val permissions = MutableStateFlow(permissionChecker.snapshot())
    private val dismissed = MutableStateFlow<Set<MonitoringRequirement>>(emptySet())

    val uiState: StateFlow<TriggersUiState> = combine(
        triggerRepository.observeTriggers(),
        unitSystemProvider.unitSystem,
        monitoringStatus.snapshot,
        permissions,
        dismissed,
    ) { triggers, units, snapshot: MonitoringSnapshot, perms, dismissedSet ->
        val anyArmed = triggers.any { it.trigger.status == TriggerStatus.ARMED }
        TriggersUiState.Ready(
            triggers = triggers,
            unitSystem = units,
            live = snapshot.progress,
            waitingForAccuracy = snapshot.waitingForAccuracy,
            requirement = if (anyArmed) perms.topRequirement(dismissedSet) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TriggersUiState.Loading)

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    /** Screen resumed or a permission dialog returned: re-check and (re)start monitoring if possible. */
    fun onResume() {
        permissions.value = permissionChecker.snapshot()
        monitoringStarter.refresh()
    }

    fun dismiss(requirement: MonitoringRequirement) {
        if (requirement.optional) dismissed.update { it + requirement }
    }

    /**
     * The switch: on = ARMED (also re-arms a FIRED trigger), off = PAUSED.
     * Re-arming keeps lastFiredAt for history.
     */
    fun setWatching(id: TriggerId, watching: Boolean) {
        viewModelScope.launch { if (watching) triggerRepository.arm(id) else triggerRepository.pause(id) }
    }

    fun delete(id: TriggerId) {
        viewModelScope.launch { if (triggerRepository.deleteTrigger(id)) _deleted.send(Unit) }
    }
}
