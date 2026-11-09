package com.stevenpg.roundthecorner2.feature.triggers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TriggersUiState {
    data object Loading : TriggersUiState
    data class Ready(val triggers: List<TriggerDetails>, val unitSystem: ResolvedUnitSystem) : TriggersUiState
}

@HiltViewModel
class TriggersViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository,
    unitSystemProvider: UnitSystemProvider,
) : ViewModel() {

    val uiState: StateFlow<TriggersUiState> =
        combine(triggerRepository.observeTriggers(), unitSystemProvider.unitSystem) { triggers, units ->
            TriggersUiState.Ready(triggers, units)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TriggersUiState.Loading)

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

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
