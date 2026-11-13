package com.stevenpg.roundthecorner2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.preferences.UserPreferencesRepository
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Supplies the app-wide theme settings. Until the first preferences read
 * completes the state is [MainActivityUiState.Loading], which keeps the splash
 * screen up so the user never sees a flash of the wrong theme.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = preferencesRepository.preferences
        .map<UserPreferences, MainActivityUiState>(MainActivityUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityUiState.Loading)
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Ready(val preferences: UserPreferences) : MainActivityUiState
}
