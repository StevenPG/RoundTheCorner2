package com.stevenpg.roundthecorner2.feature.places.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.PlaceRepository
import com.stevenpg.roundthecorner2.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlacesUiState {
    data object Loading : PlacesUiState
    data class Ready(val places: List<Place>) : PlacesUiState
}

/** One-shot messages shown as snackbars. */
sealed interface PlacesMessage {
    data class Deleted(val name: String) : PlacesMessage
    data class InUse(val name: String, val triggerCount: Int) : PlacesMessage
}

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    val uiState: StateFlow<PlacesUiState> = placeRepository.observePlaces()
        .map<List<Place>, PlacesUiState>(PlacesUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState.Loading)

    // A Channel (not StateFlow) so each message is shown exactly once.
    private val _messages = Channel<PlacesMessage>(Channel.BUFFERED)
    val messages: Flow<PlacesMessage> = _messages.receiveAsFlow()

    fun delete(place: Place) {
        viewModelScope.launch {
            when (val result = placeRepository.deletePlace(place.id)) {
                DeleteResult.Deleted -> _messages.send(PlacesMessage.Deleted(place.name))
                is DeleteResult.InUse -> _messages.send(PlacesMessage.InUse(place.name, result.triggerCount))
                DeleteResult.NotFound -> Unit // Already gone; the list will update itself.
            }
        }
    }
}
